# !/bin/bash


# requires sudo apt update 
# requires sudo apt install unzip

# check these three globals

Emma_Algorithm="/c/temp/iupac/compound_candidate_identifier.py"
Zip_Source_Directory="/c/temp/iupac/acs2/data"
Working_Directory="/c/temp/iupac/acs2/test2"

# check that the source and working directories exist
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

# check that the python file exists -- maybe jus a warning?
if [ ! -f "${Emma_Algorithm}" ]; then
    echo "Error: ${Emma_Algorithm} does not exist" >&2
    exit 1
fi

# paramters <doi> and --nozip (or -nozip)
doi="?"
doUnzip="?"
dois=""

if [[ "$1" == "--nozip" || "$1" == "-nozip" ]]; then
    doUnzip=1
fi
if [[ "$1" == "--zip" || "$1" == "-zip" ]]; then
    doUnzip=0
fi

if [[ ! "$2" == "" ]]; then
    doi="$2"
fi

if [[ "$doi" == "?" && "$doUnzip" == "?" && ! "$1" == "" ]]; then
    doi="$1"
fi


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
if [[ "${doi}" == "?" ]]; then 
  read -p "Please enter the DOI of the ACS set you want to work on (e.g. jo4c02094): " doi
fi

# allow for a comma-separated list
if [[ ! "${doi}" == "?" ]]; then
    IFS="," read -ra dois <<< "${doi}"
    IFS=$' \t\n' 
fi

# default to --zip for more than one
if [[ doUnzip == "?" && dois[@] -ne 1 ]]; then
   doUnzip = 0
fi

tempfile=$(mktemp)

# now one BIG loop

for doi in "${dois[@]}"
do

    echo "processing $doi"

    # make sure the doi has zip files
    Zip_Root="${Zip_Source_Directory}/${doi}"
    files=("${Zip_Root}"*.zip)
    if [ ${#files[@]} == 0 ]; then
        echo "Error: no zip files found for ${Zip_root}*.zip"
        continue
    fi

    echo "${#files[@]} zip files for $doi"

    DOI_Dir="${Working_Directory}/${doi}"
    # Clear the folder for prediction if it exists
    if [ ! -d "${DOI_Dir}" ]; then
        mkdir $DOI_Dir
    fi

    Prediction_Directory="${DOI_Dir}/${doi}_prediction"
    # Clear the folder for prediction if it exists
    if [ -d "${Prediction_Directory}" ]; then
    rm -rf "${Prediction_Directory}"
    fi

    Unzip_Dir="${DOI_Dir}/${doi}_unzip"

    if [[ doUnzip == "?" && -d "$Unzip_Dir" ]]; then
    doUnzip=$(yes_or_no "Do you want to unzip files?")
    fi

    unzip_recursive() {
    
    echo "test ${Unzip_Dir}"

    # TODO clear out __MACOSX files; could also be __MACOS I think
    find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
        
    # Continuously search for zip files until none are left
    
    while [ "$(find . -path "*/__MACOSX" -prune -o -type f -name '*.zip' -print | wc -l)" -gt 0 ]; do

        # Find each zip, extract to {name}.zip__, then delete the archive
        # Note - output from echo is not coming through from the exec shell
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
        rm -f "$DOI_Dir"/*.zip 

        # Clear any existing unzipped folder if present
        rm -rf "$DOI_Dir"/*_unzip

        # Create a new folder to store unzipped data if it has not existed beforehand
        # e.g. jo4c02094_unzip
        echo unzip dir is ${Unzip_Dir}
        mkdir ${Unzip_Dir}

        # copy zip files to the DOI directory
        cp "${Zip_Root}"*.zip ${DOI_Dir}
        cd ${DOI_Dir}
        ls *.zip

        # unzip these top-level zip files into the <DOI>_unzip directory
        unzip "${Zip_Root}"*.zip -d ${Unzip_Dir}

        # cd to the top of the <DOI>_unzip directory
        cd ${Unzip_Dir}

        # Call the recursive function to unzip all .zip files in the dataset
        unzip_recursive

        # cd to the top of the <DOI>_unzip directory
        cd ${Unzip_Dir}
        rm -rf __MACOS*

    fi # if doUnZip

    # Create a new folder to store the output from running compound_candidate_identifier.py 
    mkdir "${Prediction_Directory}"

    # Create the list of all the directories and files in the dataset in the prediction folder
    OUTPUT_FILE="${Prediction_Directory}/file_list_${doi}.txt"
    echo "creating ${OUTPUT_FILE}"
    cd "${Unzip_Dir}"
    find "." -print > "$OUTPUT_FILE"

    # Run the compound_candidate_identifier script to generate output files
    cd "${Prediction_Directory}"

    # this next line fails because pandas is not present and can't be installed or found
    py "${Emma_Algorithm}" $doi > "${Prediction_Directory}/emma.log"  2> "$tempfile"
    iserr=$(stat -c%s "$tempfile")
    echo "$iserr"
    if [[ ! iserr == "0" ]]; then
        errFile="${Prediction_Directory}/emma.err"
        echo "Python errors with ${doi} - see $errFile"
        cp "$tempfile" "$errFile"
    fi
 
    cd "${Prediction_Directory}"


done # for doi

# Clean up the temporary file
rm -f "$tempfile"

echo "done"
