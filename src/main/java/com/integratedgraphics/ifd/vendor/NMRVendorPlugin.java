package com.integratedgraphics.ifd.vendor;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {
	
	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.VENDOR_DATASET");
    final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_MANUFACTURER_NAME");

    protected NMRVendorPlugin() {
    	super();
    }
    
    @Override
	protected void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}

}
