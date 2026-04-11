package com.integratedgraphics.ifd.dataobject.jeol;

import java.io.IOException;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.dataobject.nmrml.NmrMLDataObjectVendorPlugin;

public class JeolDataObjectVendorPlugin extends NmrMLDataObjectVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.dataobject.jeol.JeolDataObjectVendorPlugin.class);
	}


	public JeolDataObjectVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			System.out.println("JEOL " + originPath);
			setParams(jeol, jeol.read());
		} catch (Exception e) {
			String err = e.getMessage() + " reading " + originPath;
			addProperty("note", err);
			System.err.println(err);
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