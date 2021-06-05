package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSSpecDataRepresentation;

public class IFSNMRSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String NMR_REP_VENDOR_DATASET = "nmr.rep.vendor.dataset";
	public final static String NMR_REP_SPECTRUM_PDF = "nmr.rep.spectrum.pdf";
	public final static String NMR_REP_SPECTRUM_IMAGE = "nmr.rep.spectrum.image";
	public final static String NMR_REP_SPECTRUM_DESCRIPTION = "nmr.rep.spectrum.description";
	public final static String NMR_REP_PEAKLIST = "nmr.rep.peaklist";
	public final static String NMR_REP_JCAMP_FID_1D = "nmr.rep.jcamp.fid.1d";
	public final static String NMR_REP_JCAMP_FID_2D = "nmr.rep.jcamp.fid.2d";
	public final static String NMR_REP_JCAMP_SPEC_1r_1D = "nmr.rep.jcamp.spec.1r.1d";
	public final static String NMR_REP_JCAMP_SPEC_1i1r_1D = "nmr.rep.jcamp.spec.1i1r.1d";
	public final static String NMR_REP_JCAMP_SPEC_2D = "nmr.rep.jcamp.spec.2d";

	private final static String[] repNames = new String[] {
			NMR_REP_VENDOR_DATASET,
			NMR_REP_JCAMP_FID_1D,
			NMR_REP_JCAMP_FID_2D,
			NMR_REP_JCAMP_SPEC_1r_1D,
			NMR_REP_JCAMP_SPEC_1i1r_1D,
			NMR_REP_JCAMP_SPEC_2D,
			NMR_REP_SPECTRUM_PDF,
			NMR_REP_SPECTRUM_IMAGE,
			NMR_REP_SPECTRUM_DESCRIPTION,
			NMR_REP_PEAKLIST,		
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSNMRSpecDataRepresentation(String type, IFSReference ref, Object data) {
		super(type, ref, data);
	}

}
