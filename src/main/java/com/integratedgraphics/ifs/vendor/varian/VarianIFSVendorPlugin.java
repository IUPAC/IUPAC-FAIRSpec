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
	public String accept(IFSExtractorI extractor, String ifsPath, byte[] bytes) {
		super.accept(extractor, ifsPath, bytes);
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
	public String processRepresentation(String ifsPath, byte[] bytes) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}


}