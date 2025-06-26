package com.integratedgraphics.extractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.extract.PropertyManagerI;

/**
 * Copyright 2021-2024 Integrated Graphics and Robert M. Hanson
 * 
 * A class to handle the extraction of objects from a "raw" dataset by
 * processing the full paths within a ZIP file as directed by an extraction
 * template (from the extract/ folder for the test)
 * 
 * following the sequence:
 * 
 * initialize(ifdExtractScriptFile)
 * 
 * setLocalSourceDir(sourceDir)
 * 
 * setCachePattern(pattern)
 * 
 * setRezipCachePattern(pattern)
 * 
 * extractObjects(targetDir);
 * 
 * Features:
 * 
 * 
 * ... uses template-directed processing of full file paths
 * 
 * ... metadata property information is from
 * org.iupac.common.fairspec.properties
 * 
 * ... allows for an XLSX or OpenSheets file that contains additional metadata
 * 
 * ... creates IFDFAIRSpecFindingAid objects ready for serialization
 * 
 * ... serializes using org.iupac.util.IFDDefaultJSONSerializer
 * 
 * ... zip files are processed recursively
 * 
 * ... zip files other than Bruker directories are unpacked
 * 
 * ... "broken" Bruker directories (those without a simple integer root path)
 * are corrected.
 * 
 * ... binary MNova files are scanned for metadata, PNG, and MOL files (only,
 * not spectra)
 * 
 * ... MNova metadata references page number in file using #page=
 * 
 * 
 * See superclasses for more information
 * 
 * @author hansonr
 *
 */
public class IFDExtractor extends IFDExtractorLayer3 {

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/extractor/IFDExtractor.java";

	// TODO: test rootpath and file lists for case with two root paths -- does it
	// make sense that that manifests are cleared?

	// TODO: update GitHub README.md



	private static final String debugFlags = "-stopAfter=end";

	public static final String PAGE_ID_PROPERTY_SOURCE = "*idf.property.compound.id.source*";

	protected static String getCommandLineHelp() {
		return "\nformat: java -jar IFDExtractor.jar [IFD-extract.json] [localSourceArchive] [targetDir] [extractorFlags]" //
				+ "\n" + "\nwhere" //
				+ "\n" //
				+ "\n[IFD-extract.json] is the IFD extraction template for this collection" //
				+ "\n[localSourceArchive] is the source .zip, .tar.gz, .tar, .tgz, or .rar file" //
				+ "\n[targetDir] is the target directory for the collection (which you are responsible to empty first)" //
				+ "\n" //
				+ "\n" + "[extractorFlags] are one or more of:" //
				+ "\n" //
				+ "\n-addPublicationMetadata (only for post-publication-related collections; include ALL Crossref or DataCite metadata)" //
				+ "\n-byID (order compounds by ID, not by index; overrides IFD_extract.json setting)"
				+ "\n-dataciteDown (only for post-publication-related collections)" //
				+ "\n-debugging (lots of messages)" //
				+ "\n-debugReadonly (readonly, no publicationmetadata)" //
				+ "\n-findingAidOnly (only create a finding aid)" //
				+ "\n-nolaunch (don't launch the landing page)" //
				+ "\n-noclean (don't empty the destination collection directory before extraction; allows additional files to be zipped)" //
				+ "\n-noignored (don't include ignored files -- treat them as REJECTED)" //
				+ "\n-nolandingPage (don't create a landing page)" //
				+ "\n-nopubinfo (ignore all publication info)" //
				+ "\n-nostopOnFailure (continue if there is an error)" //
				+ "\n-nozip (don't zip up the target directory)" //
				+ "\n-readonly (just create a log file)" //
				+ "\n-requirePubInfo (throw an error is datacite cannot be reached; post-publication-related collections only)"				
				+ "\n" + "\nor, to run the DOICrawler:"
				+ "\n" //
				+ "\n" + "\njava -jar IFDExtractor.jar -doi [DOI] [targetDir] [crawlerFlags]" //
				+ "\n" + "\nwhere" //
				+ "\n" //
				+ "\n[DOI] is a Document Object Identifier such as 10.14469/hpc/10386" //
				+ "\n[targetDir] is the target directory for the output" //
				+ "\n" //
				+ "\n" + "and [crawlerFlags] as above and also optionally:" //
				+ "\n" //
				+ "\n-download (additionally download files from the repository)" //
				;
	}

