IFDExtractor.jar is a Java executable that takes as input an "IUPAC FAIRSpec-Ready" 
aggregation of files and produces an IUPAC FAIRSpec Collection (a zip file) with 
associated IUPAC FAIRSpec Finding Aid and HTML landing page. 

It does this by parsing the list of files in the archive, applying a regex-based 
"IFD extraction template" (example IFD.extract-test.json provided here) to those file paths 
in order to produce an internal Java-based
model of the collection, with "samples", "structures", and "spectra", 
and various associations of those, such as "compounds" (which are structure/spectra associations).

Input can be a remote compressed file (from FigShare in this example) or 
a local file (zip, tar, tar.gz, tgz, rar), or a local file directory.

The key aspects of this include:

a) A "reasonably structured" aggregation organized, for example, along the lines of
"compounds" and including subdirectories for spectroscopic types (IR, NMR, RAMAN, MS, etc.) 
and subtypes (1H, 13C, COSY, HMQS, etc.)
 
b) A matching JSON extraction template file ("IFD-extract-test.json" here)  

command-line format:
 
java -jar IFDExtractor.jar [JSON template] [optional sourceArchive] [optional targetDir] [flags]

where

[JSON template] is the IFD extraction template for this collection ("IFD-extract.json", perhaps)
[sourceArchive] is a LOCAL source .zip, .tar.gz, or .tgz file if that exists
[targetDir] is the target directory for the collection (default "site")

[flags] are one or more of:

-addPublicationMetadata (only for post-publication-related collections; include ALL Crossref or DataCite metadata)
-byID (order compounds by ID, not by index; overrides IFD_extract.json setting)
-dataciteDown (only for post-publication-related collections)
-debugging (lots of messages)
-debugReadonly (readonly, no publicationmetadata)
-findingAidOnly (only create a finding aid)
-nolaunch (don't launch the landing page)
-noclean (don't empty the destination collection directory before extraction; allows additional files to be zipped)
-noignored (don't include ignored files -- treat them as REJECTED)
-nolandingPage (don't create a landing page)
-nopubinfo (ignore all publication info)
-nostopOnFailure (continue if there is an error)
-nozip (don't zip up the target directory)
-readonly (just create a log file)
-requirePubInfo (throw an error is datacite cannot be reached; post-publication-related collections only)


This is all in beta testing; plenty of it may not work as expected.
Contact Bob Hanson (hansonr@stolaf.edu) if you want to try it out.

