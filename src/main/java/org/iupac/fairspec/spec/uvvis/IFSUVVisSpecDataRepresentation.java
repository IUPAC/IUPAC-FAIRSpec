package org.iupac.fairspec.spec.uvvis;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSUVVisSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String MS_REP_VENDOR_DATASET = "IFS.uvvis.rep.vendor.dataset";
	public final static String MS_REP_SPECTRUM_PDF = "IFS.uvvis.rep.spectrum.pdf";
	public final static String MS_REP_SPECTRUM_IMAGE = "IFS.uvvis.rep.spectrum.image";
	public final static String MS_REP_SPECTRUM_DESCRIPTION = "IFS.uvvis.rep.spectrum.description";
	public final static String MS_REP_PEAKLIST = "IFS.uvvis.rep.peaklist";
	public final static String MS_REP_JCAMP = "IFS.uvvis.rep.jcamp";

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

	public IFSUVVisSpecDataRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
