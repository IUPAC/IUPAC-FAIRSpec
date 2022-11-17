package org.iupac.fairdata.derived;

import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisCollection extends IFDCollection<IFDStructureDataAnalysis> {

	private boolean byID;

	public IFDStructureDataAnalysisCollection(boolean byID) {
		super(null, null);
		this.byID = byID;
	}

	public void addAnalysis(IFDStructureDataAnalysis a) {
		super.add(a);
		a.setByID(byID);
	}

}