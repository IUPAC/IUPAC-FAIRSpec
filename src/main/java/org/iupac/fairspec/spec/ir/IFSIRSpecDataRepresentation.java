package org.iupac.fairspec.spec.ir;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSIRSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IFS_REP_SPEC_IR_VENDOR_DATASET = "IFS.representation.spec.ir.vendor.dataset";
	public final static String IFS_REP_SPEC_IR_SPECTRUM_PDF = "IFS.representation.spec.ir.spectrum.pdf";
	public final static String IFS_REP_SPEC_IR_SPECTRUM_IMAGE = "IFS.representation.spec.ir.spectrum.image";
	public final static String IFS_REP_SPEC_IR_SPECTRUM_DESCRIPTION = "IFS.representation.spec.ir.spectrum.description";
	public final static String IFS_REP_SPEC_IR_PEAKLIST = "IFS.representation.spec.ir.peaklist";
	public final static String IFS_REP_SPEC_IR_JCAMP = "IFS.representation.spec.ir.jcamp";

	private final static String[] repNames = new String[] {
			IFS_REP_SPEC_IR_VENDOR_DATASET,
			IFS_REP_SPEC_IR_JCAMP,
			IFS_REP_SPEC_IR_SPECTRUM_PDF,
			IFS_REP_SPEC_IR_SPECTRUM_IMAGE,
			IFS_REP_SPEC_IR_SPECTRUM_DESCRIPTION,
			IFS_REP_SPEC_IR_PEAKLIST		
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSIRSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
