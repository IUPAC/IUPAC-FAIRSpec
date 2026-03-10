# !/bin/bash

# defaults for these three globals

Emma_Algorithm="/c/temp/iupac/compound_candidate_identifier.py"

# holds a bunch of ZIP files downloaded from FigShare
Zip_Source_Directory="/c/temp/iupac/acs2/data"

# where everything is going to be put
Working_Directory="/c/temp/iupac/acs2/test2"

# Each set of zip files for a given "doi" (actually just part of that) 
# will have its own directory. A "doi" in this case is the starting string of the zip file
# for example, for 
# pub DOI https://doi.org/10.1021/acs.joc.4c03150
# we have:
# SI URI https://acs.figshare.com/articles/dataset/A_Twist_on_Controlling_the_Equilibrium_of_Dynamic_Thia-Michael_Reactions/28555606 
# SI DOI https://doi.org/10.1021/acs.joc.4c03150.s005	
# SI URI https://acs.figshare.com/articles/dataset/A_Twist_on_Controlling_the_Equilibrium_of_Dynamic_Thia-Michael_Reactions/28555603
# SI DOI https://doi.org/10.1021/acs.joc.4c03150.s00
# and the "doi" here is just the common element there, sort of -- really a prefix: "jo4c03150"
# because the Figshare download from the ACS portal delivers
# jo4c03150_si_004.zip
# jo4c03150_si_005.zip

# requires sudo apt update 
# requires sudo apt install unzip


OPTS=$(getopt -o :d:cf:tunp -a -l doi:,clean,file:,test,unzip,newpredictiononly -n "$0" -- "$@")
if [[ $? -ne 0 ]]; then
    echo "$? failed parsing [optional -[d]oi] <doi(s)> -[c]lean -[f]ile <doiListFile>"
    exit 1;
fi

doi="?"
doClean=false
dois="?"
isTest=false
unzipOnly=false
newPredictionOnly=false


eval set -- "$OPTS"
while true; do
  # noting that | --xxx is not necessary; here for convenience only; -xxx will also work
  case "$1" in
    -d | --doi) # one or comma-separated list
        doi="$2"
        shift 2
        ;;
    -u | --unziponly) # just unzip as needed; no prediction
        unzipOnly=true
        shift
        ;;
    -n | --newpredictiononly)
        newPredictionOnly=true
        shift
        ;;
    -c | --clean)
        doClean=true
        shift
        ;;
    -f | --file) # one per line
        mapfile -t dois < "$2"  
        echo "read ${#dois[@]} DOIs from $2"
        doi=""
        shift 2
        ;;
    -t | --test) # just report source file information
        isTest=true
        shift 1
        ;;
    --) 
    echo "-- $1"
        shift
        break
        ;;
    *)
        echo "Internal error!" >&2
        exit 1
        ;;  
    esac
done

# also accepts one or more comma-separated DOIs
for arg; do
    dois="?"
    if [[ $doi == "?" ]]; then
        doi=$arg
    else
        doi+=",$arg"
    fi
done

echo "doClean=${doClean} doi=${doi} dois=${dois}"
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
if [[ ! "${doi}" == "?" && ! ${doi} == "" ]]; then
    IFS="," read -ra dois <<< "${doi}"
    IFS=$' \t\n' 
fi

tempFile=$(mktemp)

# the recursive unzip

unzip_recursive() {

    # cd to the top of the <DOI>_unzip directory
    cd ${Unzip_Dir}

    # TODO clear out __MACOSX files; could also be __MACOS I think
    find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
        
    # Continuously search for zip files until none are left
    
    while [ "$(find . -path "*/__MACOSX" -prune -o -type f -name '*.zip' -print | wc -l)" -gt 0 ]; do
        # Find each zip, extract to {name}.zip__, then delete the archive
        # Note - output from echo is not coming through from the exec shell
        while read -d '' file; do
            echo "Unzipping file: $file"
            dest_dir="${file}__"
            if unzip "$file" -x '__MACOSX/*' -d "$dest_dir"; then
                rm "$file"
                echo "Extracted and removed: $file"
            else
                echo "Error unzipping: $file"
                exit 1
            fi
        done < <(find . -type f -name "*.zip" -print0)
        find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
    done
}


# now one BIG loop

