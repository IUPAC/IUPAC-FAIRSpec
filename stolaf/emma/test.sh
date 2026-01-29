# !/bin/bash

cd ../..
cd c:/temp/dryad
ls
pwd

cd 0c01297
ls
pwd



ls
pwd

# DEST_DIR="/Users/emmaclift/git/IUPAC-FAIRSpec/c:/dryad/test"
mkdir test2

unzip dataset.zip -d test2

cd test2

# I THINK THIS RECURSIVE LOOP IS FIXED

unzip_files() {
    for file in *.zip; do
      filename="${file}.."
    	unzip "$file" -d "$filename"
      echo "$file"
    	#filename=$(basename "$file" .zip)
    	pwd
    	echo "FILENAME: $filename"
      # might need to add some renaming concept to fix the randomly named zip files
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

# Run the function
unzip_recursive



#unzip_files

#for file in *.zip; do
#	unzip "$file"
#  echo "$file"
#	filename=$(basename "$file" .zip)
#	if [ -d "$filename" ]; then
#    echo "'$filename' is a directory."
#    pwd
#    cd "$filename"
#    pwd
#    for file_nextlayer in *.zip; do
#      unzip "$file_nextlayer"
#    done

#  else
#    echo "'$filename' is not a directory (or does not exist)."
#  fi
#done

# add functionality
# if there is a __MACXOS folder, just delete that folder. it's weird
pwd
ls
rm -r __MACOSX
rm -r */__MACOSX



ls
pwd

if [ -z "$1" ]; then
    echo "Usage: $0 <directory_path>"
    exit 1
fi

echo "File List" > file_list.txt



OUTPUT_FILE="file_list_0c01297.txt"
echo $OUTPUT_FILE

find "$1" -print > "$OUTPUT_FILE"

mv "$OUTPUT_FILE" ../../../../../stolaf/emma
pwd

echo "All file paths starting from '$1' have been saved to '$OUTPUT_FILE'."




