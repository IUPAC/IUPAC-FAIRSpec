# Dryad's Fetching Python Script

## Set Up 

1. Clone the repository from [GitHub Link](https://github.com/FayNguyen03/dryad_script.git)

2. Sign up for a Dryad API account by following this [Dryad API Accounts Instruction](https://github.com/datadryad/dryad-app/blob/main/documentation/apis/api_accounts.md)

3. Install Python3 and import the necessary Python modules:

`pip install requests dotenv`

4. Create a `.env` file in the same directory with the Python script following this:

```
CLIENT_ID={YOUR_DRYAD_CLIENT_ID}
CLIENT_SECRET={YOUR_DRYAD_CLIENT_SECRET}
PARENT_DIRECTORY={YOUR_LOCAL_DIRECTORY_TO_STORE_DOWLOAD_FILE_INCLUDING_SLASH_AT_THE_END} (Relative Path)
```

## Fetch Dryad dataset

1. Open terminal and change directory to get to the folder that contains the script

2. To get a or multiple dataset(s) with specific `doi:10561/dryad.XXXXXX`, run the script in the terminal:

`python3 Dryad.py XXXXX1 XXXXXX2`

3. The script will fetch the data and create a `dataset.zip` file at the local directory that you want to store the downloaded file.

# CLI

## CLI Flag Table (Updating)

| Required | Flag | Long Name | Argument (if available) | Description | Note |
| :--- | :---: | ---: | :--- | :---: | ---: |
|  | -h |help |  | |  | 
|  | -v | version |  |Get the current version of the FindingAidCreator |  | 
| [X] | -T | targetDir | <TARGET_DIR> | Target output directory for the finding aid | |
| [X] | -test | test | <SOURCE> | | dryad/icl/acs | 
| [X] | -D | doi | <DOI>| DOI/Identifier ||
|  | -a | assetOnly | | Asset Only ||
|  | -A | addPublicationMetadata | |Include ALL Crossref or DataCiteOnly for post-publication-related collections; in metadata. ||
|  | -c | noClean | |Don't empty the destination collection directory before extraction; allows additional files to be zipped ||
|  | -C | dataciteDonw | |Only for post-publication-related collections.||
|  | -debug | debugging | | This will print out all debugging messages ||
|  | -E | embedPdf | | Loads PDF documents into finding aids for cross-domain viewing of spectra ||
|  | -F | findingAidOnly | | Only create a finding aid ||
|  | -g | noLandingPage | | Don't create a landing page ||
|  | -i | noIgnored | | Don't include ignored files -- treat them as REJECTED ||
|  | -I | requiredPubInfo | | Throw an error is datacite cannot be reached; post-publication-related collections only ||
|  | -l | noLaunch | | Don't launch the landing page ||
|  | -N | insitu | | Setting insitu true generates an entirely self-contained finding aid, without local files and any rezipping in the origin directory. ||
|  | -l | noLaunch | | Don't launch the landing page on browser when finished ||
|  | -O | readOnly | | Just create a log file ||
|  | -p | noPubInfo | | Ignore all publication info ||
|  | -P | extractSpecProperties | | Extract spectra properties ||
|  | -R | debugReadonly | | Readonly, no publication metadata ||
|  | -s | noStopOnFailure | | Continue if there is an error| |
|  | -S | localSource | <LOCAL_SOURCE_PATH> | Local Source Archive Path||
|  | -W | crawler | | Run the crawler | include -test icl|
|  | -Y | addIfdTypes | |Add IFD Types ||
|  | -x | noDownload | | Do not download files from the repository | For crawler only|
|  | -X | IFDExtractFile | | Input IFD-extract.json configuration file, if used | |
|  | -z | noZip | | Don't zip up the target directory ||

## Use the Debug Interface

1. Use the **debug tool** in Eclipse:

![Click the bug button to open the debugging window](CLI1.png)

2. Create a new test

![Click the icon of document with the star to create a new test](CLI2.png)

3. Set the configuration for the test 

![Set the name for the test as CLI_Test and choose the main class com.integratedgraphics.extractor.IFDExtractor](CLI3.png)

4. Argument choices:

- Show the manual:

![Click onto the Arguments tab and add -h as the arguments](CLI4.png)

The console displays:

![Console displays the manual](CLI5.png)

- Dryad: 

`-test dryad -debug -T c:/temp/dryad/ -S c:/temp/dryad/mcvdnckbb/dataset.zip -D mcvdnckbb`

- ACS: 

`-test acs -debug -T c:/temp/acs/ -D acs.orglett.0c00874`

Don't need to include the local source since the extractor will fetch the data from online sources.

- ICL: 

`-W -test icl -debug -T c:/temp/icl/ -o 10.14469/hpc/1463 X ./src/main/resources/com/integratedgraphics/extractor/extract/ImperialCollege/IFD-extract.json`

![Set arguments for crawler](CLI6.png)

This will generate two folders 

![Folders generated from the extractor](CLI7.png)

## Run from terminal

### Create the IFDExtractor.jar file

- Create `bin` folder in `IUPAC-FAIRSpec`

```
rm -rf bin
# Compile all the .java files
javac -d bin -cp "lib/*" src/**/*.java
# Merge resources into the bin folder
# Don't include the resource files in this
# By using the trailing slash (src/main/resources/), rsync copies the 
# contents (packages/files) directly into bin, effectively merging them.
# rsync -av --exclude="*.java" src/main/resources/ bin/
```

- Create a `manifest.txt` file in the `./IUPAC-FAIRSPEC/` folder containing (always have a new line at the end):

```
Main-Class: com.integratedgraphics.extractor.IFDExtractor
Class-Path: ../bin/ ../lib/commons-compress-1.22.jar ../lib/commons-io-2.11.0.jar ../lib/CDK-SwingJS.jar ../lib/JmolDataD.jar ../src/main/resources/

```

- Generate a `IFDExtractor.jar` file in `dist`:

```
rm -rf dist/IFDExtractor.jar 
jar cvfm dist/IFDExtractor.jar dist/manifest.txt -C bin .
```

### Run the class `IFDExtractor`:

- Run `IFDExtractor.class` with dependencies

```
# MacOS separates path by :, Windows separates by ;
# Version
java -cp "bin:lib/*:" com.integratedgraphics.extractor.IFDExtractor -v

# Manual
java -cp "bin:lib/*:" com.integratedgraphics.extractor.IFDExtractor -h

# Run the extractor
java -cp "./bin:./lib/commons-compress-1.22.jar:./lib/commons-io-2.11.0.jar:./lib/CDK-SwingJS.jar:./lib/JmolDataD.jar" com.integratedgraphics.extractor.IFDExtractor -test dryad -T c:/temp/dryad/ -S c:/temp/dryad/mcvdnckbb/dataset.zip -D mcvdnckbb -debug

java -cp "./bin:./lib/commons-compress-1.22.jar:./lib/commons-io-2.11.0.jar:./lib/CDK-SwingJS.jar:./lib/JmolDataD.jar" com.integratedgraphics.extractor.IFDExtractor -test acs -T c:/temp/acs/ -D acs.joc.0c00770
java -cp "./bin:./lib/*.jar" com.integratedgraphics.extractor.IFDExtractor -test acs -T c:/temp/acs/ -D acs.joc.0c00770

jar --create --file extractor.jar --main-class com.integratedgraphics.extractor.IFDExtractor -C bin .
java -cp "extractor.jar:./lib/commons-compress-1.22.jar:./lib/commons-io-2.11.0.jar:./lib/CDK-SwingJS.jar:./lib/JmolDataD.jar" com.integratedgraphics.extractor.IFDExtractor -test acs -T c:/temp/acs/ -D acs.joc.0c00770
```

- Run `IFDExtractor.jar` (already including dependencies)

```
# In IUPAC-FAIRSPEC root directory
# Manual
java -jar dist/IFDExtractor.jar -h

# Version
java -jar dist/IFDExtractor.jar -v

# Run the extractor

java -jar dist/IFDExtractor.jar -W -test icl -T c:/temp/icl/ -D 10.14469/hpc/14635 -X src/main/resources/com/integratedgraphics/extractor/extract/ImperialCollege/IFD-extract.json

java -jar dist/IFDExtractor.jar -test dryad -T c:/temp/dryad/ -S c:/temp/dryad/f7m0cfz7t/dataset.zip -D f7m0cfz7t

java -jar dist/IFDExtractor.jar -test dryad -T c:/temp/dryad/ -S c:/temp/dryad/mcvdnckbb/dataset.zip -D mcvdnckbb

java -jar dist/IFDExtractor.jar -test acs -T c:/temp/acs/ -D acs.joc.0c00770
```