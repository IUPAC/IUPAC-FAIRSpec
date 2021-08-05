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
	public String accept(IFSExtractorI extractor, String ifsPath, byte[] bytes) {
		super.accept(extractor, ifsPath, bytes);
		System.out.println("! TODO: accept JDX file " + ifsPath);
		return processRepresentation(null, null);
	}

	@Override
	public String getVendorName() {
		return "JCAMP-DX";
	}

	@Override
	public String processRepresentation(String ifsPath, byte[] bytes) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}