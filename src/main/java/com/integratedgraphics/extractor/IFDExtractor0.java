package com.integratedgraphics.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;


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
 * ... allow CLI command
 * 
 * See superclasses for more information
 * 
 * @author hansonr
 *
 */
public class IFDExtractor0 {

	protected static class CLI {

		List<String> cliExtractorFlagList = new ArrayList<>();

		boolean cliIsCrawler = false;

		String cliLocalSourceArchivePath = null;

		String cliDOI = null;
		String cliTargetDir = null;
		String cliSource = "dryad";

		private String cliExtractFilePath;

		private Options getOptions() {
			Options options = new Options();

			// help -h
			OptionBuilder.withLongOpt("help");
			OptionBuilder.withDescription("Get help for commands.");
			options.addOption(OptionBuilder.create("h"));

			// version -v
			OptionBuilder.withLongOpt("version");
			OptionBuilder.withDescription("Get the current version of the IFD Extractor.");
			options.addOption(OptionBuilder.create("v"));

			// Required options

			// targetDirectory -T
			OptionBuilder.withLongOpt("targetDir");
			OptionBuilder.withDescription("Target output directory for the finding aid");
			OptionBuilder.isRequired();
			OptionBuilder.hasArg(true);
			OptionBuilder.withArgName("TARGET_DIR");
			options.addOption(OptionBuilder.create("T"));

			// -test
			OptionBuilder.withLongOpt("test");
			OptionBuilder.withDescription("For testing purpose (-test [dryad|acs|icl])");
			OptionBuilder.hasArg(true);
			OptionBuilder.withArgName("SOURCE");
			options.addOption(OptionBuilder.create(null));

			// -debug 
			OptionBuilder.withLongOpt("debug");
			OptionBuilder.withDescription("This will print out all debugging messages");
			options.addOption(OptionBuilder.create(null));

			// Optional options

			// assetsonly -a
			OptionBuilder.withLongOpt("assetsOnly");
			OptionBuilder.withDescription("Asset Only");
			options.addOption(OptionBuilder.create("a"));

			// addPublicationMetadata -A
			OptionBuilder.withLongOpt("addPublicationMetadata");
			OptionBuilder.withDescription(
					"Include ALL Crossref or DataCiteOnly for post-publication-related collections; in metadata.");
			options.addOption(OptionBuilder.create("A"));

// byID is deprecated -- always byID true		    
//		    //byID -B
//		    OptionBuilder.withLongOpt("byID");
//			OptionBuilder.withDescription("Order compounds by ID, not by index; overrides IFD_extract.json setting");
//		    options.addOption(OptionBuilder.create("B"));
//		    
			// noclean -c
			OptionBuilder.withLongOpt("noClean");
			OptionBuilder.withDescription(
					"Don't empty the destination collection directory before extraction; allows additional files to be zipped");
			options.addOption(OptionBuilder.create("c"));

			// dataciteDown -C
			OptionBuilder.withLongOpt("dataciteDown");
			OptionBuilder.withDescription("Only for post-publication-related collections.");
			options.addOption(OptionBuilder.create("C"));

			// DOI -D
			OptionBuilder.withLongOpt("doi");
			OptionBuilder.withDescription("DOI/Identifier.");
			OptionBuilder.hasArg(true);
			OptionBuilder.withArgName("DOI");
			options.addOption(OptionBuilder.create("D"));

			// embedpdf -E
			OptionBuilder.withLongOpt("embedPdf");
			OptionBuilder.withDescription("Loads PDF documents into finding aids for cross-domain viewing of spectra");
			options.addOption(OptionBuilder.create("E"));

			// findingAidOnly -F
			OptionBuilder.withLongOpt("findingAidOnly");
			OptionBuilder.withDescription("Only create a finding aid");
			options.addOption(OptionBuilder.create("F"));

			// nolandingPage -g
			OptionBuilder.withLongOpt("noLandingPage");
			OptionBuilder.withDescription("Don't create a landing page");
			options.addOption(OptionBuilder.create("g"));

			// noignored -i
			OptionBuilder.withLongOpt("noIgnored");
			OptionBuilder.withDescription("Don't include ignored files -- treat them as REJECTED");
			options.addOption(OptionBuilder.create("i"));

			// nolaunch -l
			OptionBuilder.withLongOpt("noLaunch");
			OptionBuilder.withDescription("Don't launch the landing page");
			options.addOption(OptionBuilder.create("l"));

			// insitu -N
			OptionBuilder.withLongOpt("insitu");
			OptionBuilder.withDescription(
					"Setting insitu true generates an entirely self-contained finding aid, without local files and any rezipping in the origin directory.");
			options.addOption(OptionBuilder.create("N"));

			// readonly -O
			OptionBuilder.withLongOpt("readOnly");
			OptionBuilder.withDescription("Just create a log file");
			options.addOption(OptionBuilder.create("O"));

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

			pubInfoGroup.setRequired(false);
			options.addOptionGroup(pubInfoGroup);

			// extractspecproperties -P
			OptionBuilder.withLongOpt("extractSpecProperties");
			OptionBuilder.withDescription("For crawler: Extract spectra properties");
			options.addOption(OptionBuilder.create("P"));

			// debugReadonly -R
			OptionBuilder.withLongOpt("debugReadonly");
			OptionBuilder.withDescription("Readonly, no publication metadata");
			options.addOption(OptionBuilder.create("R"));

			// nostopOnFailure -s
			OptionBuilder.withLongOpt("noStopOnFailure");
			OptionBuilder.withDescription("Continue if there is an error");
			options.addOption(OptionBuilder.create("s"));

			// localSourceArchive -S
			OptionBuilder.withLongOpt("localSource");
			OptionBuilder.withDescription("Local Source Archive");
			OptionBuilder.hasArg(true);
			OptionBuilder.withArgName("LOCAL_SOURCE_PATH");
			options.addOption(OptionBuilder.create("S"));

			// nodownload -x
			OptionBuilder.withLongOpt("noDownload");
			OptionBuilder.withDescription("For crawler only: do not download files from the repository");
			options.addOption(OptionBuilder.create("x"));

			// IFDExtract.json file -X
			OptionBuilder.withLongOpt("IFDExtractFile");
			OptionBuilder.withDescription("Input IFD-extract.json configuration file, if used");
			OptionBuilder.hasArg(true);
			OptionBuilder.withArgName("IFD_EXTRAC_FILE");
			options.addOption(OptionBuilder.create("X"));

			// crawl -W
			OptionBuilder.withLongOpt("crawler");
			OptionBuilder.withDescription("Run the crawler");
			options.addOption(OptionBuilder.create("W"));

			// addifdtypes -Y
			OptionBuilder.withLongOpt("addIfdTypes");
			OptionBuilder.withDescription("Add IFD Types");
			options.addOption(OptionBuilder.create("Y"));

			// nozip -z
			OptionBuilder.withLongOpt("noZip");
			OptionBuilder.withDescription("Don't zip up the target directory");
			options.addOption(OptionBuilder.create("z"));

			return options;
		}

