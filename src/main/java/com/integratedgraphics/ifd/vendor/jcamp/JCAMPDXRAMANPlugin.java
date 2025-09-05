package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.MetadataReceiverI;

public class JCAMPDXRAMANPlugin extends JCAMPDXIFDVendorPlugin {

//			##TITLE= ethanolamine
//			##JCAMP-DX= 5.01
//			##DATA TYPE= RAMAN SPECTRUM
//			##DATA CLASS= XYDATA
//			##ORIGIN= Department of Chemistry, UWI, Jamaica
//			##OWNER= public domain
//			##LONGDATE= 2012/03/09 13:08:48.0525 -0500 $$ export date from JSpecView
//			##SPECTROMETER/DATA SYSTEM= scan
//			##XUNITS= 1/CM
//			##YUNITS= RAMAN INTENSITY
//			##XFACTOR= 1E0
//			##YFACTOR= 9.5147402E-7
//			##FIRSTX= 33.81494141
//			##FIRSTY= 0.86726397
//			##LASTX= 3989.93711266
//			##NPOINTS= 1489
//			##XYDATA= (X++(Y..Y))

	
	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXRAMANPlugin.class);
	}

	public JCAMPDXRAMANPlugin() {
		setJCAMPType("RAMAN");
	}
	
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		// TODO
		return getVendorDataSetKey();
	}

}