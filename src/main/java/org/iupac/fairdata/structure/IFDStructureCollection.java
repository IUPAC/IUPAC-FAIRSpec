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
	
	@Override
	public boolean add(IFDStructure s) {
		return super.add(s);
	}

	public IFDStructure getStructureFromLocalName(String resourceID, String localName) {
		for (int i = 0; i < size(); i++) {
			IFDStructure struc = get(i);
			if (struc.getRepresentation(resourceID, localName) != null)
				return struc;
		}
		return null;
	}

	@Override
	protected boolean doSerializeItems() {
		return doTypeSerialization;
	}


}