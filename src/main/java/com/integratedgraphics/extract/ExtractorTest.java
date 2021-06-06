package com.integratedgraphics.extract;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;

/**
 * A test class Just a test to see what we can find in a Bruker PDF document.
 * Answer was just the Bruker logo!
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	private String pathway;

	public ExtractorTest(File ifsExtractScript, File targetDir) throws IOException {
		super.extractFile(ifsExtractScript, targetDir);
	}


	public static void main(String[] args) {
		String script = (args.length < 1 ? "./extract/acs.orglett.0c00571" : args[0]);
		String target = (args.length < 2 ? "c:/temp/ifs/acs.orglett.0c00571" : args[1]);
		try {
			new ExtractorTest(new File(script), new File(target));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}