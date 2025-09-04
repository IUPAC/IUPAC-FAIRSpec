package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.MetadataReceiverI;

public class JCAMPDXMSPlugin extends JCAMPDXIFDVendorPlugin {

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
		setJCAMPType("MS");
	}
	
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		// TODO
		return getVendorDataSetKey();
	}

}