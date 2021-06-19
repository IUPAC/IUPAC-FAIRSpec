package com.vendor.jeol;

import org.iupac.fairspec.api.IFSDefaultVendorPlugin;
import org.iupac.fairspec.api.IFSExtractorI;

public class JeolIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.jeol.JeolIFSVendorPlugin.class);
	}

	public JeolIFSVendorPlugin() {
		paramRegex = "\\.jdf$";
	}
	
	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		// TODO Auto-generated method stub
		System.out.println("! TODO: accept JEOL file " + fname);
		return true;
	}
	
}