package com.integratedgraphics.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.extractor.ExtractorUtils;
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
public class ExtractorTestDryad {

	public static void main(String[] args) {

		
		String dryadid;

		
		if (args.length == 0)
			dryadid = "3tx95x6sq";
		else 
			dryadid = args[0];
		
		String tempDir = "c:/temp/dryad/";
		String ifdExtractFile = "./extract/dryad/" + dryadid + "/IFD-extract.json";
		String localSourceArchive = tempDir + dryadid + "/dataset.zip";
		String targetDir = tempDir + dryadid + "_out/";
		String flags = null;
		
		
//		try {
//			dumpZipFile(localSourceArchive, 1);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		tempDir = ExtractorUtils.setTempDir(tempDir);
		ExtractorUtils.useZipFile(true);
		
		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
		
	}

	private static void dumpZipFile(String file, int level) throws IOException {
		ZipFile zf = new ZipFile(file);
		 Enumeration<? extends ZipEntry> it = zf.entries();
	    while (it.hasMoreElements()) {
	        ZipEntry entry = it.nextElement();
	        System.out.println(entry.getName() + " " + entry.getCompressedSize() + " " + entry.getSize());
	        if (entry.getName().endsWith(".zip")) {
	        	InputStream is = zf.getInputStream(entry);
	        	file = "c:/temp/t" + level + ".zip";
	    		FileOutputStream fos = new FileOutputStream(file);
	    		FAIRSpecUtilities.getLimitedStreamBytes(is, -1, fos, false, true);
	    		dumpZipFile(file, ++level);
	        }
	    }
	    zf.close();
		
	}

}