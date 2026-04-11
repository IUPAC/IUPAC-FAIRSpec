package com.integratedgraphics.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDObject;

import com.integratedgraphics.extractor.FindingAidCreator;
import com.integratedgraphics.extractor.IFDExtractor;
import com.integratedgraphics.extractor.IFDExtractorMain;

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
				" {\"" + FAIRSpecExtractorHelper.FAIRSPEC_EXTRACT_VERSION + "\":\"" + IFDExtractor.version + "\",\"keys\":[\n" + // 
				" {\"" + IFDConst.IFD_PROPERTY_FINDINGAID_ID + "\":\"$ACSCODE$\"},\n" + //
				" {\"" + FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_AUTOMATION_TSV_FILE + "\":\"$TSVFILE$\"},\n" + //
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_DOI+"\":\"$PUBDOI$\"},\n" +  //
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI + "\":\"https://creativecommons.org/licenses/by-nc/4.0\"},\n" + // 
				" {\"" + IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME + "\":\"cc-by-nc-4.0\"},\n" + // 
				"\n$RESOURCES$" +  //
				"\"\"]}\n" + // 
				"";
		
		private final static String resourceLine =  //
				" {\"FAIRSpec.extractor.local_source_file\":\"$ZIPFILE$\"},\n" //
				+ " {\""+IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_REPOSITORY_DOI+"\":\"$DATADOI$\"},\n" //
				+ " {\"FAIRSpec.extractor.automation_resource_id\":\"$RESOURCEID$\"},\n"
				; 
	
		private int index;
		private String pubDOI;
		private String tsvFile;
		private String acscode;
		private List<String> resourceIDs;
		private List<String> zipFiles;
		private List<String> dataDOIs;

		private String journalID;

		public ACSInfo(int index, String pubdoi) {
			this.index = index;
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
					.replace("$ACSCODE$", acscode)
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
		
		@Override
		public String toString() {
			return "[ACSInfo " + index + " " + acscode + "]";
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

		int i1 = (findACSID != null ? articles.size() : Math.min(articles.size(), Math.max(1, Math.max(first, last))));
		int i0 = (findACSID != null ? 1 : Math.min(Math.max(1, Math.min(first, last)), i1));
		int failed = 0;
		int n = 0;
		int nWarnings = 0;
		int nErrors = 0;
		int nCompounds = 0;
		int nStructures = 0;
		int nSpectra = 0;
		IFDObject.nProperties = IFDObject.nRepresentations = 0;
		
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
			extractor.testID = acsInfo.toString();
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
			nStructures += extractor.faHelper.getStructureCollection().size();
			nSpectra += extractor.faHelper.getSpecCollection().size();
			nCompounds += extractor.faHelper.getCompoundCollection().size();
			nWarnings += extractor.warnings;
			nErrors += extractor.errors;
			extractor.logToSys("!Extractor.runExtraction job " + acsInfo.acscode + " time/sec="
					+ (System.currentTimeMillis() - t0) / 1000.0);
			ifdExtractFile = null;
			if (extractor.warnings > 0) {
				String w = "======== " + i + ": " + extractor.warnings + " warnings for " + targetDir + "\n"
						+ extractor.strWarnings + "\n";
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
		System.out.println("Extractor processed:\n" + nStructures + " structures\n" + nSpectra + " spectra\n" 
				+ nCompounds + " compounds\n" + IFDObject.nProperties + " properties\n"
				+ IFDObject.nRepresentations + " representations\n");

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
			
			///*2 jo4c02094 fails glolal directory F-NMR lost*/"https://doi.org/10.1021/acs.joc.4c02094.s002",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			///*8 jo4c02622 embedded Bruker directories and misplaced MNova files*/"https://doi.org/10.1021/acs.joc.4c02622.s001",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			///*13 jo4c02689 fails one cmpd per zip file; fails first column is NOT global*/"https://doi.org/10.1021/acs.joc.4c02689.s001",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			///*148 ol5c00493 fails: fids annulation final.rar*/"https://doi.org/10.1021/acs.orglett.5c00493.s001",//Lewis Acid-Mediated Domino Glycosylation/Cyclization of Substituted Glycals: A Stereoselective Route Toward the Synthesis of 1,2-Annulated Câ€‘Glycosides
			//RAR /*127 ol5c00493 */"https://doi.org/10.1021/acs.orglett.5c00493.s001",//Lewis Acid-Mediated Domino Glycosylation/Cyclization of Substituted Glycals: A Stereoselective Route Toward the Synthesis of 1,2-Annulated C‑Glycosides

			// javaLine
			/*1 jo4c02089 */"https://doi.org/10.1021/acs.joc.4c02089.s002",//Ring-Size-Dependent Selectivity of the β‑Hydride Elimination in Heck-Type Arylations of exo-Methylene Cycloketones: Chalcones versus 2‑Benzyl-1-naphthols
			/*-66 jo4c02094 */"https://doi.org/10.1021/acs.joc.4c02094.s002",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through C–I and C–O Bond Cleavages: An Access to Functionalized Coumarins
			/*2 jo4c02094 */"https://doi.org/10.1021/acs.joc.4c02094.s003",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through C–I and C–O Bond Cleavages: An Access to Functionalized Coumarins
			/*2 jo4c02094 */"https://doi.org/10.1021/acs.joc.4c02094.s004",//<i>Meta</i>-, Regioselective Amination of Cyclic Diaryliodoniums through C–I and C–O Bond Cleavages: An Access to Functionalized Coumarins
			/*3 jo4c02335 */"https://doi.org/10.1021/acs.joc.4c02335.s002",//TEMPO-Mediated Direct C(sp2)–H Alkoxylation/Aryloxylation of 1,4-Quinones
			/*4 jo4c02429 */"https://doi.org/10.1021/acs.joc.4c02429.s002",//Electrochemical Cyclization–Desulfurization Approach for the Synthesis of 1,3-Benzoxazines Using Cascade C–O and C–N Bond Formation
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s002",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s003",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s004",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s005",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s006",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s007",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s008",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5 jo4c02450 */"https://doi.org/10.1021/acs.joc.4c02450.s009",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*6 jo4c02490 */"https://doi.org/10.1021/acs.joc.4c02490.s002",//CO2•– Enabled Synthesis of Phenanthridinones, Oxindoles, Isoindolinones, and Spirolactams


			/*7 jo4c02600 */"https://doi.org/10.1021/acs.joc.4c02600.s002",//Visible-Light-Mediated Three-Component Alkene 1,2-Alkylpyridylation Reaction Using Alkylboronic Acids as Radical Precursors for the Synthesis of 4‑Alkylpyridines
			/*8 jo4c02622 */"https://doi.org/10.1021/acs.joc.4c02622.s001",//Direct Synthesis of 2‑Functionalized 3‑Nitroindoles from Diazo(nitro)acetanilides
			/*8 jo4c02622 */"https://doi.org/10.1021/acs.joc.4c02622.s002",//Direct Synthesis of 2‑Functionalized 3‑Nitroindoles from Diazo(nitro)acetanilides
			/*8 jo4c02622 */"https://doi.org/10.1021/acs.joc.4c02622.s004",//Direct Synthesis of 2‑Functionalized 3‑Nitroindoles from Diazo(nitro)acetanilides
			/*9 jo4c02634 */"https://doi.org/10.1021/acs.joc.4c02634.s002",//Synthesis of N‑Heterocycle-Ligated Porphyrins Using Iodobenzene Diacetate Enabled Regioselective Cross-Dehydrogenation of Porphyrins and NH-Heteroaromatics
			/*10 jo4c02638 */"https://doi.org/10.1021/acs.joc.4c02638.s002",//NHC-BH3‑Mediated Reduction of Sulfonyl Hydrazides into Disulfides and Further Cross-Coupling with Chlorostibine and Bioactivities
			/*11 jo4c02652 */"https://doi.org/10.1021/acs.joc.4c02652.s002",//Aryl Borane as a Catalyst for Dehydrative Amide Synthesis
			/*12 jo4c02669 */"https://doi.org/10.1021/acs.joc.4c02669.s001",//Controlling the Symmetry of Perylene Derivatives via Selective ortho-Borylation

			/*13 jo4c02689 */"https://doi.org/10.1021/acs.joc.4c02689.s001",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*13 jo4c02689 */"https://doi.org/10.1021/acs.joc.4c02689.s002",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge <i>Spongia officinalis</i> and Their Anti-inflammatory Activities
			/*13 jo4c02689 */"https://doi.org/10.1021/acs.joc.4c02689.s003",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*13 jo4c02689 */"https://doi.org/10.1021/acs.joc.4c02689.s004",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*14 jo4c02691 */"https://doi.org/10.1021/acs.joc.4c02691.s002",//Thiazoles via Formal [4 + 1] of NaSH to (Z)‑Bromoisocyanoalkenes
			/*15 jo4c02715 */"https://doi.org/10.1021/acs.joc.4c02715.s002",//From Pseudocyclic to Macrocyclic Ionophores: Strategies toward the Synthesis of Cyclic Monensin Derivatives
			/*16 jo4c02737 */"https://doi.org/10.1021/acs.joc.4c02737.s002",//Photooxidation and Cleavage of Ethynylated 9,10-Dimethoxyanthracenes with Acid-Labile Ether Bonds
			/*17 jo4c02787 */"https://doi.org/10.1021/acs.joc.4c02787.s002",//Annulative Coupling of Sulfoxonium Ylides with Aldehydes and Naphthols or Coumarins: Easy Access to Fused Dihydrofurans
			/*18 jo4c02834 */"https://doi.org/10.1021/acs.joc.4c02834.s001",//Multicomponent Tf2O‑Triggered Dearomative Triazinylmethylation of Isoquinolines Using Acetonitrile
			/*19 jo4c02842 */"https://doi.org/10.1021/acs.joc.4c02842.s002",//One-Pot Alkynylation/Isomerization Cascade of β‑Formylated Enoates to Functionalized Ynones
			/*20 jo4c02844 */"https://doi.org/10.1021/acs.joc.4c02844.s002",//Heteroarylstilbenes: Visible-Light-Tunable Photochromic Systems in Water
			/*21 jo4c02860 */"https://doi.org/10.1021/acs.joc.4c02860.s001",//Nickel-Catalyzed One-Pot H/D Exchange and Asymmetric Michael Addition in the Presence of D2O
			/*22 jo4c02893 */"https://doi.org/10.1021/acs.joc.4c02893.s002",//A Photo-Smiles Rearrangement: Mechanistic Investigation of the Formation of Blatter Radical Helicenes
			/*23 jo4c02921 */"https://doi.org/10.1021/acs.joc.4c02921.s002",//Enantioselective Synthesis of Spirocyclic Isoxazolones Using a Conia-Ene Type Reaction
			/*24 jo4c02967 */"https://doi.org/10.1021/acs.joc.4c02967.s002",//Vicinal Thiosulfonylation of ortho-(Alkynyl)benzyl Thiosulfonates/Sulfurothioates for Direct Synthesis of Sulfonyl-Derived Isothiochromenes
			/*25 jo4c03015 */"https://doi.org/10.1021/acs.joc.4c03015.s002",//B←N Lewis Pair-Functionalized Perylenes: Tuning Optoelectronic Properties via Regioisomerization
			/*26 jo4c03046 */"https://doi.org/10.1021/acs.joc.4c03046.s002",//Quantification of the H‑Bond-Donating Ability of Trifluoromethyl Ketone Hydrates Using a Colorimetric Sensor
			/*26 jo4c03046 */"https://doi.org/10.1021/acs.joc.4c03046.s003",//Quantification of the H‑Bond-Donating Ability of Trifluoromethyl Ketone Hydrates Using a Colorimetric Sensor
			/*27 jo4c03071 */"https://doi.org/10.1021/acs.joc.4c03071.s001",//Tandem Synthesis of Polysubstituted Pyrroles via Cu(I)-Catalyzed Cyclization of Ketene N,S-Acetals with β‑Ketodinitriles
			/*28 jo4c03085 */"https://doi.org/10.1021/acs.joc.4c03085.s002",//A Route to the Mild Synthesis of α‑Selenomethylketones via Vinyl Azides
			/*29 jo4c03092 */"https://doi.org/10.1021/acs.joc.4c03092.s002",//Regio- and Chemoselective Synthesis of Spiro Cyclobutane-Isobenzofuranimines via Cascade Oxycyclization and [2 + 2] Cycloaddition of o‑Alkynolbenzamides
			/*30 jo4c03094 */"https://doi.org/10.1021/acs.joc.4c03094.s002",//Synthesis of a Natural Product-Based 5H‑Thiazolo[5′,4′:5,6]pyrido[2,3‑b]indole Derivative via Solid-Phase Synthesis
			/*31 jo4c03118 */"https://doi.org/10.1021/acs.joc.4c03118.s001",//Atmosphere Effects on Arene Reduction with Lithium and Ethylenediamine in THF
			/*32 jo4c03140 */"https://doi.org/10.1021/acs.joc.4c03140.s002",//Asymmetric Total Synthesis of Gymnothespirolignan A via a Bioinspired Double Cyclization Approach
			/*33 jo4c03150 */"https://doi.org/10.1021/acs.joc.4c03150.s004",//A Twist on Controlling the Equilibrium of Dynamic Thia-Michael Reactions
			/*33 jo4c03150 */"https://doi.org/10.1021/acs.joc.4c03150.s005",//A Twist on Controlling the Equilibrium of Dynamic Thia-Michael Reactions
			/*34 jo4c03151 */"https://doi.org/10.1021/acs.joc.4c03151.s002",//Minisci C–H Alkylation of Heterocycles with Unactivated Alkyl Iodides Enabled by Visible Light Photocatalysis
			/*35 jo4c03180 */"https://doi.org/10.1021/acs.joc.4c03180.s002",//PhICl2/KSeCN Mediated Synthesis of Selenopheno[3,2‑b]indoles and 3‑Selenocyanato-2-benzoselenophene Indoles from 1,3-Diynes via Double Electrophilic Cyclization
			/*36 jo4c03181 */"https://doi.org/10.1021/acs.joc.4c03181.s002",//(3+2) Annulation of Donor–Acceptor Cyclopropanes with Difluoroenoxysilanes: Syntheses of gem-Difluorocyclopentenes via α,α-Difluoroketone Scaffolds
			/*37 jo5c00007 */"https://doi.org/10.1021/acs.joc.5c00007.s001",//C3-Heteroarylation of Indoles via Cu(II)-Catalyzed Aminocupration and Subsequent Nucleophilic Addition of o‑Alkynylanilines
			/*38 jo5c00061 */"https://doi.org/10.1021/acs.joc.5c00061.s002",//Dynamically Generated Carbenium Species via Photoisomerization of Cyclic Alkenes: Mild Friedel–Crafts Alkylation

			/*39 jo5c00117 */"https://doi.org/10.1021/acs.joc.5c00117.s002",//Sodium Poly(heptazine imide)-Enabled Oxytrifluoromethylation of Alkenes for the Synthesis of α‑CF3 Ketones
			/*40 jo5c00149 */"https://doi.org/10.1021/acs.joc.5c00149.s002",//Photoinduced Cascade Synthesis of Oxindoles and Isoquinolinediones
			/*41 jo5c00156 */"https://doi.org/10.1021/acs.joc.5c00156.s002",//Regioselective Synthesis of (E)‑3-((Alkylamino)methylene)-2-thioxothiochroman-4-one Derivatives and Their Regioselective Cycloaddition with Alkynes
			/*42 jo5c00161 */"https://doi.org/10.1021/acs.joc.5c00161.s002",//Rise of Ketone α‑Hydrolysis: Revisiting SNAcyl, E1cB Mechanisms and Carbon-Based Leaving Groups in One Reaction for Drug-Targeting Applications
			/*43 jo5c00171 */"https://doi.org/10.1021/acs.joc.5c00171.s002",//Intramolecular ortho Photocycloaddition of 4‑Substituted 7‑(4′-Alkenyloxy)-1-indanones and Ensuing Reaction Cascades

			/*44 jo5c00184 */"https://doi.org/10.1021/acs.joc.5c00184.s002",//Palladium Catalyst-Controlled Regiodivergent C–H Arylation of Thiazoles
			/*45 jo5c00188 */"https://doi.org/10.1021/acs.joc.5c00188.s001",//Synthesis of Sulfonylthiophenes through [3+2] Cycloaddition of Pyridinium 1,4-Zwitterionic Thiolates with (E)‑β-Iodovinyl Sulfones or Bromoallylsulfones
			/*46 jo5c00189 */"https://doi.org/10.1021/acs.joc.5c00189.s002",//Synthesis of Phenanthrenes and Naphthoquinone Fused Benzoxepines via Hauser–Kraus Annulation of Sulfonylphthalide with Morita–Baylis–Hillman Adducts of Nitroalkenes
			/*47 jo5c00193 */"https://doi.org/10.1021/acs.joc.5c00193.s002",//Photoinduced Regio- and Stereoselective Hydrotrifluoromethylation of Glycals with Langlois Reagent


			/*48 jo5c00246 */"https://doi.org/10.1021/acs.joc.5c00246.s002",//In Situ Generation and [3 + 2] Annulation Reactions of PropiolaldehydeA Metal-Free, Cascade Route to Pyrazole and Bipyrazole Carboxaldehydes in One Pot
			/*49 jo5c00270 */"https://doi.org/10.1021/acs.joc.5c00270.s002",//3,3-Bis(hydroxyaryl)oxindoles and Spirooxindoles Bearing a Xanthene Moiety: Synthesis, Mechanism, and Biological Activity


			/*50 jo5c00316 */"https://doi.org/10.1021/acs.joc.5c00316.s002",//Base-Assisted and Silica Gel-Promoted Indole-Substituted Indene Synthesis
			/*51 jo5c00317 */"https://doi.org/10.1021/acs.joc.5c00317.s002",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids B–D Via Ireland-Claisen Rearrangement
			/*51 jo5c00317 */"https://doi.org/10.1021/acs.joc.5c00317.s003",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids B–D Via Ireland-Claisen Rearrangement
			/*51 jo5c00317 */"https://doi.org/10.1021/acs.joc.5c00317.s004",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids B–D Via Ireland-Claisen Rearrangement
			/*52 jo5c00330 */"https://doi.org/10.1021/acs.joc.5c00330.s002",//Synthesis of 3,6-Dihydro‑2H‑thiopyrans from α‑Diazo-β-diketones and Vinylthiiranes via [5 + 1] Annulation
			/*52 jo5c00330 */"https://doi.org/10.1021/acs.joc.5c00330.s003",//Synthesis of 3,6-Dihydro‑2H‑thiopyrans from α‑Diazo-β-diketones and Vinylthiiranes via [5 + 1] Annulation


			/*53 jo5c00369 */"https://doi.org/10.1021/acs.joc.5c00369.s002",//Phenyl Spacer Modulation of 2,5-Substituted D–A-Type Siloles for Efficient Nondoped OLEDs
			/*54 jo5c00389 */"https://doi.org/10.1021/acs.joc.5c00389.s002",//The Anthranil Core as a π‑Conjugated Bridge in the Synthesis of Molecular Photosensitizers

			/*55 jo5c00424 */"https://doi.org/10.1021/acs.joc.5c00424.s002",//Visible-Light-Driven Tandem Cyclization of <i>o</i>‑Hydroxyaryl Enaminones: Access to 3‑(α-Arylsulfonamido)trifluoroethyl Chromones
			/*56 jo5c00472 */"https://doi.org/10.1021/acs.joc.5c00472.s002",//FeCl3/TBHP-Mediated Oxidation of Indoles: Divergent Product Selectivity under Mechanochemical and Solution-Based Conditions
			/*57 jo5c00474 */"https://doi.org/10.1021/acs.joc.5c00474.s002",//One-Pot High-Yield Synthesis of a Tripodal Triamine Building Block in an Ammonia Ethanolic Solution
			/*58 jo5c00508 */"https://doi.org/10.1021/acs.joc.5c00508.s001",//Asymmetric and Symmetric S-zig-zag-Fused BODIPYs: Synthesis and Photophysical and Oxidative Properties
			/*59 jo5c00513 */"https://doi.org/10.1021/acs.joc.5c00513.s002",//Blue-Light-Irradiated Copper-Catalyzed Regio-tunable Double C–H/C–H Cross-Coupling Reaction: A Sustainable Approach to Construct C2‑Indolyl-1,4-naphthoquinones
			/*60 jo5c00521 */"https://doi.org/10.1021/acs.joc.5c00521.s001",//Synthesis of Octatrimethylsilyl-[8]cycloparaphenylene for Multifunctionalized Cycloparaphenylene


			/*61 jo5c00545 */"https://doi.org/10.1021/acs.joc.5c00545.s002",//Total Synthesis of the Melodinus Alkaloid (±)-Melohemsine K
			/*62 jo5c00606 */"https://doi.org/10.1021/acs.joc.5c00606.s002",//Reversibility and Enantioselectivity of Palladium-Catalyzed Allylic Aminations: Ligand, Base-Additive, and Solvent Effects
			/*63 jo5c00609 */"https://doi.org/10.1021/acs.joc.5c00609.s002",//Curcumin as a Cinnamoyl Transfer Reagent via C–C(CO) Bond Scissoring in the Microwave-Assisted Reaction with Hydroxy‑p‑QMs
			/*64 jo5c00613 */"https://doi.org/10.1021/acs.joc.5c00613.s002",//Ni/Pd Dual-Catalysis Strategy for C(sp2)–Sb Cross-Coupling of Halostibines with Aryl Triflates and Applications of Products as Coupling Reagents, Ligands, and Anticancer Compounds





			/*65 jo5c00670 */"https://doi.org/10.1021/acs.joc.5c00670.s002",//1,3-Dipolar Cycloaddition of Nitrile Imines to 2‑Imino-thiazolo[3,2‑a]pyrimidin-3-ones: Dipole-Initiated Thiazolone-Imidazolone Rearrangement


			/*66 jo5c00727 */"https://doi.org/10.1021/acs.joc.5c00727.s002",//Regiodivergence in the Cycloadditions between a Cyclic Nitrone and Carbonyl-Type Dipolarophiles
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s001",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s002",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s003",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s004",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s005",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s006",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s007",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s008",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s009",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*67 jo5c00741 */"https://doi.org/10.1021/acs.joc.5c00741.s010",//Photocyclization of 8‑Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy

			/*68 jo5c00774 */"https://doi.org/10.1021/acs.joc.5c00774.s002",//Inhibited Hydrolysis of 4‑(N,N‑Dimethylamino)aryl Imines in a Weak Acid by Supramolecular Encapsulation inside Cucurbit[7]uril

			/*69 jo5c00794 */"https://doi.org/10.1021/acs.joc.5c00794.s002",//Triplet Ketone Catalysis-Enabled Functionalization of Thioanisoles with Maleimides


			/*70 jo5c00884 */"https://doi.org/10.1021/acs.joc.5c00884.s001",//NCS-Mediated Direct Synthesis of β‑Chlorosulfoxides from Unactivated Alkenes and Thiophenols
			/*71 jo5c00912 */"https://doi.org/10.1021/acs.joc.5c00912.s002",//C–C Coupling Enabled by Mn Complexes with Diacyl Peroxides: Alkylation versus Oxygenation

			/*72 jo5c00952 */"https://doi.org/10.1021/acs.joc.5c00952.s002",//Distinct Selectivity of 2‑Aryl Thioquinazolinones in the Sulfur Directing Rh(III)-Catalyzed Amidation Reaction
			/*73 jo5c00954 */"https://doi.org/10.1021/acs.joc.5c00954.s002",//Further Confirmation of the Structure of 3′-(2-Pyridyldithio)-3′-deoxyadenosine and 3′-Thio-3′-deoxyadenosine: Synthetic Convergence with Cordycepin

			/*74 jo5c01002 */"https://doi.org/10.1021/acs.joc.5c01002.s001",//Synthesis of 2‑Hydroxycarbazoles via Rh(III)-Catalyzed Cascade Cyclization of Indolyl Nitrones with Alkylidenecyclopropanes
			/*75 jo5c01060 */"https://doi.org/10.1021/acs.joc.5c01060.s002",//In-Catalyzed Regioselective [3+2] Cycloaddition of Alkenes with α,β-Unsaturated Keto-Carboxylic Acid Derivatives for the Synthesis of γ‑Butyrolactones
			/*76 jo5c01068 */"https://doi.org/10.1021/acs.joc.5c01068.s002",//Stereocontrolled Ring-Opening of Oxazolidinone-Fused Aziridines for the Synthesis of 2‑Amino Ethers
			/*77 jo5c01082 */"https://doi.org/10.1021/acs.joc.5c01082.s002",//Pd-Catalyzed One-Pot Synthesis of Indole-3-carboxylic Acids via a Sequential Water-Mediated Nucleophilic Addition to Amides/Esters and Carbon–Heteroatom Cross-Coupling Reaction Strategy
			/*77 jo5c01082 */"https://doi.org/10.1021/acs.joc.5c01082.s003",//Pd-Catalyzed One-Pot Synthesis of Indole-3-carboxylic Acids via a Sequential Water-Mediated Nucleophilic Addition to Amides/Esters and Carbon–Heteroatom Cross-Coupling Reaction Strategy

			/*78 jo5c01113 */"https://doi.org/10.1021/acs.joc.5c01113.s002",//Phenanthrene-Fused BN-Acenaphth(yl)ene: Synthesis, Structures, and Photophysical Studies

			/*79 jo5c01156 */"https://doi.org/10.1021/acs.joc.5c01156.s002",//Annulative Coupling of β‑Ketosulfoxonium Ylides, β‑Ketothioamides, and Aldehydes: Access to Highly Functionalized Dihydrothiophenes


			/*80 jo5c01245 */"https://doi.org/10.1021/acs.joc.5c01245.s002",//Electrochemically Oxidative C–H/N–H Cross-Coupling Reactions of Sulfoximines with Imidazopyridines

			/*81 jo5c01353 */"https://doi.org/10.1021/acs.joc.5c01353.s002",//Chiral P,N,N-Ligands for the Manganese-Catalyzed Asymmetric Formal Hydroamination of Allylic Alcohols
			/*82 jo5c01387 */"https://doi.org/10.1021/acs.joc.5c01387.s002",//A Noncarbenoid Approach to Imidazolidines via ZnCl2‑Catalyzed Annulation of 4‑Alkoxycarbonyl-1,2-diaza-1,3-dienes with 1,3,5-Triazinanes
			/*83 jo5c01409 */"https://doi.org/10.1021/acs.joc.5c01409.s002",//Total Synthesis of Alanense B via Stereoselective Enolate Alkylation
			/*84 jo5c01422 */"https://doi.org/10.1021/acs.joc.5c01422.s002",//Regioselectivity Switching in the Tandem Reaction of Skipped Diynones with Allylic Alcohols: Stereoselective Synthesis of 4‑Allyl-2-Methylene-3(2<i>H</i>)‑Furanones
			/*85 jo5c01427 */"https://doi.org/10.1021/acs.joc.5c01427.s002",//Substrate-Directed Annulative-Sulfonylation/Desulfonylation Cascade Using (E)‑β-Iodovinyl Sulfones: A Diverse Approach for the Synthesis of Imidazo[1,2‑a]pyridines with Sulfone Motifs
			/*86 jo5c01465 */"https://doi.org/10.1021/acs.joc.5c01465.s002",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)–OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*86 jo5c01465 */"https://doi.org/10.1021/acs.joc.5c01465.s003",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)–OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*86 jo5c01465 */"https://doi.org/10.1021/acs.joc.5c01465.s004",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)–OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*86 jo5c01465 */"https://doi.org/10.1021/acs.joc.5c01465.s005",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)–OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process

			/*87 jo5c01490 */"https://doi.org/10.1021/acs.joc.5c01490.s002",//Synthesis of 2‑Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted C–H Bond Activation and Annulation of Anilines with CF3‑Imidoyl Sulfoxonium Ylides
			/*87 jo5c01490 */"https://doi.org/10.1021/acs.joc.5c01490.s003",//Synthesis of 2‑Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted C–H Bond Activation and Annulation of Anilines with CF3‑Imidoyl Sulfoxonium Ylides
			/*87 jo5c01490 */"https://doi.org/10.1021/acs.joc.5c01490.s004",//Synthesis of 2‑Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted C–H Bond Activation and Annulation of Anilines with CF3‑Imidoyl Sulfoxonium Ylides
			/*88 jo5c01501 */"https://doi.org/10.1021/acs.joc.5c01501.s002",//Electrochemical Oxidative Ring Opening of 1‑Acetylindoline-3-one to Access α‑Ketoamides
			/*89 jo5c01538 */"https://doi.org/10.1021/acs.joc.5c01538.s002",//Concise, Atom-Economical, and Enantiodivergent Total Synthesis of β‑Lycorane via Organocatalyzed [4+2] Cycloaddition Reaction
			/*90 jo5c01545 */"https://doi.org/10.1021/acs.joc.5c01545.s002",//Dual-State Mechano- and Electrochromic Responses Enabled by Sterically Strained Diazaanthraquinodimethanes












			/*91 jo5c01570 */"https://doi.org/10.1021/acs.joc.5c01570.s002",//Gold-Catalyzed Single O‑Transfer to Internal CF3‑Alkynes. Regioselective Synthesis of 4‑Trifluoromethylated Oxazoles
			/*92 jo5c01584 */"https://doi.org/10.1021/acs.joc.5c01584.s002",//Tunable Synthesis of Polysubstituted Pyrroles via Silver-Catalyzed (3 + 2) Cycloaddition of α,β-Unsaturated Nitroketones with Isocyanides



			/*93 jo5c01759 */"https://doi.org/10.1021/acs.joc.5c01759.s001",//Photoredox Catalyzed Cyclization of Enaminones/Ketene N,S‑Acetals with β‑Ketodinitriles to Access Polysubstituted Pyrroles

			/*94 jo5c01777 */"https://doi.org/10.1021/acs.joc.5c01777.s001",//Synthesis of Unsymmetrical Diaryl Disulfides via I2/DMF-Assisted Reductive Cross-Coupling of Sodium Sulfinates with Arenediazonium Tetrafluoroborates/Sodium Metabisulfite

			/*95 jo5c01963 */"https://doi.org/10.1021/acs.joc.5c01963.s002",//Palladium-Catalyzed Carbonylative Cross-Coupling of Aryl (Pseudo)halides and Cyclopropanols
			/*96 jo5c01989 */"https://doi.org/10.1021/acs.joc.5c01989.s002",//Peptidyl Glycosyl Thiols and Disulfides for Enantioselective Hydrogen Atom Transfer (HAT) and Thiyl Radical Catalysis

			/*97 jo5c02047 */"https://doi.org/10.1021/acs.joc.5c02047.s002",//Photoredox-Catalyzed Deoxygenative C–H Alkylation of Azauracils via Xanthate-Activated Alcohols
			/*98 jo5c02084 */"https://doi.org/10.1021/acs.joc.5c02084.s002",//Organophotoredox-Catalyzed Umpolung Strategy to β‑Fluoroamides via Carbamoylative Fluorination of Alkene in Aqueous Medium
			/*99 jo5c02091 */"https://doi.org/10.1021/acs.joc.5c02091.s002",//Organophotocatalytic Regioselective Silylation/Germylation and Cascade Cyclization of 1,7-Dienes: Access to Silylated/Germylated Benzazepine Derivatives
			/*100 jo5c02110 */"https://doi.org/10.1021/acs.joc.5c02110.s001",//Sequential Ni-Catalyzed Cross-Coupling and Ru-Catalyzed Isomerization of E/Z Alkenyl Ethers: A Stereoconvergent Route to Allylsilanes

			/*101 jo5c02152 */"https://doi.org/10.1021/acs.joc.5c02152.s001",//Fe-BPsalan Complex-Catalyzed Asymmetric 1,3-Dipolar [3 + 2] Cycloaddition of Nitrones with α,β-Unsaturated Acyl Imidazoles
			/*101 jo5c02152 */"https://doi.org/10.1021/acs.joc.5c02152.s002",//Fe-BPsalan Complex-Catalyzed Asymmetric 1,3-Dipolar [3 + 2] Cycloaddition of Nitrones with α,β-Unsaturated Acyl Imidazoles
			/*102 jo5c02154 */"https://doi.org/10.1021/acs.joc.5c02154.s002",//Steric Hindrance-Driven Access to 3,4,7-Trihydro-1,2-oxaphosphepine 2‑Oxides from Vinyloxiranes and Phosphoryl Diazomethanes
			/*103 jo5c02221 */"https://doi.org/10.1021/acs.joc.5c02221.s002",//Palladium-Catalyzed [3 + 2] Annulation of o‑Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)–H Bond Activation to Yield 1‑Cyanoindane
			/*103 jo5c02221 */"https://doi.org/10.1021/acs.joc.5c02221.s003",//Palladium-Catalyzed [3 + 2] Annulation of o‑Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)–H Bond Activation to Yield 1‑Cyanoindane
			/*103 jo5c02221 */"https://doi.org/10.1021/acs.joc.5c02221.s004",//Palladium-Catalyzed [3 + 2] Annulation of o‑Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)–H Bond Activation to Yield 1‑Cyanoindane
			/*103 jo5c02221 */"https://doi.org/10.1021/acs.joc.5c02221.s005",//Palladium-Catalyzed [3 + 2] Annulation of o‑Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)–H Bond Activation to Yield 1‑Cyanoindane
			/*103 jo5c02221 */"https://doi.org/10.1021/acs.joc.5c02221.s006",//Palladium-Catalyzed [3 + 2] Annulation of o‑Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)–H Bond Activation to Yield 1‑Cyanoindane
			/*104 jo5c02305 */"https://doi.org/10.1021/acs.joc.5c02305.s002",//Palladium-Catalyzed Denitrogenative Suzuki Coupling of Benzothiatriazine Dioxides for Bi(hetero)aryl-2-sulfonamides
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s001",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s002",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s003",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s004",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s005",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s006",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s007",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s008",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*105 jo5c02308 */"https://doi.org/10.1021/acs.joc.5c02308.s009",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*106 jo5c02383 */"https://doi.org/10.1021/acs.joc.5c02383.s001",//Chemoselective Synthesis of Fluorinated 1,3-Thiazines via Sonication-Assisted [3 + 3] Annulation of Thioamides and α‑CF3 Styrene


			/*107 jo5c02414 */"https://doi.org/10.1021/acs.joc.5c02414.s002",//Benzo[1,2,3]thiadiazole as an Attractive Heteroaromatic Platform for Two-Photon Absorbing (TPA) Fluorophores: TPA Enhancement in Quasi-Quadrupolar S,N-Heteroarene-Cored Dyes via Skeletal Editing and Regioisomeric Control


			/*108 jo5c02497 */"https://doi.org/10.1021/acs.joc.5c02497.s001",//C–H/N–H Annulation of N‑Aryl Triazolinediones with Diazo Compounds: A Route to Cinnoline Analogues
			/*109 jo5c02500 */"https://doi.org/10.1021/acs.joc.5c02500.s001",//A Concise Approach to Enantioselective Synthesis of Euolutchuols A, B, and D and Their Structure Assignment
			/*110 jo5c02524 */"https://doi.org/10.1021/acs.joc.5c02524.s002",//Pd- and Cu-Cocatalyzed Anaerobic Olefin Aminoboration



			/*111 jo5c02653 */"https://doi.org/10.1021/acs.joc.5c02653.s001",//Visible-Light-Induced Amination of Pyridylphosphonium Salts: Synthesis of Heteroarylamines via Radical–Radical Coupling

			/*112 jo5c02735 */"https://doi.org/10.1021/acs.joc.5c02735.s002",//Enantioselective Organocatalytic Desymmetric Acylation as an Access to Orthogonally Protected myo-Inositols
			/*113 jo5c02870 */"https://doi.org/10.1021/acs.joc.5c02870.s002",//Synthesis of Quinolizidine-Based 1,4-Azaphosphinines via Cyclization of Heteroarylmethyl(alkynyl)phosphinates
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s001",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s002",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s003",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s004",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s005",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s006",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s007",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*114 ol4c03845 */"https://doi.org/10.1021/acs.orglett.4c03845.s008",//Enantio- and Regioselective Propargylation and Allenylation of β,γ-Unsaturated α‑Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*115 ol4c04173 */"https://doi.org/10.1021/acs.orglett.4c04173.s002",//Pd(OAc)2‑Catalyzed Approach to Phenanthridin-6(5H)‑one Skeletons

			/*116 ol4c04348 */"https://doi.org/10.1021/acs.orglett.4c04348.s002",//Construction of 3,6-Difluoropyridones via a Double Defluorinative [3 + 3] Annulation of α‑Fluoro-α-sulfonylacetamides with 2‑CF3‑Alkenes
			/*117 ol4c04509 */"https://doi.org/10.1021/acs.orglett.4c04509.s002",//Thermocontrolled Radical Nucleophilicity vs Radicophilicity in Regiodivergent C–H Functionalization
			/*118 ol4c04625 */"https://doi.org/10.1021/acs.orglett.4c04625.s002",//Sc-Catalyzed Asymmetric [2 + 2] Annulation of 2‑Alkynylnaphthols with Dienes to Access Cyclobutene Frameworks
			/*119 ol4c04737 */"https://doi.org/10.1021/acs.orglett.4c04737.s002",//Diverse Annulations of Alkyl(phenyl)phosphinic Chlorides and Imines
			/*119 ol4c04737 */"https://doi.org/10.1021/acs.orglett.4c04737.s003",//Diverse Annulations of Alkyl(phenyl)phosphinic Chlorides and Imines

			/*120 ol4c04779 */"https://doi.org/10.1021/acs.orglett.4c04779.s002",//Diastereoselective Synthesis of Cyclobutanes via Rh-Catalyzed Unprecedented C–C Bond Cleavage of Alkylidenecyclopropanes
			/*121 ol4c04827 */"https://doi.org/10.1021/acs.orglett.4c04827.s001",//Catalytic Enantioselective [4+1]-Annulation of Carboxylic Acids with Cyclopropenes
			/*122 ol4c04834 */"https://doi.org/10.1021/acs.orglett.4c04834.s002",//Visible Light-Induced Sequential Nitrogen Insertion and Benzotriazolation of Quinoxaline-2(1H)‑ones
			/*123 ol5c00129 */"https://doi.org/10.1021/acs.orglett.5c00129.s002",//Visible-Light-Driven Synergistic Se/Fe Catalysis for the Synthesis of 2‑Aminoquinoline Derivatives
			/*124 ol5c00146 */"https://doi.org/10.1021/acs.orglett.5c00146.s001",//TBHP-Promoted Trifluoromethyl-difluoromethylthiolation of Unactivated Alkenes with CF3SO2Na and PhSO2SCF2H
			/*125 ol5c00325 */"https://doi.org/10.1021/acs.orglett.5c00325.s002",//Trifluoromethylation on a Nucleoside Sugar Scaffold: Design and Synthesis of 6′-Trifluoromethylcyclopentenyl-purine and -pyrimidine Nucleosides


			/*126 ol5c00519 */"https://doi.org/10.1021/acs.orglett.5c00519.s002",//Pyridyl Pyrimidylsulfones as Latent Pyridyl Nucleophiles in Palladium-Catalyzed Cross-Coupling Reactions
			/*127 ol5c00576 */"https://doi.org/10.1021/acs.orglett.5c00576.s002",//A Metal-Catalyzed Tunable Reaction of Ylides with Hydroxylamine Derivatives
			/*128 ol5c00600 */"https://doi.org/10.1021/acs.orglett.5c00600.s002",//Direct Synthesis of Amides from Nitro Compounds and Alcohols via Borrowing Hydrogenation
			/*129 ol5c00623 */"https://doi.org/10.1021/acs.orglett.5c00623.s002",//Fe/Mn-Synergistic Promoted C(sp3)–Bi Cross-Coupling of Alkyl Chlorides with Chlorobismuthanes to Access Air-Stable Alkylbismuthanes
			/*130 ol5c00666 */"https://doi.org/10.1021/acs.orglett.5c00666.s002",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*130 ol5c00666 */"https://doi.org/10.1021/acs.orglett.5c00666.s003",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*130 ol5c00666 */"https://doi.org/10.1021/acs.orglett.5c00666.s004",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*131 ol5c00705 */"https://doi.org/10.1021/acs.orglett.5c00705.s002",//Generation and Use of Bicyclo[1.1.0]butyllithium under Continuous Flow Conditions
			/*132 ol5c00711 */"https://doi.org/10.1021/acs.orglett.5c00711.s002",//Highly Stereoselective [2 + 4] Annulation of Phosphenes and Enones with β‑Electron-Donating Groups
			/*132 ol5c00711 */"https://doi.org/10.1021/acs.orglett.5c00711.s003",//Highly Stereoselective [2 + 4] Annulation of Phosphenes and Enones with β‑Electron-Donating Groups
			/*133 ol5c00782 */"https://doi.org/10.1021/acs.orglett.5c00782.s001",//Carbonyl vs Hydroxy: Rhodium catalyzed carbonyl ylide triggered diastereoselective synthesis of 2,5-methano-1,3-benzoxazepines
			/*134 ol5c00806 */"https://doi.org/10.1021/acs.orglett.5c00806.s001",//Modular Approach for Photoinduced Cycloaddition Enabling the Synthesis of Diverse Bioactive Oxazoles

			/*135 ol5c00879 */"https://doi.org/10.1021/acs.orglett.5c00879.s002",//Total Synthesis of (±)-Streptoglyceride A and of Putative (±)-Streptoglyceride C
			/*136 ol5c00933 */"https://doi.org/10.1021/acs.orglett.5c00933.s001",//A Mechanistic Perspective on Photocatalytic EnT-Enabled C3-N-Heteroarylation of Aryl Quinoxaline via C(sp2)–C(sp2) Coupling
			/*137 ol5c00948 */"https://doi.org/10.1021/acs.orglett.5c00948.s002",//Nitroalkanes as Ketone Synthetic Equivalents in C–N and C–S Bond Formation Reactions
			/*138 ol5c00976 */"https://doi.org/10.1021/acs.orglett.5c00976.s002",//CAN-Mediated Synthesis of 2‑Deoxy Sugars: Access to Chiral 2‑Deoxy 3‑Bisindolyl‑C‑glycosides
			/*139 ol5c00995 */"https://doi.org/10.1021/acs.orglett.5c00995.s002",//Unlocking the Potential of CF3-Alkynes in Gold-Catalyzed Oxygen Transfer: A Direct Route to Trifluoromethylated Compounds
			/*140 ol5c01081 */"https://doi.org/10.1021/acs.orglett.5c01081.s002",//Ce(OTf)<sub>3</sub>‑Catalyzed Asymmetric 6π Cyclization of Triaryldivinyl Ketones
			/*140 ol5c01081 */"https://doi.org/10.1021/acs.orglett.5c01081.s003",//Ce(OTf)3‑Catalyzed Asymmetric 6π Cyclization of Triaryldivinyl Ketones
			/*141 ol5c01172 */"https://doi.org/10.1021/acs.orglett.5c01172.s002",//Synthesis of Parent Peropyrene and Its Derivatization
			/*142 ol5c01196 */"https://doi.org/10.1021/acs.orglett.5c01196.s002",//Reaction Profile Forecasting by Artificial Data Generation for Wittig-Type Geminal Bromofluoroolefination
			/*143 ol5c01237 */"https://doi.org/10.1021/acs.orglett.5c01237.s002",//Electrochemical Functionalization of Alkenes with 1,3-Dicarbonyl Compounds via Radical Addition

			/*144 ol5c01472 */"https://doi.org/10.1021/acs.orglett.5c01472.s002",//Synthesis of 8H‑Indolo[3,2,1-de]phenanthridin-8-ones via Pd(OAc)2‑Catalyzed Double Oxidative Coupling Dehydrogenations
			/*145 ol5c01509 */"https://doi.org/10.1021/acs.orglett.5c01509.s002",//Bioinspired Stereoselective Total Synthesis of the Caged Sesquiterpenoid Daphnepapytone A

			/*146 ol5c01576 */"https://doi.org/10.1021/acs.orglett.5c01576.s002",//Ru(II)-Catalyzed Reactions of ortho-Hydroxy Enaminones with Diazonaphthoquinones: Synthesis of Unsymmetrical Biaryl Diols

			/*147 ol5c01713 */"https://doi.org/10.1021/acs.orglett.5c01713.s002",//Scalable Photoinduced Cycloaddition for the Synthesis of Biorelevant Oxazoles
			/*148 ol5c01721 */"https://doi.org/10.1021/acs.orglett.5c01721.s002",//Manganese Complex-Catalyzed (De)hydrogenative Cyclization toward the Selective Synthesis of 2‑Substituted and 2,3-Disubstituted 4‑Quinolones
			/*149 ol5c01805 */"https://doi.org/10.1021/acs.orglett.5c01805.s002",//P(O)Me2–Alkenes: From Synthesis to Applications

			/*150 ol5c01936 */"https://doi.org/10.1021/acs.orglett.5c01936.s001",//General Strategy to Access N‑Aryl Heptamethine Indocyanines for Optical Tuning

			/*151 ol5c01966 */"https://doi.org/10.1021/acs.orglett.5c01966.s002",//Chan-Evans-Lam Cu(II)-Catalyzed C–O Cross-Couplings: Broadening Synthetic Access to Functionalized Vinylic Ethers
			/*152 ol5c02004 */"https://doi.org/10.1021/acs.orglett.5c02004.s002",//Ligand-Enabled Room-Temperature Three-Component Strategy for Mono-α-arylation of Acetone with Cyclic Diaryliodonium Salts and Alkenes

			/*153 ol5c02079 */"https://doi.org/10.1021/acs.orglett.5c02079.s002",//Access to SCF3‑Substituted Indolizines via a Photocatalytic Late-Stage Functionalization Protocol
			/*154 ol5c02150 */"https://doi.org/10.1021/acs.orglett.5c02150.s002",//All-Aza meso-Benzo-Fused Triphyrins(2.1.1): Large Bathochromic Shift and Intensified Absorption via π‑Extended Conjugation


			/*155 ol5c02286 */"https://doi.org/10.1021/acs.orglett.5c02286.s002",//Rh(III)-Catalyzed Atroposelective C–H Cyanation of 1‑Aryl benzo[h]isoquinolines
			/*156 ol5c02366 */"https://doi.org/10.1021/acs.orglett.5c02366.s002",//Ir(III)-Photocatalyzed Stereoselective Synthesis of 2‑Azadienes
			/*157 ol5c02373 */"https://doi.org/10.1021/acs.orglett.5c02373.s001",//Copper-Catalyzed Enantioselective Acylcyanation of Alkenes

			/*158 ol5c02418 */"https://doi.org/10.1021/acs.orglett.5c02418.s001",//Inherent Weakly Coordinating Oxo-Group-Directed Ruthenium(II)-Catalyzed C5 Functionalization of 2‑Arylquinolin-4(1H)‑ones
			/*159 ol5c02448 */"https://doi.org/10.1021/acs.orglett.5c02448.s001",//Irradiation of Bifunctional Masked Ketone Pro-Aromatics Unveils Autoinductive Autocatalysis via Electron Donor–Acceptor (EDA) Complexes
			/*160 ol5c02494 */"https://doi.org/10.1021/acs.orglett.5c02494.s001",//HFIP-Assisted Denitrogenative Radical Fragmentation Pathway of Areneazo-2-(2-nitro)propanes: Cross-Coupling of Aryl Radicals with Alkynes and Coupling Reagents


			/*161 ol5c02536 */"https://doi.org/10.1021/acs.orglett.5c02536.s001",//Nickel-Catalyzed Selective Monoamination of 1,2-Diols: An Affordable Approach to Amino Alcohols

			/*162 ol5c02563 */"https://doi.org/10.1021/acs.orglett.5c02563.s001",//Access to Tetrahydropyrido[1,2‑a]indol-6-one Derivatives via NHC-Catalyzed Radical Dearomatization of Indoles

			/*163 ol5c02636 */"https://doi.org/10.1021/acs.orglett.5c02636.s001",//Iridium-Catalyzed Regio- and Enantioselective C7-Allylic Alkylation of 4‑Aminoindoles
			/*163 ol5c02636 */"https://doi.org/10.1021/acs.orglett.5c02636.s002",//Iridium-Catalyzed Regio- and Enantioselective C7-Allylic Alkylation of 4‑Aminoindoles

			/*164 ol5c02670 */"https://doi.org/10.1021/acs.orglett.5c02670.s001",//Visible-Light-Promoted Selenylative Radical Cyclization of 2‑Vinyl‑N‑Aryl Imines to 3H‑Indoles via Oxygen Photosensitization
			/*165 ol5c02689 */"https://doi.org/10.1021/acs.orglett.5c02689.s001",//Isolation of a Transition-State Geometry via Intermolecular Stabilization in the Solid State

			/*166 ol5c02724 */"https://doi.org/10.1021/acs.orglett.5c02724.s001",//Rapid Access to Spiro[4.6]-(di)benzodiazepine Indenes: Regioselective Rh(III)-Catalyzed Cascade C–H Activation/Spiroannulation
			/*167 ol5c02783 */"https://doi.org/10.1021/acs.orglett.5c02783.s001",//Photoinduced Ligated Boryl Radical-Mediated Alkynylation of Inert Iodoalkanes
			/*168 ol5c02876 */"https://doi.org/10.1021/acs.orglett.5c02876.s001",//Construction of Acyclic Vicinal All-Carbon Quaternary and Tertiary Carbon Stereocenters via a Stereoselective Claisen Rearrangement
			/*169 ol5c02878 */"https://doi.org/10.1021/acs.orglett.5c02878.s001",//A Visible-Light Organophotoredox-Catalyzed anti-Markovnikov Hydrocarbamoylation of Unactivated Alkene and Cyclization Cascade via Radical Relay
			/*170 ol5c03138 */"https://doi.org/10.1021/acs.orglett.5c03138.s002",//Visible-Light-Induced CdSeS/CdZnSe/ZnSe/ZnS Quantum-Dot-Catalyzed Three-Component Reaction for the Synthesis of α‑(4-Pyridyl)benzylaniline Derivatives
			/*171 ol5c03157 */"https://doi.org/10.1021/acs.orglett.5c03157.s002",//Construction of Benzothiophene-Fused 1,2,4-Triazocines via a Skeletal Editing Strategy and DFT Study on the Reaction Mechanism
			/*171 ol5c03157 */"https://doi.org/10.1021/acs.orglett.5c03157.s003",//Construction of Benzothiophene-Fused 1,2,4-Triazocines via a Skeletal Editing Strategy and DFT Study on the Reaction Mechanism
			/*172 ol5c03186 */"https://doi.org/10.1021/acs.orglett.5c03186.s001",//Alectinib Synthesis through Formal α‑Arylation of Enone
			/*173 ol5c03195 */"https://doi.org/10.1021/acs.orglett.5c03195.s002",//Easy Access to Vinylene-Linked Conjugated Homopolymers from Phosphonates via an O2‑Mediated Aldehyde-Free Strategy
			/*174 ol5c03229 */"https://doi.org/10.1021/acs.orglett.5c03229.s002",//Visible-Light-Induced Difunctionalization of Styrenes with Aryl Hydrazines and tert-Butyl Nitrite: One-Pot Access to α‑Arylethanone Oximes
			/*175 ol5c03241 */"https://doi.org/10.1021/acs.orglett.5c03241.s001",//Photochemical Flow Synthesis of Trisubstituted Oxazoles Enabled by High-Power UV–B LED Modules
			/*176 ol5c03345 */"https://doi.org/10.1021/acs.orglett.5c03345.s002",//Three-Component Reaction through Rh(III)-Catalyzed Strain Release of Bicyclo[1.1.0]butanes



			/*177 ol5c03615 */"https://doi.org/10.1021/acs.orglett.5c03615.s001",//Regiodivergent Addition of Allylic Acetals in Ruthenium-Catalyzed C–H/N–H Annulation Reaction
			/*178 ol5c03626 */"https://doi.org/10.1021/acs.orglett.5c03626.s002",//Furan Oxidation-Cyclization to Oxazepines: Favoring 7‑exo-trig over 6‑endo-trig and 5‑exo-trig Trajectories


			/*179 ol5c03906 */"https://doi.org/10.1021/acs.orglett.5c03906.s002",//C‑4 Functional Group-Driven Stereodivergent Synthesis and Late-Stage Diversification of 6‑Deoxy l‑talo/gulo Rare Sugars
			/*180 ol5c04004 */"https://doi.org/10.1021/acs.orglett.5c04004.s002",//Pd-Catalyzed Enantioselective Double C–H Activation and Transmetalation: Synthesis of 2‑Heteroaryl/aryl-Ferrocenealdehydes



			/*181 ol5c04277 */"https://doi.org/10.1021/acs.orglett.5c04277.s002",//Copper-Catalyzed Divergent C–S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*181 ol5c04277 */"https://doi.org/10.1021/acs.orglett.5c04277.s003",//Copper-Catalyzed Divergent C–S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*181 ol5c04277 */"https://doi.org/10.1021/acs.orglett.5c04277.s004",//Copper-Catalyzed Divergent C–S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*181 ol5c04277 */"https://doi.org/10.1021/acs.orglett.5c04277.s005",//Copper-Catalyzed Divergent C–S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation

			/*182 ol5c04403 */"https://doi.org/10.1021/acs.orglett.5c04403.s001",//Visible-Light-Promoted Catalytic Redox-Neutral Alkynylation of Alkyl Boronic Acids/Boronates

			/*183 ol5c04507 */"https://doi.org/10.1021/acs.orglett.5c04507.s002",//Photoredox/Nickel Dual-Catalyzed Asymmetric C‑Alkylation of Nitroalkanes with α‑Bromoketones


			/*184 ol5c04750 */"https://doi.org/10.1021/acs.orglett.5c04750.s002",//Synthesis of Carborane-Fused Lactones via Electrochemical C(sp3)–H Lactonization: Access to Benzofused Lactone Bioisosteres
			/*185 ol5c04969 */"https://doi.org/10.1021/acs.orglett.5c04969.s001",//Access to Alkylative/Hydrodefluorination of Trifluoromethyl Ketones Using Photoexcited Dihydropyridines






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
				if (info != null) {
					articles.add(info);
					System.out.println("article " + articles.size() + ": " + info.acscode);
				}
				info = new ACSInfo(articles.size() + 1, pubdoi);
				lastPubDOI = pubdoi;
			}
			String supplementId = datadoi.substring(datadoi.length() - 3); // 004
			info.add(supplementId);
		}
		if (info != null) {
			articles.add(info);
			System.out.println("article " + articles.size() + ": " + info.acscode);
		}
		System.out.println(articles.size() + " test articles; " + acs2TestSet.length + " SI ZIP file");
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
			localSourceArchive = localSourceACS;
			targetDir = localTargetDir;
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

	static String localSourceACS = "c:/temp/iupac/acs2/test2";// -";
	static String localTargetDir = "c:/temp/iupac/acs2/acs2025";

	public static void main(String[] args) {
		// args[] may override localSourceArchive as ars[1] 
		// and testDir as args[2]; args[0] is ignored;
		int first = 145; // first test to run
		int last = 145; // last test to run
		run(args, first, last, localSourceACS, localTargetDir);
	}
}