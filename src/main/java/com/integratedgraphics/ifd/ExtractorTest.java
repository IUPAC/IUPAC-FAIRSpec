package com.integratedgraphics.ifd;

import java.io.File;
import java.io.IOException;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.util.IFDUtilities;

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

	public ExtractorTest(String key, File ifdExtractScriptFile, File targetDir, String localSourceDir)
			throws IOException, IFDException {

		log("!ExtractorTest\n ifdExtractScriptFIle= " + ifdExtractScriptFile
				+ "\n localSourceDir = " + localSourceDir
				+ "\n targetDir = " + targetDir.getAbsolutePath()
				);

		
		String findingAidFileName = (key == null ? "" : key + ".");

		if (super.extractAndCreateFindingAid(ifdExtractScriptFile, localSourceDir, targetDir, findingAidFileName) == null && !allowNoPubInfo) {
			throw new IFDException("Test failed");
		}

		log("!ExtractorTest extracted " + manifestCount + " files (" + extractedByteCount + " bytes)" + "; ignored "
				+ ignoredCount + " files (" + ignoredByteCount + " bytes)");
	}

	private static void runExtractionTests(int first, int last, String targetDir, String sourceDir, String[] args) {
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
		String fname;
		switch (args.length) {
		default:
		case 3:
			sourceDir = args[2];
			//$FALL-THROUGH$
		case 2:
			targetDir = args[1];
			//$FALL-THROUGH$
		case 1:
			fname = args[0];
			break;
		case 0:
			fname = null;
		}

		System.out.println("ExtractorTest.runExtractionTests output to " + new File(targetDir).getAbsolutePath());
		new File(targetDir).mkdirs();

		String json = "";

		IFDUtilities.setLogging(targetDir + "/extractor.log");

		errorLog = "";
		int n = 0;
		String job = null;
		for (int itest = (args.length == 0 ? i0 : 0); itest <= (args.length == 0 ? i1 : 0); itest++) {
			testID = itest;
			// ./extract/ should be in the main Eclipse project directory.

			if (args.length == 0) {
				job = key = testSet[itest];
				log("!ExtractorTest.runExtractionTests found Test " + itest + " " + job);
				int pt = key.indexOf("/");
				if (pt >= 0)
					key = key.substring(0, pt);
				fname = "./extract/" + key + "/IFD-extract.json";
			}
			if (json.length() == 0) {
				json = "{\"findingaids\":[\n";
			} else {
				json += ",\n";
			}
			n++;
			json += "\"" + key + "\"";
			long t0 = System.currentTimeMillis();
			try {
				File ifdExtractScriptFile = new File(fname);
				File targetPath = new File(targetDir);
				String sourcePath = new File(sourceDir).getAbsolutePath();
				new ExtractorTest(key, ifdExtractScriptFile, targetPath, sourcePath);
				System.out.println("ExtractorTest.runExtractionTests ok " + key);
			} catch (Exception e) {
				failed++;
				System.err.println("ExtractorTest.runExtractionTests Exception " + e + " for test " + itest);
				e.printStackTrace();
				if (stopOnAnyFailure)
					break;
			}
			log("!!ExtractorTest.runExtractionTests job " + job 
					+ " time/sec=" + (System.currentTimeMillis() - t0)/1000.0);

		}
		log("!! DONE total=" + n + " failed=" + failed);
		json += "\n]}\n";
		try {
			if (createFindingAidJSONList && !readOnly) {
				File f = new File(targetDir + "/_IFD_findingaids.json");
				IFDUtilities.writeBytesToFile(json.getBytes(), f);
				System.out.println("ExtractorTest.runExtractionTests File " + f.getAbsolutePath() + " created \n" + json);
			} else {
				System.out.println("ExtractorTest.runExtractionTests _IFD_findingaids.json was not created for\n" + json);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("");
		String flags = "\n first = " + first + " last = " + last //
				+ "\n stopOnAnyFailure = " + stopOnAnyFailure //
				+ "\n debugging = " + debugging //
				+ " readOnly = " + readOnly  //
				+ " debugReadOnly = " + debugReadOnly //
				+ "\n allowNoPubInfo = " + allowNoPubInfo //
				+ " skipPubInfo = " + skipPubInfo //
				+ "\n sourceDir = " + sourceDir //
				+ " targetDir = " + targetDir //
				+ "\n createZippedCollection = " + createZippedCollection //
				+ " createFindingAidJSONList = " + createFindingAidJSONList//
				+ "\n IFD version "+ IFDConst.IFD_VERSION + "\n";
		log("!ExtractorTest.runExtractionTests flags " + flags);
		System.err.println(errorLog);
		IFDUtilities.setLogging(null);
	}

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

	/**
	 * null to download from FigShare; a local dir if you have already downloaded
	 * the zip files
	 */
	private static String sourceDir;
	private static boolean debugReadOnly;

	public static void main(String[] args) {

		// TODO -- add command line interface and GUI.
		
		sourceDir = "c:/temp/iupac/zip";
		
		// normally false:
		
		debugReadOnly = false; // quick settings - no file creation

	    addPublicationMetadata = false; // true to place metadata into the finding aid
		
		// normally true:
		
	    boolean dataciteUp = true;

	    // this next is independent of readOnly
		createZippedCollection = !debugReadOnly; // false to bypass final creation of an _IFD_collection.zip file
		createFindingAidJSONList = !debugReadOnly; // false for testing and you don't want to mess up _IFD_findingaids.json

		stopOnAnyFailure = true; // set false to allow continuing after an error.
		
		readOnly = debugReadOnly; // for testing; when true, not output other than a log file is produced
		debugging = false; // true for verbose listing of all files
		createFindingAidsOnly = false; // true if extraction files already exist or you otherwise don't want not write
		
		allowNoPubInfo = debugReadOnly; // true to allow no internet connection and so no pub calls
		skipPubInfo = !dataciteUp || debugReadOnly;  // true to allow no internet connection and so no pub calls
	
		int first = 0; // first test to run
		int last = 12;//12; // last test to run; 12 max, 9 for smaller files only; 11 to skip single-mnova
						// file test
		
		if (first == last) {
			createFindingAidJSONList = false;
		}

		String targetDir = "./site/ifd";
		
		if (targetDir != null)
			new File(targetDir).mkdir(); // letting this fail ungracefully
		
		runExtractionTests(first, last, targetDir, sourceDir, args);
	}

}