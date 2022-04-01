package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;

/**
 * A generic interface indicating some sort of data. Implemented here as
 * IFDDataObject, but potentially implemented for any sort of data object.
 * 
 * Allows for named representions. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDDataObject extends IFDRepresentableObject<IFDRepresentation> {

	{
		setProperties("IFD_PROP_DATA_OBJECT_", null); // currently no such values
	}
	

	@Override
	protected IFDDataObjectRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDDataObjectRepresentation(ref, obj, len, type, subtype);

	}	

	public IFDDataObject(String name, String type) {
		super(name, type);		
	}

	@Override
	public String toString() {
		return (name == null ? super.toString()
				: "[" + type + " " + index + " " + name + " " + (size() > 0 ? get(0) : null) + "]");
	}

	@Override
	protected void serializeProps(IFDSerializerI serializer) {
		super.serializeProps(serializer);
	}

	@Override
	public Object clone() {
		IFDDataObject data = new IFDDataObject(name, type);
		for (int i = 0; i < size(); i++)
			data.add(get(i));
		return data;
	}

}
