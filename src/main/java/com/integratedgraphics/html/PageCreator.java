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
			"assets/FAIRSpec-swingjs.js",
			ifdConfigJS,
			ifdFindingAidsJS,
			"index.htm",
	};

	public static void buildSite(File htmlPath, boolean isLocal, boolean doLaunch) throws Exception {
		if (htmlPath == null)
			return;
		new File(htmlPath, "assets").mkdirs();
		for (int i = 0; i < files.length; i++) {
			byte[] bytes = FAIRSpecUtilities.getResourceBytes(PageCreator.class, "site/" + files[i]);
			if (!isLocal && files[i] == ifdConfigJS) {
				bytes = new String(bytes).replace("true", "false").getBytes();
			}
			File f = new File(htmlPath + "/" + files[i]);
			System.out.println("PageCreator creating " + f.getAbsolutePath());
			FAIRSpecUtilities.writeBytesToFile(bytes, f);
		}
		System.out
				.println("PageCreater created " + files.length + " files in " + htmlPath.getAbsolutePath());
		if (doLaunch) {
			String path = htmlPath.getAbsolutePath().replace('\\', '/') + "/index.htm";
			try {
				FAIRSpecUtilities.showUrl(path);
			} catch (Exception e) {
				System.out.println("PageCreator could not launch " + path);
			}
		}
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