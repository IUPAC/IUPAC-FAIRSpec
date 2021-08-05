package org.iupac.fairspec.struc;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.core.IFSRepresentableObject;

@SuppressWarnings("serial")
public class IFSStructure extends IFSRepresentableObject<IFSStructureRepresentation> {

	public static final String IFS_PROP_STRUC_COMPOUND_ID  = "IFS.property.struc.compound.id";
	public static final String IFS_PROP_STRUC_SMILES       = "IFS.property.struc.smiles";
	public static final String IFS_PROP_STRUC_INCHI        = "IFS.property.struc.inchi";
	public static final String IFS_PROP_STRUC_INCHIKEY     = "IFS.property.struc.inchikey";

	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_COMPOUND_ID),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_SMILES),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_INCHI),
				new IFSProperty(IFSStructure.IFS_PROP_STRUC_INCHIKEY),
		});
	}	

	public IFSStructure(String name) throws IFSException {
		super(name, ObjectType.Structure);
	}
	
	public IFSStructure(String path, String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Structure);
		setPath(path);
		if (param.equals(IFSStructure.IFS_PROP_STRUC_COMPOUND_ID) || IFSConst.isRepresentation(param))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFSStructureRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSStructureRepresentation(ref, obj, len, name, null);

	}
	
	@Override
	public String toString() {
		if (name == null)
			return super.toString();
		String refs = "";
		for (int i = 0; i < size(); i++) {
			refs += get(i).getSubtype() + ";";
		}
		return "[IFSStructure " + index + " " + name + " " + refs + "]";
	}

}
