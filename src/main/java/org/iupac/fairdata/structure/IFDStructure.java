package org.iupac.fairdata.structure;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDStructure extends IFDRepresentableObject<IFDStructureRepresentation> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_STRUCTURE_FLAG);
	
	@Override
	protected String getIFDPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDStructure() {
		super(null, null);
		setProperties(propertyPrefix, null);
	}
	
	@Override
	protected IFDStructureRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		if (ref != null && ref.getLocalName() != null && (ref.getLocalName().indexOf("S1s") >= 0
				|| ref.getLocalName().indexOf("S1t") >= 0))
			System.out.println(this);
		
		
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
		return "[IFDStructure " + index + " label=" + label + " id=" + id + " refs=" + refs + "]";
	}
}
