package org.iupac.fairdata.struc;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDStructure extends IFDRepresentableObject<IFDStructureRepresentation> {

	public static final String IFD_PROP_STRUC_COMPOUND_LABEL  = "IFD.property.struc.compound.label";
	public static final String IFD_PROP_STRUC_SMILES       = "IFD.property.struc.smiles";
	public static final String IFD_PROP_STRUC_INCHI        = "IFD.property.struc.inchi";
	public static final String IFD_PROP_STRUC_INCHIKEY     = "IFD.property.struc.inchikey";

	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDStructure.IFD_PROP_STRUC_COMPOUND_LABEL),
				new IFDProperty(IFDStructure.IFD_PROP_STRUC_SMILES),
				new IFDProperty(IFDStructure.IFD_PROP_STRUC_INCHI),
				new IFDProperty(IFDStructure.IFD_PROP_STRUC_INCHIKEY),
		});
	}	

	public IFDStructure(String name) throws IFDException {
		super(name, ObjectType.Structure);
	}
	
	public IFDStructure(String path, String param, String value) throws IFDException {
		super(param + ";" + value, ObjectType.Structure);
		setPath(path);
		if (param.equals(IFDStructure.IFD_PROP_STRUC_COMPOUND_LABEL) || IFDConst.isRepresentation(param))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFDStructureRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDStructureRepresentation(ref, obj, len, name, null);

	}
	
	@Override
	public String toString() {
		if (name == null)
			return super.toString();
		String refs = "";
		for (int i = 0; i < size(); i++) {
			refs += get(i).getSubtype() + ";";
		}
		return "[IFDStructure " + index + " " + name + " " + refs + "]";
	}

}
