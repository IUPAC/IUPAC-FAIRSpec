package org.iupac.fairspec.spec.ms;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSMSSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String MS_REP_VENDOR_DATASET = "IFS.spec.ms.rep.vendor.dataset";
	public final static String MS_REP_SPECTRUM_PDF = "IFS.spec.ms.rep.spectrum.pdf";
	public final static String MS_REP_SPECTRUM_IMAGE = "IFS.spec.ms.rep.spectrum.image";
	public final static String MS_REP_SPECTRUM_DESCRIPTION = "IFS.spec.ms.rep.spectrum.description";
	public final static String MS_REP_PEAKLIST = "IFS.spec.ms.rep.peaklist";
	public final static String MS_REP_JCAMP = "IFS.spec.ms.rep.jcamp";

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

	public IFSMSSpecDataRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
