package org.iupac.fairspec.spec.hrms;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSHRMSSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IFS_REP_SPEC_HRMS_VENDOR_DATASET = "IFS.representation.spec.hrms.vendor.dataset";
	public final static String IFS_REP_SPEC_HRMS_SPECTRUM_PDF = "IFS.representation.spec.hrms.spectrum.pdf";
	public final static String IFS_REP_SPEC_HRMS_SPECTRUM_IMAGE = "IFS.representation.spec.hrms.spectrum.image";
	public final static String IFS_REP_SPEC_HRMS_SPECTRUM_DESCRIPTION = "IFS.representation.spec.hrms.spectrum.description";
	
	private final static String[] repNames = new String[] {
			IFS_REP_SPEC_HRMS_VENDOR_DATASET,
			IFS_REP_SPEC_HRMS_SPECTRUM_PDF,
			IFS_REP_SPEC_HRMS_SPECTRUM_IMAGE,
			IFS_REP_SPEC_HRMS_SPECTRUM_DESCRIPTION
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSHRMSSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
