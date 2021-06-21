package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;

@SuppressWarnings("serial")
public class IFSStructure extends IFSRepresentableObject<IFSStructureRepresentation> {

	public IFSStructure(String name) {
		super(name, ObjectType.Structure);
	}
	
	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSConst.IFS_STRUCTURE_PROP_COMPOUND_ID),
				new IFSProperty(IFSConst.IFS_STRUCTURE_PROP_SMILES),
				new IFSProperty(IFSConst.IFS_STRUCTURE_PROP_INCHI),
				new IFSProperty(IFSConst.IFS_STRUCTURE_PROP_INCHIKEY),
		});
	}
	


	public IFSStructure(String path, String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Structure);
		setPath(path);
		if (param.equals(IFSConst.IFS_STRUCTURE_PROP_COMPOUND_ID))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFSStructureRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSStructureRepresentation(ref, obj, len, name, null);

	}
	
	@Override
	public String toString() {
		return (name == null ? super.toString() : "[IFSStructure " + index + " " + name + "]");
	}

}
