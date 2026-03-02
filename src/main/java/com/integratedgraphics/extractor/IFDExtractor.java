package com.integratedgraphics.extractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.test.ExtractorTestACS;
import com.integratedgraphics.test.ExtractorTestDryad;

/**
 * This class interprets command line options and starts the appropriate
 * program. The primary subclass of FindingAidCreator is now IFDExtractorMain.
 * By removing this loader class from that stack, we allow a cleaner, faster
 * loading from the command line and can generate less extraneous output.
 * 
 * @author Fay Nguyen
 *
 */
class IFDExtractor {

	public static final String version = "0.1.2+2026.02.17";
	// 2026.01.25 version 0.1.2-beta refactored and much improved; adds CLI and
	// timestamp-merging of NMR data
	// 2025.07.24 version 0.1.0-beta with FAIRSpec-ready paper
	// 2025.02.17 version 0.0.7-beta integrates the crawler
	// 2024.12.02 version 0.0.6 fully refactored, revised; adds creation of landing
	// page and -nolandingpage -nolaunch flags
	// 2024.11.03 version 0.0.6 adding support for DOICrawler
	// 2024.05.28 version 0.0.5 moved to com.integratedgraphics.extractor.Extractor
	// 2023.01.09 version 0.0.4 adds MNova_Page_Header parameter
	// 2023.01.07 version 0.0.4 adds CDX reading by Jvalmol
	// 2023.01.01 version 0.0.4 accepts structures automatically from ./structures/
	// and ./structures.zip
	// 2022.12.30 version 0.0.4 ACS 0-7 with structures; fixing rezip issue of
	// Bruker files placed in _IFD.ignored.json
	// 2022.12.29 version 0.0.4 ACS 0-4 with structures; fixing *-* Regex for ACS#4
	// acs.orglett.0c00788
	// 2022.12.27 version 0.0.4 ACS 0-2 working
	// 2022.12.27 version 0.0.4 introduces FAIRSpecCompoundAssociation
	// 2022.12.23 version 0.0.4 fixes from ACS testing, Bruker directories with
	// multiple numbered subdirectories adds "-<n>" to the id
	// 2022.12.14 version 0.0.4 allows for local directory parsing (no zip or
	// tar.gz)
	// 2022.12.13 verison 0.0.4 adds "EXIT" and comment-only "..." for
	// IFD-extract.json
	// 2022.12.10 version 0.0.4 adds CDXML reading by Jmol and conversion of CIF to
	// PNG along with Jmol 15.2.82 fixes for V3000 and XmlChemDrawReader
	// 2022.12.01 version 0.0.4 fixes multi-page MNova with compound association
	// (ACS 22567817#./extract/acs.joc.0c00770)
	// 2022.11.29 version 0.0.4 allows for a representation to be both a structure
	// and a data object
	// 2022.11.27 version 0.0.4 adds parameters from a Metadata file as XLSX or ODS
	// 2022.11.23 version 0.0.3 fixes missing properties in NMR; upgrades to
	// double-precision Jmol-SwingJS JmolDataD.jar
	// 2022.11.21 version 0.0.3 fixes minor details; ICL.v6, ACS.0, ACS.5 working
	// adds command-line arguments, distinguishes REJECTED and IGNORED
	// 2022.11.17 version 0.0.3 allows associations "byID"
	// 2022.11.14 version 0.0.3 "compound identifier" as organizing association
	// 2022.06.09 MNovaMetadataReader CDX export fails due to buffer pointer error.

	List<String> cliExtractorFlagList = new ArrayList<>();

	boolean cliIsCrawler = false;
	boolean runningSchemaValidation = false;
	private String schemaFile;

	String cliLocalSourceArchivePath = null;

	String cliDOI = null;
	String cliDataDOI = null;
	String cliDataURL = null;
	String cliPubDOI = null;
	String cliPubURL = null;
	String cliTargetDir = null;
	String cliSource = "dryad";

	private String cliExtractFilePath;