for doi in "${dois[@]}"
do
    # trim lines and skip blank lines and # lines
    doi=$(echo "$doi" | xargs)
    if [[ "${doi}" == "" || ${doi:0:1} == "#" ]]; then
        continue
    fi
    if [[ "${doi}" == "EXIT" || "${doi}" == "STOP" ]]; then
        break;
    fi

    DOI_Dir="${Working_Directory}/${doi}"
    # Create the DOI directory if it does not exist
    if [ ! -d "${DOI_Dir}" ]; then
        mkdir $DOI_Dir
    fi

    # the list of all the directories and files in the dataset
    fileListFile="${Working_Directory}/file_list_${doi}.txt"

    Unzip_Dir="${DOI_Dir}/${doi}_unzip"

    doCleanMe=$doClean

    if [ ! -f ${fileListFile} ]; then
        doCleanMe=true
    fi

    Prediction_Dir="${DOI_Dir}/${doi}_prediction"
    finalOutputFile="${Prediction_Dir}/${doi}_final_output.tsv"

    if $doCleanMe; then

        echo "cleaning $doi"

        # make sure the doi has zip files
        Zip_Root="${Zip_Source_Directory}/${doi}"
        files=("${Zip_Root}"*.zip)
        if [ ${#files[@]} == 0 ]; then
            echo "Error: no zip files found for ${Zip_root}*.zip"
            continue
        fi

        echo "${#files[@]} zip files for $doi"

        # only to here if testing
        if $isTest; then
            continue
        fi

        # Clear existing zip files from this directory
        rm -f "$DOI_Dir"/*.zip 

        # Clear the existing unzip folder if present
        rm -rf "${Unzip_Dir}"

        # Create a new folder to store unzipped data if it has not existed beforehand
        # e.g. jo4c02094_unzip
        echo unzip dir is ${Unzip_Dir}
        mkdir ${Unzip_Dir}

        # copy zip files to the DOI directory
        cp "${Zip_Root}"*.zip ${DOI_Dir}
        cd ${DOI_Dir}
        ls *.zip

        # unzip these top-level zip files into the <DOI>_unzip directory

        for file in "${DOI_Dir}/"*.zip; do
            echo $file
            file=$(basename ${file})
            fileRoot=$(echo "$file" | cut -d '.' -f 1)   
            unzip "${DOI_Dir}/"'*.zip' -x '__MACOS*/*' '*/__MACOS*/*' -d "${Unzip_Dir}/${fileRoot}"    
        done
        
        # Call the recursive function to unzip all .zip files in the dataset
        unzip_recursive

        # cd to the top of the <DOI>_unzip directory
        cd ${Unzip_Dir}
        rm -rf __MACOS*
       # Create the list of all the directories and files in the dataset
        fileList=$(find . -print)
        fileList=${fileList//\.\//}
        echo "${fileList}" > "${fileListFile}"
    elif $newPredictionOnly; then
        if [ -d "${Prediction_Dir}" ]; then
            if [ -f "${finalOutputFile}" ]; then
                continue
            fi
        fi
    fi

    # The prediction only needs the file list, not the unzipped files. We could delete those here.

    if $unzipOnly; then
        continue
    fi

     echo "predicting $doi"
     
    # Clear the folder for prediction if it exists
    if [ -d "${Prediction_Dir}" ]; then
        rm -rf "${Prediction_Dir}"
    fi

    # Create a new folder to store the output from running compound_candidate_identifier.py 
    mkdir "${Prediction_Dir}"

    # add the file list to Prediction (for processing)
    cp "${fileListFile}" "${Prediction_Dir}/"

    # Run the compound_candidate_identifier script to generate output files from the prediction directory
    cd "${Prediction_Dir}"

    rm -f "${Working_Directory}/${doi}"_.*
  

    # this next line fails because pandas is not present and can't be installed or found
    py "${Emma_Algorithm}" $doi > "${Prediction_Dir}/emma.log"  2> "$tempFile"
    iserr=$(stat -c%s "$tempFile")

    if [[ ! ${iserr} == "0" ]]; then
        errFile="${Prediction_Dir}/emma.err"
        echo "!!!!!!!!!!Python errors for ${doi} - see $errFile !!!!!!!!!!"
        cp "$tempFile" "$errFile"
        cp "$tempFile" "${Working_Directory}/${doi}.err"
    else
        echo "No Python errors for ${doi}" 
    fi
 
    cd "${Prediction_Dir}"


done # for doi

# Clean up the temporary file
rm -f "$tempFile"

echo "done"
