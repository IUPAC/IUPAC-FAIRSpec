package org.iupac.fairdata.data.raman;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

public class IFDRamanSpecDataRepresentation extends IFDDataObjectRepresentation {


	public final static String IFD_REP_SPEC_RAMAN_VENDOR_DATASET = "IFD.representation.spec.raman.vendor.dataset";
	public final static String IFD_REP_SPEC_RAMAN_SPECTRUM_PDF = "IFD.representation.spec.raman.spectrum.pdf";
	public final static String IFD_REP_SPEC_RAMAN_SPECTRUM_IMAGE = "IFD.representation.spec.raman.spectrum.image";
	public final static String IFD_REP_SPEC_RAMAN_SPECTRUM_DESCRIPTION = "IFD.representation.spec.raman.spectrum.description";
	public final static String IFD_REP_SPEC_RAMAN_PEAKLIST = "IFD.representation.spec.raman.peaklist";
	public final static String IFD_REP_SPEC_RAMAN_JCAMP = "IFD.representation.spec.raman.jcamp";

	private final static String[] repNames = new String[] {
			IFD_REP_SPEC_RAMAN_VENDOR_DATASET,
			IFD_REP_SPEC_RAMAN_JCAMP,
			IFD_REP_SPEC_RAMAN_SPECTRUM_PDF,
			IFD_REP_SPEC_RAMAN_SPECTRUM_IMAGE,
			IFD_REP_SPEC_RAMAN_SPECTRUM_DESCRIPTION,
			IFD_REP_SPEC_RAMAN_PEAKLIST	
	};

	public static String[] getRepnames() {
		return repNames;
	}


	public IFDRamanSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
