package org.iupac.fairdata.spec.ms;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.spec.IFDSpecDataRepresentation;

public class IFDMSSpecDataRepresentation extends IFDSpecDataRepresentation {

	public final static String IFD_REP_SPEC_MS_VENDOR_DATASET = "IFD.representation.spec.ms.vendor.dataset";
	public final static String IFD_REP_SPEC_MS_SPECTRUM_PDF = "IFD.representation.spec.ms.spectrum.pdf";
	public final static String IFD_REP_SPEC_MS_SPECTRUM_IMAGE = "IFD.representation.spec.ms.spectrum.image";
	public final static String IFD_REP_SPEC_MS_SPECTRUM_DESCRIPTION = "IFD.representation.spec.ms.spectrum.description";
	public final static String IFD_REP_SPEC_MS_PEAKLIST = "IFD.representation.spec.ms.peaklist";
	public final static String IFD_REP_SPEC_MS_JCAMP = "IFD.representation.spec.ms.jcamp";

	private final static String[] repNames = new String[] {
			IFD_REP_SPEC_MS_VENDOR_DATASET,
			IFD_REP_SPEC_MS_JCAMP,
			IFD_REP_SPEC_MS_SPECTRUM_PDF,
			IFD_REP_SPEC_MS_SPECTRUM_IMAGE,
			IFD_REP_SPEC_MS_SPECTRUM_DESCRIPTION,
			IFD_REP_SPEC_MS_PEAKLIST	
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDMSSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
