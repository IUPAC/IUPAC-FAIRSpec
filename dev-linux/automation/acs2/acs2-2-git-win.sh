# !/bin/bash

# acs2-2-git-win.sh

# 2026.04.09 BH -- excludes .xyz, .txt files from pruned file list 
# 2026.03.12 BH -- fixes ZIP and .xxx.ZIP issues
# 2026.03.11 BH -- streamlined file list generation
# 2026.03.10 BH from acs2_preprocessing2-git-win.sh
# works with adapted alogrithm and experiments with ways to enhance functionality

# defaults for these three globals

Emma_Algorithm="/c/temp/iupac/compound_id.py"

# holds a bunch of ZIP files downloaded from FigShare
Zip_Source_Directory="/c/temp/iupac/acs2/data"

# where everything is going to be put
Working_Directory="/c/temp/iupac/acs2/test2"
Output_Dir="${Working_Directory}/output"
 
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


OPTS=$(getopt -o :d:cf:tunpl -a -l doi:,clean,file:,test,unzip,newpredictiononly,listonly -n "$0" -- "$@")

doi="?"
doClean=false
dois="?"
isTest=false
unzipOnly=false
listOnly=false
newPredictionOnly=false

if [[ $? -ne 0 ]]; then
    echo "$? failed parsing [optional -[d]oi] <doi(s)> -[c]lean -[f]ile <doiListFile>"
    exit 1
fi

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
    -l | --listonly) # create a new file list, but don't unzip
        listOnly=true
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

echo "doClean=${doClean} doi=${doi} dois=${dois} listOnly=${listOnly} unzipOnly=${unzipOnly}"


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

    # Continuously search for zip files until none are left
    
    while [ "$(find . -path "*/__MACOSX" -prune -o -type f -iname '*.zip' -print | wc -l)" -gt 0 ]; do
        # Find each zip, extract to {name}.zip__, then delete the archive
        # Note - output from echo is not coming through from the exec shell
        # remove any .* files, especially .*.zip  (jo4c02893) 
        find . -name "\.*" -delete
        while read -d '' file; do
            echo "Unzipping file: $file"
            dest_dir="${file}__"
            if unzip -o "$file" -x '__MACOS*/*' '*/__MACOS*/*' '.*' -d "$dest_dir"; then
                rm "$file"
                rm -f "${dest_dir}/.*"
                echo "Extracted and removed: $file"
            else
                echo "Error unzipping: $file"
                exit 1
            fi
        done < <(find . -type f -iname "*.zip" -print0)
    done
    # TODO clear out __MACOSX files; could also be __MACOS I think
    find . -name "__MACOSX" -type d -exec rm -rf {} + 2>/dev/null
    find . -name "*desktop.ini" -type f -exec rm -rf {} + 2>/dev/null
        
}

resort_fdata() {
    # make sure the desired item is the FIRST in the directory
    local -n f=$1
    var=$2
    if [[ "$var" == "" ]]; then
        d=$(sort <<< "$f")
    else
        var1="/${var}"
        var2="/ .${var}"
        d="${f//$var1/$var2}"
        d1=$(sort <<< "$d")
        d="${d1//$var2/$var1}"
    fi
    f="$d"
}

