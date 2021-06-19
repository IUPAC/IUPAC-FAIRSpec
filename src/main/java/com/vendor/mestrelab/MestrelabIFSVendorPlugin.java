package com.vendor.mestrelab;

import org.iupac.fairspec.api.IFSDefaultVendorPlugin;
import org.iupac.fairspec.api.IFSExtractorI;

public class MestrelabIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	public MestrelabIFSVendorPlugin() {
		paramRegex = "\\.mnova$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		// TODO Auto-generated method stub
		System.out.println("! TODO: accept mnova file " + fname);
		return true;
	}

	@Override
	public String getVendorName() {
		return "Mestrelab";
	}
	
}