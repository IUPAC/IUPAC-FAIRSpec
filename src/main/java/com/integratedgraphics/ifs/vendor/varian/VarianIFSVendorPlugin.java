package com.integratedgraphics.ifs.vendor.varian;

import java.io.ByteArrayInputStream;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifs.vendor.NmrMLIFSVendorPlugin;


public class VarianIFSVendorPlugin extends NmrMLIFSVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.varian.VarianIFSVendorPlugin.class);
	}

	public VarianIFSVendorPlugin() {
		paramRegex = "procpar$";
		rezipRegex = "procpar";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		try {
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(new ByteArrayInputStream(bytes));
			Acqu acq = varian.read();
			setParams(varian.getDimension(), acq);
		} catch (Exception e) {
			e.printStackTrace();
		}
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