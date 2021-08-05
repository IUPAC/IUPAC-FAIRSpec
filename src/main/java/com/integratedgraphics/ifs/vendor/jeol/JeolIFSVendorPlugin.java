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
	public String accept(IFSExtractorI extractor, String ifsPath, byte[] bytes) {
		super.accept(extractor, ifsPath, bytes);
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
	public String processRepresentation(String ifsPath, byte[] bytes) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}