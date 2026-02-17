# Dryad's Fetching Python Script

## Set Up 

1. Sign up for a Dryad API account by following this [Dryad API Accounts Instruction](https://github.com/datadryad/dryad-app/blob/main/documentation/apis/api_accounts.md)

2. Install Python3 and import the necessary Python modules:

`pip install requests dotenv`

3. Create a `.env` file in the same directory with the Python script following this:

```
CLIENT_ID={YOUR_DRYAD_CLIENT_ID}
CLIENT_SECRET={YOUR_DRYAD_CLIENT_SECRET}
PARENT_DIRECTORY={YOUR_LOCAL_DIRECTORY_TO_STORE_DOWLOAD_FILE_INCLUDING_SLASH_AT_THE_END} (Absolute Path Recommended)
```

## Fetch Dryad dataset

1. Open terminal and change directory to the folder that contains the script

```
cd dev-linux/dryad-script/
```

2. To get a or multiple dataset(s) with specific `doi:10561/dryad.XXXXXX`, run the script in the terminal:

`python3 Dryad.py XXXXX1 XXXXXX2`

3. The script will fetch the data and create a `dataset.zip` file at the local directory that you want to store the downloaded file.