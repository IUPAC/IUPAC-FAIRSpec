package com.integratedgraphics.test;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * 
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTestSTO extends ExtractorTest {

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

	private static void runTests(String[] args) {
		runSTOTest(args);
	}

	private static void runSTOTest(String[] args) {
		
		String dir = "c:/temp/iupac/stolaf/";
		String targetDir = dir + "../stolaf-ifd";

		String[] testSet = new String[] { dir + "IFD-extract.json" };
		String sourceArchive = null;//dir + "archive.tar.gz";		

		//debugging = true;
		//readOnly = true;
		
		args = setSourceTargetArgs(args, sourceArchive, targetDir, null);
		runExtraction(args, testSet, -1, -1);		
	}

	public static void main(String[] args) {
		runTests(args);
	}

}