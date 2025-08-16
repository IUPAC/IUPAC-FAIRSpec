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
		String targetDir = dir + "icl-ifd2025.07.23"; // 10386
		String flags = null;// this did not work: "-embedpdf";

		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
	}

}