package com.vendor.jcamp;

import org.iupac.fairspec.api.IFSDefaultVendorPlugin;
import org.iupac.fairspec.api.IFSExtractorI;

public class JCAMPDXIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.jcamp.JCAMPDXIFSVendorPlugin.class);
	}

	public JCAMPDXIFSVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		// TODO Auto-generated method stub
		System.out.println("! TODO: accept JDX file " + fname);
		return true;
	}

	@Override
	public String getVendorName() {
		return "JCAMP-DX";
	}

}