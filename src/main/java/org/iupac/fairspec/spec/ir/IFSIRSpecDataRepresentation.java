package org.iupac.fairspec.spec.ir;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSIRSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String IR_REP_VENDOR_DATASET = "IFS.ir.rep.vendor.dataset";
	public final static String IR_REP_SPECTRUM_PDF = "IFS.ir.rep.spectrum.pdf";
	public final static String IR_REP_SPECTRUM_IMAGE = "IFS.ir.rep.spectrum.image";
	public final static String IR_REP_SPECTRUM_DESCRIPTION = "IFS.ir.rep.spectrum.description";
	public final static String IR_REP_PEAKLIST = "IFS.ir.rep.peaklist";
	public final static String IR_REP_JCAMP = "IFS.ir.rep.jcamp";

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

	public IFSIRSpecDataRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
