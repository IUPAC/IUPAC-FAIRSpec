package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSNMRSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IFS_REP_SPEC_NMR_VENDOR_DATASET = "IFS.representation.spec.nmr.vendor.dataset";
	public final static String IFS_REP_SPEC_NMR_SPECTRUM_PDF = "IFS.representation.spec.nmr.spectrum.pdf";
	public final static String IFS_REP_SPEC_NMR_SPECTRUM_IMAGE = "IFS.representation.spec.nmr.spectrum.image";
	public final static String IFS_REP_SPEC_NMR_SPECTRUM_DESCRIPTION = "IFS.representation.spec.nmr.spectrum.description";
	public final static String IFS_REP_SPEC_NMR_PEAKLIST = "IFS.representation.spec.nmr.peaklist";
	public final static String IFS_REP_SPEC_NMR_JCAMP_FID_1D = "IFS.representation.spec.nmr.jcamp.fid.1d";
	public final static String IFS_REP_SPEC_NMR_JCAMP_FID_2D = "IFS.representation.spec.nmr.jcamp.fid.2d";
	public final static String IFS_REP_SPEC_NMR_JCAMP_SPEC_1r_1D = "IFS.representation.spec.nmr.jcamp.spec.1r.1d";
	public final static String IFS_REP_SPEC_NMR_JCAMP_SPEC_1i1r_1D = "IFS.representation.spec.nmr.jcamp.spec.1i1r.1d";
	public final static String IFS_REP_SPEC_NMR_JCAMP_SPEC_2D = "IFS.representation.spec.nmr.jcamp.spec.2d";

	private final static String[] repNames = new String[] { 
			IFS_REP_SPEC_NMR_VENDOR_DATASET, 
			IFS_REP_SPEC_NMR_JCAMP_FID_1D,
			IFS_REP_SPEC_NMR_JCAMP_FID_2D, 
			IFS_REP_SPEC_NMR_JCAMP_SPEC_1r_1D, 
			IFS_REP_SPEC_NMR_JCAMP_SPEC_1i1r_1D, 
			IFS_REP_SPEC_NMR_JCAMP_SPEC_2D,
			IFS_REP_SPEC_NMR_SPECTRUM_PDF, 
			IFS_REP_SPEC_NMR_SPECTRUM_IMAGE, 
			IFS_REP_SPEC_NMR_SPECTRUM_DESCRIPTION, 
			IFS_REP_SPEC_NMR_PEAKLIST
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSNMRSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
