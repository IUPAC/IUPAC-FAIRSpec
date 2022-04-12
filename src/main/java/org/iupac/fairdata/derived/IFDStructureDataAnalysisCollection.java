package org.iupac.fairdata.derived;

import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisCollection extends IFDCollection<IFDStructureDataAnalysis> {

	public IFDStructureDataAnalysisCollection() {
		super(null, null);
	}

	public void addAnalysis(IFDStructureDataAnalysis a) {
		super.add(a);
	}

}