package com.integratedgraphics.ifs.vendor.mestrelab;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;

import com.integratedgraphics.ifs.util.IFSDefaultVendorPlugin;

public class MestrelabIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	public MestrelabIFSVendorPlugin() {
		paramRegex = "\\.mnova[^/]*$";
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

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}