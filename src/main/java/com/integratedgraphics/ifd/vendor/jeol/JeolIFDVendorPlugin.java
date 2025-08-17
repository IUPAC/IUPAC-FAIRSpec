package com.integratedgraphics.ifd.vendor.jeol;

import java.io.IOException;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.nmrml.NmrMLIFDVendorPlugin;

public class JeolIFDVendorPlugin extends NmrMLIFDVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jeol.JeolIFDVendorPlugin.class);
	}


	public JeolIFDVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			System.out.println("JEOL " + originPath);
			setParams(jeol, jeol.read());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getVendorDataSetKey();
	}

	@Override
	public String getVendorName() {
		return "JEOL";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}