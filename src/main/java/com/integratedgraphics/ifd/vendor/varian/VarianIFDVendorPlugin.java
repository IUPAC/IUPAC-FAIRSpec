package com.integratedgraphics.ifd.vendor.varian;

import java.io.ByteArrayInputStream;

import org.iupac.fairdata.api.IFDExtractorI;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.vendor.NmrMLIFDVendorPlugin;


public class VarianIFDVendorPlugin extends NmrMLIFDVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.varian.VarianIFDVendorPlugin.class);
	}

	public VarianIFDVendorPlugin() {
		paramRegex = "procpar$";
		rezipRegex = "procpar";
	}

	@Override
	public String accept(IFDExtractorI extractor, String ifdPath, byte[] bytes) {
		super.accept(extractor, ifdPath, bytes);
		try {
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(new ByteArrayInputStream(bytes));
			Acqu acq = varian.read();
			setParams(varian.getDimension(), acq);
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
	public String processRepresentation(String ifdPath, byte[] bytes) {
		return IFD_REP_DATA_SPEC_NMR_VENDOR_DATASET;
	}


}