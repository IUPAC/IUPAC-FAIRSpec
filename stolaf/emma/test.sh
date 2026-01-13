# !/bin/bash

cd ../..
cd c:/temp/dryad
ls
pwd

cd 3tx95x6sq
ls
pwd



ls
pwd

# DEST_DIR="/Users/emmaclift/git/IUPAC-FAIRSpec/c:/dryad/test"
mkdir test2

unzip dataset.zip -d test2

cd test2

for file in *.zip; do
	unzip "$file"
	echo "$file"
	echo "hi"
	filename=$(basename "$file" .zip)
	if [ -d "$filename" ]; then
    echo "'$filename' is a directory."
    pwd
    cd "$filename"
    pwd
    for file_nextlayer in *.zip; do
      unzip "$file_nextlayer"
    done

  else
    echo "'$filename' is not a directory (or does not exist)."
  fi
done


ls
pwd

if [ -z "$1" ]; then
    echo "Usage: $0 <directory_path>"
    exit 1
fi

echo "File List" > file_list.txt

OUTPUT_FILE="file_list_3tx95x6sq.txt"
echo $OUTPUT_FILE

find "$1" -print > "$OUTPUT_FILE"

echo "All file paths starting from '$1' have been saved to '$OUTPUT_FILE'."




