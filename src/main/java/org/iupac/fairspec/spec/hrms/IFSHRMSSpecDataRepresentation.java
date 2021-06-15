package org.iupac.fairspec.spec.hrms;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSHRMSSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String HRMS_REP_VENDOR_DATASET = "IFS.hrms.rep.vendor.dataset";
	public final static String HRMS_REP_SPECTRUM_PDF = "IFS.hrms.rep.spectrum.pdf";
	public final static String HRMS_REP_SPECTRUM_IMAGE = "IFS.hrms.rep.spectrum.image";
	public final static String HRMS_REP_SPECTRUM_DESCRIPTION = "IFS.hrms.rep.spectrum.description";
	
	private final static String[] repNames = new String[] {
			HRMS_REP_VENDOR_DATASET,
			HRMS_REP_SPECTRUM_PDF,
			HRMS_REP_SPECTRUM_IMAGE,
			HRMS_REP_SPECTRUM_DESCRIPTION
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSHRMSSpecDataRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
