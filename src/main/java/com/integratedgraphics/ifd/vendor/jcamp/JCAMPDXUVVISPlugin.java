package com.integratedgraphics.ifd.vendor.jcamp;

import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.JCAMPPlugin;

public class JCAMPDXUVVISPlugin extends JCAMPDXIFDVendorPlugin implements JCAMPPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_UVVIS_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_UVVIS.VENDOR_DATASET");
    protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UVVIS_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UVVIS.INSTR_MANUFACTURER_NAME");


//			##TITLE=Aquation of trans-[Co(en)2Cl2]...
//			##DATATYPE=UV/VIS SPECTRUM
//			##BLOCKS=20
//			##CONCENTRATION=[Co(en)2Cl2]+ 6mM, [H+] 0.1 M ...
//			##COMMENT=Time in 15 minute intervals, t...
//			##JCAMPDX=4.24
//			##BLOCKID=1
//			##ORIGIN=Dept of Chem, UWI, Mona, JAMAI...
//			##OWNER=R.J. Lancashire
//			##DATE=96/11/06
//			##TIME=19:14:14.00
//			##SPECTROMETERDATASYSTEM=PERKIN-ELMER LAMBDA 19 UV/VIS/...
//			##RESOLUTION=1.000000
//			##DELTAX=-1.000000
//			##XUNITS=nanometers
//			##YUNITS=ABSORBANCE
//			##XFACTOR=1.00
//			##YFACTOR=.00000011920928955078
//			##FIRSTX=700.00
//			##LASTX=350.00
//			##NPOINTS=351
//			##FIRSTY=.20
//			##MAXY=-.005953
//			##MINY=.20
//			##XYDATA=<data>

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXNMRPlugin.class);
	}

	public JCAMPDXUVVISPlugin() {
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
		return "JCAMP-DX(UVVIS)";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_UVVIS_VENDOR_DATASET;
	}

    @Override
	public void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UVVIS_INSTR_MANUFACTURER_NAME, getVendorName());
	}
}