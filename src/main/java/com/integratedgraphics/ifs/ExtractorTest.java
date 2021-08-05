package com.integratedgraphics.ifs;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.util.Util;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: <code>
	acs.joc.0c00770
	acs.orglett.0c00571
	acs.orglett.0c00624
	acs.orglett.0c00755
	acs.orglett.0c00788
	acs.orglett.0c00874
	acs.orglett.0c00967
	acs.orglett.0c01022
	acs.orglett.0c01043
	acs.orglett.0c01153
	acs.orglett.0c01197
	acs.orglett.0c01277
	acs.orglett.0c01297
 </code>
 * 
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	private static boolean createFindingAidJSONList;
	private static boolean stopOnAnyFailure;

	/**
	 * ACS/FigShare codes /21947274
	 * 
	 * for example: https://ndownloader.figshare.com/files/21947274
	 */
	private final static String[] testSet = { 
			"acs.joc.0c00770/22567817", // 0 727 files; zips of bruker dirs + mnovas
			"acs.orglett.0c00624/21947274", // 1 1143 files; MANY bruker dirs
			"acs.orglett.0c00788/22125318", // 2 jeol jdfs
			"acs.orglett.0c00874/22233351", // 3 bruker dirs
			"acs.orglett.0c00967/22111341", // 4 bruker dirs + jeol jdfs
			"acs.orglett.0c01022/22195341", // 5 many mnovas, cdx and png extracted
			"acs.orglett.0c01197/22491647", // 6 many mnovas
			"acs.orglett.0c01277/22613762", // 7 bruker dirs
			"acs.orglett.0c01297/22612484", // 8 bruker dirs
			// these next four are very large (> 100 MB) and take some time to process if
			// not using a local sourceDir
			"acs.orglett.0c00755/22150197", // 9 MANY bruker dirs
			"acs.orglett.0c01153/22284726,22284729", // 10 two remote locations; bruker dirs
			"acs.orglett.0c00571/21975525", // 11 180+MB 3212 files; zips of bruker zips and HRMS
			"acs.orglett.0c01043/22232721", // 12 single 158-MB mnova -- IGNORING!
	};

	public static void main(String[] args) {

		String sourceDir = "c:/temp/iupac/zip";// null or a local dir if you have already downloaded the zip files
		boolean debugReadOnly = false;

		// normally false:
		
		readOnly = debugReadOnly; // for testing; when true, not output other than a log file is produced
		debugging = false; // true for verbose listing of all files
		createFindingAidsOnly = false; // true if extraction files already exist or you otherwise don't want not write
		allowNoPubInfo = true;//debugReadOnly; // true to allow no internet connection and so no pub calls
		skipPubInfo = debugReadOnly;
		
		// normally true:
		
		// this next is independent of readOnly
		createZippedCollection = !debugReadOnly; // false to bypass final creation of an _IFS_collection.zip file
		createFindingAidJSONList = !debugReadOnly; // false for testing and you don't want to mess up _IFS_findingaids.json

		stopOnAnyFailure = true; // set false to allow continuing after an error.

		int first =6; // first test to run
		int last = 6; // last test to run; 12 max, 9 for smaller files only; 11 to skip single-mnova
						// file test

		String targetDir = "./site/ifs";
			
		runExtraction(first, last, targetDir, sourceDir, args);
	}

	public ExtractorTest(String key, File ifsExtractScriptFile, File targetDir, String localSourceDir)
			throws IOException, IFSException {

		String findingAidFileName = (key == null ? "" : key + ".");

		if (super.extractAndCreateFindingAid(ifsExtractScriptFile, localSourceDir, targetDir, findingAidFileName) == null && !allowNoPubInfo) {
			throw new IFSException("Test failed");
		}

		System.out.println("extracted " + manifestCount + " files (" + extractedByteCount + " bytes)" + "; ignored "
				+ ignoredCount + " files (" + ignoredByteCount + " bytes)");
	}

	private static void runExtraction(int first, int last, String targetDir, String sourceDir, String[] args) {
		int i0 = first;
		int i1 = last;

		int failed = 0;

//		String s = "test/ok/1c.nmr";
//		Pattern p = Pattern.compile("^\\Qtest/ok/\\E(.+)\\Q.nmr\\E");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find());
//		String v = m.group(1);
//		

//		
		String key = null;
		String script;
		switch (args.length) {
		default:
		case 3:
			sourceDir = args[2];
			//$FALL-THROUGH$
		case 2:
			targetDir = args[1];
			//$FALL-THROUGH$
		case 1:
			script = args[0];
			break;
		case 0:
			script = null;
		}

		System.out.println("output to " + new File(targetDir).getAbsolutePath());
		new File(targetDir).mkdirs();

		String json = "";

		Util.setLogging(targetDir + "/extractor.log");

		errorLog = "";
		int n = 0;
		String job = null;
		for (int itest = (args.length == 0 ? i0 : 0); itest <= (args.length == 0 ? i1 : 0); itest++) {

			// ./extract/ should be in the main Eclipse project directory.

			if (args.length == 0) {
				job = key = testSet[itest];
				System.out.println("\n!\n! found Test " + itest + " " + key);
				int pt = key.indexOf("/");
				if (pt >= 0)
					key = key.substring(0, pt);
				script = "./extract/" + key + "/IFS-extract.json";
			}
			if (json.length() == 0) {
				json = "{\"findingaids\":[\n";
			} else {
				json += ",\n";
			}
			n++;
			try {
				new ExtractorTest(key, new File(script), new File(targetDir), sourceDir);
				json += "\"" + key + "\"";
				System.out.println("ok " + key);
			} catch (Exception e) {
				failed++;
				System.err.println("Exception " + e + " for test " + itest);
				e.printStackTrace();
				if (stopOnAnyFailure)
					break;
			}
		}
		log("! DONE total=" + n + " failed=" + failed + " for " + job);
		json += "\n]}\n";
		try {
			if (createFindingAidJSONList && !readOnly) {
				File f = new File(targetDir + "/_IFS_findingaids.json");
				Util.writeBytesToFile(json.getBytes(), f);
				System.out.println("File " + f.getAbsolutePath() + " created \n" + json);
			} else {
				System.out.println("_IFS_findingaids.json was not created for\n" + json);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(job);
		System.err.println(errorLog);
		Util.setLogging(null);
	}

// BH note: 
//
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

}