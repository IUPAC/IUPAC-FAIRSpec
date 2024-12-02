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
public class ExtractorTestICL extends ExtractorTest {

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/henry/v_acs/";
		String ifdExtractFile = dir + "IFD-extract.json";
		String sourceArchive = dir + "Archive.tar.gz";	
		String targetDir = dir + "icl-ifd";
		
		//debugging = true;
		//readOnly = true;		
		
		runExtraction(setSourceTargetArgs(args, ifdExtractFile, sourceArchive, targetDir, null));
	}

}