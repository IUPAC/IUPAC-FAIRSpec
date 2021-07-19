package com.integratedgraphics.ifs.vendor.jeol;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifs.vendor.NmrMLIFSVendorPlugin;

public class JeolIFSVendorPlugin extends NmrMLIFSVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.jeol.JeolIFSVendorPlugin.class);
	}


	public JeolIFSVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, String zipName, byte[] bytes) {
		super.accept(extractor, fname, zipName, bytes);
		try {
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(new ByteArrayInputStream(bytes));
			Acqu acq = jeol.read();
			setParams(jeol.getDimension(), acq);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public String getVendorName() {
		return "JEOL";
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}