package com.integratedgraphics.ifd.vendor.varian;

import java.io.ByteArrayInputStream;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.nmrml.NmrMLIFDVendorPlugin;


public class VarianIFDVendorPlugin extends NmrMLIFDVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.varian.VarianIFDVendorPlugin.class);
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(new ByteArrayInputStream(bytes));
			setParams(varian, varian.read());
			return processRepresentation(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getVendorName() {
		return "Varian";
	}

	@Override
	public String processRepresentation(String originPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}


}