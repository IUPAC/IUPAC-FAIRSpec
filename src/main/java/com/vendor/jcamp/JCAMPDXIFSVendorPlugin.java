package com.vendor.jcamp;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;

import com.vendor.IFSDefaultVendorPlugin;

public class JCAMPDXIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.jcamp.JCAMPDXIFSVendorPlugin.class);
	}

	public JCAMPDXIFSVendorPlugin() {
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getParamRegex() {
		return "\\.jdx$|\\.dx$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		System.out.println("! TODO: accept JDX file " + fname);
		return true;
	}


}