package com.integratedgraphics.ifs.vendor.jcamp;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;

import com.integratedgraphics.ifs.util.IFSDefaultVendorPlugin;

public class JCAMPDXIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.jcamp.JCAMPDXIFSVendorPlugin.class);
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

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}