#!/bin/bash

# acs2-2-git-mac.sh

# 2026.03.18 FN -- initiated a generalized script that runs the corresponding script based on the operating system

# requires paths to the Python script, folder that stores the .zip files and the folder that stores the output

# 
IUPAC_Directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/../../../"

# Global Variables
# Path to Emma's Python script (Absolute Path)
Emma_Algorithm="$IUPAC_Directory/dev-linux/automation/compound_id.py"

# Path to the folder holds a bunch of ZIP files downloaded from FigShare
Zip_Source_Directory="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/c:/temp/acs2/data"

# Path to the output folder
Working_Directory="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/c:/temp/acs2/test2"

# Check if Emma's algorithm Python file exists
if [[ ! -f "$Emma_Algorithm" ]]; then
    echo "$Emma_Algorithm does not exist"
    exit 1
fi

# Check if the zip source folder exists
if [[ ! -d "$Zip_Source_Directory" ]]; then
    echo "$Zip_Source_Directory does not exist"
    exit 1
fi

# Check if the zip source folder exists
if [[ ! -d "$Working_Directory" ]]; then
    echo "$Working_Directory does not exist"
    exit 1
fi

# Export these variables to the environment
export IUPAC_Directory
export Emma_Algorithm
export Zip_Source_Directory
export Working_Directory

# Retrieve the name of the operating system
OS_NAME=$(uname -s)

case "$OS_NAME" in
    Darwin)
        bash $IUPAC_Directory/dev-linux/automation/acs2/acs2-2-git-mac.sh "$@"
        ;;
    CYGWIN*|MSYS*|MINGW*)
        bash $IUPAC_Directory/dev-linux/automation/acs2/acs2-2-git-win.sh "$@"
        ;;
    *)
        echo "Unknown operating system: $OS_NAME"
        exit 1
        ;;
esac

# Run the script: bash dev-linux/automation/acs2/acs2-bash.sh -f 