package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;

public class JCAMPDXIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
	    System.out.println("! JCAMPDX Plugin TODO: accept JDX file " + originPath);
		return super.accept(extractor, originPath, bytes);
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