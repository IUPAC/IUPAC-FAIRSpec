package com.integratedgraphics.extract;

import java.io.File;
import java.io.IOException;

/**
 * A test class Just a test to see what we can find in a Bruker PDF document.
 * Answer was just the Bruker logo!
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	public ExtractorTest(File ifsExtractScript, File targetDir) throws IOException {
		super.getObjectsForFile(ifsExtractScript, targetDir);
	}


	public static void main(String[] args) {
		String script = (args.length < 1 ? "./extract/acs.orglett.0c00571/IFS-extract.json" : args[0]);
		String targetDir = (args.length < 2 ? "c:/temp/ifs/acs.orglett.0c00571" : args[1]);
		try {
			new ExtractorTest(new File(script), new File(targetDir));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}