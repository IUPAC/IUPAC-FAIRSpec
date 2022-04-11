package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * A generic interface indicating some sort of data. Implemented here as
 * IFDDataObject, but potentially implemented for any sort of data object.
 * 
 * Allows for multiple named representations. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDDataObject extends IFDRepresentableObject<IFDDataObjectRepresentation> {

	{
		setProperties("IFD_PROP_DATAOBJECT", null); // These are loaded based on subtype
	}
	

	public IFDDataObject() {
		super(null, null);
	}

	@Override
	public Object clone() {
		IFDDataObject o = null;
		try {
			o = getClass().newInstance();
			o.setPath(rootPath);
			o.label = label;		
			o.type = type;
		} catch (Exception e) {
			// ignore
		}
		for (int i = 0; i < size(); i++)
			o.add(get(i));
		return o;
	}

	@Override
	public String toString() {
		return (label == null ? super.toString()
				: "[" + type + " " + (parentCollection != null) + " " + index + " id=" + id + " " + label + " " + (size() > 0 ? get(0) : null) + "]");
	}

}
