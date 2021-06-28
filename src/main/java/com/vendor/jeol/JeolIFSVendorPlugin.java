package com.vendor.jeol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.iupac.fairspec.util.IFSDefaultVendorPlugin;
import org.nmrml.converter.Acqu2nmrML;
import org.nmrml.cv.SpectrometerMapper;
import org.nmrml.parser.Acqu;

import com.vendor.NmrMLIFSVendorPlugin;

import jspecview.common.Spectrum;

public class JeolIFSVendorPlugin extends NmrMLIFSVendorPlugin {

	protected static SpectrometerMapper vendorMapper;

	static {
		register(com.vendor.jeol.JeolIFSVendorPlugin.class);
	}


	public JeolIFSVendorPlugin() {
		paramRegex = "\\.jdf$";
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		try {
			if (vendorMapper == null) {
				vendorMapper = new SpectrometerMapper(Acqu2nmrML.class.getResourceAsStream("resources/jeol.ini"));
			}
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			jeol.setVendorMapper(vendorMapper);
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