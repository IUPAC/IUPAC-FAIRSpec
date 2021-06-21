package com.vendor.jeol;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.iupac.fairspec.util.IFSDefaultVendorPlugin;

public class JeolIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.jeol.JeolIFSVendorPlugin.class);
	}

	public JeolIFSVendorPlugin() {
		paramRegex = "\\.jdf$";
	}
	
	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		// TODO Auto-generated method stub
		System.out.println("! TODO: accept JEOL file " + fname);
		return true;
	}

	@Override
	public String getVendorName() {
		return "JEOL";
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.NMR_REP_VENDOR_DATASET;
	}
	
}