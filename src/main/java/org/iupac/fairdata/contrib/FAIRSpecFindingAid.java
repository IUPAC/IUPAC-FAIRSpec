package org.iupac.fairdata.contrib;

import org.iupac.fairdata.core.IFDFindingAid;

/**
 * A class used by IFDFAIRSpecHelper to identify this as an IUPAC FAIRData
 * Finding Aid that is specialized for spectroscopic data.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecFindingAid extends IFDFindingAid {

	public FAIRSpecFindingAid(String name, String type, String creator) {
		super(name, type, creator);
	}

}
