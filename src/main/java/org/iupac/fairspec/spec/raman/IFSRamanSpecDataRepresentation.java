package org.iupac.fairspec.spec.raman;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSSpecDataRepresentation;

public class IFSRamanSpecDataRepresentation extends IFSSpecDataRepresentation {

	public final static String RAMAN_REP_VENDOR_DATASET = "raman.rep.vendor.dataset";
	public final static String RAMAN_REP_SPECTRUM_PDF = "raman.rep.spectrum.pdf";
	public final static String RAMAN_REP_SPECTRUM_IMAGE = "raman.rep.spectrum.image";
	public final static String RAMAN_REP_SPECTRUM_DESCRIPTION = "raman.rep.spectrum.description";
	public final static String RAMAN_REP_PEAKLIST = "raman.rep.peaklist";
	public final static String RAMAN_REP_JCAMP = "raman.rep.jcamp";

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

	public IFSRamanSpecDataRepresentation(String type, IFSReference ref, Object data) {
		super(type, ref, data);
	}

}