	private Options getOptions() {
		Options options = new Options();

		// help -h
		newOption(options, 'h', "help", "Get help for commands.");

		// version -v
		newOption(options, 'v', "version", "Get the current version of the IFD Extractor.");

		// Required options

		// targetDirectory -T
		newOptionVal(options, 'T', "targetDir", "Target output directory for the finding aid", "TARGET_DIR", false);

		// test
		newOptionVal(options, '\0', "test", "For testing purpose (-test [dryad|acs|icl])", "SOURCE", false);

		// Optional options

		// localSourceArchive -S
		newOptionVal(options, 'S', "localSource", "Local Source Archive", "LOCAL_SOURCE_PATH", false);

		// IFDExtract.json file -X
		newOptionVal(options, 'X', "IFDExtractFile", "Input IFD-extract.json configuration file, if used",
				"IFD_EXTRAC_FILE", false);


		// debug
		newOption(options, '\0', "debug", "This will print out all debugging messages");

		// validate
		newOption(options, '\0', "validate", "This will run the schema validation");

		// schema
		newOptionVal(options, '\0', "schema", "Optional file location for schema; implies --validate", "SCHEMA", false);

		// DOI -D
		newOptionVal(options, 'D', "doi", "test DOI", "DOI", false);

		// dataDOI
		newOptionVal(options, '\0', "dataDOI", "data DOI", "DATADOI", false);

		// dataURL
		newOptionVal(options, '\0', "dataURL", "data URL", "DATAURL", false);

		// pubDOI
		newOptionVal(options, '\0', "pubDOI", "publication DOI", "PUBDOI", false);

		// pubURL
		newOptionVal(options, '\0', "pubURL", "publication URL", "PUBURL", false);

		// assetsonly -a
		newOption(options, 'a', "assetsOnly", "assets only; JavaScript/HTML has changed, so just rebuild website; don't actually process any files");

		// addPublicationMetadata -A
		newOption(options, 'A', "addPublicationMetadata",
				"Include ALL Crossref or DataCiteOnly for post-publication-related collections; in metadata.");

		// noclean -c
		newOption(options, 'c', "noClean",
				"Don't empty the destination collection directory before extraction; allows additional files to be zipped");

		// dataciteDown -C
		newOption(options, 'C', "dataciteDown", "Only for post-publication-related collections.");

		// embedpdf -E
		newOption(options, 'E', "embedPdf",
				"Loads PDF documents into finding aids for cross-domain viewing of spectra");

		// findingAidOnly -F
		newOption(options, 'F', "findingAidOnly", "Only create a finding aid");

		// nolandingPage -g
		newOption(options, 'g', "noLandingPage", "Don't create a landing page");

		// noignored -i
		newOption(options, 'i', "noIgnored", "Don't include ignored files -- treat them as REJECTED");

		// nolaunch -l
		newOption(options, 'l', "noLaunch", "Don't launch the landing page");

		// insitu -N
		newOption(options, 'N', "insitu",
				"Creates a self-contained finding aid pointing to the original data with no collection");

		// readonly -O
		newOption(options, 'O', "readOnly", "Just create a log file");

		// extractspecproperties -P
		newOption(options, 'P', "extractSpecProperties", "For crawler: Extract spectra properties");

		// debugReadonly -R
		newOption(options, 'R', "debugReadonly", "Readonly, no publication metadata");

		// nostopOnFailure -s
		newOption(options, 's', "noStopOnFailure", "Continue if there is an error");

		// nodownload -x
		newOption(options, 'x', "noDownload", "For crawler, do not download files from the repository; for extractor download data files only if necessary");

		// crawl -W
		newOption(options, 'W', "crawler", "Run the crawler");

		// addifdtypes -Y
		newOption(options, 'Y', "addIfdTypes", "Add IFD Types");

		// nozip -z
		newOption(options, 'z', "noZip", "Don't zip up the target directory");

		// analyze only
		newOption(options, '\0', "analyzeOnly", "Analyze the dataset only -- no finding aid or FAIRSpec collection creation");


		// Group for public information
		OptionGroup pubInfoGroup = new OptionGroup();

		// requirePubInfo -I
		OptionBuilder.withLongOpt("requirePubInfo");
		OptionBuilder.withDescription(
				"Throw an error is datacite cannot be reached; post-publication-related collections only");
		pubInfoGroup.addOption(OptionBuilder.create("I"));

		// nopubinfo -p
		OptionBuilder.withLongOpt("noPubInfo");
		OptionBuilder.withDescription("Ignore all publication info");
		pubInfoGroup.addOption(OptionBuilder.create("p"));
		options.addOptionGroup(pubInfoGroup);

		return options;
	}

