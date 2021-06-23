package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;

@SuppressWarnings("serial")
public class IFSStructure extends IFSRepresentableObject<IFSStructureRepresentation> {

	public static final String IFS_PROP_STRUC_COMPOUND_ID  = "IFS.property.struc.compound.id";
	public static final String IFS_PROP_STRUC_SMILES       = "IFS.property.struc.smiles";
	public static final String IFS_PROP_STRUC_INCHI        = "IFS.property.struc.inchi";
	public static final String IFS_PROP_STRUC_INCHIKEY     = "IFS.property.struc.inchikey";

	public IFSStructure(String name) {
		super(name, ObjectType.Structure);
	}
	
	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_COMPOUND_ID),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_SMILES),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_INCHI),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_INCHIKEY),
		});
	}
	


	public IFSStructure(String path, String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Structure);
		setPath(path);
		if (param.equals(IFSStructure.IFS_PROP_STRUC_COMPOUND_ID))
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
