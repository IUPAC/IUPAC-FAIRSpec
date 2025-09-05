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
public class ExtractorTestNFDI {

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/nfdi/pb/";
//		String ifdExtractFile = dir + "IFD-extract-procter.json";
//		String localSourceArchive = dir + "Procter/";
//		String targetDir = dir + "icl-procter";

		String ifdExtractFile = dir + "IFD_extract.json";
		String localSourceArchive = dir + "zip";
		String targetDir = dir + "site";//null;//dir + "icl-procter-2025-08";

		String flags = null;

		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
	}

}