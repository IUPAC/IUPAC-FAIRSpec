package com.vendor.varian;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;

import com.vendor.IFSDefaultVendorPlugin;

public class VarianIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.varian.VarianIFSVendorPlugin.class);
	}

	public VarianIFSVendorPlugin() {
		
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getParamRegex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		// TODO Auto-generated method stub
		return false;
	}


}