	private static void newOption(Options options, char c, String name, String desc) {
		newOptionVal(options, c, name, desc, null, false);
	}

	private static void newOptionVal(Options options, char c, String name, String desc, String argName,
			boolean isRequired) {
		OptionBuilder.withLongOpt(name);
		OptionBuilder.withDescription(desc);
		if (isRequired)
			OptionBuilder.isRequired();
		if (argName != null) {
			OptionBuilder.withArgName(argName);
			OptionBuilder.hasArg(true);
		}
		if (c == 0)
			options.addOption(OptionBuilder.create(null));
		else
			options.addOption(OptionBuilder.create(c));
	}

	private void checkOptions(CommandLine line, Options options) {
		if (line.hasOption("h")) {
			helpManual(options);
			return;
		}

		// file options

		cliDOI = line.getOptionValue("D");
		cliDataDOI = line.getOptionValue("dataDOI");
		cliDataURL = line.getOptionValue("dataURL");
		cliPubDOI = line.getOptionValue("pubDOI");

		cliTargetDir = line.getOptionValue("T");
		schemaFile = line.getOptionValue("schema");

		if (line.hasOption("X")) {
			cliExtractFilePath = line.getOptionValue("X");
		}

		if (line.hasOption("S")) {
			cliLocalSourceArchivePath = line.getOptionValue("S");
		}

		// check special test options

		String testCase = line.getOptionValue("test");
		if (testCase != null)
			testCase = testCase.toLowerCase();

		if ("icl".equals(testCase) || line.hasOption("W")) {
			cliIsCrawler = true;
			if (!"icl".equals(testCase)) {
				throw new RuntimeException("Error: Crawler only works with --test ICL.");
			}
		}
		if (testCase != null) {
			if (cliDOI == null) {
				throw new RuntimeException("Test cases require a DOI using --doi or -D");
			}
			switch (testCase) {
			case "acs":
				break;
			case "dryad":
				if (!line.hasOption("S")) {
					throw new RuntimeException(
							"Error: Require dryad dataset local source archive using --localSource.");
				}
				break;
			case "icl":
				if (!cliIsCrawler) {
					throw new RuntimeException("Error: This source only works with --crawler.");
				}
				break;
			default:
				throw new RuntimeException("Error: Invalid source: " + testCase);
			}
			cliSource = testCase;
		}

		// add all other flags to cliExtractorFlagList

		if (line.hasOption("a")) {
			cliExtractorFlagList.add("-assetonly");
		}
		if (line.hasOption("A")) {
			cliExtractorFlagList.add("-addpublicationmetadata");
		}

//			if(line.hasOption("B")) {
//				extractorFlagList.add("-byid");
//			}
//			
		if (line.hasOption("c")) {
			cliExtractorFlagList.add("-noclean");
		}
		if (line.hasOption("C")) {
			cliExtractorFlagList.add("-datacitedown");
		}
		if (line.hasOption("debug")) {
			cliExtractorFlagList.add("-debugging");
		}
		if (line.hasOption("E")) {
			cliExtractorFlagList.add("-embedpdf");
		}
		if (line.hasOption("F")) {
			cliExtractorFlagList.add("-findingaidonly");
		}
		if (line.hasOption("g")) {
			cliExtractorFlagList.add("-nolandingpage");
		}
		if (line.hasOption("i")) {
			cliExtractorFlagList.add("-noignored");
		}
		if (line.hasOption("l")) {
			cliExtractorFlagList.add("-nolaunch");
		}
		if (line.hasOption("N")) {
			cliExtractorFlagList.add("-insitu");
		}
		if (line.hasOption("O")) {
			cliExtractorFlagList.add("-readonly");
		}
		if (line.hasOption("p")) {
			cliExtractorFlagList.add("-nopubinfo");
		}
		if (cliIsCrawler && line.hasOption("P")) {
			cliExtractorFlagList.add("-extractspecproperties");
		}
		if (line.hasOption("R")) {
			cliExtractorFlagList.add("-readonly");
		}
		if (line.hasOption("I")) {
			cliExtractorFlagList.add("-requirepubinfo");
		}
		if (line.hasOption("s")) {
			cliExtractorFlagList.add("-nostoponfailure");
		}
		if (cliIsCrawler && line.hasOption("x")) {
			cliExtractorFlagList.add("-nodownload");
		}
		if (line.hasOption("Y")) {
			cliExtractorFlagList.add("-addifdtypes");
		}
		if (line.hasOption("z")) {
			cliExtractorFlagList.add("-nozip");
		}
		if (schemaFile != null || line.hasOption("validate")) {
			runningSchemaValidation = true;
		}
	}

