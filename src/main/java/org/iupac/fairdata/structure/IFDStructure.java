package org.iupac.fairdata.structure;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDStructure extends IFDRepresentableObject<IFDStructureRepresentation> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_STRUCTURE_FLAG);
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDStructure() {
		super(null, null);
		setProperties(propertyPrefix, null);
	}
	
	@Override
	protected IFDStructureRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDStructureRepresentation(ref, obj, len, type, subtype);
	}
	
	@Override
	public String toString() {
		if (label == null)
			return super.toString();
		String refs = "";
		for (int i = 0; i < size(); i++) {
			refs += get(i).getMediaType() + ";";
		}
		return "[IFDStructure " + index + " " + label + " " + refs + "]";
	}
}
