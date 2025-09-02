package com.integratedgraphics.ifd.vendor.jcamp;

import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.JCAMPPlugin;

public class JCAMPDXMSPlugin extends JCAMPDXIFDVendorPlugin implements JCAMPPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_MS_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_MS.VENDOR_DATASET");
    protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_MS_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_MS.INSTR_MANUFACTURER_NAME");

//			##TITLE=$$ Begin of the data block
//			##JCAMP-DX=5.00 $$ ACD/Spectrus 2012 v 14.01
//			##BLOCK_ID=2
//			##TIC=22597760.000000
//			##DATA TYPE=MASS SPECTRUM
//			##DATA CLASS=PEAK TABLE
//			##ORIGIN=
//			##OWNER=
//			##NPOINTS=182
//			##FIRSTX=20.00000000
//			##LASTX=248.00000000
//			##MAXX=10000.00000000
//			##MINX=0.00274300
//			##MAXY=0.00000000
//			##MINY=0.00000000
//			##XFACTOR=1.0000000000
//			##YFACTOR=1.000000000000000
//			##FIRSTY=0.49094903
//			##XUNITS=M/Z
//			##YUNITS=RELATIVE ABUNDANCE
//			##PEAK TABLE=(XY..XY)
	
	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXMSPlugin.class);
	}

	public JCAMPDXMSPlugin() {
	}
	
	@Override
	public void setMap(Map<String, String> map) {
		this.map = map;
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		// TODO
		return getVendorDataSetKey();
	}

	@Override
	public String getVendorName() {
		return "JCAMP-DX/MS";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_MS_VENDOR_DATASET;
	}

    @Override
	public void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_MS_INSTR_MANUFACTURER_NAME, getVendorName());
	}
}