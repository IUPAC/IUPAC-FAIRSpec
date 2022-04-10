package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.ExtractorI;

import com.integratedgraphics.ifd.util.DefaultVendorPlugin;

public class JCAMPDXIFDVendorPlugin extends DefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public String accept(ExtractorI extractor, String ifdPath, byte[] bytes) {
		super.accept(extractor, ifdPath, bytes);
		System.out.println("! TODO: accept JDX file " + ifdPath);
		return processRepresentation(null, null);
	}

	@Override
	public String getVendorName() {
		return "JCAMP-DX";
	}

	@Override
	public String processRepresentation(String ifdPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}