		private void checkOptions(CommandLine line, Options options) {
			if (line.hasOption("h")) {
				helpManual(options);
				return;
			}

			// file options

			cliDOI = line.getOptionValue("o");
			cliTargetDir = line.getOptionValue("T");
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

			if (line.hasOption("W")) {
				cliIsCrawler = true;
				if (testCase == null || !testCase.equals("icl")) {
					throw new RuntimeException("Error: Crawler only works with --test ICL.");
				}
			}
			if (testCase != null) {
				switch (testCase) {
				case "acs":
					break;
				case "dryad":
					if (!line.hasOption("S")) {
						System.err.print("Error: Require dryad dataset local source archive using --localSource.");
						throw new RuntimeException(
								"Error: Require dryad dataset local source archive using --localSource.");
					}
					break;
				case "icl":
					if (!cliIsCrawler) {
						System.err.print("Error: The ICL source only works with --crawler.");
						throw new RuntimeException("Error: This source only works with crawler.");
					}
					break;
				default:
					System.err.print("Error: Invalid source: " + testCase);
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
			if (line.hasOption("D")) {
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
		}

		// print out the help manuals
		private void helpManual(Options options) {
			String header = "\nFAIRSPec Finding Aid CLI manual version " + FindingAidCreator.version + "\n" //
					+ "Crawler: java -jar IFDExtractor.jar -W -test icl -T <TARGET_DIR> -o \"10.14469/hpc/XXXXX\" [other flags]\n" //
					+ "Dryad: java -jar IFDExtractor.jar -test dryad -T <TARGET_DIR> -S <LOCAL_SOURCE_ARCHIVE> -o \"12345\"\n" //
					+ "ACS: java -jar IFDExtractor.jar -test acs -T <TARGET_DIR> -S <LOCAL_SOURCE_ARCHIVE> -o 10.14469/hpc/XXXXX\n" //
					+ "Manual: java -jar IFDExtractor.jar -h/--help\nOptions list:\n\n\n"; //
			String footer = "\nPlease report issues at https://github.com/IUPAC/IUPAC-FAIRSpec/issues";
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("IFDExtractor", header, options, footer, true);
		}

		boolean parseCommandLine(String[] args) {
			Options options = getOptions();
			// No arguments
			if (args.length == 0) {
				System.out.println("Please include required arguments");
				helpManual(options);
				return false;
			}
			// Argument for manual
			if (args.length == 1) {
				if (args[0].equals("-h") || args[0].equals("--help")) {
					helpManual(options);
					return false;
				} else if (args[0].equals("-v") || args[0].equals("--version")) {
					System.out.println("Finding Aid Creator version " + FindingAidCreator.version);
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

	}

	private static void runCLI(CLI cli) throws Exception {

		String ifdExtractFilePath = cli.cliExtractFilePath; // -X option
		String localArchivePath = cli.cliLocalSourceArchivePath; // -S option
		String targetDir = cli.cliTargetDir; // -T option

		// Include slash at the end of the output directory
		if (!targetDir.endsWith("/")) {
			targetDir += "/";
		}
		// Special case with crawler
		if (cli.cliIsCrawler && cli.cliSource.equals("icl")) {
			List<String> crawlerArgs = new ArrayList<String>();
			crawlerArgs.add(cli.cliDOI);
			crawlerArgs.add(targetDir);
			for (String arg : cli.cliExtractorFlagList) {
				crawlerArgs.add(arg);
			}
			DOICrawler crawler = new DOICrawler(crawlerArgs.toArray(new String[0]));
			crawler.setCustomizer(new ICLDOICrawler(crawler));
			crawler.crawl();
			return;
		}

		targetDir += cli.cliDOI.toLowerCase() + "_out/";

		// Handle the extract file path
		
		// these next are for Eclipse testing
		
		// note that "./" now stands for the RESOURCE in com/integrategraphics/extractor/

		switch (cli.cliSource) {
		case "dryad":
			if (ifdExtractFilePath == null)
				ifdExtractFilePath = "./extract/dryad/" + cli.cliDOI + "/IFD-extract.json";
			break;
		case "acs":
			// for acs include the whole doi acs.*.XXXXX
			if (ifdExtractFilePath == null)
				ifdExtractFilePath = "./extract/" + cli.cliDOI + "/IFD-extract.json";
			IFDExtractorImpl extractor = new IFDExtractorImpl();
			extractor.processFlags(cli.cliExtractorFlagList.toArray(new String[0]), "");
			// Exception handling
			switch (cli.cliDOI) {
			case "acs.orgLett9b02307":
				if (localArchivePath == null) {
					throw new RuntimeException(
							"Download NMR.rar from https://www.repository.cam.ac.uk/bitstreams/983933fe-6c07-4793-bf32-0d715d2d9087/download,\nrename it into acs.orglett.9b02307.NMR.rar,\nand pass the path of the folder containing this RAR file to the flag -S.");
				}
				localArchivePath = new File(localArchivePath).getAbsolutePath();
				new IFDExtractorImpl().run(new File(ifdExtractFilePath).getAbsoluteFile(),
						new File(targetDir).getAbsoluteFile(), localArchivePath);
				return;
			default:
				break;
			}
			break;
		default:
			break;
		}
		if (ifdExtractFilePath != null) {
			new IFDExtractorImpl().runExtraction(ifdExtractFilePath, localArchivePath, targetDir, null,
					String.join(" ", cli.cliExtractorFlagList));
		}

		// shouldn't we continue here?
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
			System.err.println("Require at least one variable");
			return;
		}
		IFDExtractor0.CLI cli = new IFDExtractor0.CLI();
		try {
			// If it just prints out the help or header, quit
			if (cli.parseCommandLine(args))
				runCLI(cli);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
