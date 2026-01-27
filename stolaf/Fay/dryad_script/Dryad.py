import os
import sys
import json
import shutil
import zipfile
import logging
import requests
import urllib.parse
from pathlib import Path
from dotenv import load_dotenv
from datetime import datetime, timedelta

PARENT_DIRECTORY = None
CLIENT_ID = None
CLIENT_SECRET = None

def initiate_logging():
    logging.basicConfig(
        filename=Path(__file__).parent/'events.log',
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        filemode='a'
    )

def get_dryad_token() -> str:
    """
    Sends a POST request with Dryad Client ID and Dryad Client Secret to get the token

    Args:
        CLIENT_ID: Dryad Client ID
        CLIENT_SECRET: Dryad Client Secret

    Returns:
        A Bearer token that is expired in 10 hours
    """
    if not CLIENT_ID or CLIENT_ID == "" or not CLIENT_SECRET or CLIENT_SECRET == "":
        print("Error: CLIENT_ID and CLIENT_SECRET not found in .env file")
        logging.error("CLIENT_ID and CLIENT_SECRET not found in .env file")
        return
    # If the token is cached
    cached_token = load_cached_token()
    if cached_token:
        return cached_token
    AUTH_URL = f"https://datadryad.org/oauth/token?client_id={CLIENT_ID}&client_secret={CLIENT_SECRET}&grant_type=client_credentials"
    headers = {
        "content-type": "application/x-www-form-urlencoded",
        "charset": "UTF-8"
    }
    response = requests.request("POST", AUTH_URL, data="", headers=headers)
    if response.status_code == 200:
        data = response.json()
        token = data["access_token"]
        cache_token(token)
        logging.info("Successfully get token")
        return token
    else:
        print(f"Request failed with status code: {response.status_code}")
        print(f"Response text: {response.text}")
        logging.error(f"Request failed with status code: {response.status_code}")
        logging.error(f"Response text: {response.text}")
        return None

def zip_folder(source_path: str, destination_path: str, remove_source: bool = True, zip_name: str = "dataset.zip"):
    """
    Zips the contents of a folder into a specified zip file.

    Args:
        source_path: The path to the source folder to be zipped
        destination_path: The path to the folder store the zip die
        remove_source: Remove source files after zipping (Optional)
        zip_name: The name of the output zip file (Optional)
    """
    zip_file_path = os.path.join(destination_path, zip_name)
    with zipfile.ZipFile(zip_file_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(source_path):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, source_path)
                zipf.write(file_path, arcname)
        logging.info(f"Zip up the folder and create {zip_name}")
    if(remove_source):
        shutil.rmtree(source_path)
        logging.info(f"Zip up the folder and create {zip_name}")

def cache_token(token: str, expired_hours: int = 10):
    """
    Saves the Bearer token to the cache

    Args:
        token: The Bearer Token
        expired_hours: Hours until token expires 
    """
    cache_file_path = Path(__file__).parent/".token_cache.json"
    expiry_time = (datetime.now() + timedelta(minutes=expired_hours * 60 - 1)).isoformat()
    cache_data = {
        "token": token,
        "expiry": expiry_time
    }
    with open(cache_file_path, 'w') as f:
        json.dump(cache_data, f)

def load_cached_token() -> str | None:
    """
    Loads the token from cache if it's still valid.
    
    Returns:
        Cached token if valid, None otherwise
    """
    cache_file = Path(__file__).parent/".token_cache.json"
    if not os.path.exists(cache_file):
        return None
    
    try:
        with open(cache_file, 'r') as f:
            cache_data = json.load(f)
        
        expiry_time = datetime.fromisoformat(cache_data["expiry"])
        # If the cache has not been expired yet
        if datetime.now() < expiry_time:
            print("CACHE HIT")
            logging.info("CACHE HIT")
            return cache_data["token"]
        else:
            print("CACHE MISS")
            logging.info("CACHE MISS")
            os.remove(cache_file)
            return None
    except (json.JSONDecodeError, KeyError, ValueError):
        return None
    
def encode_dryad_doi_url(doi_identifier: str, postfix: str = "") -> str:
    """
    Encodes the specific Dryad identifier part of a DOI to create an encoded URL.

    Args:
        doi_identifier: The raw DOI string
        postfix: Handle various API endpoints
    Returns:
        A fully URL-encoded string that resolves to the resource.
    """
    BASE_URL = "https://datadryad.org/api/v2/datasets/" 
    encoded_identifier = urllib.parse.quote_plus("doi:10.5061/dryad." + doi_identifier)
    FULL_URL = BASE_URL + encoded_identifier + postfix
    return FULL_URL

