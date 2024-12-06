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
public class ExtractorTestICL {

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/henry/v_acs/";
		String ifdExtractFile = dir + "IFD-extract.json";
		String localSourceArchive = dir + "Archive.tar.gz";
		String targetDir = dir + "icl-ifd2024c";

		// debugging = true;
		// readOnly = true;

		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir);
	}

}