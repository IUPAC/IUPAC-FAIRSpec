package com.vendor.jeol;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;

import com.vendor.IFSDefaultVendorPlugin;

public class JeolIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.jeol.JeolIFSVendorPlugin.class);
	}

	public JeolIFSVendorPlugin() {
		
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getParamRegex() {
		return "\\.jdf$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		System.out.println("! TODO: accept JEOL file " + fname);
		return true;
	}
	
}