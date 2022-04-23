package org.iupac.fairdata.structure;

import org.iupac.fairdata.core.IFDCollection;

@SuppressWarnings("serial")
public class IFDStructureCollection extends IFDCollection<IFDStructure> {

	public IFDStructureCollection() {
		super(null, null);
	}
	
	public IFDStructureCollection(IFDStructure structure) {
		this();
		add(structure);
	}

}