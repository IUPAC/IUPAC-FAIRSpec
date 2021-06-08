package org.iupac.fairspec.spec.ir;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSSpecDataRepresentation;

public class IFSIRSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IR_REP_VENDOR_DATASET = "ir.rep.vendor.dataset";
	public final static String IR_REP_SPECTRUM_PDF = "ir.rep.spectrum.pdf";
	public final static String IR_REP_SPECTRUM_IMAGE = "ir.rep.spectrum.image";
	public final static String IR_REP_SPECTRUM_DESCRIPTION = "ir.rep.spectrum.description";
	public final static String IR_REP_PEAKLIST = "ir.rep.peaklist";
	public final static String IR_REP_JCAMP = "ir.rep.jcamp";

	private final static String[] repNames = new String[] {
			IR_REP_VENDOR_DATASET,
			IR_REP_JCAMP,
			IR_REP_SPECTRUM_PDF,
			IR_REP_SPECTRUM_IMAGE,
			IR_REP_SPECTRUM_DESCRIPTION,
			IR_REP_PEAKLIST,		
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSIRSpecDataRepresentation(String type, IFSReference ref, Object data) {
		super(type, ref, data);
	}

}
