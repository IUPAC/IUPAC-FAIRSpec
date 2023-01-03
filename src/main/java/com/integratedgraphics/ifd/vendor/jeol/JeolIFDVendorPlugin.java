package com.integratedgraphics.ifd.vendor.jeol;

import java.io.IOException;

import org.iupac.fairdata.extract.ExtractorI;

import com.integratedgraphics.ifd.vendor.nmrml.NmrMLIFDVendorPlugin;

public class JeolIFDVendorPlugin extends NmrMLIFDVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jeol.JeolIFDVendorPlugin.class);
	}


	public JeolIFDVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public String accept(ExtractorI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			System.out.println("JEOL " + originPath);
			setParams(jeol, jeol.read());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processRepresentation(null, null);
	}

	@Override
	public String getVendorName() {
		return "JEOL";
	}

	@Override
	public String processRepresentation(String originPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}