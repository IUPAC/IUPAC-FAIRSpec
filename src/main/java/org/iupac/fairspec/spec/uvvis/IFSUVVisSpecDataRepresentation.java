package org.iupac.fairspec.spec.uvvis;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSUVVisSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String MS_REP_VENDOR_DATASET = "IFS.uvvis.representation.vendor.dataset";
	public final static String MS_REP_SPECTRUM_PDF = "IFS.uvvis.representation.spectrum.pdf";
	public final static String MS_REP_SPECTRUM_IMAGE = "IFS.uvvis.representation.spectrum.image";
	public final static String MS_REP_SPECTRUM_DESCRIPTION = "IFS.uvvis.representation.spectrum.description";
	public final static String MS_REP_PEAKLIST = "IFS.uvvis.representation.peaklist";
	public final static String MS_REP_JCAMP = "IFS.uvvis.representation.jcamp";

	private final static String[] repNames = new String[] {
			MS_REP_VENDOR_DATASET,
			MS_REP_JCAMP,
			MS_REP_SPECTRUM_PDF,
			MS_REP_SPECTRUM_IMAGE,
			MS_REP_SPECTRUM_DESCRIPTION,
			MS_REP_PEAKLIST,		
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSUVVisSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
