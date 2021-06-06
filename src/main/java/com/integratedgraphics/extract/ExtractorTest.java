package com.integratedgraphics.extract;

import java.io.File;
import java.io.IOException;

/**
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: 
 * <code>
	acs.joc.0c00770
	acs.orglett.0c00571
	acs.orglett.0c00624
	acs.orglett.0c00755
	acs.orglett.0c00788
	acs.orglett.0c00874
	acs.orglett.0c00967
	acs.orglett.0c01022
	acs.orglett.0c01043
	acs.orglett.0c01153
	acs.orglett.0c01197
	acs.orglett.0c01277
	acs.orglett.0c01297
 </code>
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	private final static String[] testSet = {
			"acs.joc.0c00770",
			"acs.orglett.0c00571",
			"acs.orglett.0c00624",
			"acs.orglett.0c00755",
			"acs.orglett.0c00788",
			"acs.orglett.0c00874",
			"acs.orglett.0c00967",
			"acs.orglett.0c01022",
			"acs.orglett.0c01043",
			"acs.orglett.0c01153",
			"acs.orglett.0c01197",
			"acs.orglett.0c01277",
			"acs.orglett.0c01297",
	};
	
	public ExtractorTest(File ifsExtractScript, File targetDir) throws IOException {
		// first create super.objects, a List<String>
		getObjectsForFile(ifsExtractScript);
		// now actually do the extraction.
		extractObjects(targetDir);
	}


	public static void main(String[] args) {
		// ./extract/ should be in the main Eclipse project directory.
		String def = (args.length < 1 ? testSet[1] : null);
		String script = (args.length < 1 ? "./extract/" + def + "/IFS-extract.json" : args[0]);
		String targetDir = (args.length >= 2 ? args[1] : def == null ? new File(script).getParent() :  "c:/temp/ifs/" + def);
		try {
			new ExtractorTest(new File(script), new File(targetDir));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}