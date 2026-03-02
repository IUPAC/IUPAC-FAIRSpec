package com.integratedgraphics.ifd.dataobject.varian;

import java.io.ByteArrayInputStream;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.dataobject.nmrml.NmrMLDataObjectVendorPlugin;


public class VarianDataObjectVendorPlugin extends NmrMLDataObjectVendorPlugin {

	static {
		register(VarianDataObjectVendorPlugin.class);
	}

	public VarianDataObjectVendorPlugin() {
		super();
		paramRegex += "|procpar$";
		// rezip triggers for procs in a directory (1, 2, 3...) below a pdata directory,
		// such as pdata/1/procs. We do not add the "/" before pdata, because that could
		// be the| symbol, and that will be attached by IFDDefaultVendorPlugin in
		// super.getRezipRegex()
		rezipRegex = "procpar$";


	}
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		try {
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(new ByteArrayInputStream(bytes));
			setParams(varian, varian.read());
			return getVendorDataSetKey();
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
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}


}