	// print out the help manuals
	private void helpManual(Options options) {
		String header = "\nFAIRSPec Finding Aid CLI manual version " + version + "\n" //
				+ "Using IFD-extract.json: java -jar IFDExtractor.jar " //
				+ "--IFDExtractFile <IFD-extract.json file>" //
				+ "--targetDir <TARGET_DIR>" //
				+ "\n" //
				+ "Crawler: java -jar IFDExtractor.jar --crawler " //
				+ ICLDOICrawler.syntaxString //
				+ ExtractorTestDryad.syntaxString //
				+ ExtractorTestACS.syntaxString //
				+ "Manual: java -jar IFDExtractor.jar " //
				+ "--help\n" //
				+ "Options list:\n\n\n"; //
		String footer = "\nPlease report issues at https://github.com/IUPAC/IUPAC-FAIRSpec/issues";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("IFDExtractor", header, options, footer, true);
	}

	boolean parseCommandLine(String[] args) {
		Options options = getOptions();
		// No arguments
		if (args.length == 0) {
			helpManual(options);
			return false;
		}
		// Argument for manual
		if (args.length == 1) {
			if (args[0].equals("-h") || args[0].equals("--help")) {
				helpManual(options);
				return false;
			} else if (args[0].equals("-v") || args[0].equals("--version")) {
				System.out.println("IFDExractor version " + version);
				return false;
			} else {
				System.out.println("Please include required arguments");
				helpManual(options);
				return false;
			}
		}
		CommandLineParser parser = new PosixParser();
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			checkOptions(line, options);
			return true;
		} catch (Exception e) {
			// Handle cases where the required options are not provided
			// Handle cases where the input doesn't match the defined options
			System.err.println("Error parsing arguments: " + e.getMessage());
			return false;
		}
	}

	private void load() throws Exception {

		String ifdExtractFilePath = cliExtractFilePath; // -X option
		String localArchivePath = cliLocalSourceArchivePath; // -S option
		String targetDir = cliTargetDir; // -T option

		// Include slash at the end of the output directory
		targetDir = ExtractorUtils.fixPath(targetDir, true);
		// Special case with crawler
		if (cliIsCrawler && cliSource.equals("icl")) {
			List<String> crawlerArgs = new ArrayList<String>();
			crawlerArgs.add(cliDOI);
			crawlerArgs.add(targetDir);
			for (String arg : cliExtractorFlagList) {
				crawlerArgs.add(arg);
			}
			DOICrawler crawler = new DOICrawler(crawlerArgs.toArray(new String[0]));
			crawler.setCustomizer(new ICLDOICrawler(crawler));
			crawler.crawl();
			targetDir = crawler.targetPath.getAbsolutePath();
		} else {
			if (targetDir == null)
				targetDir = "./" + cliDOI;
			if (cliDOI != null)
				targetDir += cliDOI.toLowerCase() + "_out/";

			// Handle the extract file path

			// these next are for Eclipse testing

			// note that "./" now stands for the RESOURCE in
			// com/integrategraphics/extractor/

			switch (cliSource == null ? "" : cliSource) {
			case "dryad":
				if (ifdExtractFilePath == null)
					ifdExtractFilePath = "./extract/dryad/" + cliDOI + "/IFD-extract.json";
				break;
			case "acs":
				// for acs include the whole doi acs.*.XXXXX
				if (ifdExtractFilePath == null) {
					ifdExtractFilePath = "./extract/" + cliDOI + "/IFD-extract.json";
				}
				// Exception handling
				switch (cliDOI) {
				case "acs.orgLett9b02307":
					if (localArchivePath == null) {
						throw new RuntimeException(
								"Download NMR.rar from https://www.repository.cam.ac.uk/bitstreams/983933fe-6c07-4793-bf32-0d715d2d9087/download,\nrename it into acs.orglett.9b02307.NMR.rar,\nand pass the path of the folder containing this RAR file to the flag -S.");
					}
					localArchivePath = new File(localArchivePath).getAbsolutePath();
					new IFDExtractorMain().run(new File(ifdExtractFilePath).getAbsoluteFile(),
							new File(targetDir).getAbsoluteFile(), localArchivePath);
					return;
				default:
					if (cliDOI.startsWith("[")) {
						ExtractorTestACS.runSet(cliDOI, localArchivePath, targetDir);
						return;
					}
					break;
				}
				break;
			default:
				break;
			}
			if (ifdExtractFilePath != null) {
				new IFDExtractorMain().runExtraction(ifdExtractFilePath, localArchivePath, targetDir, null,
						String.join(" ", cliExtractorFlagList));
			}
		}
		if (cliDOI != null && runningSchemaValidation) {
			// If the syntax has -schema flag, run schema validation on the finding aid
			// generated
			String[] ret = { null };
			boolean ok = vaidateFindingAid(targetDir, cliDOI, ret);
			System.out.println(ret[0]);
			if (ok) {
				System.out.printf("Validation SUCCESSFUL for %s\n", targetDir);
			} else {
				System.out.printf("Validation FAILED for %s\n", targetDir);

			}
		}
	}

	/**
	 * Validate a FAIRSpec Finding Aid.
	 * 
	 * Requires check-jsonschema installed and allowed on command line.
	 * 
	 * pip install check-jsonschema
	 * 
	 * Syntax: check-jsonschema --verbose --schemafile <schemaPath> <findingAidPath>
	 * -o text;
	 * 
	 * @param targetDir
	 * @param ret       text return
	 * @return true if valid; false if not
	 * 
	 */
	private boolean vaidateFindingAid(String targetDir, String name, String[] ret) {
		name = ExtractorUtils.pathToFileName(name);
		targetDir = ExtractorUtils.fixPath(targetDir, true);
		String findingAidPath = targetDir + "IFD.findingaid.json";
		String schemaPath = targetDir + "IFD.findingaid.schema.json";
		boolean ok = FAIRSpecUtilities.validateFindingAid(targetDir, findingAidPath, schemaPath, ret);
		String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		Path successLogPath = Paths.get(targetDir + name + "_schema_valid.txt");
		Path errorLogPath = Paths.get(targetDir + name + "_schema_error.txt");
		Path p = null;
		try {
			p = errorLogPath;
			Files.deleteIfExists(p);
			p = successLogPath;
			Files.deleteIfExists(p);
			if (ok) {
				Files.createFile(p);
				FAIRSpecUtilities.appendToFile(p, "Schema validation run on: " + timeStamp + "\n");
				FAIRSpecUtilities.appendToFile(p, ret[0]);
			} else {
				p = errorLogPath;
				Files.createFile(p);
				FAIRSpecUtilities.appendToFile(p, "Schema validation run on: " + timeStamp + "\n");
				FAIRSpecUtilities.appendToFile(p, ret[0]);
			}
		} catch (Exception e) {
			System.out.println("Could not write to file " + p);
		}
		return ok;
	}

	/**
	 * See above for explanation of parameters and options.
	 * 
	 * There is no general API for creating finding aids. This interface is
	 * primarily for in-house testing.
	 * 
	 */
	private static void loadExtractor(String[] args) {
		try {
			IFDExtractor loader = new IFDExtractor();
			// If it just prints out the help or header, quit
			if (loader.parseCommandLine(args))
				loader.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		loadExtractor(args);
	}

}
