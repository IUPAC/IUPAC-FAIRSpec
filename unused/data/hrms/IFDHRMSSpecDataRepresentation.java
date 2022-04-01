package org.iupac.fairdata.data.hrms;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

public class IFDHRMSSpecDataRepresentation extends IFDDataObjectRepresentation {

	public final static String IFD_REP_SPEC_HRMS_VENDOR_DATASET = "IFD.representation.spec.hrms.vendor.dataset";
	public final static String IFD_REP_SPEC_HRMS_SPECTRUM_PDF = "IFD.representation.spec.hrms.spectrum.pdf";
	public final static String IFD_REP_SPEC_HRMS_SPECTRUM_IMAGE = "IFD.representation.spec.hrms.spectrum.image";
	public final static String IFD_REP_SPEC_HRMS_SPECTRUM_DESCRIPTION = "IFD.representation.spec.hrms.spectrum.description";
	
	private final static String[] repNames = new String[] {
			IFD_REP_SPEC_HRMS_VENDOR_DATASET,
			IFD_REP_SPEC_HRMS_SPECTRUM_PDF,
			IFD_REP_SPEC_HRMS_SPECTRUM_IMAGE,
			IFD_REP_SPEC_HRMS_SPECTRUM_DESCRIPTION
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDHRMSSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
