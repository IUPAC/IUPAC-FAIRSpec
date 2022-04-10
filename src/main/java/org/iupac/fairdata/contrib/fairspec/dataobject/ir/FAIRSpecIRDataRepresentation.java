package org.iupac.fairdata.data.ir;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

public class IFDIRSpecDataRepresentation extends IFDDataObjectRepresentation {

	public final static String IFD_REP_SPEC_IR_VENDOR_DATASET = "IFD.representation.spec.ir.vendor.dataset";
	public final static String IFD_REP_SPEC_IR_SPECTRUM_PDF = "IFD.representation.spec.ir.spectrum.pdf";
	public final static String IFD_REP_SPEC_IR_SPECTRUM_IMAGE = "IFD.representation.spec.ir.spectrum.image";
	public final static String IFD_REP_SPEC_IR_SPECTRUM_DESCRIPTION = "IFD.representation.spec.ir.spectrum.description";
	public final static String IFD_REP_SPEC_IR_PEAKLIST = "IFD.representation.spec.ir.peaklist";
	public final static String IFD_REP_SPEC_IR_JCAMP = "IFD.representation.spec.ir.jcamp";

	private final static String[] repNames = new String[] {
			IFD_REP_SPEC_IR_VENDOR_DATASET,
			IFD_REP_SPEC_IR_JCAMP,
			IFD_REP_SPEC_IR_SPECTRUM_PDF,
			IFD_REP_SPEC_IR_SPECTRUM_IMAGE,
			IFD_REP_SPEC_IR_SPECTRUM_DESCRIPTION,
			IFD_REP_SPEC_IR_PEAKLIST		
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDIRSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
