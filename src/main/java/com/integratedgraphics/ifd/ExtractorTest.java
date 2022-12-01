package com.integratedgraphics.ifd;

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

	/**
	 * 
	 * @param args
	 * @param sourceArchive
	 * @param targetDir
	 * @return
	 */
	private static String[] setSourceTargetArgs(String[] args, String sourceArchive, String targetDir, String flags) {
		String[] a = new String[4];
		if (args.length > 0)
			a[0] = args[0];
		if (args.length < 2 || args[1] == null)
			a[1] = sourceArchive;
		if (args.length < 3 || args[2] == null)
			a[2] = targetDir;
		if (args.length < 4 || args[3] == null)
			a[3] = flags;
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
	private static String[] acsTestSet = { 
			"22567817#./extract/acs.joc.0c00770/IFD-extract.json", // 0 727 files; zips of bruker dirs + mnovas
			"21947274#./extract/acs.orglett.0c00624/IFD-extract.json", // 1 1143 files; MANY bruker dirs
			"22125318#./extract/acs.orglett.0c00788/IFD-extract.json", // 2 jeol jdfs
			"22233351#./extract/acs.orglett.0c00874/IFD-extract.json", // 3 bruker dirs
			"22111341#./extract/acs.orglett.0c00967/IFD-extract.json", // 4 bruker dirs + jeol jdfs
			"22195341#./extract/acs.orglett.0c01022/IFD-extract.json", // 5 many mnovas, cdx and png extracted
			"22491647#./extract/acs.orglett.0c01197/IFD-extract.json", // 6 many mnovas
			"22613762#./extract/acs.orglett.0c01277/IFD-extract.json", // 7 bruker dirs
			"22612484#./extract/acs.orglett.0c01297/IFD-extract.json", // 8 bruker dirs
			// these next four are very large (> 100 MB) and take some time to process if
			// not using a local sourceDir
			"22150197#./extract/acs.orglett.0c00755/IFD-extract.json", // 9 MANY bruker dirs
			"22284726,22284729#./extract/acs.orglett.0c01153/IFD-extract.json", // 10 two remote locations; bruker dirs + cdx
			"21975525#./extract/acs.orglett.0c00571/IFD-extract.json", // 11 180+MB 3212 files; zips of bruker zips and HRMS
			"22232721#./extract/acs.orglett.0c01043/IFD-extract.json", // 12 single 158-MB mnova -- IGNORING!
	};

	private static void runTests(String[] args) {
//		int first = 0; // first test to run
//		int last = 0; // last test to run; 12 max, 9 for smaller files only; 11 to skip single-mnova
//						// file test
		//runACSTest(args, first, last);
		runACSTest(args, 0);
		//runACSTest(args, 5);
		//runACSTest(args, 0, 11);
		//runUCLTest(args);
		//runUCLTest56(args);
	}

	private static void runACSTest(String[] args, int i) {
		runACSTest(args, i, i);
	}


	private static void runUCLTest56(String[] args) {
		String[] testSet = new String[] { "c:/temp/henry/v6/IFD-extract.json" };
		
//		String sourceArchive = "c:/temp/henry/test.zip";		
		String sourceArchive = "c:/temp/henry/v6/56.zip";		
		String targetDir = "c:/temp/henry/v6/ifd";

		//debugging = true;
		//readOnly = true;
		
		args = setSourceTargetArgs(args, sourceArchive, targetDir, null);
		runExtraction(args, testSet, -1, -1);		
	}

	private static void runUCLTest(String[] args) {
		
		String dir = "c:/temp/henry/v7/";

		String[] testSet = new String[] { dir + "IFD-extract.json" };
		String sourceArchive = dir + "archive.tar.gz";		
		String targetDir = dir + "ifd";

		//debugging = true;
		//readOnly = true;
		
		args = setSourceTargetArgs(args, sourceArchive, targetDir, null);
		runExtraction(args, testSet, -1, -1);		
	}

	private static void runACSTest(String[] args, int first, int last) {


		/**
		 * null to download from FigShare; a local dir if you have already downloaded
		 * the zip files
		 */
		String sourceArchive = "c:/temp/iupac/zip";
		String targetDir = "c:/temp/iupac/ifd";//./site/ifd";
		
		args = setSourceTargetArgs(args, sourceArchive, targetDir, "-datacitedown;");		
		runExtraction(args, acsTestSet, first, last);
	}

	public static void main(String[] args) {
		runTests(args);
	}

}