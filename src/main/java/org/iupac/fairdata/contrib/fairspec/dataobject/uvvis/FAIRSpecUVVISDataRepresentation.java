package org.iupac.fairdata.data.uvvis;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

public final class IFDUVVISSpecDataRepresentation extends IFDDataObjectRepresentation {

	public final static String IFD_REP_UVVIS_VENDOR_DATASET = "IFD.representation.spec.uvvis.vendor.dataset";
	public final static String IFD_REP_UVVIS_SPECTRUM_PDF = "IFD.representation.spec.uvvis.spectrum.pdf";
	public final static String IFD_REP_UVVIS_SPECTRUM_IMAGE = "IFD.representation.spec.uvvis.spectrum.image";
	public final static String IFD_REP_UVVIS_SPECTRUM_DESCRIPTION = "IFD.representation.spec.uvvis.spectrum.description";
	public final static String IFD_REP_UVVIS_PEAKLIST = "IFD.representation.spec.uvvis.peaklist";
	public final static String IFD_REP_UVVIS_JCAMP = "IFD.representation.spec.uvvis.jcamp";

	private final static String[] repNames = new String[] {
			IFD_REP_UVVIS_VENDOR_DATASET,
			IFD_REP_UVVIS_JCAMP,
			IFD_REP_UVVIS_SPECTRUM_PDF,
			IFD_REP_UVVIS_SPECTRUM_IMAGE,
			IFD_REP_UVVIS_SPECTRUM_DESCRIPTION,
			IFD_REP_UVVIS_PEAKLIST
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDUVVISSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
