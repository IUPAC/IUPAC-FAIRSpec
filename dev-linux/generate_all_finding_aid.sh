# !/bin/bash

# Absolute path to the IUPAC-FAIRSPEC directory
IUPAC_DIRECTORY="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/" 
cd $IUPAC_DIRECTORY

read -p "Input the path (absolute/relative) to the parent folder you want to store the output " path

if [[ ! -d "${path}" ]]; then
    echo "${path} does not exist"
    mkdir $path
fi

dryad_doi=( 
        "2bvq83c2q"
        "ghx3ffc2d"
        "3tx95x6sq"
        "f7m0cfz7t"
        "mcvdnckbb"
        "v6wwpzh7x"
        )

icl_doi=( 
        "10.14469/hpc/10386"
        "10.14469/hpc/14635" 
        )

acs_doi=( 
        "acs.orglett.0c00874"
        "acs.orglett.0c00788"
        "acs.orgLett.9b02307"
        "acs.orglett.0c01297"
        "acs.orglett.0c01277"
        "acs.joc.0c00770"
        "acs.orglett.0c01197"
        "acs.orglett.0c01153"
        "acs.orglett.0c01043"
        "acs.orglett.0c01022"
        "acs.orglett.0c00967"
        "acs.orglett.0c00755"
        "acs.orglett.0c00624"
        "acs.orglett.0c00571"
    )

for doi in "${acs_doi[@]}"; do
    java -jar "dist/IFDExtractor.jar" -test "acs" -T "${path}/acs/" -D "${doi}"
    echo "SUCCESSFULLY GENERATE THE FINDING AID FOR ${doi}"
done

for doi in "${icl_doi[@]}"; do
    java -jar "dist/IFDExtractor.jar" -W -test "icl" -T "${path}/icl/" -D "${doi}" -X "src/main/resources/com/integratedgraphics/extractor/extract/ImperialCollege/IFD-extract.json -N"
    echo "SUCCESSFULLY GENERATE THE FINDING AID FOR ${doi}"
done

read -p "Input the path (absolute/relative) to the folder that stores the dryad datasets" dryad_path

if [[ ! -d "${dryad_path}" ]]; then
    echo "${dryad_path} does not exist"
    exit 1
fi

for doi in "${dryad_doi[@]}"; do
    java -jar "dist/IFDExtractor.jar" -test "dryad" -T "${path}/dryad/" -S "${dryad_path}/${doi}/dataset.zip" -D "${doi}"
    echo "SUCCESSFULLY GENERATE THE FINDING AID FOR ${doi}"
done

