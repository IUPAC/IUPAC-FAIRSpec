package org.iupac.fairspec.spec.raman;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSRamanSpecDataRepresentation extends IFSSpecDataRepresentation {


	public final static String IFS_REP_SPEC_RAMAN_VENDOR_DATASET = "IFS.representation.spec.raman.vendor.dataset";
	public final static String IFS_REP_SPEC_RAMAN_SPECTRUM_PDF = "IFS.representation.spec.raman.spectrum.pdf";
	public final static String IFS_REP_SPEC_RAMAN_SPECTRUM_IMAGE = "IFS.representation.spec.raman.spectrum.image";
	public final static String IFS_REP_SPEC_RAMAN_SPECTRUM_DESCRIPTION = "IFS.representation.spec.raman.spectrum.description";
	public final static String IFS_REP_SPEC_RAMAN_PEAKLIST = "IFS.representation.spec.raman.peaklist";
	public final static String IFS_REP_SPEC_RAMAN_JCAMP = "IFS.representation.spec.raman.jcamp";

	private final static String[] repNames = new String[] {
			IFS_REP_SPEC_RAMAN_VENDOR_DATASET,
			IFS_REP_SPEC_RAMAN_JCAMP,
			IFS_REP_SPEC_RAMAN_SPECTRUM_PDF,
			IFS_REP_SPEC_RAMAN_SPECTRUM_IMAGE,
			IFS_REP_SPEC_RAMAN_SPECTRUM_DESCRIPTION,
			IFS_REP_SPEC_RAMAN_PEAKLIST	
	};

	public static String[] getRepnames() {
		return repNames;
	}


	public IFSRamanSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
