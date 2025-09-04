package com.integratedgraphics.ifd.vendor.jcamp;

import org.iupac.fairdata.extract.MetadataReceiverI;

public class JCAMPDXUVVISPlugin extends JCAMPDXIFDVendorPlugin {

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
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXUVVISPlugin.class);
	}

	public JCAMPDXUVVISPlugin() {
		setJCAMPType("UVVIS");
	}
	
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		// TODO
		return getVendorDataSetKey();
	}

}