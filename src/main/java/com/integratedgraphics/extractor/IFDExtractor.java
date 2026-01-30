package com.integratedgraphics.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.integratedgraphics.test.ExtractorTestACS;


/**
 * This class interprets command line options and starts the appropriate program. 
 * The primary subclass of FindingAidCreator is now IFDExtractorMain. By removing
 * this loader class from that stack, we allow a cleaner, faster loading from the 
 * command line and can generate less extraneous output. 
 * 
 * @author Fay Nguyen
 *
 */
class IFDExtractor {

	public static final String version = "0.1.2+2026.01.25";
	// 2026.01.25 version 0.1.2-beta refactored and much improved; adds CLI and timestamp-merging of NMR data
	// 2025.07.24 version 0.1.0-beta with FAIRSpec-ready paper
	// 2025.02.17 version 0.0.7-beta integrates the crawler
	// 2024.12.02 version 0.0.6 fully refactored, revised; adds creation of landing
	// page and -nolandingpage -nolaunch flags
	// 2024.11.03 version 0.0.6 adding support for DOICrawler
	// 2024.05.28 version 0.0.5 moved to com.integratedgraphics.extractor.Extractor
	// 2023.01.09 version 0.0.4 adds MNova_Page_Header parameter
	// 2023.01.07 version 0.0.4 adds CDX reading by Jmol
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

			// -schema 
			OptionBuilder.withLongOpt("schema");
			OptionBuilder.withDescription("This will run the schema validation");
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

			cliDOI = line.getOptionValue("D");
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
			if (line.hasOption("schema")) {
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
					+ "--test icl " //
					+ "--DOI \"10.14469/hpc/XXXXX\" " //
					+ "--targetDir <TARGET_DIR> " //
					+ "[additional flags] " //
					+ "\n" //
					+ "Dryad: java -jar IFDExtractor.jar " //
					+ "--test dryad " //
					+ "--targetDir <TARGET_DIR> " //
					+ "--localSource <LOCAL_SOURCE_ARCHIVE> " //
					+ "--DOI 2bvq83c2q " //
					+ "\n" //
					+ "ACS: java -jar IFDExtractor.jar " //
					+ "--test acs " //
					+ "--DOI acs.joc.0c00770 " //
					+ "--targetDir <TARGET_DIR> " //
					+ "--localSource <LOCAL_SOURCE_ARCHIVE>" //
					+ "\n" //
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
		if (!targetDir.endsWith("/")) {
			targetDir += "/";
		}
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
			if (runningSchemaValidation){
				schemaValidation(targetDir);
			}
			return;
		}

		if (cliDOI != null)
			targetDir += cliDOI.toLowerCase() + "_out/";

		// Handle the extract file path
		
		// these next are for Eclipse testing
		
		// note that "./" now stands for the RESOURCE in com/integrategraphics/extractor/

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
		// shouldn't we continue here?
		if (runningSchemaValidation){
			schemaValidation(targetDir);
		}
	}

	private static void saveToFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(new File(filePath.toString()))) {
            writer.write(content);
        }
    }

	/**
	 *  
	 * If 
	 * 
	 */
	private void schemaValidation(String targetDir) throws IOException{
		// Check whether the target directory exists
		Path outputFolderPath = Paths.get(targetDir).toAbsolutePath();
		if (!Files.isDirectory(outputFolderPath)){
			System.err.printf("The folder %s does not exist or is not a directory.\n", outputFolderPath);
			return;
		}
		// 
		String successLogName = this.cliDOI + "_schema_valid.txt";
		String errorLogName = this.cliDOI + "_schema_valid_error.txt";
		
		Path findingAidPath = Paths.get(outputFolderPath + "/IFD.findingaid.json");
		Path findingAidSchemaPath = Paths.get(outputFolderPath + "/IFD.findingaid.schema.json");
		if (!Files.exists(findingAidPath)){
			System.err.printf("The file %s does not exist in the folder %s.\n", findingAidPath.getFileName(), outputFolderPath);
			return;
		}
		if (!Files.exists(findingAidSchemaPath)){
			System.err.printf("The file %s does not exist in the folder %s.\n", findingAidSchemaPath.getFileName(), outputFolderPath);
			return;
		}
		List<String> command = new ArrayList<>();
        command.add("check-jsonschema");
        command.add("--verbose");
        command.add("--schemafile");
        command.add(findingAidSchemaPath.toString());
        command.add(findingAidPath.toString());
        command.add("-o");
        command.add("text");

		try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true); 
            
            Process process = builder.start();
			
			String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
			
			Path errorLogPath =  Paths.get(outputFolderPath + "/" + errorLogName);
			if (Files.exists(errorLogPath)){
				Files.delete(errorLogPath);
			}
			Path successLogPath =  Paths.get(outputFolderPath + "/" + successLogName);
			if (Files.exists(successLogPath)){
				Files.delete(successLogPath);
			}
            if (exitCode == 0) {
				Files.createFile(successLogPath);
				saveToFile(successLogPath, "Schema validation run on: " + timeStamp + "\n");
                saveToFile(successLogPath, output.toString());
				System.out.printf("Validation SUCCESSFUL for %s\n", targetDir);
            } else {
				
				Files.createFile(errorLogPath);
				saveToFile(errorLogPath, "Schema validation run on: " + timeStamp + "\n");
                saveToFile(errorLogPath, output.toString());
				System.out.printf("Validation FAILED for %s\n", targetDir);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
