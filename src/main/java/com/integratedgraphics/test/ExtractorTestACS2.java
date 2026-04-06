package com.integratedgraphics.test;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.extractor.FindingAidCreator;
import com.integratedgraphics.extractor.IFDExtractor;
import com.integratedgraphics.extractor.IFDExtractorMain;
import com.integratedgraphics.html.PageCreator;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects from the ACS
 * 2026 tranch using automation
 * 
 </code>
 * 
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTestACS2 extends ExtractorTest {

	private static class ACSInfo {
		
		private final static String extractionTemplate =  //
				"{\"" + FAIRSpecExtractorHelper.FAIRSPEC_EXTRACT_VERSION + "\":\"" + IFDExtractor.version + "\",\"keys\":[\n" + // 
				" {\"" + FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_AUTOMATION_TSV_FILE + "\":\"$TSVFILE$\"},\n" + //
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_DOI+"\":\"$PUBDOI$\"},\n" +  //
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI + "\":\"https://creativecommons.org/licenses/by-nc/4.0\"},\n" + // 
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME + "\":\"cc-by-nc-4.0\"},\n" + // 
				"\n$RESOURCES$" +  //
				"]}\n" + // 
				"";
		
		private final static String resourceLine =  //
				" {\"FAIRSpec.extractor.local_source_file\":\"$ZIPFILE$\"},\n" //
				+ " {\""+IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_REPOSITORY_DOI+"\":\"$DATADOI$\"},\n" //
				+ " {\"FAIRSpec.extractor.automation_resource_id\":\"$RESOURCEID$\"},\n"
				; 
		private String pubDOI;
		private String tsvFile;
		private String acscode;
		private List<String> resourceIDs;
		private List<String> zipFiles;
		private List<String> dataDOIs;

		private String journalID;

		public ACSInfo(String pubdoi) {
			pubDOI = pubdoi; // https://doi.org/10.1021/acs.joc.5c00794
			String fullID = pubdoi.split("/")[4]; // acs.joc.5c00794
			String[] a = fullID.split("\\.");
			journalID = a[1]; // joc, orglett
			String prefix = null;
			switch (journalID) {
			case "joc":
				prefix = "jo";
				break;
			case "orglett":
				prefix = "ol";
				break;
			default:
				throw new RuntimeException("journal prefix unknown for " + pubdoi);
			}
			acscode = prefix + a[2]; // 5c00794
			tsvFile = acscode + "/" + acscode + "_prediction/" + acscode + "_final_output.tsv";				
			resourceIDs = new ArrayList<>();
			zipFiles = new ArrayList<>();			
			dataDOIs = new ArrayList<>();			
		}
		
		public void add(String supplementID) {
			String resourceID = acscode + "_si_" + supplementID;
			zipFiles.add(acscode + "/" + resourceID + ".zip");
			resourceIDs.add(resourceID);
			dataDOIs.add(pubDOI + ".s" + supplementID);
			
		}

		public String getExtractJson() {
			String s = extractionTemplate //
					.replace("$PUBDOI$", pubDOI)
					.replace("$TSVFILE$", tsvFile);
			String res = "";
			for (int i = 0; i < resourceIDs.size(); i++) {
				res += resourceLine//
						.replace("$ZIPFILE$", zipFiles.get(i)) //
						.replace("$DATADOI$", dataDOIs.get(i)) //
						.replace("$RESOURCEID$", resourceIDs.get(i));
			}
			return s.replace("$RESOURCES$", res);
		}
	}
	/**
	 * Run a full extraction based on arguments, possibly a test set
	 * 
	 * @param args [null, sourceAchive, targetDir, flags...]
	 * @param first
	 * @param last
	 * @param createFindingAidJSONList
	 */
     static void runACSExtractionTest(String[] args,
			String findACSID, int first, int last, boolean createFindingAidJSONList) {
		String localSourceArchive = args[1];
		String targetDir = args[2];
		new File(targetDir).mkdirs();
		String json = null;

		String moreFlags = null;

		int i0 = (findACSID != null ? 1 : Math.max(1, Math.min(first, last)));
		int i1 = (findACSID != null ? articles.size() - 1 : Math.min(articles.size() - 1, Math.max(1, Math.max(first, last))));
		int failed = 0;
		int n = 0;
		int nWarnings = 0;
		int nErrors = 0;
		String warnings = "";
		IFDExtractorMain extractor = null;
		String sflags = null;
		String targetDir0 = targetDir;
		// ./extract/ should be in the main Eclipse project directory.
		// [0] "./extract/acs.joc.0c00770/IFD-extract.json#22567817", // 0 727 files;
		// zips of bruker dirs + mnovas
		for (int i = i0; i <= i1; i++) {
			ACSInfo acsInfo = articles.get(i - 1);
			if (findACSID != null && !acsInfo.pubDOI.contains(findACSID))
				continue;
			extractor = new IFDExtractorMain();
			extractor.logToSys("Extractor.runExtractionTest output to " + new File(targetDir).getAbsolutePath());
			extractor.logToSys("Extractor.runExtraction " + i + " " + acsInfo.acscode);
			targetDir = targetDir0 + "/" + acsInfo.acscode;
			n++;
			if (json == null) {
				json = "{\"findingaids\":[\n";
			} else {
				json += ",\n";
			}
			json += "\"" + targetDir + "/IFD.findingaid.json\"";
			long t0 = System.currentTimeMillis();
			extractor.testID = i;
			FAIRSpecUtilities.setLogging(null);
			FAIRSpecUtilities.setLogging(targetDir + "/extractor.log");
			String ifdExtractFile = targetDir + "/IFD-extract.json";
			extractor.processFlags(args, moreFlags);
			// false for testing and you don't want to mess up _IFD_findingaids.json
			try {
				File ifdExtractScriptFile = new File(ifdExtractFile).getAbsoluteFile();
				String extractJson = acsInfo.getExtractJson();
				System.out.println(extractJson);
				ifdExtractScriptFile.getParentFile().mkdirs();
				FAIRSpecUtilities.putToFile(extractJson.getBytes(), ifdExtractScriptFile);
				File targetPath = new File(targetDir).getAbsoluteFile();
				String sourcePath = (localSourceArchive == null || "-".equals(localSourceArchive) ? "-" : new File(localSourceArchive).getAbsolutePath());
				extractor.run(ifdExtractScriptFile, targetPath, sourcePath);
				extractor.logToSys("Extractor.runExtraction ok " + acsInfo.acscode);
			} catch (Exception e) {
				failed++;
				extractor.logErr("Exception " + e + " " + i, "runExtraction");
				e.printStackTrace();
				if (extractor.stopOnAnyFailure)
					break;
			}
			if (extractor.assetsOnly)
				continue;
			nWarnings += extractor.warnings;
			nErrors += extractor.errors;
			extractor.logToSys("!Extractor.runExtraction job " + acsInfo.acscode + " time/sec="
					+ (System.currentTimeMillis() - t0) / 1000.0);
			ifdExtractFile = null;
			if (extractor.warnings > 0) {
				String w = "======== " + i + ": " + extractor.warnings + " warnings for " + targetDir + "\n"
						+ extractor.strWarnings;;
				warnings += w;
				try {
					FAIRSpecUtilities.writeBytesToFile(w.getBytes(),
							new File(targetDir + "/_IFD_warnings.txt"));
					FAIRSpecUtilities.writeBytesToFile(warnings.getBytes(),
							new File(targetDir0 + "/_IFD_warnings.txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			extractor.createExtractorFilesJSON(nErrors, nWarnings, false);
		}
		if (extractor != null) {
			if (createFindingAidJSONList)
				createFindingAidJSONList = !extractor.debugReadOnly && (first != last || first < 0);
			sflags = "\n first = " + first + " last = " + last + "\n"//
					+ extractor.dumpFlags() + "\n createFindingAidJSONList = " + createFindingAidJSONList //
					+ "\n IFD version " + IFDConst.IFD_VERSION + "\n";
			if (!createFindingAidJSONList)
				json = null;
			else if (json != null)
				json += "\n]}\n";
			File htmlPath = new File(targetDir0);
			try {
				if (json != null)
					extractor.buildSite(htmlPath, null, false);
				((FindingAidCreator) extractor).setTargetPath(htmlPath);
				extractor.finalizeExtraction(json, n, failed, nWarnings, nErrors, sflags);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
		FAIRSpecUtilities.setLogging(null);
	}
	
	/**
	 *  Set the args[] for MetadataExtractor to 
	 *  [ifdExtractJSONFilename, localSourceArchive, targetDir, flags...]
	 *  
	 * @param args
	 * @param localSourceArchive
	 * @param targetDir
	 * @return
	 */
	protected static String[] setSourceTargetArgs(String[] args, String ifdExtractJSONFilename, String localSourceArchive, String targetDir, String flags) {
		if (args == null)
			args = new String[4];
		String[] a = new String[Math.max(4,  args.length)];
		a[0] = (args.length < 1 || args[0] == null ? ifdExtractJSONFilename : args[0]);
		a[1] = (args.length < 2 || args[1] == null ? localSourceArchive : args[1]);
		a[2] = (args.length < 3 || args[2] == null ? targetDir : args[2]);
		a[3] = (args.length < 4 || args[3] == null ? flags : args[3]);
		for (int i = 4; i < args.length; i++)
			a[i] = args[i];
		return a;
	}

	//FigShare searching:
	//import requests as rq
	//from pprint import pprint as pp
	//
	//HEADERS = {'content-type': 'application/json'}
	//
	//r = rq.post('https://api.figshare.com/v2/articles/search', data='{"search_for": "university of sheffield", "page_size":20}', headers=HEADERS)
	//print(r.status_code)
	//results = r.json()
	//pp(results)

	/**
	 * ACS/FigShare codes /21947274
	 * 
	 * for example: https://ndownloader.figshare.com/files/21947274
	 */
	private static String[] acs2TestSet = { //
			/*1*/"https://doi.org/10.1021/acs.joc.4c02094.s004",//<i>Meta</i>-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			/*2*/"https://doi.org/10.1021/acs.joc.4c02689.s004",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*3*/"https://doi.org/10.1021/acs.joc.4c02691.s002",//Thiazoles via Formal [4 + 1] of NaSH to (Z)â€‘Bromoisocyanoalkenes
			/*4*/"https://doi.org/10.1021/acs.joc.4c02715.s002",//From Pseudocyclic to Macrocyclic Ionophores: Strategies toward the Synthesis of Cyclic Monensin Derivatives
			/*5*/"https://doi.org/10.1021/acs.joc.4c02737.s002",//Photooxidation and Cleavage of Ethynylated 9,10-Dimethoxyanthracenes with Acid-Labile Ether Bonds
			/*6*/"https://doi.org/10.1021/acs.joc.4c02842.s002",//One-Pot Alkynylation/Isomerization Cascade of Î²â€‘Formylated Enoates to Functionalized Ynones
			/*7*/"https://doi.org/10.1021/acs.joc.4c02860.s001",//Nickel-Catalyzed One-Pot H/D Exchange and Asymmetric Michael Addition in the Presence of D2O
			/*8*/"https://doi.org/10.1021/acs.joc.4c02921.s002",//Enantioselective Synthesis of Spirocyclic Isoxazolones Using a Conia-Ene Type Reaction
			/*9*/"https://doi.org/10.1021/acs.joc.4c02967.s002",//Vicinal Thiosulfonylation of ortho-(Alkynyl)benzyl Thiosulfonates/Sulfurothioates for Direct Synthesis of Sulfonyl-Derived Isothiochromenes
			/*10*/"https://doi.org/10.1021/acs.joc.4c03071.s001",//Tandem Synthesis of Polysubstituted Pyrroles via Cu(I)-Catalyzed Cyclization of Ketene N,S-Acetals with Î²â€‘Ketodinitriles
			/*11*/"https://doi.org/10.1021/acs.joc.4c03092.s002",//Regio- and Chemoselective Synthesis of Spiro Cyclobutane-Isobenzofuranimines via Cascade Oxycyclization and [2 + 2] Cycloaddition of oâ€‘Alkynolbenzamides
			/*12*/"https://doi.org/10.1021/acs.joc.4c02490.s002",//CO2â€¢â€“ Enabled Synthesis of Phenanthridinones, Oxindoles, Isoindolinones, and Spirolactams
			/*13*/"https://doi.org/10.1021/acs.joc.5c00007.s001",//C3-Heteroarylation of Indoles via Cu(II)-Catalyzed Aminocupration and Subsequent Nucleophilic Addition of oâ€‘Alkynylanilines
			/*14*/"https://doi.org/10.1021/acs.joc.5c00149.s002",//Photoinduced Cascade Synthesis of Oxindoles and Isoquinolinediones
			/*15*/"https://doi.org/10.1021/acs.joc.5c00171.s002",//Intramolecular ortho Photocycloaddition of 4â€‘Substituted 7â€‘(4â€²-Alkenyloxy)-1-indanones and Ensuing Reaction Cascades
			/*16*/"https://doi.org/10.1021/acs.joc.5c00513.s002",//Blue-Light-Irradiated Copper-Catalyzed Regio-tunable Double Câ€“H/Câ€“H Cross-Coupling Reaction: A Sustainable Approach to Construct C2â€‘Indolyl-1,4-naphthoquinones
			/*17*/"https://doi.org/10.1021/acs.joc.5c00526.s003",//Precise Synthesis of Ester-Functionalized Cyclo[6]- and Cyclo[7]furans
			/*18*/"https://doi.org/10.1021/acs.joc.5c00609.s002",//Curcumin as a Cinnamoyl Transfer Reagent via Câ€“C(CO) Bond Scissoring in the Microwave-Assisted Reaction with Hydroxyâ€‘pâ€‘QMs
			/*19*/"https://doi.org/10.1021/acs.joc.5c00613.s002",//Ni/Pd Dual-Catalysis Strategy for C(sp2)â€“Sb Cross-Coupling of Halostibines with Aryl Triflates and Applications of Products as Coupling Reagents, Ligands, and Anticancer Compounds
			/*20*/"https://doi.org/10.1021/acs.joc.5c00649.s002",//Do We See the True Color of Anthocyanidins?
			/*21*/"https://doi.org/10.1021/acs.joc.5c00794.s002",//Triplet Ketone Catalysis-Enabled Functionalization of Thioanisoles with Maleimides
			/*22*/"https://doi.org/10.1021/acs.joc.5c00912.s002",//Câ€“C Coupling Enabled by Mn Complexes with Diacyl Peroxides: Alkylation versus Oxygenation
			/*23*/"https://doi.org/10.1021/acs.joc.4c02546.s002",//Synergy-Promoted Specific Alkyltriphenylphosphonium Binding to CB[8]
			/*24*/"https://doi.org/10.1021/acs.joc.4c02574.s002",//Câ€“H Aminoalkylation of 5â€‘Membered Heterocycles: Influence of Descriptors, Data Set Size, and Data Quality on the Predictiveness of Machine Learning Models and Expansion of the Substrate Space Beyond 1,3-Azoles
			/*25*/"https://doi.org/10.1021/acs.joc.4c02600.s002",//Visible-Light-Mediated Three-Component Alkene 1,2-Alkylpyridylation Reaction Using Alkylboronic Acids as Radical Precursors for the Synthesis of 4â€‘Alkylpyridines
			/*26*/"https://doi.org/10.1021/acs.joc.4c02622.s001",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*26*/"https://doi.org/10.1021/acs.joc.4c02622.s002",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*26*/"https://doi.org/10.1021/acs.joc.4c02622.s004",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*27*/"https://doi.org/10.1021/acs.joc.4c02638.s002",//NHC-BH3â€‘Mediated Reduction of Sulfonyl Hydrazides into Disulfides and Further Cross-Coupling with Chlorostibine and Bioactivities
			/*28*/"https://doi.org/10.1021/acs.joc.4c02652.s002",//Aryl Borane as a Catalyst for Dehydrative Amide Synthesis
			/*29*/"https://doi.org/10.1021/acs.joc.4c02669.s001",//Controlling the Symmetry of Perylene Derivatives via Selective ortho-Borylation
	};
	
	static List<ACSInfo> getArticles() {
		List<ACSInfo> articles = new ArrayList<>();
		ACSInfo info = null;
		String lastPubDOI = null;
		for (int i = 0; i < acs2TestSet.length; i++) {
			String extractInfo = acs2TestSet[i];
			String datadoi = extractInfo; // https://doi.org/10.1021/acs.joc.5c00794.s002
			String pubdoi = datadoi.substring(0, datadoi.length() - 5);
			if (!pubdoi.equals(lastPubDOI)) {
				if (info != null)
					articles.add(info);
				info = new ACSInfo(pubdoi);
				lastPubDOI = pubdoi;
			}
			String supplementId = datadoi.substring(datadoi.length() - 3); // 004
			info.add(supplementId);
		}
		return articles;
	}
	
	final static List<ACSInfo> articles = getArticles();
	
	
	
	
	public static String syntaxString = 
			"ACS: java -jar IFDExtractor.jar " //
			+ "--test acs " //
			+ "[--DOI acs.joc.0c00770 or --DOI [2] or --DOI [n-m] where n >= 0 and 0 < m <= " + articles.size() //
			+ "--targetDir <TARGET_DIR> " //
			+ "--localSource <LOCAL_SOURCE_ARCHIVE>" //
			+ "\n    note that this a limited set of examples acs.joc.0c0070 is just one of them, [0]"
			+ "\n"; //

	private static void run(String[] args, int first, int last, String localSourceArchive, String targetDir) {
		// file test
		String findACSID = null;
		String flags = null;// "-datacitedown"
		/**
		 * the local test directory (c:/temp/iupac/acs2/test2)
		 *  
		 * a local dir if you have already downloaded the zip files, otherwise null to
		 * download from FigShare;
		 */
		if (args == null) {
			args = new String[4];
		} else {
			localSourceArchive = "c:/temp/iupac/acs2/test2";// -";
			targetDir = "c:/temp/iupac/acs2/acs2025";
		}
		args = setSourceTargetArgs(args, null, localSourceArchive, targetDir, flags);
		boolean createFindingAidJSONList = true;
		runACSExtractionTest(args, findACSID, first, last, createFindingAidJSONList);
	}

	/**
	 * Run the --DOI parameter if in the form [i,j] where i >= 0 and j <= 13
	 * 
	 * @param firstLast
	 */
	public static void runSet(String firstLast, String localSourceArchive, String targetDir) {
		firstLast = firstLast.replace('-', ',');
		int pt = firstLast.indexOf('[');
		targetDir = targetDir.replace('[', '_').replace(']', '_').replace('\\', '/');
		if (pt >= 0) {
			String[] a = firstLast.substring(pt + 1, firstLast.indexOf("]")).split(",");
			int first = Integer.parseInt(a[0]);
			int last = (a.length == 1 ? first : Integer.parseInt(a[1]));
			run(null, first, last, localSourceArchive, targetDir);
		}

	}

	public static void main(String[] args) {
		// args[] may override localSourceArchive as ars[1] 
		// and testDir as args[2]; args[0] is ignored;
		int first = 26; // first test to run
		int last =  26; // last test to run; 13 max, 9 for smaller files only; 11 to skip single-mnova
		run(args, first, last, null, null);
	}

}