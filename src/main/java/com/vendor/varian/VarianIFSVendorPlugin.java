package com.vendor.varian;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.iupac.fairspec.util.IFSDefaultVendorPlugin;

public class VarianIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.varian.VarianIFSVendorPlugin.class);
	}

	public VarianIFSVendorPlugin() {
		paramRegex = "TODO";
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getVendorName() {
		return "Varian";
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}


}