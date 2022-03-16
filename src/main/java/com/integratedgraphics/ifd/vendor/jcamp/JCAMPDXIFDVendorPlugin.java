package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.api.IFDExtractorI;
import org.iupac.fairdata.spec.nmr.IFDNMRSpecDataRepresentation;

import com.integratedgraphics.ifd.util.IFDDefaultVendorPlugin;

public class JCAMPDXIFDVendorPlugin extends IFDDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public String accept(IFDExtractorI extractor, String ifdPath, byte[] bytes) {
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
		return IFDNMRSpecDataRepresentation.IFD_REP_SPEC_NMR_VENDOR_DATASET;
	}

}