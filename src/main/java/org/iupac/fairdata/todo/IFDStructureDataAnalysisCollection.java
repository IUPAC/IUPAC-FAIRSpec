package org.iupac.fairdata.todo;

import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisCollection extends IFDCollection<IFDStructureDataAnalysis> {

	public IFDStructureDataAnalysisCollection(String name) {
		this(name, null);
	}
	public IFDStructureDataAnalysisCollection(String name, String type) {
		super(name, type);
	}

	public void addAnalysis(IFDStructureDataAnalysis a) {
		super.add(a);
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDStructureDataAnalysis.class };
	}



}