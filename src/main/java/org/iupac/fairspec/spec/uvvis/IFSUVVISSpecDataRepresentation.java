package org.iupac.fairspec.spec.uvvis;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public final class IFSUVVISSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IFS_REP_UVVIS_VENDOR_DATASET = "IFS.representation.spec.uvvis.vendor.dataset";
	public final static String IFS_REP_UVVIS_SPECTRUM_PDF = "IFS.representation.spec.uvvis.spectrum.pdf";
	public final static String IFS_REP_UVVIS_SPECTRUM_IMAGE = "IFS.representation.spec.uvvis.spectrum.image";
	public final static String IFS_REP_UVVIS_SPECTRUM_DESCRIPTION = "IFS.representation.spec.uvvis.spectrum.description";
	public final static String IFS_REP_UVVIS_PEAKLIST = "IFS.representation.spec.uvvis.peaklist";
	public final static String IFS_REP_UVVIS_JCAMP = "IFS.representation.spec.uvvis.jcamp";

	private final static String[] repNames = new String[] {
			IFS_REP_UVVIS_VENDOR_DATASET,
			IFS_REP_UVVIS_JCAMP,
			IFS_REP_UVVIS_SPECTRUM_PDF,
			IFS_REP_UVVIS_SPECTRUM_IMAGE,
			IFS_REP_UVVIS_SPECTRUM_DESCRIPTION,
			IFS_REP_UVVIS_PEAKLIST
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSUVVISSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
