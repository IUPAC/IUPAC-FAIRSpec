package org.iupac.fairdata.data.nmr;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

public class IFDNMRSpecDataRepresentation extends IFDDataObjectRepresentation {

	public final static String IFD_REP_SPEC_NMR_VENDOR_DATASET       = "IFD.representation.spec.nmr.vendor.dataset";
	public final static String IFD_REP_SPEC_NMR_SPECTRUM_PDF         = "IFD.representation.spec.nmr.spectrum.pdf";
	public final static String IFD_REP_SPEC_NMR_SPECTRUM_IMAGE       = "IFD.representation.spec.nmr.spectrum.image";
	public final static String IFD_REP_SPEC_NMR_SPECTRUM_DESCRIPTION = "IFD.representation.spec.nmr.spectrum.description";
	public final static String IFD_REP_SPEC_NMR_PEAKLIST             = "IFD.representation.spec.nmr.peaklist";
	public final static String IFD_REP_SPEC_NMR_JCAMP_FID_1D         = "IFD.representation.spec.nmr.jcamp.fid.1d";
	public final static String IFD_REP_SPEC_NMR_JCAMP_FID_2D         = "IFD.representation.spec.nmr.jcamp.fid.2d";
	public final static String IFD_REP_SPEC_NMR_JCAMP_SPEC_1r_1D     = "IFD.representation.spec.nmr.jcamp.spec.1r.1d";
	public final static String IFD_REP_SPEC_NMR_JCAMP_SPEC_1i1r_1D   = "IFD.representation.spec.nmr.jcamp.spec.1i1r.1d";
	public final static String IFD_REP_SPEC_NMR_JCAMP_SPEC_2D        = "IFD.representation.spec.nmr.jcamp.spec.2d";

	private final static String[] repNames = new String[] { 
			IFD_REP_SPEC_NMR_VENDOR_DATASET, 
			IFD_REP_SPEC_NMR_JCAMP_FID_1D,
			IFD_REP_SPEC_NMR_JCAMP_FID_2D, 
			IFD_REP_SPEC_NMR_JCAMP_SPEC_1r_1D, 
			IFD_REP_SPEC_NMR_JCAMP_SPEC_1i1r_1D, 
			IFD_REP_SPEC_NMR_JCAMP_SPEC_2D,
			IFD_REP_SPEC_NMR_SPECTRUM_PDF, 
			IFD_REP_SPEC_NMR_SPECTRUM_IMAGE, 
			IFD_REP_SPEC_NMR_SPECTRUM_DESCRIPTION, 
			IFD_REP_SPEC_NMR_PEAKLIST
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDNMRSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
