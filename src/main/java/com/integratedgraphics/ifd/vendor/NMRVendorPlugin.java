package com.integratedgraphics.ifd.vendor;

import java.util.List;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.ifd.util.DefaultVendorPlugin;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {
	
	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET");
    final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME");

    protected NMRVendorPlugin() {
    	super();
    }
    
    @Override
	protected void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}

	protected void addIFDMetadata(String data) {
		List<String[]> list = FAIRSpecUtilities.getIFDPropertyMap(data);
		for (int i = 0, n = list.size(); i < n; i++) {
			String[] item = list.get(i);
			addProperty(item[0], item[1]);
		}
	}

}