	public IFDExtractor() {
		initializeExtractor();
	}

	public void runExtraction(String ifdExtractFile, String localSourceArchive, String targetDir, String flags) {
		runExtraction(new String[] { ifdExtractFile, localSourceArchive, targetDir, flags });
	}

	public void runExtraction(String[] args) {

		System.out.println(Arrays.toString(args));
	    processFlags(args, debugFlags);
		String localSourceArchive = null;
		String targetDir = null;
		String ifdExtractJSONFilename;
		switch (args.length) {
		default:
		case 3:
			targetDir = args[2];
			//$FALL-THROUGH$
		case 2:
			localSourceArchive = args[1];
			if ("-".equals(localSourceArchive))
				localSourceArchive = null;
			//$FALL-THROUGH$
		case 1:
			ifdExtractJSONFilename = args[0];
			break;
		case 0:
			ifdExtractJSONFilename = null;
		}
		if (ifdExtractJSONFilename == null)
			throw new NullPointerException("No IFD-extract.json or test set?");
		if (targetDir == null)
			targetDir = "site";
		FAIRSpecUtilities.setLogging(targetDir + "/extractor.log");
		int failed = 0;
		logToSys("Extractor.runExtraction output to " + new File(targetDir).getAbsolutePath());
		// ./extract/ should be in the main Eclipse project directory.
		long t0 = System.currentTimeMillis();
		new File(targetDir).mkdirs();
		String flags = "\n" + dumpFlags() + "\n IFD version " + IFDConst.IFD_VERSION + "\n";
		String json = (readOnly ? null : "{\"findingaids\":[\".\"]}");
		try {
			File ifdExtractScriptFile = new File(ifdExtractJSONFilename).getAbsoluteFile();
			File targetPath = new File(targetDir).getAbsoluteFile();
			String sourcePath = (localSourceArchive == null ? null : new File(localSourceArchive).getAbsolutePath());
			run(ifdExtractScriptFile, targetPath, sourcePath);
			logToSys("Extractor.runExtraction ok ");
		} catch (Exception e) {
			failed = 1;
			logErr("Exception " + e, "runExtraction");
			e.printStackTrace();
		}
		String warnings = "";
		if (failed == 0 || !stopOnAnyFailure) {
			logToSys("!Extractor.runExtraction time/sec=" + (System.currentTimeMillis() - t0) / 1000.0);
			ifdExtractJSONFilename = null;
			if (this.warnings > 0) {
				warnings += "======== " + ": " + this.warnings + " warnings for " + targetDir + "\n" + strWarnings;
				try {
					FAIRSpecUtilities.writeBytesToFile((warnings).getBytes(),
							new File(targetDir + "/_IFD_warnings.txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		finalizeExtraction(json, 1, failed, -1, -1, flags);
		FAIRSpecUtilities.setLogging(null);
	}

	@Override
	public String processFlags(String[] args, String moreFlags) {
		String flags = super.processFlags(args, moreFlags);
		stopAfter = getFlagEquals(flags, "-stopafter");
		return flags;
	}

	public String dumpFlags() {
		String s = " stopOnAnyFailure = " + stopOnAnyFailure //
				+ "\n debugging = " + debugging //
				+ "\n readOnly = " + readOnly //
				+ "\n debugReadOnly = " + debugReadOnly //
				+ "\n allowNoPubInfo = " + !allowNoPubInfo //
				+ "\n noLandingPage = " + !createLandingPage //
				+ "\n noLaunch = " + !launchLandingPage //
				+ "\n skipPubInfo = " + skipPubInfo //
				+ "\n localSourceArchive = " + localSourceDir //
				+ "\n targetDir = " + targetDir //
				+ "\n createZippedCollection = " + createZippedCollection; //
		return s;
	}

	/**
	 * @return the FindingAid as a string
	 */
	public final String extractAndCreateFindingAid(File ifdExtractScriptFile, String localArchive, File targetPath)
			throws IOException, IFDException {

		this.targetPath = targetPath;

		// set up the extraction

		processPhase1(ifdExtractScriptFile, localArchive);
		FAIRSpecUtilities.refreshLog();

		checkStopAfter("1");

		// now actually do the extraction.

		processPhase2(targetPath);
		FAIRSpecUtilities.refreshLog();
		checkStopAfter("2");
	
		// finish up all processing
		return processPhase3();
	}

	public void finalizeExtraction(String json, int n, int failed, int nWarnings, int nErrors, String flags) {
		if (failed == 0) {
			try {
				if (json != null) {
					String dir = targetPath.getAbsolutePath().replace('\\','/');
					// TODO the problem is here. We have abolute paths.
					String s = FAIRSpecUtilities.rep(json, dir + "/", "./");
					File f = new File(dir + "/_IFD_findingaids.json");
					FAIRSpecUtilities.writeBytesToFile(s.getBytes(), f);
					logToSys("Extractor.runExtraction File " + f.getAbsolutePath() + " created ");
					f = new File(dir + "/_IFD_findingaids.js");
					FAIRSpecUtilities.writeBytesToFile(("IFD.findingAids=" + s).getBytes(), f);
					logToSys("Extractor.runExtraction File " + f.getAbsolutePath() + " created \n" + json);
				} else {
					logToSys("Extractor.runExtraction _IFD_findingaids.json was not created for\n" + json);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (nWarnings == -1)
			nWarnings = warnings;
		if (nErrors == -1)
			nErrors = errors;
		if (nWarnings > 0) {
			try {
				FAIRSpecUtilities.writeBytesToFile((warnings + nWarnings + " warnings\n" + strWarnings).getBytes(),
						new File(targetDir + "/_IFD_warnings.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logToSys("");
		System.err.flush();
		System.out.flush();
		System.err.println(errorLog);
		System.err.flush();
		System.out.flush();
		logToSys("!Extractor.runExtraction flags " + flags);
		logToSys("!Extractor " + (failed == 0 ? "done" : "failed") + " total=" + n + " failed=" + failed + " errors="
				+ nErrors + " warnings=" + nWarnings);
	}

	@Override
	public String getCodeSource() {
		return codeSource;
	}

	@Override
	public String getVersion() {
		return version;
	}

	public void run(File ifdExtractScriptFile, File targetPath, String localsourceArchive)
			throws IOException, IFDException {
		log("!Extractor\n ifdExtractScriptFile= " + ifdExtractScriptFile + "\n localsourceArchive = "
				+ localsourceArchive + "\n targetDir = " + targetPath.getAbsolutePath());
		
		if (assetsOnly && !insitu) {
			buildSite(targetPath);
			return;
		}
		
		String serializedFA = extractAndCreateFindingAid(ifdExtractScriptFile, localsourceArchive, targetPath);
		if (serializedFA == null) {
			if (!allowNoPubInfo) {
				throw new IFDException("Extractor failed");
			}
		} else if (createLandingPage) {
			buildLandingPage(serializedFA, targetPath);
		}

		log("!Extractor extracted " + lstManifest.size() + " files (" + lstManifest.getByteCount() + " bytes)"
				+ "; ignored " + lstIgnored.size() + " files (" + lstIgnored.getByteCount() + " bytes)" + "; rejected "
				+ lstRejected.size() + " files (" + lstRejected.getByteCount() + " bytes)");
	}

	private void buildLandingPage(String serializedFA, File targetPath) throws IOException {
		String dir = extractScriptFileDir;
		File htmlPath = (insitu && dir != null ? new File(dir) : targetPath);
		if (insitu)
			FAIRSpecUtilities.writeBytesToFile(serializedFA.getBytes(), new File(htmlPath, "IFD.findingaid.json"));
		buildSite(htmlPath);
	}

	/**
	 * Minimal command-line interface for now. There are several flags set from
	 * ExtractorTest. Right now these are not included in the options, and we also
	 * need to use proper -x or --xxxx flags.
	 * 
	 * Just haven't implemented that yet.
	 * 
	 * @param args [0] extractionFile.json, [1] sourcePath, [2] targetDir
	 * 
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			System.out.println(getCommandLineHelp());
			return;
		}
		if (args[0].equals("-doi")) {
			// entry point for DOICrawler
			args[0] = null;
			DOICrawler.main(args);
			return;
		}
		// just run one IFD-extract.json
		new IFDExtractor().runExtraction(args);
	}

}
