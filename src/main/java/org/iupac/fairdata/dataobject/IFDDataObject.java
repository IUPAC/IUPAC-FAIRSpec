package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDProperty;
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

	/**
	 * replaced and to be ignored, but does not need to be reported (MNOVA file)
	 */
	private boolean replaced;

	@Override
	protected String getIFDPropertyPrefix() {
		return propertyPrefix;
	}
	
	public IFDDataObject() {
		super(null, null);
	}

	/** This adds the IFD.dataObject properties for a any subclass.
	 * 
	 */
	@Override
	protected void setProperties(String subclassPropertyPrefix, String notKey) {
		super.setProperties(propertyPrefix, null);
		super.setProperties(subclassPropertyPrefix, null);
	}

	@Override
	public IFDDataObject clone() {
		IFDDataObject o = null;
		try {
			o = (IFDDataObject) super.clone();//getClass().newInstance();
		} catch (Exception e) {
			// ignore
		}
		return o;
	}

	 @Override
	public String getObjectFlag() {
		return IFDConst.IFD_DATAOBJECT_FLAG;
	};
	
	/**
	 * Turn IFD.dataobject.originating_sample_id into
	 * IFD.dataobject.fairspec.nmr.originating_sample_id
	 */
	@Override
	public IFDProperty setPropertyValue(String key, Object value) {
		IFDProperty p = IFDConst.getIFDProperty(htProps, key);
		if (p == null) {
			String prefix = getIFDPropertyPrefix();
			if (key.startsWith(IFDConst.IFD_PROPERTY_DATAOBJECT_FLAG) && !key.startsWith(prefix)) {
				key = prefix + key.substring(key.lastIndexOf("."));
			}
		}
		return super.setPropertyValue(key, value);
	}


	@Override
	public String toString() {
		return (label == null ? super.toString()
				: "[" + type + " " + (parentCollection != null) + " " + index + " id=" + id + " label=" + label + " rep[0]=" + (size() > 0 ? get(0) : null) + "]");
	}

	public void setReplaced() {
		replaced = true;
		setValid(false);
	}
	
	public boolean isReplaced() {
		return replaced;
	}

}