prune_file_list() {


        # fileList does not contain directories, but if we are looking
        # for a directory, we want to put at least the first file in the directory into that
        # not exactly true
        
        var=$1
        var1="/${var}"
        isfile=$2
        # remove /.*
        # remove /pdata/*
        # and reverse order of lines
        # also remove unneeded directories
        if $isfile; then
            fdata=$fileList
        else
            fdata="${dirList}"
        fi
        fdata=$(echo "$fdata" | grep "${var1}$")
        if ! $isfile; then
            resort_fdata fdata "${var}" 
            resort_fdata fileList ""
            #echo "pruning for ${var1}"
            #echo "${fdata}"
        fi
        if [[ ! "$fdata" == "" ]]; then
            lines=""
            files=""
            readarray -t files <<< "$fileList"
            readarray -t lines <<< "$fdata" 
            nlines=${#lines[@]}
            thisline=0
            nfiles=${#files[@]}
            thisfile=0
            newPath=true
            if $isfile; then
                # we have /procpar 
                # delete anything else in its directory
                while [[ $thisfile -lt $nfiles ]]; do
                    if $newPath; then
                        newPath=false
                        line=${lines[thisline]}
                        path="${line%%$var1}/"
                        havePath=false
                    fi
                    file=${files[thisfile]}
                    if [[ "${file}" == "${path}"* ]]; then
                        havePath=true
                        if [[ ! "$file" == "$line" ]]; then
                            #echo "removing ${files[thisfile]}"
                            files[thisfile]=""
                        fi
                    elif $havePath; then
                        ((thisline++))
                        if [[ $thisline -eq $nlines ]]; then
                            break
                        fi
                        newPath=true
                        continue
                    fi
                    ((thisfile++))
                done
            else 
                # if we have /pdata/ files in this file list
                # delete anything else in the directory containing it or its directory
                # except if there is no actual file in the directory, just leave the first file there (acqu)
                while [[ $thisfile -lt $nfiles ]]; do
                    if $newPath; then
                        newPath=false
                        line=${lines[thisline]}
                            #echo -e "\nline=\n${line}" 
                        path0="${line%%$var1*}/"
                        path1="${line}/"
                        if [[ "$fileList" == *"$path1"* ]]; then
                            haveVar2=true
                        else
                            haveVar2=false
                            # so we will ignore
                            echo "zip file does not have $path1 !"
                        fi
                        havePath=false
                    fi
                    file=${files[thisfile]}
                    if [[ "${file}" == "${path0}"* ]]; then
                        # xxxx/*
                        if ! $havePath; then
                            if $haveVar2; then
                                # have xxxx/pdata/ somewhere
                                # replace file with directory
                                #echo -e "setting line $thisfile  to \n${line}"
                                files[thisfile]="${line}"
                                havePath=true
                            fi
                            ((thisfile++))
                            continue
                        fi
                        #echo "setting line $thisfile to blank"
                        files[thisfile]=""
                    else
                        #echo -e "no match for $thisfile file\n$file\nis not in path\n$path0"
                        # on to the next yyyy/...
                        # Skip all directory lines that contain the current path
                        while true; do 
                            ((thisline++))
                            # check that the next line has not already been removed 
                            nextline=${lines[thisline]}
                            #echo -e "checking\n${nextline}" 
                            if [[ "${nextline}" == "${path0}"* ]]; then 
                                #echo -e "skipping\n${nextline}"
                                continue
                            fi
                            #echo -e "next:\n$nextline\n$file"
                            break
                        done
                        if [[ $thisline -eq $nlines ]]; then
                            break
                        fi
                        # do not increment file pointer
                        newPath=true
                        continue
                    fi
                    ((thisfile++))
                done
            fi
            SAVEIFS=$IFS
            IFS=$'\n'
            fileList="${files[*]}"
            IFS=$SAVEIFS
            # remove blank lines
            fileList=$(echo "$fileList" | awk 'NF')
        fi
 }

# now one BIG loop

errors=""
errCount=0
doiCount=0
for doi in "${dois[@]}"
do
    # trim lines and skip blank lines and # lines
    doi="${doi%% *}"
    doi=$(echo "$doi" | xargs)
    if [[ "${doi}" == "" || ${doi:0:1} == "#" ]]; then
        continue
    fi

    if [[ "${doi}" == "EXIT" || "${doi}" == "STOP" ]]; then
        break
    fi

    DOI_Dir="${Working_Directory}/${doi}"
    # Create the DOI directory if it does not exist
    if [ ! -d "${DOI_Dir}" ]; then
        mkdir $DOI_Dir
    fi

    # the list of all the directories and files in the dataset
    fileListName="${doi}_file_list.txt"
    fileListFilePruned="${Working_Directory}/${fileListName}"
    fileListNameRaw="${doi}_file_list_raw.txt"
    fileListFileRaw="${Working_Directory}/${fileListNameRaw}"

    Unzip_Dir="${DOI_Dir}/${doi}_unzip"

    doCleanMe=$doClean

    if [ ! -f ${fileListFilePruned} ]; then
        doCleanMe=true
    fi

    nf=$(cat "${fileListFileRaw}" | grep -c '^')

    if [ ! -f ${fileListFilePruned} ]; then
        doCleanMe=true
    elif (( nf < 2 )); then
       doCleanMe=true
    fi

    Prediction_Dir="${DOI_Dir}/${doi}_prediction"
    finalOutputFile="${Prediction_Dir}/${doi}_final_output.tsv"
 
    ((doiCount++))

    if $doCleanMe; then

        # clear output files that are in the working directory
        rm -f "${Working_Directory}/${doi}"_*

        if (! $listOnly); then 



            echo "cleaning $doi"


            # make sure the doi has zip files
            check_dir_exists ${Zip_Source_Directory}
            Zip_Root="${Zip_Source_Directory}/${doi}"
            files=("${Zip_Root}"*.zip)
            if [ ${#files[@]} == 0 ]; then
                echo "Error: no zip files found for ${Zip_root}*.zip"
                errors+="$\n${doiCount} ${doi} no zip files"
                (( errCount += 1 ))
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
                bname=$(basename ${file})
                fileRoot=$(echo "$bname" | cut -d '.' -f 1)   
                Root_Dir="${Unzip_Dir}/${fileRoot}"
                unzip -o "${file}" -x '*desktop*' '__MACOS*/*' '*/__MACOS*/*' '.*' -d "${Root_Dir}"    
                rm -f "${Root_Dir}/.*"
                ls "${Root_Dir}"
            done
            
            # Call the recursive function to unzip all .zip files in the dataset
            unzip_recursive
            
        fi

        # create the working-directory file list
        # cd to the top of the <DOI>_unzip directory
        cd ${Unzip_Dir}
        rm -rf __MACOS*
        # Create the list of all the directories and files in the dataset
        fileList=$(find . -type f | grep -v -F "/." | grep -v -F "dirinfo" | grep -v -F ".xyz" | grep -v -F ".txt" | grep -v -F ".doc")
        find . -type d -empty -delete  # at least this once, because the unzipping is already done
        dirList=$(find . -type d -print)
        # remove "./" 
        fileList=${fileList//\.\//}
        dirList=${dirList//\.\//}
        # for xxx/pdata, we need to ensure that and nested xxx/..../..../pdata
        # in this case, xxx/ will appear in dirList[i] + 1
        # is still in the list that is the LAST entry in xxx/
        # in case there is a nested pdata that happens to be in it: ol5c0318
        #

        echo -e "${fileList}"
        echo -e "${fileListFileRaw}"

        echo "${fileList}" > "${fileListFileRaw}"
        echo "pruning ${doi}"

        # add the file list to Prediction (for processing)
        # but see jo5c00061 [10_NC] fileList=${fileList//-/\/}

        prune_file_list "pdata" false   

        prune_file_list "acqu" true 
        prune_file_list "procpar" true 
        echo "${fileList}" > "${fileListFilePruned}"


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

    echo "analyzing $doi"

    rm -f "${Working_Directory}/${doi}.err"

    # Clear the folder for prediction if it exists
    if [ -d "${Prediction_Dir}" ]; then
        rm -rf "${Prediction_Dir}"
    fi

    # Create a new folder to store the output from running compound_candidate_identifier.py 
    mkdir "${Prediction_Dir}"
    cp "${fileListFileRaw}" "${Prediction_Dir}/${fileListNameRaw}"
    cp "${fileListFilePruned}" "${Prediction_Dir}/${fileListName}"

    # ensure UTF-8
    export PYTHONIOENCODING=utf-8
    # this next line fails in gnu bash because pandas is not present and can't be installed or found
    logFile="${Prediction_Dir}/emma.log"
    cd "${Prediction_Dir}"
    py "${Emma_Algorithm}" $doi > "${logFile}"  2> "$tempFile"
    # report errors
    
    cat "$logFile"
    iserr=$(stat -c%s "$tempFile")
    if [[ ! ${iserr} == "0" ]]; then
        errFile="${Prediction_Dir}/emma.err"
        errors+="\n${doiCount} ${doi}"
        (( errCount += 1 ))
        echo "!!!!!!!!!!Python errors for ${doi} - see $errFile !!!!!!!!!!"
        cp "$tempFile" "$errFile"
        cat "$errFile"
        cp "$tempFile" "${Working_Directory}/${doi}.err"
    else
        echo "No Python errors for ${doi}" 
        cp "${Prediction_Dir}/${doi}_compound_name_list.txt" "${Working_Directory}"
        cp "${Prediction_Dir}/${doi}_compound_id_count" "${Working_Directory}"
        cp "${Prediction_Dir}/${doi}_data_object_id_list.txt" "${Working_Directory}"
        cp "${Prediction_Dir}/${doi}_data_object_id_count" "${Working_Directory}"
        cp "${Prediction_Dir}/${doi}_missing_files_list.txt" "${Working_Directory}"
        cp "${Prediction_Dir}/${doi}_missing_files_list.tsv" "${Working_Directory}"
             cp "${Prediction_Dir}/${doi}_missing_files_count" "${Working_Directory}"
        cd "${Working_Director}"
        cat "${doi}_compound_id_list.txt"
        cat "${doi}_data_object_id_list.txt"
        filenameC="${doi}_compound_id_count"
        filenameD="${doi}_data_object_id_count"
        nf=$(cat "${fileListFilePruned}" | grep -c '^')
        echo -e "\n${doiCount} ${doi} $(wc -c < $filenameC) compounds $(wc -c < $filenameD) data objects ${nf} files"
        rm -r "${Output_Dir}/${doi}_prediction"
        cp -r "${Prediction_Dir}" "${Output_Dir}"

   fi

 
 
done # for doi

# Clean up the temporary file
rm -f "$tempFile"

if [[ ! "$errors" == "" ]]; then
    echo -e "errors:${errors}\n${errCount} errors\ndone"
fi
