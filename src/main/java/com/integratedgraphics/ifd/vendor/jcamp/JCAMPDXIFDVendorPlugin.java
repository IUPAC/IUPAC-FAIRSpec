package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.ExtractorI;

import com.integratedgraphics.ifd.util.DefaultVendorPlugin;
import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;

public class JCAMPDXIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public String accept(ExtractorI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		System.out.println("! JCAMPDX Plugin TODO: accept JDX file " + originPath);
		return processRepresentation(null, null);
	}

	@Override
	public String getVendorName() {
		return "JCAMP-DX";
	}

	@Override
	public String processRepresentation(String originPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}