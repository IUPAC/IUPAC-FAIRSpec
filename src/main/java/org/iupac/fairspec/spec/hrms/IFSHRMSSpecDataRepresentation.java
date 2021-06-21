package org.iupac.fairspec.spec.hrms;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSHRMSSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String HRMS_REP_VENDOR_DATASET = "IFS.hrms.representation.vendor.dataset";
	public final static String HRMS_REP_SPECTRUM_PDF = "IFS.hrms.representation.spectrum.pdf";
	public final static String HRMS_REP_SPECTRUM_IMAGE = "IFS.hrms.representation.spectrum.image";
	public final static String HRMS_REP_SPECTRUM_DESCRIPTION = "IFS.hrms.representation.spectrum.description";
	
	private final static String[] repNames = new String[] {
			HRMS_REP_VENDOR_DATASET,
			HRMS_REP_SPECTRUM_PDF,
			HRMS_REP_SPECTRUM_IMAGE,
			HRMS_REP_SPECTRUM_DESCRIPTION
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSHRMSSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
