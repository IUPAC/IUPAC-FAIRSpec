package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;

@SuppressWarnings("serial")
public class IFDDataObjectCollection extends IFDCollection<IFDObject<?>> {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDDataObject.class };
	}
	
	public IFDDataObjectCollection(String name, IFDDataObject data) {
		this(name);
		add(data);
	}

	public IFDDataObjectCollection(String name) {
		super(name, null);
	}

	public IFDDataObjectCollection(String name, String type) {
		super(name, type);
	}
	
	@Override
	public IFDDataObject get(int i) {
		return (IFDDataObject) super.get(i);
	}

	public boolean add(IFDDataObject sd) {
		if (contains(sd))
			return false;
		if (subtype == "Unknown")
			subtype = sd.getObjectType();
		else if (sd.getObjectType() != subtype)
			subtype = "Mixed";
		super.add(sd);
		return true;		
	}
	
	public IFDDataObject getDataObjectFor(String ifdPath, String path, String localName, String param, String value, String type, String mediaType)  throws IFDException {
		String keyValue = path + "::" + ifdPath;
		IFDDataObject sd = (IFDDataObject) map.get(keyValue);
		if (sd == null) {
			String ifdtype = IFDConst.getProp("IFD_OBJECT_TYPE_" + type.toUpperCase());
			if (ifdtype == null)
				ifdtype = IFDConst.getProp("IFD_OBJECT_TYPE_UNKNOWN") + "." + type.toUpperCase();
			sd = new IFDDataObject(null, ifdtype);
			sd.setPath(ifdPath);
			sd.setPropertyValue(param, value);
 			map.put(keyValue, sd);
 			add(sd);
		} else {
			sd.setPropertyValue(param, value);
		}
		sd.addRepresentation(ifdPath, localName, param, mediaType);
		return sd;
	}

	@Override
	public boolean add(IFDObject<?> t) {
		System.err.println("IFDObject error: " + t);
		return false;
	}

	/**
	 * Replace a data object with a cloned version that has a new ID.
	 * 
	 * @param data
	 * @param idExtension
	 * @return
	 */
	public IFDDataObject cloneData(IFDDataObject data, String newID) {
		IFDDataObject newData = (IFDDataObject) data.clone();
		newData.setID(newID);
		remove(data);
		add(newData);
		return newData;
	}

}
