package com.integratedgraphics.test;

import com.integratedgraphics.extractor.IFDExtractor;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects.
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends IFDExtractor {

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
		runExtraction(args);
	}

}