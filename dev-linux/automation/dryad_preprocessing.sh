# !/bin/bash

# When the script prompts to input your password for sudo right, please input your machine password!
# Recursive unzipping function
unzip_files() {
    for file in *.zip; do
      filename="${file}.."
    	unzip "$file" -d "$filename"
      echo "$file"
    	echo "FILENAME: $filename"
    	if [ -d "$filename" ]; then
        echo "'$filename' is a directory."
        pwd
        cd "$filename"
        pwd
        unzip_files
    
      else
        echo "'$filename' is not a directory (or does not exist)."
        cd .. # might have to reevaluate this line
      fi
    done
}

unzip_recursive() {
  find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
  # Continuously search for zip files until none are left
  while [ "$(find . -path "*/__MACOSX" -prune -o -type f -name '*.zip' -print | wc -l)" -gt 0 ]; do
      # Find each zip, extract to {name}.zip.., then delete the archive
      find . -path "*/__MACOSX" -prune -o -type f -name "*.zip" -exec sh -c '
          zip_file="$0"
          dest_dir="${zip_file}.."
          if unzip -o -d "$dest_dir" "$zip_file" -x "__MACOSX/*" "*/__MACOSX/*"; then
              rm "$zip_file"
              echo "Extracted and removed: $zip_file"
          else
              echo "Error unzipping: $zip_file"
              exit 1
          fi
      ' "{}" \;
  done
  find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
}

# Absolute path to the folder that stores this script for future reference
script_root_path="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd $script_root_path
# Ask the user to input the DOI for the Dryad dataset they want to work on
read -p "Please enter the DOI of the Dryad set you want to work on: " doi

# Handle the case when the input is not provided
if [[ -z "$doi" ]]; then
  echo "Error: DOI of Dryad dataset is not set. Script terminated."
  exit 1 
fi

# Retrieve the parent directory of the dryad dataset and related info from the .env file form dryad_script for easier download
source "../dryad_script/.env"
parent_path=$PARENT_DIRECTORY

# Directory to store the downloaded files from Dryad
download_path="${parent_path}${doi}"

# If the data already exists, don't need to download to limit down the API requests
if [ ! -d "$download_path" ]; then
  # Download the dryad data
  python3 "../dryad_script/Dryad.py" $doi
else
  echo "$download_path exists. Don't need to download." 
fi

# Change directory to the parent directory of the Dryad datasets 
cd $parent_path
ls

# Clear the existing unzipped folder if avail
if [ -d "${doi}_unzip" ]; then
  sudo rm -rf "${doi}_unzip" 
fi


# Create a new folder to store unzipped data if it has not existed beforehand
mdkir "${doi}_unzip"

# Unzip dataset.zip from the downloaded folder and move it to the unzip folder
cd "$doi" 
unzip "dataset.zip" -d "../${doi}_unzip"
cd ..
cd "${doi}_unzip"
# Store the absolute path to the unzip folder for future reference
unzip_path="$PWD"
# Call the recursive function to unzip all .zip files in the dataset
unzip_recursive

ls
pwd

# Make sure that after unzipping process, we will go back to the directory of the unzip folder
if [[ "$PWD/" != "$unzip_path" ]]; then
  cd $unzip_path
fi

rm -r __MACOSX
rm -r */__MACOSX

# User must run the script: ./dryad_preprocessing.sh . for the find syntax; if there is no argument, terminate the script immediately at this point
#if [ -z "$1" ]; then
  #echo "You need to call $0 <directory_path> with an argument for find syntax"
  #exit 1
#fi

# Find and list all the unique directories and files in the dataset in the text file
OUTPUT_FILE="file_list_${doi}.txt"
echo $OUTPUT_FILE

find "." -print > "$OUTPUT_FILE"

echo "All file paths starting from '$1' have been saved to '$OUTPUT_FILE'."

# Return to the parent_directory
cd 
cd $parent_path

pwd

# Clear the folder for prediction if it exists
if [ -d "${doi}_prediction" ]; then
  sudo rm -rf ${doi}_prediction
fi

# Create a new folder to store the output from running compound_candidate_identifier.py 
mkdir "${doi}_prediction"

# Move the file list to the prediction folder
cd "${doi}_unzip"

mv "$OUTPUT_FILE" "../${doi}_prediction"

cd ../"${doi}_prediction"

# Run the compound_candidate_identifier script to generate output files
python3 "${script_root_path}/../../stolaf/emma/compound_candidate_identifier.py" $doi

cd ..

# Remove the unzip folder 
sudo rm -rf ${doi}_unzip
