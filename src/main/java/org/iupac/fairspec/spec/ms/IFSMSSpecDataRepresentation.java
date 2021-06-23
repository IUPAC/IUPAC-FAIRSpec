package org.iupac.fairspec.spec.ms;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSMSSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IFS_REP_SPEC_MS_VENDOR_DATASET = "IFS.representation.spec.ms.vendor.dataset";
	public final static String IFS_REP_SPEC_MS_SPECTRUM_PDF = "IFS.representation.spec.ms.spectrum.pdf";
	public final static String IFS_REP_SPEC_MS_SPECTRUM_IMAGE = "IFS.representation.spec.ms.spectrum.image";
	public final static String IFS_REP_SPEC_MS_SPECTRUM_DESCRIPTION = "IFS.representation.spec.ms.spectrum.description";
	public final static String IFS_REP_SPEC_MS_PEAKLIST = "IFS.representation.spec.ms.peaklist";
	public final static String IFS_REP_SPEC_MS_JCAMP = "IFS.representation.spec.ms.jcamp";

	private final static String[] repNames = new String[] {
			IFS_REP_SPEC_MS_VENDOR_DATASET,
			IFS_REP_SPEC_MS_JCAMP,
			IFS_REP_SPEC_MS_SPECTRUM_PDF,
			IFS_REP_SPEC_MS_SPECTRUM_IMAGE,
			IFS_REP_SPEC_MS_SPECTRUM_DESCRIPTION,
			IFS_REP_SPEC_MS_PEAKLIST	
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSMSSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
