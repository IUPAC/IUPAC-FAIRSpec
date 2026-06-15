#!/bin/bash

# run_compound_id.sh
# 2026.04.20 FN -- simple test script to run compound_id.py directly on a DOI or the todo.txt  

# Usage: bash run_compound_id.sh [-f <todo_file>] [-d <doi>]
# Example (single DOI):  bash run_compound_id.sh -d jo4c02622
# Example (todo file):   bash run_compound_id.sh -f todo.txt

IUPAC_Directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/../../../"

Emma_Algorithm="$IUPAC_Directory/dev-linux/automation/compound_id_fay.py"

Working_Directory="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/c:/temp/acs2/test2"

doi=""
doiTodoFilePath=""

while [[ -n "$1" ]]; do
    case "$1" in
        -d | --doi)
            doi="$2"
            shift 2
            ;;
        -f | --file)
            doiTodoFilePath="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: bash run_compound_id.sh [-f <todo_file>] [-d <doi>]"
            exit 1
            ;;
    esac
done

if [[ ! -f "$Emma_Algorithm" ]]; then
    echo "Error: compound_id.py not found at $Emma_Algorithm"
    exit 1
fi

# List of DOIs to process
dois=()

if [[ -f "$doiTodoFilePath" ]]; then
    echo "Reading DOIs from $doiTodoFilePath"
    while IFS= read -r line || [[ -n "$line" ]]; do
        trimmed_line=$(echo "$line" | xargs)
        [[ -z "$trimmed_line" || "$trimmed_line" == "#"* ]] && continue
        if [[ "$trimmed_line" == "STOP" || "$trimmed_line" == "EXIT" ]]; then
            echo "Stopping at $trimmed_line"
            break
        fi
        clean_line=${trimmed_line%% #*}
        clean_line=${clean_line%%#*}
        clean_line=$(echo "$clean_line" | xargs)
        [[ -n "$clean_line" ]] && dois+=("$clean_line")
    done < "$doiTodoFilePath"
    echo "Read ${#dois[@]} DOIs from $doiTodoFilePath"
elif [[ -n "$doi" ]]; then
    dois=("$doi")
else
    echo "Usage: bash run_compound_id.sh [-f <todo_file>] [-d <doi>]"
    echo "Example: bash run_compound_id.sh -f todo.txt"
    echo "Example: bash run_compound_id.sh -d jo4c02622"
    exit 1
fi

errors=""
doiCount=0

tempFile=$(mktemp)

declare -a csv_rows=()
csv_rows+=("DOI,compound_id_count,compound_ids")  

for doi in "${dois[@]}"; do
    doi="${doi%% *}"
    doi="${doi//[[:space:]]/}"
    [[ -z "$doi" || "${doi:0:1}" == "#" ]] && continue

    ((doiCount++))

    fileListFilePruned="${Working_Directory}/file_list_${doi}.txt"
    if [[ ! -f "$fileListFilePruned" ]]; then
        echo "Error: file list not found at $fileListFilePruned"
        echo "  Run first: bash acs2-bash.sh -u -d $doi"
        errors+=" $doi"
        continue
    fi

    Prediction_Dir="${Working_Directory}/${doi}/${doi}_prediction"
    mkdir -p "$Prediction_Dir"
    cp "$fileListFilePruned" "$Prediction_Dir/${doi}_file_list.txt"
    cd "$Prediction_Dir"
    export PYTHONIOENCODING=utf-8

    echo "Running compound_id.py for $doi ..."
    logFile="${Prediction_Dir}/emma.log"
    python3 "$Emma_Algorithm" "$doi" > "$logFile" 2> "$tempFile"
    cat "$logFile"

    iserr=$(stat -f%z "$tempFile")
    if [[ ! "$iserr" == "0" ]]; then
        errFile="${Prediction_Dir}/emma.err"
        errors+=" $doi"
        echo "!!!! Python errors for ${doi} — see $errFile !!!!"
        cp "$tempFile" "$errFile"
        cat "$errFile"
    else
        if [[ -f "${Prediction_Dir}/${doi}_compound_id_list.txt" ]]; then
            compound_ids=$(tail -1 "${Prediction_Dir}/${doi}_compound_id_list.txt" | cut -d' ' -f2-)
            compound_count=$(echo "$compound_ids" | tr ';' '\n' | wc -l)
            csv_rows+=("$doi,$compound_count,\"$compound_ids\"")
        fi
        echo "No Python errors for ${doi}"
    fi
done

csv_file="${Working_Directory}/summary_results.csv"
printf '%s\n' "${csv_rows[@]}" > "$csv_file"
echo "Created summary CSV: $csv_file"

rm -f "$tempFile"

if [[ -n "$errors" ]]; then
    echo -e "\nErrors for:$errors"
fi
