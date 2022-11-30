IFDExtractor.jar is a Java executable that takes as input an "IUPAC FAIRSpec-Ready" 
aggregation of files and produces an IUPAC FAIRSpec Collection (a zip file) with 
associated IUPAC FAIRSpec Finding Aid. 

It does this by parsing the list of files in the archive, applying a regex-based 
"IFD extraction template" to those file paths to produce an internal Java-based
model of the collection, with "samples", "structures", "data objects", and various
associations of these. 

In v. 0.0.4, the input needs to be in the form of an a archive (zip or tgz or tar.gz).
But plans are to also allow a local file directory for v. 0.0.5 as well (simple enough).

The key aspects of this include:

a) A "reasonably structured" aggregation organized, for example, along the lines of
"compounds" and including subdirectories for spectroscopic types (IR, NMR, RAMAN, MS, etc.) 
and subtypes (1H, 13C, COSY, HMQS, etc.)
 
b) A matching JSON extraction template file ("IFD-extract.json" here)  
 
format: java -jar IFDExtractor.jar [JSON template] [sourceArchive] [targetDir] [flags]


where

[JSON template] is the IFD extraction template for this collection ("IFD-extract.json", perhaps)
[sourceArchive] is the source .zip, .tar.gz, or .tgz file
[targetDir] is the target directory for the collection

[flags] are one or more of:



		if (flags.indexOf("-addpublicationmetadata;") >= 0) {
			addPublicationMetadata = true;
		}

		if (flags.indexOf("-byid;") >= 0) {
			setExtractorFlag(FAIRSpecExtractorHelper.IFD_EXTRACTOR_FLAG_ASSOCIATION_BYID, "true");
		}


-addPublicationMetadata (only for post-publication-related collections)
-byID (associations will reference string identifiers -- for structures and spectra, for example -- not just array index numbers) [this is preferred for human readability]
-datacitedown (only for post-publication-related collections)
-debugging (lots of messages)
-debugreadonly (readonly, no publicationmetadata)
-noclean (don't empty the archive directory [generally the extractor will delete all the files first in the archive directory, which is created in the target directory])
-noignored (don't include ignored files [generally "ignored" files are still kept in the collection, just not mentioned in the finding aid)
-nopubinfo (skip all template publication information, adding no such metadata) [typically just for debugging]
-nostoponfailure (continue if there is an error) [typically just for debugging]
-nozip (don't zip up the target directory) [typically just for debugging]
-readonly (just create a log file) [typically just for debugging]
-requirepubinfo (throw an error if datacite cannot be reached; post-publication-related collections only)

