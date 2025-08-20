package com.integratedgraphics.test;

import com.integratedgraphics.extractor.IFDExtractor;

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
public class ExtractorTestSTO {

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/stolaf/";
		String ifdExtractFile = dir + "IFD-extract.json";
		String localSourceArchive = null;// dir + "archive.tar.gz";
		String targetDir = "c:/temp/iupac/stolaf-ifd-2025-08";

		String flags = null;
		
		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
	}

}