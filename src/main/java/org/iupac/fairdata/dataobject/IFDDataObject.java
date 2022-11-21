package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * A generic class indicating some sort of data. 
 * 
 * Allows for multiple named representations. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDDataObject extends IFDRepresentableObject<IFDDataObjectRepresentation> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_DATAOBJECT_FLAG);

	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
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
			o.htProps.putAll(htProps);
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
				: "[" + type + " " + (parentCollection != null) + " " + index + " id=" + id + " label=" + label + " rep[0]=" + (size() > 0 ? get(0) : null) + "]");
	}

}
