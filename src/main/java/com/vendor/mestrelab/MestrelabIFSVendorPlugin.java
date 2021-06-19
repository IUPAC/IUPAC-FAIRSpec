package com.vendor.mestrelab;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;

import com.vendor.IFSDefaultVendorPlugin;

public class MestrelabIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	public MestrelabIFSVendorPlugin() {
		
	}


	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getParamRegex() {
		return "\\.mnova$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		System.out.println("! TODO: accept mnova file " + fname);
		return true;
	}

	
}