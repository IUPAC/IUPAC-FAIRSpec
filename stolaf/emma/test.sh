# !/bin/bash

cd ../..
cd c:/temp/dryad
ls
pwd

cd 2bvq83c2q
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
    	unzip "$file"
      echo "$file"
    	filename=$(basename "$file" .zip)
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
      fi
    done
}

unzip_files

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


ls
pwd

if [ -z "$1" ]; then
    echo "Usage: $0 <directory_path>"
    exit 1
fi

echo "File List" > file_list.txt



OUTPUT_FILE="file_list_2bvq83c2q.txt"
echo $OUTPUT_FILE

find "$1" -print > "$OUTPUT_FILE"

mv "$OUTPUT_FILE" ../../../../../stolaf/emma
pwd

echo "All file paths starting from '$1' have been saved to '$OUTPUT_FILE'."




