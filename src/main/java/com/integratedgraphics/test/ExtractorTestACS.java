package com.integratedgraphics.test;

import java.io.File;
import java.io.IOException;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.extractor.FindingAidCreator;
import com.integratedgraphics.extractor.IFDExtractor;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: <code>
0	acs.joc.0c00770
1	acs.orglett.0c00571
2	acs.orglett.0c00624
3	acs.orglett.0c00755
4	acs.orglett.0c00788
5	acs.orglett.0c00874
6	acs.orglett.0c00967
7	acs.orglett.0c01022
8	acs.orglett.0c01043
9	acs.orglett.0c01153
10	acs.orglett.0c01197
11	acs.orglett.0c01277
12	acs.orglett.0c01297
	
	added 2024.10.29:
	
	acs.orgLett.9b02307  (Ley/May)
 </code>
 * 
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTestACS extends ExtractorTest {

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
	private static String[] acsTestSet = { 
		// initial # ignored --  very large (> 100 MB) sets, 
		// which take some time to process if
		// not using a local sourceDir

/*0*/		"./extract/acs.joc.0c00770/IFD-extract.json#22567817",       // 0 -- struc/ added; 727 files; zips of bruker dirs + mnovas
/*1*/		"./extract/acs.orglett.0c00571/IFD-extract.json#21975525",  // 1 -- LARGE 180+MB 3212 files; zips of bruker zips and HRMS
/*2*/		"./extract/acs.orglett.0c00624/IFD-extract.json#21947274",   // 2 -- struc/ added; 1143 files; MANY bruker dirs
/*3*/		"./extract/acs.orglett.0c00755/IFD-extract.json#22150197",  // 3 -- LARGE MANY bruker dirs
/*4*/		"./extract/acs.orglett.0c00788/IFD-extract.json#22125318",   // 4 -- jeol jdfs
/*5*/		"./extract/acs.orglett.0c00874/IFD-extract.json#22233351",   // 5 -- bruker dirs
/*6*/		"./extract/acs.orglett.0c00967/IFD-extract.json#22111341",   // 6 -- bruker dirs + jeol jdfs

/*7*/		"./extract/acs.orglett.0c01022/IFD-extract.json#22195341",   // 7 -- many mnovas with CDX files

/*8*/		"./extract/acs.orglett.0c01043/IFD-extract.json#22232721",  // 8 -- LARGE single 158-MB mnova -- IGNORING!
/*9*/		"./extract/acs.orglett.0c01153/IFD-extract.json#22284726,22284720",  // 9 -- LARGE two remote locations; bruker dirs + cdx + one mnova

/*10*/		"./extract/acs.orglett.0c01197/IFD-extract.json#22491647",  // 10 -- many mnovas with PNG only

/*11*/		"./extract/acs.orglett.0c01277/IFD-extract.json#22613762",  // 11 -- bruker dirs
/*12*/		"./extract/acs.orglett.0c01297/IFD-extract.json#22612484",  // 12 --  bruker dirs
/*13*/      "./extract/acs.orgLett.9b02307/IFD-extract.json#9b02307"    // 13 -- Ley, May
	};

	/**
	 * Run a full extraction based on arguments, possibly a test set
	 * 
	 * @param args [null, sourceAchive, targetDir, flags...]
	 * @param first
	 * @param last
	 * @param createFindingAidJSONList
	 */
	private static void runACSExtractionTest(String[] args,
			int first, int last, boolean createFindingAidJSONList) {
		String localSourceArchive = args[1];
		String targetDir = args[2];
		new File(targetDir).mkdirs();
		FAIRSpecUtilities.setLogging(targetDir + "/extractor.log");

		String json = null;

		int i0 = Math.max(0, Math.min(first, last));
		int i1 = Math.max(0, Math.max(first, last));
		int failed = 0;
		int n = 0;
		int nWarnings = 0;
		int nErrors = 0;
		String warnings = "";
		IFDExtractor extractor = null;
		String sflags = null;
		String targetDir0 = targetDir;
		// ./extract/ should be in the main Eclipse project directory.
		// [0] "./extract/acs.joc.0c00770/IFD-extract.json#22567817", // 0 727 files;
		// zips of bruker dirs + mnovas
		for (int i = i0; i <= i1; i++) {
			extractor = new IFDExtractor();
			extractor.logToSys("Extractor.runExtractionTest output to " + new File(targetDir).getAbsolutePath());
			String extractInfo = acsTestSet[i];
			extractor.logToSys("Extractor.runExtraction " + i + " " + extractInfo);
			String ifdExtractFile;
			int pt = extractInfo.indexOf("#");
			if (pt == 0) {
				ifdExtractFile = null;
				System.out.println("Ignoring " + extractInfo);
				continue;
			} else if (pt > 0) {
				ifdExtractFile = extractInfo.substring(0, pt);
			} else {
				ifdExtractFile = extractInfo;
			}
			String targetSubDirectory = new File(ifdExtractFile).getParentFile().getName();
			if (targetSubDirectory.length() > 0)
				targetDir = targetDir0 + "/" + targetSubDirectory;
			n++;
			if (json == null) {
				json = "{\"findingaids\":[\n";
			} else {
				json += ",\n";
			}
			json += "\"" + targetDir + "/IFD.findingaid.json\"";
			long t0 = System.currentTimeMillis();

			extractor.testID = i;
			extractor.processFlags(args, null);
			new File(targetDir).mkdirs();
			// false for testing and you don't want to mess up _IFD_findingaids.json
			try {
				File ifdExtractScriptFile = new File(ifdExtractFile).getAbsoluteFile();
				File targetPath = new File(targetDir).getAbsoluteFile();
				String sourcePath = (localSourceArchive == null || "-".equals(localSourceArchive) ? "-" : new File(localSourceArchive).getAbsolutePath());
				extractor.run(ifdExtractScriptFile, targetPath, sourcePath);
				extractor.logToSys("Extractor.runExtraction ok " + extractInfo);
			} catch (Exception e) {
				failed++;
				extractor.logErr("Exception " + e + " " + i, "runExtraction");
				e.printStackTrace();
				if (extractor.stopOnAnyFailure)
					break;
			}
			nWarnings += extractor.warnings;
			nErrors += extractor.errors;
			extractor.logToSys("!Extractor.runExtraction job " + extractInfo + " time/sec="
					+ (System.currentTimeMillis() - t0) / 1000.0);
			ifdExtractFile = null;
			if (extractor.warnings > 0) {
				warnings += "======== " + i + ": " + extractor.warnings + " warnings for " + targetDir + "\n"
						+ extractor.strWarnings;
				try {
					FAIRSpecUtilities.writeBytesToFile((warnings).getBytes(),
							new File(targetDir0 + "/_IFD_warnings.txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

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
			((FindingAidCreator) extractor).setTargetPath(new File(targetDir0));
			extractor.finalizeExtraction(json, n, failed, nWarnings, nErrors, sflags);
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
	private static String[] setSourceTargetArgs(String[] args, String ifdExtractJSONFilename, String localSourceArchive, String targetDir, String flags) {
		if (args == null)
			args = new String[0];
		String[] a = new String[Math.max(4,  args.length)];
		a[0] = (args.length < 1 || args[0] == null ? ifdExtractJSONFilename : args[0]);
		a[1] = (args.length < 2 || args[1] == null ? localSourceArchive : args[1]);
		a[2] = (args.length < 3 || args[2] == null ? targetDir : args[2]);
		a[3] = (args.length < 4 || args[3] == null ? flags : args[3]);
		for (int i = 4; i < args.length; i++)
			a[i] = args[i];
		return a;
	}

	public static void main(String[] args) {
		// args[] may override localSourceArchive as ars[1] 
		// and testDir as args[2]; args[0] is ignored;
		int first = 12; // first test to run
		int last = 12; // last test to run; 13 max, 9 for smaller files only; 11 to skip single-mnova
					  // file test
		/**
		 * a local dir if you have already downloaded the zip files, otherwise null to
		 * download from FigShare;
		 */
		String localSourceArchive = "c:/temp/iupac/zip";//-";
		
		String targetDir = "c:/temp/iupac/ifd2024";
		String flags = null; // "-datacitedown"
		args = setSourceTargetArgs(args, null, localSourceArchive, targetDir, flags);
		boolean createFindingAidJSONList = true;
		runACSExtractionTest(args, first, last, createFindingAidJSONList);
	}

}