package com.integratedgraphics.html;

import java.io.File;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

/**
 * Create a test site for a finding aid. Just transfers files from site/* and optionally launch it.
 * 
 * @author hanso
 *
 */
public class PageCreator {

	private PageCreator() {
		// no instance necessary
	}
	
	private final static String ifdConfigJS = "_IFD_config.js";
	private final static String ifdFindingAidsJS = "_IFD_findingaids.js";

	private final static String[] files = {
			"assets/FAIRSpec.css",
			"assets/FAIRSpec-config.js",
			"assets/FAIRSpec-get.js",
			"assets/FAIRSpec-gui.js",
			"assets/FAIRSpec-jmol.js",
			ifdConfigJS,
			ifdFindingAidsJS,
			"index.htm",
	};

	public static void buildSite(File targetDir, boolean isLocal, boolean doLaunch) throws Exception {
		new File(targetDir, "assets").mkdirs();
		for (int i = 0; i < files.length; i++) {
			byte[] bytes = FAIRSpecUtilities.getResourceBytes(PageCreator.class, "site/" + files[i]);
			if (!isLocal && files[i] == ifdConfigJS) {
				bytes = new String(bytes).replace("true", "false").getBytes();
			}
			FAIRSpecUtilities.writeBytesToFile(bytes,
					new File(targetDir + "/" + files[i]));
		}
		System.out
				.println("PageCreater created " + files.length + " files in " + targetDir.getAbsolutePath());
		if (doLaunch)
			FAIRSpecUtilities.showUrl(targetDir.toString().replace('\\', '/') + "/index.htm");
	}

	public final static void main(String[] args) {
		try {
			PageCreator.buildSite(new File("C:/temp/tpc"), true, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}