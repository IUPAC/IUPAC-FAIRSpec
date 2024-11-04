package com.integratedgraphics.test;

import com.integratedgraphics.extractor.MetadataExtractor;

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
public class ExtractorTest extends MetadataExtractor {

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
/*7*/		"./extract/acs.orglett.0c01022/IFD-extract.json#22195341",   // 7 -- many mnovas
/*8*/		"./extract/acs.orglett.0c01043/IFD-extract.json#22232721",  // 8 -- LARGE single 158-MB mnova -- IGNORING!
/*9*/		"./extract/acs.orglett.0c01153/IFD-extract.json#22284726,22284720",  // 9 -- LARGE two remote locations; bruker dirs + cdx
/*10*/		"./extract/acs.orglett.0c01197/IFD-extract.json#22491647",  // 10 -- many mnovas
/*11*/		"./extract/acs.orglett.0c01277/IFD-extract.json#22613762",  // 11 -- bruker dirs
/*12*/		"./extract/acs.orglett.0c01297/IFD-extract.json#22612484",  // 12 --  bruker dirs
/*13*/      "./extract/acs.orgLett.9b02307/IFD-extract.json#9b02307"    // 13 -- Ley, May
	};

	private static void runTests(String[] args) {
		int first = 13; // first test to run
		int last = 13; // last test to run; 12 max, 9 for smaller files only; 11 to skip single-mnova
					  // file test
		runACSTest(args, first, last);
	}

	private static void runACSTest(String[] args, int first, int last) {


		/**
		 * null to download from FigShare; a local dir if you have already downloaded
		 * the zip files
		 */
		String sourceArchive = "c:/temp/iupac/zip";
		String targetDir = "c:/temp/iupac/ifd2024Ley";//./site/ifd";
		String options = null; // "-datacitedown"
		args = setSourceTargetArgs(args, sourceArchive, targetDir, options);		
		runExtraction(args, acsTestSet, first, last);
	}

	public static void main(String[] args) {
		runTests(args);
	}

}