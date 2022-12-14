package com.integratedgraphics.ifd.vendor;

import com.integratedgraphics.ifd.util.DefaultVendorPlugin;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {
	
	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET");
    final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME");

    @Override
	protected void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}

}
