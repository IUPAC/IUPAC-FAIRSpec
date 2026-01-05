package com.integratedgraphics.ifd.dataobject.varian;

import java.io.ByteArrayInputStream;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.dataobject.nmrml.NmrMLDataObjectVendorPlugin;


public class VarianDataObjectVendorPlugin extends NmrMLDataObjectVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.dataobject.varian.VarianDataObjectVendorPlugin.class);
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(new ByteArrayInputStream(bytes));
			setParams(varian, varian.read());
			return getVendorDataSetKey();
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
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}


}