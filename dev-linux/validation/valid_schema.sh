# !/bin/bash

# Script to run schema validation on generated IFD.finding_aid.json

# Prevents the pattern from being returned if no match is found
shopt -s nullglob 
# Define the path to the directory that stores the output folders (<DOI>_out/ or <DOI>/ for ICL crawler)
DATA_DIRECTORY="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/c:/temp/"

# Array to store the matched folder names
matching_folders=()

# Function to validate the schema
validate_schema(){
  matching_folders=()
  # The argument whether the dataset is from ICL or not
  local is_icl=$1
  # Regex for the output folder name (ACS/Dryad)
  regex_pattern=".*_out/$"
  # Regex for the output folder name (ACS/Dryad)
  if [[ $is_icl == 1 ]]; then
    regex_pattern="10.14469_hpc_[0-9]*\/"
  fi
  # Find the output folders
  for dir in */; do
    if [[ $dir =~ $regex_pattern ]]; then
      matching_folders+=("$dir")
    fi
  done
  
  # For each output folder
  for i in "${!matching_folders[@]}"; do
    folder="${matching_folders[i]}"
    # Retrieve the DOI of the current dataset
    if [[ $is_icl == 1 ]]; then
      folder_prefix="${folder%/}"
    else
      folder_prefix="${folder%_out/}"
    fi
    cd "$folder"
    # Remove the generated log file of previous validation 
    rm -rf "${folder_prefix}_schema_valid.txt"
    rm -rf "${folder_prefix}_schema_valid_error.txt"
    # Handle ethe case this folder does not have the finding aid or the schema
    if [ ! -f "IFD.findingaid.json" ]; then
      echo "IFD.findingaid.json does not exist in /${folder}" > "${folder_prefix}_schema_valid_error.txt"
      cd ..
      continue
    fi
    if [ ! -f "IFD.findingaid.schema.json" ]; then
      echo "IFD.findingaid.schema.json does not exist in /${folder}" > "${folder_prefix}_schema_valid_error.txt"
      cd ..
      continue
    fi

    # Timestamp for the validation
    timestamp=$(date +'%Y-%m-%d %H-%M-%S')

    # Run the validation and capture both standard output and error messages
    output=$(check-jsonschema --verbose --schemafile IFD.findingaid.schema.json IFD.findingaid.json -o text 2>&1)

    # Check the exit status of the check-jsonschema to define whether it is SUCCESFULLY or FAILED
    if [ $? -eq 0 ]; then
      echo "Validation SUCCESSFUL for $folder"
      {
        echo "Validation run on ${timestamp}"
        echo "$output"
      } > "${folder_prefix}_schema_valid.txt"
      
    else
      echo "Validation FAILED for $folder"
      {
        echo "Validation run on ${timestamp}"
        echo "ERROR in folder: $folder"
        echo "$output" 
      } > "${folder_prefix}_schema_valid_error.txt"
    fi
    cd ..
  done
}

cd $DATA_DIRECTORY

for dir in */; do
  cd $dir
  if [[ "${dir}" == "dryad/" || "${dir}" == "acs/" ]]; then 
    validate_schema 0
  elif [[ "${dir}" == "icl/" ]]; then
    validate_schema 1
  fi
  cd ..
done

exit 0