def create_new_dir(doi_identifier: str, postfix: str = None) -> str | None:
    """
    Creates a new directory to store the Dryad .zip dataset

    Args:
        doi_identifier: the DOI identifier of the Dryad Dataset (doi:10561/dryad.<doi_identifier>)

    Returns:
        The full path to the created directory, or None if creation fails        
    """
    if not PARENT_DIRECTORY or PARENT_DIRECTORY == "":
        print("Error: PARENT_DIRECTORY not found")
        logging.error("PARENT_DIRECTORY not found")
        return None
    directory_path = f"{PARENT_DIRECTORY}{doi_identifier}"
    if (postfix):
        directory_path += f"_{postfix}"
    try:
        os.makedirs(directory_path, exist_ok=True)
        print(f"Directory created successfully at: {directory_path}")
        logging.info(f"Directory created successfully at: {directory_path}")
        return directory_path
    except OSError as e:
        print(f"Error creating directory: {e}")
        logging.error(f"Error creating directory: {e}")
        return None

def get_dryad_dataset_version(doi_identifier: str, token: str) -> str | None:
    """
    Helper function: Gets the newest version of the dataset

    Args:
        doi_identifier: the DOI identifier of the Dryad Dataset (doi:10561/dryad.<doi_identifier>)
        token: The Bearer token for authentication 

    Returns:
        The newest version of the dataset      
    """
    API_URL = encode_dryad_doi_url(doi_identifier, "/versions")
    headers = {
        "Authorization": f"Bearer {token}"
    }
    response = None
    try:
        response = requests.get(API_URL, headers=headers)
        if response.status_code == 200:
            data = response.json()
            versions_count = int(data["count"])
            latest_versions_url = data["_embedded"]["stash:versions"][versions_count - 1]["_links"]["self"]["href"]
            if not latest_versions_url:
                return None
            return latest_versions_url
    except requests.exceptions.RequestException as e:
        print(f"An error occurred: {e}")
        if response is not None:
            print(f"Status code: {response.status_code}")
            print(f"Response content: {response.text}")
            logging.error(f"Status code: {response.status_code}")
            logging.error(f"Response content: {response.text}")

def get_dryad_dataset(doi_identifier: str, token: str):
    """
    Downloads Dryad dataset

    Args:
        doi_identifier: the DOI identifier of the Dryad Dataset (doi:10561/dryad.<doi_identifier>)
        token: The Bearer token for authentication    
    """
    version = get_dryad_dataset_version(doi_identifier, token)
    API_URL = f"https://datadryad.org{version}/files"
    headers = {
        "Authorization": f"Bearer {token}"
    }
    response = None
    try:
        response = requests.get(API_URL, headers=headers)
        if response.status_code == 200:
            data = response.json()
            file_list = data["_embedded"]["stash:files"]
            dataset_directory = create_new_dir(doi_identifier, "data")
            if not dataset_directory:
                print(f"Directory does not exist")
                logging.error(f"Directory does not exist")
                return
            for file in file_list:
                get_dryad_dataset_file(file["_links"]["self"]["href"], os.path.join(dataset_directory, file["path"]), token)
            final_directory = create_new_dir(doi_identifier)
            zip_folder(dataset_directory, final_directory) 
    except requests.exceptions.RequestException as e:
        logging.error(f"An error occurred: {e}")
        print(f"An error occurred: {e}")
        if response is not None:
            print(f"Status code: {response.status_code}")
            print(f"Response content: {response.text}")
            logging.error(f"Status code: {response.status_code}")
            logging.error(f"Response content: {response.text}")

def get_dryad_dataset_file(file_url: str, local_file_path: str, token: str):
    """
    Downloads individual file from a dataset using the specifc file URL

    Args:
        file_url: the specific URL to download a file
        local_file_path: the directory to store the file on local machine
        token: the Bearer token for authentication
    """
    API_URL = f"https://datadryad.org{file_url}/download"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "*/*"
    }
    response = None
    try:
        response = requests.get(API_URL, headers=headers, stream=True)
        response.raise_for_status()
        
        total_size = int(response.headers.get('content-length', 0))
        downloaded = 0
        
        with open(local_file_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percentage = (downloaded / total_size) * 100
                        print(f"  Progress: {percentage:.1f}%", end='\r')
        print(f"Downloaded: {os.path.basename(local_file_path)}")
    except requests.exceptions.RequestException as e:
        print(f"Error downloading file: {e}")
        logging.error(f"Error downloading file: {e}")

def main(args: list[str] = None):
    global PARENT_DIRECTORY
    global CLIENT_ID
    global CLIENT_SECRET
    load_dotenv(Path(__file__).parent/ '.env')
    initiate_logging()
    logging.info("\r=========================================================================" + str(datetime.now()))
    CLIENT_ID = os.getenv("CLIENT_ID", "")
    CLIENT_SECRET = os.getenv("CLIENT_SECRET", "")
    PARENT_DIRECTORY = os.getenv("PARENT_DIRECTORY", "")
    token = get_dryad_token()
    if not token:
        logging.error("Failed to obtain authentication token")
        return 
    dois = args if args else sys.argv[1:]  
    if not dois:
        print("You need to include at least one DOI of Dryad")
        logging.error("No DOI provided")
        return   
    for doi in dois:
        print(f"\nDownloading dataset for DOI: {doi}")
        logging.info(f"Downloading dataset for DOI: {doi}")
        get_dryad_dataset(doi, token)

if __name__ == "__main__":
    main()

