package com.integratedgraphics.ifd.vendor.jeol;

import java.io.IOException;

import org.iupac.fairdata.extract.ExtractorI;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.vendor.NmrMLIFDVendorPlugin;

public class JeolIFDVendorPlugin extends NmrMLIFDVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.jeol.JeolIFDVendorPlugin.class);
	}


	public JeolIFDVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public String accept(ExtractorI extractor, String ifdPath, byte[] bytes) {
		super.accept(extractor, ifdPath, bytes);
		try {
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			Acqu acq = jeol.read();
			setParams(jeol.getDimension(), acq);
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
	public String processRepresentation(String ifdPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}