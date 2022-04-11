package org.iupac.fairdata.derived;

import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisCollection extends IFDCollection<IFDStructureDataAnalysis> {

	public IFDStructureDataAnalysisCollection(String label) {
		this(label, null);
	}
	public IFDStructureDataAnalysisCollection(String label, String type) {
		super(label, type);
	}

	public void addAnalysis(IFDStructureDataAnalysis a) {
		super.add(a);
	}

}