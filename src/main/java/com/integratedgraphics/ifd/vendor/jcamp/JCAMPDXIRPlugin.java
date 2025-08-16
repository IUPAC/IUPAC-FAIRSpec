package com.integratedgraphics.ifd.vendor.jcamp;

import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.JCAMPPlugin;

public class JCAMPDXIRPlugin extends JCAMPDXIFDVendorPlugin implements JCAMPPlugin {


	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_IR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_IR.VENDOR_DATASET");
    protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_IR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_IR.INSTR_MANUFACTURER_NAME");

//			##TITLE= Compound file, contains several data records
//			##JCAMP-DX= 5.0
//			##DATA TYPE= LINK
//			##BLOCKS=3
//			##ORIGIN= Department of Chemistry, UWI, JAMAICA
//			##OWNER= public domain
//			##TITLE= acetophenone
//			##JCAMP-DX= 4.24   $$ ISAS JCAMP-DX check program V1.1/a
//			##DATA TYPE= INFRARED SPECTRUM
//			##BLOCK_ID=1
//			##ORIGIN= Dept of Chemistry, UWI, JAMAICA
//			##OWNER= Public domain
//			##DATE=99/10/22
//			##TIME=13:54:00
//			##SPECTROMETER/DATA SYSTEM= PERKIN-ELMER 1000 FT-IR
//			##CAS REGISTRY NO=98-86-2
//			##MOLFORM=C 8 H 8 O
//			##DELTAX=   .9643013    
//			##XUNITS= 1/CM
//			##YUNITS= TRANSMITTANCE
//			##XFACTOR=   .1221008    
//			##YFACTOR=   .2982387E-04
//			##FIRSTX=   499.5000    
//			##LASTX=   4000.878    
//			##MAXY=   .9772386    
//			##MINY=   .9185180E-04
//			##NPOINTS=   3632
//			##FIRSTY=   .6180727    
//			##XYDATA=(X++(Y..Y))

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXNMRPlugin.class);
	}

	public JCAMPDXIRPlugin() {
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
		return "JCAMP-DX(IR)";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_IR_VENDOR_DATASET;
	}

    @Override
	public void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_IR_INSTR_MANUFACTURER_NAME, getVendorName());
	}


}