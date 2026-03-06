# !/bin/bash

# requires sudo apt update 
# requires sudo apt install unzip

Emma_Algorithm="/mnt/c/temp/iupac/compound_candidate_identifier.py"
pZip_Source_Directory="/mnt/c/temp/iupac/acs2/data"
Working_Directory="/mnt/c/temp/iupac/acs2/test2"

if [ ! -f "${Emma_Algorithm}" ]; then
    echo "Error: ${Emma_Algorithm} does not exist" >&2
    exit 1
fi

# check that these directories exist
check_dir_exists() {
    local dir_path="$1"
    if [ ! -d "$dir_path" ]; then
        echo "Error: Directory not found at $dir_path" >&2
        exit 1
    else
        echo "OK $dir_path"
    fi
}

check_dir_exists ${Zip_Source_Directory}
check_dir_exists ${Working_Directory}

function yes_or_no {
    while true; do
        read -p "$* [y/n]: " yn
        case $yn in
            [Yy]* ) return 0;; # Return success (0) for yes
            [Nn]* ) echo "Aborted"; return 1;; # Return failure (1) for no, and exit
            * ) echo "Please answer yes or no.";; # Loop for invalid input
        esac
    done
}

# Ask the user to input the DOI for the ACS dataset they want to work on
read -p "Please enter the DOI of the ACS set you want to work on (e.g. jo4c02094): " doi

Unzip_Dir="${Working_Directory}/${doi}_unzip"
Prediction_Directory="${Working_Directory}/${doi}_prediction"
Zip_Root="${Zip_Source_Directory}/${doi}"

# make sure this doi has zip files

    if [ ! -f "${Zip_Root}"*.zip ]; then
        echo "Error: no zip files found for $doi"
        exit 1
    else
        ls --format 'single-column' "${Zip_Root}"*.zip
        echo "OK"
    fi

# Clear the folder for prediction if it exists
if [ -d "${Prediction_Directory}" ]; then
  sudo rm -rf "${Prediction_Directory}"
fi


if [ -d "$Unzip_Dir" ]; then

  doUnzip=$(yes_or_no "Do you want to unzip files?")

fi


: ' 
UNUSED
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

'

unzip_recursive() {
  
  # TODO clear out __MACOSX files; could also be __MACOS I think
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


if [ ! $doUnzip ]; then
  # Clear existing zip files from this directory
  sudo rm -f "$Working_Directory"/*.zip 

  # Clear any existing unzipped folder if present
  sudo rm -rf "$Working_Directory"/*_unzip

  # Create a new folder to store unzipped data if it has not existed beforehand
  # e.g. jo4c02094_unzip
  echo unzip dir is ${doi}_unzip
  sudo mkdir ${doi}_unzip

  # Unzip {doi}*.zip from the downloaded folder and move it to the unzip folder
  echo working...
  unzip -q "${doi}*.zip" -d "${doi}_unzip"

  # copy zip files to the working directory

  cp "${Zip_Root}"*.zip ${Working_Directory}
  cd ${Working_Directory}
  ls

  # cd to the top of the __unzip directory
  cd ${Unzip_Dir}

  # Call the recursive function to unzip all .zip files in the dataset
  unzip_recursive

  # cd to the top of the __unzip directory
  cd ${Unzip_Dir}

  sudo rm -rf __MACOS*

fi

# Create a new folder to store the output from running compound_candidate_identifier.py 
mkdir "${Prediction_Directory}"

# Create the list of all the directories and files in the dataset in the prediction folder
OUTPUT_FILE="${Prediction_Directory}/file_list_${doi}.txt"
echo "creating ${OUTPUT_FILE}"
cd "${doi}_unzip"
find "." -print > "$OUTPUT_FILE"

cd "${Prediction_Directory}"

exit

# Run the compound_candidate_identifier script to generate output files
python3 "${Emma_Algorithm}" $doi

cd "${Working_Directory}"

echo "done"
