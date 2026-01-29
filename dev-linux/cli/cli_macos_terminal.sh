# !/bin/bash

# Absolute path to the IUPAC-FAIRSPEC directory
IUPAC_DIRECTORY="/Users/faynguyen03/Documents/IUPAC-FAIRSpec/" 
cd $IUPAC_DIRECTORY

rm -rf bin
javac -d bin -target 1.8 -source 1.8 -cp "lib/*" $(find src -name "*.java")

# Create the manifest.txt in dist/ folder
# Generate the .jar file
jar cvfm dist/IFDExtractor.jar dist/manifest.txt -C bin .