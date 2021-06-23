package com.integratedgraphics.ifs;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.common.IFSException;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: 
 * <code>
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
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	public ExtractorTest(String key, File ifsExtractScriptFile, File targetDir, String localSourceDir) throws IOException, IFSException {
		
		String findingAidFileName = (key == null ? "" : key + ".") + "_IFS_findingaid.json";
		
		if (!super.extractAndCreateFindingAid(ifsExtractScriptFile, localSourceDir, targetDir, findingAidFileName)) {
			throw new IFSException("Test failed");
		}
		
		System.out.println(
				"extracted " + manifestCount + " files (" + extractedByteCount + " bytes)"
				+ "; ignored " + ignoredCount + " files (" + ignoredByteCount + " bytes)");
	

	}


	/**
	 * ACS/FigShare codes   /21947274  
	 * 
	 * for example: https://ndownloader.figshare.com/files/21947274
	 */
	private final static String[] testSet = {
			"acs.orglett.0c00571/21975525", // 0 3212 files; zips of bruker zips and HRMS
			"acs.orglett.0c01153/22284726,22284729", // 1 bruker dirs
			"acs.joc.0c00770/22567817",  // 2 727 files; zips of bruker dirs + mnovas
			"acs.orglett.0c00624/21947274", // 3 1143 files; MANY bruker dirs
			"acs.orglett.0c00788/22125318", // 4 jdfs
			"acs.orglett.0c00874/22233351", // 5 bruker dirs
			"acs.orglett.0c00967/22111341", // 6 bruker dirs + jdfs
			"acs.orglett.0c01022/22195341", // 7  many mnovas
			"acs.orglett.0c01197/22491647", // 8 many mnovas
			"acs.orglett.0c01277/22613762", // 9 bruker dirs
			"acs.orglett.0c01297/22612484", // 10 bruker dirs
			// these next four are too large to save at GitHub (> 100 MB)
			"acs.orglett.0c00755/22150197", // 11 MANY bruker dirs
			"acs.orglett.0c01043/22232721", // 12  single 158-MB mnova
	};
	
	public static void main(String[] args) {

		int i0 = 0;
		int i1 = 12; // 12 max
		
		debugging = false;//true; // verbose listing of all files
		
		int failed = 0;
		
			
//		String s = "test/ok/1c.nmr";
//		Pattern p = Pattern.compile("^\\Qtest/ok/\\E(.+)\\Q.nmr\\E");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find());
//		String v = m.group(1);
//		
			
//		
			String key = null;
			String script, targetDir = null, sourceDir = null;
			switch (args.length) {
			default:
			case 3:
				sourceDir = args[2];
			case 2:
				targetDir = args[1];
			case 1:
				script = args[0];
				break;
			case 0:
				sourceDir = "file:///c:/temp/iupac/zip";
				targetDir = "c:/temp/iupac/ifs";
				script = null;
			}

			setLogging(targetDir + "/extractor.log");
			
			for (int itest = (args.length == 0 ? i0 : 0); 
			itest <= (args.length == 0 ? i1 : 0); 
			itest++) {

			// ./extract/ should be in the main Eclipse project directory.

			if (args.length == 0) {
				key = testSet[itest];
				System.out.println("\n!\n! found Test " + itest + " " + key);
				int pt = key.indexOf("/");
				if (pt >= 0)
					key = key.substring(0, pt);
				script = "./extract/" + key + "/IFS-extract.json";
			}
			try {
				new ExtractorTest(key, new File(script), new File(targetDir), sourceDir);
				System.out.println("ok " + key);
			} catch (Exception e) {
				failed++;
				System.err.println("Exception " + e + " for test " + itest);
				e.printStackTrace();
			}

		}
		log("! DONE failed=" + failed);
		setLogging(null);
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