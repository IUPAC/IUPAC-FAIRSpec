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
				+ IFDObject.nRepresentations + " representatios\n");

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
			
			

			/*1*/"https://doi.org/10.1021/acs.joc.4c02089.s002",//Ring-Size-Dependent Selectivity of the Î²â€‘Hydride Elimination in Heck-Type Arylations of exo-Methylene Cycloketones: Chalcones versus 2â€‘Benzyl-1-naphthols
			/*2 fails glolal directory F-NMR lost*/"https://doi.org/10.1021/acs.joc.4c02094.s002",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			/*2*/"https://doi.org/10.1021/acs.joc.4c02094.s002",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			/*2*/"https://doi.org/10.1021/acs.joc.4c02094.s003",//Meta-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			/*2*/"https://doi.org/10.1021/acs.joc.4c02094.s004",//<i>Meta</i>-, Regioselective Amination of Cyclic Diaryliodoniums through Câ€“I and Câ€“O Bond Cleavages: An Access to Functionalized Coumarins
			/*3*/"https://doi.org/10.1021/acs.joc.4c02335.s002",//TEMPO-Mediated Direct C(sp2)â€“H Alkoxylation/Aryloxylation of 1,4-Quinones
			/*4*/"https://doi.org/10.1021/acs.joc.4c02429.s002",//Electrochemical Cyclizationâ€“Desulfurization Approach for the Synthesis of 1,3-Benzoxazines Using Cascade Câ€“O and Câ€“N Bond Formation
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s002",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s003",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s004",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s005",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s006",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s007",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s008",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*5*/"https://doi.org/10.1021/acs.joc.4c02450.s009",//Four Alkaloids from Alstonia scholaris with Antitumor Activity via Disturbing Glutathione Homeostasis
			/*6*/"https://doi.org/10.1021/acs.joc.4c02490.s002",//CO2â€¢â€“ Enabled Synthesis of Phenanthridinones, Oxindoles, Isoindolinones, and Spirolactams


			/*7*/"https://doi.org/10.1021/acs.joc.4c02600.s002",//Visible-Light-Mediated Three-Component Alkene 1,2-Alkylpyridylation Reaction Using Alkylboronic Acids as Radical Precursors for the Synthesis of 4â€‘Alkylpyridines
			/*8 embedded Bruker directories and misplaced MNova files*/"https://doi.org/10.1021/acs.joc.4c02622.s001",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*8*/"https://doi.org/10.1021/acs.joc.4c02622.s002",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*8*/"https://doi.org/10.1021/acs.joc.4c02622.s004",//Direct Synthesis of 2â€‘Functionalized 3â€‘Nitroindoles from Diazo(nitro)acetanilides
			/*9*/"https://doi.org/10.1021/acs.joc.4c02634.s002",//Synthesis of Nâ€‘Heterocycle-Ligated Porphyrins Using Iodobenzene Diacetate Enabled Regioselective Cross-Dehydrogenation of Porphyrins and NH-Heteroaromatics
			/*10*/"https://doi.org/10.1021/acs.joc.4c02638.s002",//NHC-BH3â€‘Mediated Reduction of Sulfonyl Hydrazides into Disulfides and Further Cross-Coupling with Chlorostibine and Bioactivities
			/*11*/"https://doi.org/10.1021/acs.joc.4c02652.s002",//Aryl Borane as a Catalyst for Dehydrative Amide Synthesis
			/*12*/"https://doi.org/10.1021/acs.joc.4c02669.s001",//Controlling the Symmetry of Perylene Derivatives via Selective ortho-Borylation

			/*13 fails one cmpd per zip file; fails first column is NOT global*/"https://doi.org/10.1021/acs.joc.4c02689.s001",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*13*/"https://doi.org/10.1021/acs.joc.4c02689.s002",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge <i>Spongia officinalis</i> and Their Anti-inflammatory Activities
			/*13*/"https://doi.org/10.1021/acs.joc.4c02689.s003",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*13*/"https://doi.org/10.1021/acs.joc.4c02689.s004",//Uncommon Diterpenoids with Diverse Frameworks from the South China Sea Sponge Spongia officinalis and Their Anti-inflammatory Activities
			/*14*/"https://doi.org/10.1021/acs.joc.4c02691.s002",//Thiazoles via Formal [4 + 1] of NaSH to (Z)â€‘Bromoisocyanoalkenes
			/*15*/"https://doi.org/10.1021/acs.joc.4c02715.s002",//From Pseudocyclic to Macrocyclic Ionophores: Strategies toward the Synthesis of Cyclic Monensin Derivatives
			/*16*/"https://doi.org/10.1021/acs.joc.4c02737.s002",//Photooxidation and Cleavage of Ethynylated 9,10-Dimethoxyanthracenes with Acid-Labile Ether Bonds
			/*17*/"https://doi.org/10.1021/acs.joc.4c02787.s002",//Annulative Coupling of Sulfoxonium Ylides with Aldehydes and Naphthols or Coumarins: Easy Access to Fused Dihydrofurans

			/*18*/"https://doi.org/10.1021/acs.joc.4c02842.s002",//One-Pot Alkynylation/Isomerization Cascade of Î²â€‘Formylated Enoates to Functionalized Ynones
			/*19*/"https://doi.org/10.1021/acs.joc.4c02844.s002",//Heteroarylstilbenes: Visible-Light-Tunable Photochromic Systems in Water
			/*20*/"https://doi.org/10.1021/acs.joc.4c02860.s001",//Nickel-Catalyzed One-Pot H/D Exchange and Asymmetric Michael Addition in the Presence of D2O
			/*21*/"https://doi.org/10.1021/acs.joc.4c02893.s002",//A Photo-Smiles Rearrangement: Mechanistic Investigation of the Formation of Blatter Radical Helicenes
			/*22*/"https://doi.org/10.1021/acs.joc.4c02921.s002",//Enantioselective Synthesis of Spirocyclic Isoxazolones Using a Conia-Ene Type Reaction
			/*23*/"https://doi.org/10.1021/acs.joc.4c02967.s002",//Vicinal Thiosulfonylation of ortho-(Alkynyl)benzyl Thiosulfonates/Sulfurothioates for Direct Synthesis of Sulfonyl-Derived Isothiochromenes
			/*24*/"https://doi.org/10.1021/acs.joc.4c03015.s002",//Bâ†N Lewis Pair-Functionalized Perylenes: Tuning Optoelectronic Properties via Regioisomerization
			/*25*/"https://doi.org/10.1021/acs.joc.4c03046.s002",//Quantification of the Hâ€‘Bond-Donating Ability of Trifluoromethyl Ketone Hydrates Using a Colorimetric Sensor
			/*25*/"https://doi.org/10.1021/acs.joc.4c03046.s003",//Quantification of the Hâ€‘Bond-Donating Ability of Trifluoromethyl Ketone Hydrates Using a Colorimetric Sensor
			/*26*/"https://doi.org/10.1021/acs.joc.4c03071.s001",//Tandem Synthesis of Polysubstituted Pyrroles via Cu(I)-Catalyzed Cyclization of Ketene N,S-Acetals with Î²â€‘Ketodinitriles
			/*27*/"https://doi.org/10.1021/acs.joc.4c03085.s002",//A Route to the Mild Synthesis of Î±â€‘Selenomethylketones via Vinyl Azides
			/*28*/"https://doi.org/10.1021/acs.joc.4c03092.s002",//Regio- and Chemoselective Synthesis of Spiro Cyclobutane-Isobenzofuranimines via Cascade Oxycyclization and [2 + 2] Cycloaddition of oâ€‘Alkynolbenzamides
			/*29*/"https://doi.org/10.1021/acs.joc.4c03094.s002",//Synthesis of a Natural Product-Based 5Hâ€‘Thiazolo[5â€²,4â€²:5,6]pyrido[2,3â€‘b]indole Derivative via Solid-Phase Synthesis
			/*30*/"https://doi.org/10.1021/acs.joc.4c03118.s001",//Atmosphere Effects on Arene Reduction with Lithium and Ethylenediamine in THF
			/*31*/"https://doi.org/10.1021/acs.joc.4c03140.s002",//Asymmetric Total Synthesis of Gymnothespirolignan A via a Bioinspired Double Cyclization Approach
			/*32*/"https://doi.org/10.1021/acs.joc.4c03150.s004",//A Twist on Controlling the Equilibrium of Dynamic Thia-Michael Reactions
			/*32*/"https://doi.org/10.1021/acs.joc.4c03150.s005",//A Twist on Controlling the Equilibrium of Dynamic Thia-Michael Reactions
			/*33*/"https://doi.org/10.1021/acs.joc.4c03151.s002",//Minisci Câ€“H Alkylation of Heterocycles with Unactivated Alkyl Iodides Enabled by Visible Light Photocatalysis
			/*34*/"https://doi.org/10.1021/acs.joc.4c03180.s002",//PhICl2/KSeCN Mediated Synthesis of Selenopheno[3,2â€‘b]indoles and 3â€‘Selenocyanato-2-benzoselenophene Indoles from 1,3-Diynes via Double Electrophilic Cyclization
			/*35*/"https://doi.org/10.1021/acs.joc.4c03181.s002",//(3+2) Annulation of Donorâ€“Acceptor Cyclopropanes with Difluoroenoxysilanes: Syntheses of gem-Difluorocyclopentenes via Î±,Î±-Difluoroketone Scaffolds
			/*36*/"https://doi.org/10.1021/acs.joc.5c00007.s001",//C3-Heteroarylation of Indoles via Cu(II)-Catalyzed Aminocupration and Subsequent Nucleophilic Addition of oâ€‘Alkynylanilines
			/*37*/"https://doi.org/10.1021/acs.joc.5c00061.s002",//Dynamically Generated Carbenium Species via Photoisomerization of Cyclic Alkenes: Mild Friedelâ€“Crafts Alkylation

			/*38*/"https://doi.org/10.1021/acs.joc.5c00117.s002",//Sodium Poly(heptazine imide)-Enabled Oxytrifluoromethylation of Alkenes for the Synthesis of Î±â€‘CF3 Ketones
			/*39*/"https://doi.org/10.1021/acs.joc.5c00149.s002",//Photoinduced Cascade Synthesis of Oxindoles and Isoquinolinediones
			/*40*/"https://doi.org/10.1021/acs.joc.5c00156.s002",//Regioselective Synthesis of (E)â€‘3-((Alkylamino)methylene)-2-thioxothiochroman-4-one Derivatives and Their Regioselective Cycloaddition with Alkynes
			/*41*/"https://doi.org/10.1021/acs.joc.5c00161.s002",//Rise of Ketone Î±â€‘Hydrolysis: Revisiting SNAcyl, E1cB Mechanisms and Carbon-Based Leaving Groups in One Reaction for Drug-Targeting Applications
			/*42*/"https://doi.org/10.1021/acs.joc.5c00171.s002",//Intramolecular ortho Photocycloaddition of 4â€‘Substituted 7â€‘(4â€²-Alkenyloxy)-1-indanones and Ensuing Reaction Cascades

			/*43*/"https://doi.org/10.1021/acs.joc.5c00184.s002",//Palladium Catalyst-Controlled Regiodivergent Câ€“H Arylation of Thiazoles
			/*44*/"https://doi.org/10.1021/acs.joc.5c00188.s001",//Synthesis of Sulfonylthiophenes through [3+2] Cycloaddition of Pyridinium 1,4-Zwitterionic Thiolates with (E)â€‘Î²-Iodovinyl Sulfones or Bromoallylsulfones
			/*45*/"https://doi.org/10.1021/acs.joc.5c00189.s002",//Synthesis of Phenanthrenes and Naphthoquinone Fused Benzoxepines via Hauserâ€“Kraus Annulation of Sulfonylphthalide with Moritaâ€“Baylisâ€“Hillman Adducts of Nitroalkenes
			/*46*/"https://doi.org/10.1021/acs.joc.5c00193.s002",//Photoinduced Regio- and Stereoselective Hydrotrifluoromethylation of Glycals with Langlois Reagent


			/*48*/"https://doi.org/10.1021/acs.joc.5c00246.s002",//In Situ Generation and [3 + 2] Annulation Reactions of Propiolaldehydeî—¸A Metal-Free, Cascade Route to Pyrazole and Bipyrazole Carboxaldehydes in One Pot
			/*49*/"https://doi.org/10.1021/acs.joc.5c00270.s002",//3,3-Bis(hydroxyaryl)oxindoles and Spirooxindoles Bearing a Xanthene Moiety: Synthesis, Mechanism, and Biological Activity
			/*50*/"https://doi.org/10.1021/acs.joc.5c00284.s003",//Deconstruction of Desacetamidocolchicineâ€™s B Ring Reveals a Class 3 Atropisomeric AC Ring with Tubulin Binding Properties
			/*50*/"https://doi.org/10.1021/acs.joc.5c00284.s004",//Deconstruction of Desacetamidocolchicineâ€™s B Ring Reveals a Class 3 Atropisomeric AC Ring with Tubulin Binding Properties
			/*51*/"https://doi.org/10.1021/acs.joc.5c00316.s002",//Base-Assisted and Silica Gel-Promoted Indole-Substituted Indene Synthesis
			/*52*/"https://doi.org/10.1021/acs.joc.5c00317.s002",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids Bâ€“D Via Ireland-Claisen Rearrangement
			/*52*/"https://doi.org/10.1021/acs.joc.5c00317.s003",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids Bâ€“D Via Ireland-Claisen Rearrangement
			/*52*/"https://doi.org/10.1021/acs.joc.5c00317.s004",//Construction of the Tetracyclic Skeleton of Polycyclic Norcembranoids Sinudenoids Bâ€“D Via Ireland-Claisen Rearrangement
			/*53*/"https://doi.org/10.1021/acs.joc.5c00330.s002",//Synthesis of 3,6-Dihydroâ€‘2Hâ€‘thiopyrans from Î±â€‘Diazo-Î²-diketones and Vinylthiiranes via [5 + 1] Annulation
			/*53*/"https://doi.org/10.1021/acs.joc.5c00330.s003",//Synthesis of 3,6-Dihydroâ€‘2Hâ€‘thiopyrans from Î±â€‘Diazo-Î²-diketones and Vinylthiiranes via [5 + 1] Annulation


			/*54*/"https://doi.org/10.1021/acs.joc.5c00369.s002",//Phenyl Spacer Modulation of 2,5-Substituted Dâ€“A-Type Siloles for Efficient Nondoped OLEDs
			/*55*/"https://doi.org/10.1021/acs.joc.5c00389.s002",//The Anthranil Core as a Ï€â€‘Conjugated Bridge in the Synthesis of Molecular Photosensitizers

			/*57*/"https://doi.org/10.1021/acs.joc.5c00424.s002",//Visible-Light-Driven Tandem Cyclization of <i>o</i>â€‘Hydroxyaryl Enaminones: Access to 3â€‘(Î±-Arylsulfonamido)trifluoroethyl Chromones
			/*58*/"https://doi.org/10.1021/acs.joc.5c00472.s002",//FeCl3/TBHP-Mediated Oxidation of Indoles: Divergent Product Selectivity under Mechanochemical and Solution-Based Conditions
			/*59*/"https://doi.org/10.1021/acs.joc.5c00474.s002",//One-Pot High-Yield Synthesis of a Tripodal Triamine Building Block in an Ammonia Ethanolic Solution
			/*60*/"https://doi.org/10.1021/acs.joc.5c00508.s001",//Asymmetric and Symmetric S-zig-zag-Fused BODIPYs: Synthesis and Photophysical and Oxidative Properties
			/*61*/"https://doi.org/10.1021/acs.joc.5c00513.s002",//Blue-Light-Irradiated Copper-Catalyzed Regio-tunable Double Câ€“H/Câ€“H Cross-Coupling Reaction: A Sustainable Approach to Construct C2â€‘Indolyl-1,4-naphthoquinones
			/*62*/"https://doi.org/10.1021/acs.joc.5c00521.s001",//Synthesis of Octatrimethylsilyl-[8]cycloparaphenylene for Multifunctionalized Cycloparaphenylene


			/*63*/"https://doi.org/10.1021/acs.joc.5c00545.s002",//Total Synthesis of the Melodinus Alkaloid (Â±)-Melohemsine K
			/*64*/"https://doi.org/10.1021/acs.joc.5c00606.s002",//Reversibility and Enantioselectivity of Palladium-Catalyzed Allylic Aminations: Ligand, Base-Additive, and Solvent Effects
			/*65*/"https://doi.org/10.1021/acs.joc.5c00609.s002",//Curcumin as a Cinnamoyl Transfer Reagent via Câ€“C(CO) Bond Scissoring in the Microwave-Assisted Reaction with Hydroxyâ€‘pâ€‘QMs




			/*66*/"https://doi.org/10.1021/acs.joc.5c00649.s002",//Do We See the True Color of Anthocyanidins?

			/*67*/"https://doi.org/10.1021/acs.joc.5c00670.s002",//1,3-Dipolar Cycloaddition of Nitrile Imines to 2â€‘Imino-thiazolo[3,2â€‘a]pyrimidin-3-ones: Dipole-Initiated Thiazolone-Imidazolone Rearrangement


			/*69*/"https://doi.org/10.1021/acs.joc.5c00727.s002",//Regiodivergence in the Cycloadditions between a Cyclic Nitrone and Carbonyl-Type Dipolarophiles
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s001",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s002",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s003",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s004",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s005",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s006",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s007",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s008",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s009",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy
			/*70*/"https://doi.org/10.1021/acs.joc.5c00741.s010",//Photocyclization of 8â€‘Aryloxybenzo[e][1,2,4]triazines Revisited: Unambiguous Structural Assignment of Planar Blatter Radicals by Correlation NMR Spectroscopy

			/*72*/"https://doi.org/10.1021/acs.joc.5c00774.s002",//Inhibited Hydrolysis of 4â€‘(N,Nâ€‘Dimethylamino)aryl Imines in a Weak Acid by Supramolecular Encapsulation inside Cucurbit[7]uril

			/*74*/"https://doi.org/10.1021/acs.joc.5c00794.s002",//Triplet Ketone Catalysis-Enabled Functionalization of Thioanisoles with Maleimides


			/*76*/"https://doi.org/10.1021/acs.joc.5c00884.s001",//NCS-Mediated Direct Synthesis of Î²â€‘Chlorosulfoxides from Unactivated Alkenes and Thiophenols
			/*77*/"https://doi.org/10.1021/acs.joc.5c00912.s002",//Câ€“C Coupling Enabled by Mn Complexes with Diacyl Peroxides: Alkylation versus Oxygenation

			/*78*/"https://doi.org/10.1021/acs.joc.5c00952.s002",//Distinct Selectivity of 2â€‘Aryl Thioquinazolinones in the Sulfur Directing Rh(III)-Catalyzed Amidation Reaction
			/*79*/"https://doi.org/10.1021/acs.joc.5c00954.s002",//Further Confirmation of the Structure of 3â€²-(2-Pyridyldithio)-3â€²-deoxyadenosine and 3â€²-Thio-3â€²-deoxyadenosine: Synthetic Convergence with Cordycepin

			/*81*/"https://doi.org/10.1021/acs.joc.5c01002.s001",//Synthesis of 2â€‘Hydroxycarbazoles via Rh(III)-Catalyzed Cascade Cyclization of Indolyl Nitrones with Alkylidenecyclopropanes
			/*82*/"https://doi.org/10.1021/acs.joc.5c01060.s002",//In-Catalyzed Regioselective [3+2] Cycloaddition of Alkenes with Î±,Î²-Unsaturated Keto-Carboxylic Acid Derivatives for the Synthesis of Î³â€‘Butyrolactones
			/*83*/"https://doi.org/10.1021/acs.joc.5c01068.s002",//Stereocontrolled Ring-Opening of Oxazolidinone-Fused Aziridines for the Synthesis of 2â€‘Amino Ethers
			/*84*/"https://doi.org/10.1021/acs.joc.5c01082.s002",//Pd-Catalyzed One-Pot Synthesis of Indole-3-carboxylic Acids via a Sequential Water-Mediated Nucleophilic Addition to Amides/Esters and Carbonâ€“Heteroatom Cross-Coupling Reaction Strategy
			/*84*/"https://doi.org/10.1021/acs.joc.5c01082.s003",//Pd-Catalyzed One-Pot Synthesis of Indole-3-carboxylic Acids via a Sequential Water-Mediated Nucleophilic Addition to Amides/Esters and Carbonâ€“Heteroatom Cross-Coupling Reaction Strategy

			/*86*/"https://doi.org/10.1021/acs.joc.5c01113.s002",//Phenanthrene-Fused BN-Acenaphth(yl)ene: Synthesis, Structures, and Photophysical Studies
			/*87*/"https://doi.org/10.1021/acs.joc.5c01140.s002",//Solvent Effects on Câ€“H Abstraction by Hydroperoxyl Radicals: Implication for Antioxidant Strategies
			/*88*/"https://doi.org/10.1021/acs.joc.5c01156.s002",//Annulative Coupling of Î²â€‘Ketosulfoxonium Ylides, Î²â€‘Ketothioamides, and Aldehydes: Access to Highly Functionalized Dihydrothiophenes
			/*89*/"https://doi.org/10.1021/acs.joc.5c01207.s001",//tert-BuONO-Promoted Nitrosation of 4â€‘Nitroisoxazole-Based Enamines: Synthesis of 5â€‘Cyanoisoxazoles and Their Application

			/*91*/"https://doi.org/10.1021/acs.joc.5c01245.s002",//Electrochemically Oxidative Câ€“H/Nâ€“H Cross-Coupling Reactions of Sulfoximines with Imidazopyridines
			/*92*/"https://doi.org/10.1021/acs.joc.5c01279.s003",//Iron-Catalyzed Oxidation of Triols Containing Vicinal Diols into Hydroxylactones: A DFT Study
			/*93*/"https://doi.org/10.1021/acs.joc.5c01353.s002",//Chiral P,N,N-Ligands for the Manganese-Catalyzed Asymmetric Formal Hydroamination of Allylic Alcohols
			/*94*/"https://doi.org/10.1021/acs.joc.5c01387.s002",//A Noncarbenoid Approach to Imidazolidines via ZnCl2â€‘Catalyzed Annulation of 4â€‘Alkoxycarbonyl-1,2-diaza-1,3-dienes with 1,3,5-Triazinanes
			/*95*/"https://doi.org/10.1021/acs.joc.5c01409.s002",//Total Synthesis of Alanense B via Stereoselective Enolate Alkylation
			/*96*/"https://doi.org/10.1021/acs.joc.5c01422.s002",//Regioselectivity Switching in the Tandem Reaction of Skipped Diynones with Allylic Alcohols: Stereoselective Synthesis of 4â€‘Allyl-2-Methylene-3(2<i>H</i>)â€‘Furanones
			/*97*/"https://doi.org/10.1021/acs.joc.5c01427.s002",//Substrate-Directed Annulative-Sulfonylation/Desulfonylation Cascade Using (E)â€‘Î²-Iodovinyl Sulfones: A Diverse Approach for the Synthesis of Imidazo[1,2â€‘a]pyridines with Sulfone Motifs
			/*98*/"https://doi.org/10.1021/acs.joc.5c01465.s002",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)â€“OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*98*/"https://doi.org/10.1021/acs.joc.5c01465.s003",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)â€“OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*98*/"https://doi.org/10.1021/acs.joc.5c01465.s004",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)â€“OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*98*/"https://doi.org/10.1021/acs.joc.5c01465.s005",//Copper-Catalyzed Vinylsulfonium Salt Diversification with P(O)â€“OH Bonds: Phosphoryloxylation via an Ionic Ring-Opening Process
			/*99*/"https://doi.org/10.1021/acs.joc.5c01466.s002",//Origin of Substituent-Modulated Regioselectivity in Phosphine-Catalyzed [3 + 2] Cyclization of Allenoates and Enones: A Kinetic Shift toward Curtinâ€“Hammett Control
			/*100*/"https://doi.org/10.1021/acs.joc.5c01490.s002",//Synthesis of 2â€‘Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted Câ€“H Bond Activation and Annulation of Anilines with CF3â€‘Imidoyl Sulfoxonium Ylides
			/*100*/"https://doi.org/10.1021/acs.joc.5c01490.s003",//Synthesis of 2â€‘Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted Câ€“H Bond Activation and Annulation of Anilines with CF3â€‘Imidoyl Sulfoxonium Ylides
			/*100*/"https://doi.org/10.1021/acs.joc.5c01490.s004",//Synthesis of 2â€‘Trifluoromethylindolines via Rh(III)-Catalyzed Chelation-Assisted Câ€“H Bond Activation and Annulation of Anilines with CF3â€‘Imidoyl Sulfoxonium Ylides
			/*101*/"https://doi.org/10.1021/acs.joc.5c01501.s002",//Electrochemical Oxidative Ring Opening of 1â€‘Acetylindoline-3-one to Access Î±â€‘Ketoamides
			/*102*/"https://doi.org/10.1021/acs.joc.5c01538.s002",//Concise, Atom-Economical, and Enantiodivergent Total Synthesis of Î²â€‘Lycorane via Organocatalyzed [4+2] Cycloaddition Reaction
			/*103*/"https://doi.org/10.1021/acs.joc.5c01545.s002",//Dual-State Mechano- and Electrochromic Responses Enabled by Sterically Strained Diazaanthraquinodimethanes












			/*105*/"https://doi.org/10.1021/acs.joc.5c01570.s002",//Gold-Catalyzed Single Oâ€‘Transfer to Internal CF3â€‘Alkynes. Regioselective Synthesis of 4â€‘Trifluoromethylated Oxazoles
			/*106*/"https://doi.org/10.1021/acs.joc.5c01584.s002",//Tunable Synthesis of Polysubstituted Pyrroles via Silver-Catalyzed (3 + 2) Cycloaddition of Î±,Î²-Unsaturated Nitroketones with Isocyanides

			/*108*/"https://doi.org/10.1021/acs.joc.5c01688.s001",//Pyrene-Bridged Tetrathienyles: Synthesis and Photochromism

			/*109*/"https://doi.org/10.1021/acs.joc.5c01759.s001",//Photoredox Catalyzed Cyclization of Enaminones/Ketene N,Sâ€‘Acetals with Î²â€‘Ketodinitriles to Access Polysubstituted Pyrroles

			/*111*/"https://doi.org/10.1021/acs.joc.5c01777.s001",//Synthesis of Unsymmetrical Diaryl Disulfides via I2/DMF-Assisted Reductive Cross-Coupling of Sodium Sulfinates with Arenediazonium Tetrafluoroborates/Sodium Metabisulfite

			/*112*/"https://doi.org/10.1021/acs.joc.5c01963.s002",//Palladium-Catalyzed Carbonylative Cross-Coupling of Aryl (Pseudo)halides and Cyclopropanols
			/*113*/"https://doi.org/10.1021/acs.joc.5c01989.s002",//Peptidyl Glycosyl Thiols and Disulfides for Enantioselective Hydrogen Atom Transfer (HAT) and Thiyl Radical Catalysis

			/*114*/"https://doi.org/10.1021/acs.joc.5c02047.s002",//Photoredox-Catalyzed Deoxygenative Câ€“H Alkylation of Azauracils via Xanthate-Activated Alcohols
			/*115*/"https://doi.org/10.1021/acs.joc.5c02084.s002",//Organophotoredox-Catalyzed Umpolung Strategy to Î²â€‘Fluoroamides via Carbamoylative Fluorination of Alkene in Aqueous Medium
			/*116*/"https://doi.org/10.1021/acs.joc.5c02091.s002",//Organophotocatalytic Regioselective Silylation/Germylation and Cascade Cyclization of 1,7-Dienes: Access to Silylated/Germylated Benzazepine Derivatives
			/*117*/"https://doi.org/10.1021/acs.joc.5c02110.s001",//Sequential Ni-Catalyzed Cross-Coupling and Ru-Catalyzed Isomerization of E/Z Alkenyl Ethers: A Stereoconvergent Route to Allylsilanes

			/*119*/"https://doi.org/10.1021/acs.joc.5c02152.s001",//Fe-BPsalan Complex-Catalyzed Asymmetric 1,3-Dipolar [3 + 2] Cycloaddition of Nitrones with Î±,Î²-Unsaturated Acyl Imidazoles
			/*119*/"https://doi.org/10.1021/acs.joc.5c02152.s002",//Fe-BPsalan Complex-Catalyzed Asymmetric 1,3-Dipolar [3 + 2] Cycloaddition of Nitrones with Î±,Î²-Unsaturated Acyl Imidazoles
			/*120*/"https://doi.org/10.1021/acs.joc.5c02154.s002",//Steric Hindrance-Driven Access to 3,4,7-Trihydro-1,2-oxaphosphepine 2â€‘Oxides from Vinyloxiranes and Phosphoryl Diazomethanes
			/*121*/"https://doi.org/10.1021/acs.joc.5c02221.s002",//Palladium-Catalyzed [3 + 2] Annulation of oâ€‘Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)â€“H Bond Activation to Yield 1â€‘Cyanoindane
			/*121*/"https://doi.org/10.1021/acs.joc.5c02221.s003",//Palladium-Catalyzed [3 + 2] Annulation of oâ€‘Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)â€“H Bond Activation to Yield 1â€‘Cyanoindane
			/*121*/"https://doi.org/10.1021/acs.joc.5c02221.s004",//Palladium-Catalyzed [3 + 2] Annulation of oâ€‘Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)â€“H Bond Activation to Yield 1â€‘Cyanoindane
			/*121*/"https://doi.org/10.1021/acs.joc.5c02221.s005",//Palladium-Catalyzed [3 + 2] Annulation of oâ€‘Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)â€“H Bond Activation to Yield 1â€‘Cyanoindane
			/*121*/"https://doi.org/10.1021/acs.joc.5c02221.s006",//Palladium-Catalyzed [3 + 2] Annulation of oâ€‘Bromobenzyl Cyanide with Norbornene Derivatives via C(sp3)â€“H Bond Activation to Yield 1â€‘Cyanoindane
			/*122*/"https://doi.org/10.1021/acs.joc.5c02305.s002",//Palladium-Catalyzed Denitrogenative Suzuki Coupling of Benzothiatriazine Dioxides for Bi(hetero)aryl-2-sulfonamides
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s001",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s002",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s003",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s004",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s005",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s006",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s007",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s008",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*123*/"https://doi.org/10.1021/acs.joc.5c02308.s009",//Characteristic Flavaglines with Therapeutic Potential for Ischemic Stroke from Aglaia perviridis
			/*124*/"https://doi.org/10.1021/acs.joc.5c02383.s001",//Chemoselective Synthesis of Fluorinated 1,3-Thiazines via Sonication-Assisted [3 + 3] Annulation of Thioamides and Î±â€‘CF3 Styrene


			/*127*/"https://doi.org/10.1021/acs.joc.5c02414.s002",//Benzo[1,2,3]thiadiazole as an Attractive Heteroaromatic Platform for Two-Photon Absorbing (TPA) Fluorophores: TPA Enhancement in Quasi-Quadrupolar S,N-Heteroarene-Cored Dyes via Skeletal Editing and Regioisomeric Control


			/*128*/"https://doi.org/10.1021/acs.joc.5c02497.s001",//Câ€“H/Nâ€“H Annulation of Nâ€‘Aryl Triazolinediones with Diazo Compounds: A Route to Cinnoline Analogues
			/*129*/"https://doi.org/10.1021/acs.joc.5c02500.s001",//A Concise Approach to Enantioselective Synthesis of Euolutchuols A, B, and D and Their Structure Assignment
			/*130*/"https://doi.org/10.1021/acs.joc.5c02524.s002",//Pd- and Cu-Cocatalyzed Anaerobic Olefin Aminoboration



			/*131*/"https://doi.org/10.1021/acs.joc.5c02653.s001",//Visible-Light-Induced Amination of Pyridylphosphonium Salts: Synthesis of Heteroarylamines via Radicalâ€“Radical Coupling

			/*133*/"https://doi.org/10.1021/acs.joc.5c02735.s002",//Enantioselective Organocatalytic Desymmetric Acylation as an Access to Orthogonally Protected myo-Inositols
			/*134*/"https://doi.org/10.1021/acs.joc.5c02870.s002",//Synthesis of Quinolizidine-Based 1,4-Azaphosphinines via Cyclization of Heteroarylmethyl(alkynyl)phosphinates
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s001",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s002",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s003",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s004",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s005",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s006",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s007",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*135*/"https://doi.org/10.1021/acs.orglett.4c03845.s008",//Enantio- and Regioselective Propargylation and Allenylation of Î²,Î³-Unsaturated Î±â€‘Ketoesters with Allenyl Boronate Regulated by Chiral Copper/Zinc
			/*136*/"https://doi.org/10.1021/acs.orglett.4c04173.s002",//Pd(OAc)2â€‘Catalyzed Approach to Phenanthridin-6(5H)â€‘one Skeletons
			/*137*/"https://doi.org/10.1021/acs.orglett.4c04259.s002",//Pd-Catalyzed/Ligand-Controlled Regioselective Asymmetric Hydrosulfonylation of Alkylallenes or Arylallenes
			/*138*/"https://doi.org/10.1021/acs.orglett.4c04348.s002",//Construction of 3,6-Difluoropyridones via a Double Defluorinative [3 + 3] Annulation of Î±â€‘Fluoro-Î±-sulfonylacetamides with 2â€‘CF3â€‘Alkenes
			/*139*/"https://doi.org/10.1021/acs.orglett.4c04509.s002",//Thermocontrolled Radical Nucleophilicity vs Radicophilicity in Regiodivergent Câ€“H Functionalization
			/*140*/"https://doi.org/10.1021/acs.orglett.4c04625.s002",//Sc-Catalyzed Asymmetric [2 + 2] Annulation of 2â€‘Alkynylnaphthols with Dienes to Access Cyclobutene Frameworks
			/*141*/"https://doi.org/10.1021/acs.orglett.4c04737.s002",//Diverse Annulations of Alkyl(phenyl)phosphinic Chlorides and Imines
			/*141*/"https://doi.org/10.1021/acs.orglett.4c04737.s003",//Diverse Annulations of Alkyl(phenyl)phosphinic Chlorides and Imines

			/*142*/"https://doi.org/10.1021/acs.orglett.4c04779.s002",//Diastereoselective Synthesis of Cyclobutanes via Rh-Catalyzed Unprecedented Câ€“C Bond Cleavage of Alkylidenecyclopropanes
			/*143*/"https://doi.org/10.1021/acs.orglett.4c04827.s001",//Catalytic Enantioselective [4+1]-Annulation of Carboxylic Acids with Cyclopropenes
			/*144*/"https://doi.org/10.1021/acs.orglett.4c04834.s002",//Visible Light-Induced Sequential Nitrogen Insertion and Benzotriazolation of Quinoxaline-2(1H)â€‘ones
			/*145*/"https://doi.org/10.1021/acs.orglett.5c00129.s002",//Visible-Light-Driven Synergistic Se/Fe Catalysis for the Synthesis of 2â€‘Aminoquinoline Derivatives
			/*146*/"https://doi.org/10.1021/acs.orglett.5c00146.s001",//TBHP-Promoted Trifluoromethyl-difluoromethylthiolation of Unactivated Alkenes with CF3SO2Na and PhSO2SCF2H
			/*147*/"https://doi.org/10.1021/acs.orglett.5c00325.s002",//Trifluoromethylation on a Nucleoside Sugar Scaffold: Design and Synthesis of 6â€²-Trifluoromethylcyclopentenyl-purine and -pyrimidine Nucleosides

			/*148*/"https://doi.org/10.1021/acs.orglett.5c00493.s001",//Lewis Acid-Mediated Domino Glycosylation/Cyclization of Substituted Glycals: A Stereoselective Route Toward the Synthesis of 1,2-Annulated Câ€‘Glycosides
			/*149*/"https://doi.org/10.1021/acs.orglett.5c00519.s002",//Pyridyl Pyrimidylsulfones as Latent Pyridyl Nucleophiles in Palladium-Catalyzed Cross-Coupling Reactions
			/*150*/"https://doi.org/10.1021/acs.orglett.5c00576.s002",//A Metal-Catalyzed Tunable Reaction of Ylides with Hydroxylamine Derivatives
			/*151*/"https://doi.org/10.1021/acs.orglett.5c00600.s002",//Direct Synthesis of Amides from Nitro Compounds and Alcohols via Borrowing Hydrogenation
			/*152*/"https://doi.org/10.1021/acs.orglett.5c00623.s002",//Fe/Mn-Synergistic Promoted C(sp3)â€“Bi Cross-Coupling of Alkyl Chlorides with Chlorobismuthanes to Access Air-Stable Alkylbismuthanes
			/*153*/"https://doi.org/10.1021/acs.orglett.5c00666.s002",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*153*/"https://doi.org/10.1021/acs.orglett.5c00666.s003",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*153*/"https://doi.org/10.1021/acs.orglett.5c00666.s004",//Synthesis of Dithioester Derivatives by Base-Mediated Fragmentation of 1,3-Dithiolanes
			/*154*/"https://doi.org/10.1021/acs.orglett.5c00705.s002",//Generation and Use of Bicyclo[1.1.0]butyllithium under Continuous Flow Conditions
			/*155*/"https://doi.org/10.1021/acs.orglett.5c00711.s002",//Highly Stereoselective [2 + 4] Annulation of Phosphenes and Enones with Î²â€‘Electron-Donating Groups
			/*155*/"https://doi.org/10.1021/acs.orglett.5c00711.s003",//Highly Stereoselective [2 + 4] Annulation of Phosphenes and Enones with Î²â€‘Electron-Donating Groups
			/*156*/"https://doi.org/10.1021/acs.orglett.5c00782.s001",//Carbonyl vs Hydroxy: Rhodium catalyzed carbonyl ylide triggered diastereoselective synthesis of 2,5-methano-1,3-benzoxazepines
			/*157*/"https://doi.org/10.1021/acs.orglett.5c00806.s001",//Modular Approach for Photoinduced Cycloaddition Enabling the Synthesis of Diverse Bioactive Oxazoles
			/*158*/"https://doi.org/10.1021/acs.orglett.5c00841.s002",//A Flash Conversion to Aromatic Azo Compounds Expedited by Hydrazineâ€“Trifluoroacetate Hydrogen Bonding
			/*159*/"https://doi.org/10.1021/acs.orglett.5c00879.s002",//Total Synthesis of (Â±)-Streptoglyceride A and of Putative (Â±)-Streptoglyceride C
			/*160*/"https://doi.org/10.1021/acs.orglett.5c00933.s001",//A Mechanistic Perspective on Photocatalytic EnT-Enabled C3-N-Heteroarylation of Aryl Quinoxaline via C(sp2)â€“C(sp2) Coupling
			/*161*/"https://doi.org/10.1021/acs.orglett.5c00948.s002",//Nitroalkanes as Ketone Synthetic Equivalents in Câ€“N and Câ€“S Bond Formation Reactions
			/*162*/"https://doi.org/10.1021/acs.orglett.5c00976.s002",//CAN-Mediated Synthesis of 2â€‘Deoxy Sugars: Access to Chiral 2â€‘Deoxy 3â€‘Bisindolylâ€‘Câ€‘glycosides
			/*163*/"https://doi.org/10.1021/acs.orglett.5c00995.s002",//Unlocking the Potential of CF3-Alkynes in Gold-Catalyzed Oxygen Transfer: A Direct Route to Trifluoromethylated Compounds
			/*164*/"https://doi.org/10.1021/acs.orglett.5c01081.s002",//Ce(OTf)<sub>3</sub>â€‘Catalyzed Asymmetric 6Ï€ Cyclization of Triaryldivinyl Ketones
			/*164*/"https://doi.org/10.1021/acs.orglett.5c01081.s003",//Ce(OTf)3â€‘Catalyzed Asymmetric 6Ï€ Cyclization of Triaryldivinyl Ketones
			/*165*/"https://doi.org/10.1021/acs.orglett.5c01172.s002",//Synthesis of Parent Peropyrene and Its Derivatization
			/*166*/"https://doi.org/10.1021/acs.orglett.5c01196.s002",//Reaction Profile Forecasting by Artificial Data Generation for Wittig-Type Geminal Bromofluoroolefination
			/*167*/"https://doi.org/10.1021/acs.orglett.5c01237.s002",//Electrochemical Functionalization of Alkenes with 1,3-Dicarbonyl Compounds via Radical Addition
			/*168*/"https://doi.org/10.1021/acs.orglett.5c01266.s002",//Quinolâ€“Enedione Rearrangement
			/*169*/"https://doi.org/10.1021/acs.orglett.5c01472.s002",//Synthesis of 8Hâ€‘Indolo[3,2,1-de]phenanthridin-8-ones via Pd(OAc)2â€‘Catalyzed Double Oxidative Coupling Dehydrogenations
			/*170*/"https://doi.org/10.1021/acs.orglett.5c01509.s002",//Bioinspired Stereoselective Total Synthesis of the Caged Sesquiterpenoid Daphnepapytone A

			/*171*/"https://doi.org/10.1021/acs.orglett.5c01576.s002",//Ru(II)-Catalyzed Reactions of ortho-Hydroxy Enaminones with Diazonaphthoquinones: Synthesis of Unsymmetrical Biaryl Diols
			/*172*/"https://doi.org/10.1021/acs.orglett.5c01666.s003",//Loading Thiacorrole with Various Aromatic Rings: Enhanced NIR Absorption and Improved OER Activity
			/*173*/"https://doi.org/10.1021/acs.orglett.5c01713.s002",//Scalable Photoinduced Cycloaddition for the Synthesis of Biorelevant Oxazoles
			/*174*/"https://doi.org/10.1021/acs.orglett.5c01721.s002",//Manganese Complex-Catalyzed (De)hydrogenative Cyclization toward the Selective Synthesis of 2â€‘Substituted and 2,3-Disubstituted 4â€‘Quinolones
			/*175*/"https://doi.org/10.1021/acs.orglett.5c01805.s002",//P(O)Me2â€“Alkenes: From Synthesis to Applications

			/*176*/"https://doi.org/10.1021/acs.orglett.5c01936.s001",//General Strategy to Access Nâ€‘Aryl Heptamethine Indocyanines for Optical Tuning

			/*177*/"https://doi.org/10.1021/acs.orglett.5c01966.s002",//Chan-Evans-Lam Cu(II)-Catalyzed Câ€“O Cross-Couplings: Broadening Synthetic Access to Functionalized Vinylic Ethers
			/*178*/"https://doi.org/10.1021/acs.orglett.5c02004.s002",//Ligand-Enabled Room-Temperature Three-Component Strategy for Mono-Î±-arylation of Acetone with Cyclic Diaryliodonium Salts and Alkenes

			/*179*/"https://doi.org/10.1021/acs.orglett.5c02079.s002",//Access to SCF3â€‘Substituted Indolizines via a Photocatalytic Late-Stage Functionalization Protocol
			/*180*/"https://doi.org/10.1021/acs.orglett.5c02150.s002",//All-Aza meso-Benzo-Fused Triphyrins(2.1.1): Large Bathochromic Shift and Intensified Absorption via Ï€â€‘Extended Conjugation
			/*181*/"https://doi.org/10.1021/acs.orglett.5c02270.s002",//Image-Based Machine Learning Using Inkjet-Printed Chemicals: Mixing Ratio Prediction and Metal Ion Detection

			/*182*/"https://doi.org/10.1021/acs.orglett.5c02286.s002",//Rh(III)-Catalyzed Atroposelective Câ€“H Cyanation of 1â€‘Aryl benzo[h]isoquinolines
			/*183*/"https://doi.org/10.1021/acs.orglett.5c02366.s002",//Ir(III)-Photocatalyzed Stereoselective Synthesis of 2â€‘Azadienes
			/*184*/"https://doi.org/10.1021/acs.orglett.5c02373.s001",//Copper-Catalyzed Enantioselective Acylcyanation of Alkenes

			/*185*/"https://doi.org/10.1021/acs.orglett.5c02418.s001",//Inherent Weakly Coordinating Oxo-Group-Directed Ruthenium(II)-Catalyzed C5 Functionalization of 2â€‘Arylquinolin-4(1H)â€‘ones
			/*186*/"https://doi.org/10.1021/acs.orglett.5c02448.s001",//Irradiation of Bifunctional Masked Ketone Pro-Aromatics Unveils Autoinductive Autocatalysis via Electron Donorâ€“Acceptor (EDA) Complexes
			/*187*/"https://doi.org/10.1021/acs.orglett.5c02494.s001",//HFIP-Assisted Denitrogenative Radical Fragmentation Pathway of Areneazo-2-(2-nitro)propanes: Cross-Coupling of Aryl Radicals with Alkynes and Coupling Reagents
			/*188*/"https://doi.org/10.1021/acs.orglett.5c02495.s001",//Stereoelectronic Tuning of Heteroaromatic Î³â€‘Amino Acids for Turn Mimetic Design
			/*189*/"https://doi.org/10.1021/acs.orglett.5c02499.s001",//Subtle Strain-Release-Driven Aroylation of Four-Membered Rings via Decarboxylative Giese-Type Reactions
			/*190*/"https://doi.org/10.1021/acs.orglett.5c02536.s001",//Nickel-Catalyzed Selective Monoamination of 1,2-Diols: An Affordable Approach to Amino Alcohols
			/*191*/"https://doi.org/10.1021/acs.orglett.5c02559.s002",//Benzo[b]furan Platforms for Tailorable Photochromic Molecules
			/*192*/"https://doi.org/10.1021/acs.orglett.5c02563.s001",//Access to Tetrahydropyrido[1,2â€‘a]indol-6-one Derivatives via NHC-Catalyzed Radical Dearomatization of Indoles

			/*193*/"https://doi.org/10.1021/acs.orglett.5c02636.s001",//Iridium-Catalyzed Regio- and Enantioselective C7-Allylic Alkylation of 4â€‘Aminoindoles
			/*193*/"https://doi.org/10.1021/acs.orglett.5c02636.s002",//Iridium-Catalyzed Regio- and Enantioselective C7-Allylic Alkylation of 4â€‘Aminoindoles

			/*194*/"https://doi.org/10.1021/acs.orglett.5c02670.s001",//Visible-Light-Promoted Selenylative Radical Cyclization of 2â€‘Vinylâ€‘Nâ€‘Aryl Imines to 3Hâ€‘Indoles via Oxygen Photosensitization
			/*195*/"https://doi.org/10.1021/acs.orglett.5c02689.s001",//Isolation of a Transition-State Geometry via Intermolecular Stabilization in the Solid State

			/*196*/"https://doi.org/10.1021/acs.orglett.5c02724.s001",//Rapid Access to Spiro[4.6]-(di)benzodiazepine Indenes: Regioselective Rh(III)-Catalyzed Cascade Câ€“H Activation/Spiroannulation
			/*197*/"https://doi.org/10.1021/acs.orglett.5c02783.s001",//Photoinduced Ligated Boryl Radical-Mediated Alkynylation of Inert Iodoalkanes
			/*198*/"https://doi.org/10.1021/acs.orglett.5c02876.s001",//Construction of Acyclic Vicinal All-Carbon Quaternary and Tertiary Carbon Stereocenters via a Stereoselective Claisen Rearrangement
			/*199*/"https://doi.org/10.1021/acs.orglett.5c02878.s001",//A Visible-Light Organophotoredox-Catalyzed anti-Markovnikov Hydrocarbamoylation of Unactivated Alkene and Cyclization Cascade via Radical Relay
			/*200*/"https://doi.org/10.1021/acs.orglett.5c03138.s002",//Visible-Light-Induced CdSeS/CdZnSe/ZnSe/ZnS Quantum-Dot-Catalyzed Three-Component Reaction for the Synthesis of Î±â€‘(4-Pyridyl)benzylaniline Derivatives
			/*201*/"https://doi.org/10.1021/acs.orglett.5c03157.s002",//Construction of Benzothiophene-Fused 1,2,4-Triazocines via a Skeletal Editing Strategy and DFT Study on the Reaction Mechanism
			/*201*/"https://doi.org/10.1021/acs.orglett.5c03157.s003",//Construction of Benzothiophene-Fused 1,2,4-Triazocines via a Skeletal Editing Strategy and DFT Study on the Reaction Mechanism
			/*202*/"https://doi.org/10.1021/acs.orglett.5c03186.s001",//Alectinib Synthesis through Formal Î±â€‘Arylation of Enone
			/*203*/"https://doi.org/10.1021/acs.orglett.5c03195.s002",//Easy Access to Vinylene-Linked Conjugated Homopolymers from Phosphonates via an O2â€‘Mediated Aldehyde-Free Strategy
			/*204*/"https://doi.org/10.1021/acs.orglett.5c03229.s002",//Visible-Light-Induced Difunctionalization of Styrenes with Aryl Hydrazines and tert-Butyl Nitrite: One-Pot Access to Î±â€‘Arylethanone Oximes
			/*205*/"https://doi.org/10.1021/acs.orglett.5c03241.s001",//Photochemical Flow Synthesis of Trisubstituted Oxazoles Enabled by High-Power UVâ€“B LED Modules
			/*206*/"https://doi.org/10.1021/acs.orglett.5c03345.s002",//Three-Component Reaction through Rh(III)-Catalyzed Strain Release of Bicyclo[1.1.0]butanes



			/*207*/"https://doi.org/10.1021/acs.orglett.5c03615.s001",//Regiodivergent Addition of Allylic Acetals in Ruthenium-Catalyzed Câ€“H/Nâ€“H Annulation Reaction
			/*208*/"https://doi.org/10.1021/acs.orglett.5c03626.s002",//Furan Oxidation-Cyclization to Oxazepines: Favoring 7â€‘exo-trig over 6â€‘endo-trig and 5â€‘exo-trig Trajectories
			/*209*/"https://doi.org/10.1021/acs.orglett.5c03636.s002",//Generation [via Tetradehydro-Dielsâ€“Alder Reactions] and Reactivity of 6â€‘Fluorocyclohexa-1,2,4-triene Intermediates

			/*210*/"https://doi.org/10.1021/acs.orglett.5c03906.s002",//Câ€‘4 Functional Group-Driven Stereodivergent Synthesis and Late-Stage Diversification of 6â€‘Deoxy lâ€‘talo/gulo Rare Sugars
			/*211*/"https://doi.org/10.1021/acs.orglett.5c04004.s002",//Pd-Catalyzed Enantioselective Double Câ€“H Activation and Transmetalation: Synthesis of 2â€‘Heteroaryl/aryl-Ferrocenealdehydes



			/*212*/"https://doi.org/10.1021/acs.orglett.5c04277.s002",//Copper-Catalyzed Divergent Câ€“S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*212*/"https://doi.org/10.1021/acs.orglett.5c04277.s003",//Copper-Catalyzed Divergent Câ€“S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*212*/"https://doi.org/10.1021/acs.orglett.5c04277.s004",//Copper-Catalyzed Divergent Câ€“S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation
			/*212*/"https://doi.org/10.1021/acs.orglett.5c04277.s005",//Copper-Catalyzed Divergent Câ€“S Bond Cleavage of Vinylsulfonium Salts: Controllable Phosphoryloxylation and Phosphinoylation

			/*213*/"https://doi.org/10.1021/acs.orglett.5c04403.s001",//Visible-Light-Promoted Catalytic Redox-Neutral Alkynylation of Alkyl Boronic Acids/Boronates

			/*214*/"https://doi.org/10.1021/acs.orglett.5c04507.s002",//Photoredox/Nickel Dual-Catalyzed Asymmetric Câ€‘Alkylation of Nitroalkanes with Î±â€‘Bromoketones


			/*215*/"https://doi.org/10.1021/acs.orglett.5c04750.s002",//Synthesis of Carborane-Fused Lactones via Electrochemical C(sp3)â€“H Lactonization: Access to Benzofused Lactone Bioisosteres
			/*216*/"https://doi.org/10.1021/acs.orglett.5c04969.s001",//Access to Alkylative/Hydrodefluorination of Trifluoromethyl Ketones Using Photoexcited Dihydropyridines

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
				info = new ACSInfo(pubdoi);
				lastPubDOI = pubdoi;
			}
			String supplementId = datadoi.substring(datadoi.length() - 3); // 004
			info.add(supplementId);
		}
		if (info != null)
			articles.add(info);
		System.out.println(articles.size() + " test articles");
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
		int first = 21; // first test to run
		int last = 21; // last test to run
		run(args, first, last, localSourceACS, localTargetDir);
	}
}