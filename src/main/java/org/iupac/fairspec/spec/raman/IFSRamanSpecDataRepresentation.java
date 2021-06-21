package org.iupac.fairspec.spec.raman;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

public class IFSRamanSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String RAMAN_REP_VENDOR_DATASET = "raman.representation.vendor.dataset";
	public final static String RAMAN_REP_SPECTRUM_PDF = "raman.representation.spectrum.pdf";
	public final static String RAMAN_REP_SPECTRUM_IMAGE = "raman.representation.spectrum.image";
	public final static String RAMAN_REP_SPECTRUM_DESCRIPTION = "raman.representation.spectrum.description";
	public final static String RAMAN_REP_PEAKLIST = "raman.representation.peaklist";
	public final static String RAMAN_REP_JCAMP = "raman.representation.jcamp";

	private final static String[] repNames = new String[] {
			RAMAN_REP_VENDOR_DATASET,
			RAMAN_REP_JCAMP,
			RAMAN_REP_SPECTRUM_PDF,
			RAMAN_REP_SPECTRUM_IMAGE,
			RAMAN_REP_SPECTRUM_DESCRIPTION,
			RAMAN_REP_PEAKLIST,		
				};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSRamanSpecDataRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
