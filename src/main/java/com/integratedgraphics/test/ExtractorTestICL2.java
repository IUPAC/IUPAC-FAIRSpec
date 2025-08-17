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
public class ExtractorTestICL2 {

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/henry/";
//		String ifdExtractFile = dir + "IFD-extract-procter.json";
//		String localSourceArchive = dir + "Procter/";
//		String targetDir = dir + "icl-procter";

		String ifdExtractFile = dir + "IFD-extract-HR.json";
		String localSourceArchive = dir + "Procter";
		String targetDir = dir + "icl-procter2";

		String flags = null;

		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
	}

}