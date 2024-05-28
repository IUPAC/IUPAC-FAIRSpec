package com.integratedgraphics.ifd.vendor.jcamp;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.ExtractorI;
import org.iupac.fairdata.util.ZipUtil;

import com.integratedgraphics.ifd.vendor.DefaultVendorPlugin;
import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;

import jspecview.source.JDXReader;

public class JCAMPDXIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	@Override
	public String accept(ExtractorI extractor, String originPath, byte[] bytes, boolean isEmbedded) {
	    System.out.println("! JCAMPDX Plugin TODO: accept JDX file " + originPath);
		return super.accept(extractor, originPath, bytes, isEmbedded);
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