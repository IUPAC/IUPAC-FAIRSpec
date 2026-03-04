# !/bin/bash

ACS_Directory="c:/temp/iupac/acs2/test"
IUPAC_Directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/../../"

echo $IUPAC_Directory


# When the script prompts to input your password for sudo right, please input your machine password!
# Recursive unzipping function
unzip_files() {
    for file in *.zip; do
      filename="${file}__"
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
      # Find each zip, extract to {name}.zip__, then delete the archive
      find . -path "*/__MACOSX" -prune -o -type f -name "*.zip" -exec sh -c '
          zip_file="$0"
          dest_dir="${zip_file}__"
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

# Ask the user to input the DOI for the ACS dataset they want to work on
read -p "Please enter the DOI of the ACS set you want to work on: " doi


# Change directory to the parent directory of the ACS datasets 
cd $ACS_Directory
pwd

# Clear the existing unzipped folder if avail
if [ -d "${doi}_unzip" ]; then
  sudo rm -rf "${doi}_unzip" 
fi


# Create a new folder to store unzipped data if it has not existed beforehand
mkdir ${doi}_unzip
echo unzip dir is ${doi}_unzip
# Unzip dataset.zip from the downloaded folder and move it to the unzip folder
pwd
unzip "${doi}.zip" -d "${doi}_unzip"
cd "${doi}_unzip"
ls
pwd

# Store the absolute path to the unzip folder for future reference
unzip_path="$PWD"

echo unzip path is ---${unzip_path}---


# Call the recursive function to unzip all .zip files in the dataset
unzip_recursive


# Make sure that after unzipping process, we will go back to the directory of the unzip folder
if [[ "$PWD/" != "$unzip_path" ]]; then
  cd $unzip_path
fi

rm -r __MACOSX
rm -r */__MACOSX

# Find and list all the unique directories and files in the dataset in the text file
OUTPUT_FILE="file_list_${doi}.txt"

find "." -print > "$OUTPUT_FILE"

echo "All file paths starting from '$1' have been saved to '$OUTPUT_FILE'."

# Return to the ACS_Directory
cd $ACS_Directory

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

exit

cd ../"${doi}_prediction"

# Run the compound_candidate_identifier script to generate output files
python3 "${IUPAC_Directory}/stolaf/emma/compound_candidate_identifier.py" $doi

cd ..

