format: java -jar IFDExtractor.jar [IFD-extract.json] [sourceZip] [targetDir] [flags]


where

[IFD-extract.json] is the IFD extraction template for this collection
[sourceZip] is the source ZIP file
[targetDir] is the target directory for the collection (which you are responsible to empty first)

[flags] are one or more of:

-readonly (just create a log file)
-nozip (don't zip up the target directory)
-nostoponfailure (continue if there is an error)
-debugging (lots of messages)
-debugreadonly (readonly, no publicationmetadata)
-addPublicationMetadata (only for post-publication-related collections)
-datacitedown (only for post-publication-related collections)
-requirepubinfo (throw an error is datacite cannot be reached; post-publication-related collections only)