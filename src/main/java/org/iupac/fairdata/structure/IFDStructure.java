package org.iupac.fairdata.structure;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDStructure extends IFDRepresentableObject<IFDStructureRepresentation> {

	{
		setProperties("IFD_PROP_STRUC", "IFD_PROP_STRUC_REP");
	}	
	
	public IFDStructure(String name) {
		super(name, null);
	}
	
	public IFDStructure(String path, String param, String value) {
		super(param + ";" + value, null);
		setPath(path);
		if (param.equals(IFDConst.IFD_PROP_SAMPLE_LABEL) || IFDConst.isRepresentation(param))
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