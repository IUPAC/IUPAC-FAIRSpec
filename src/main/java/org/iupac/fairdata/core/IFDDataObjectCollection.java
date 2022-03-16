package org.iupac.fairdata.core;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDException;

@SuppressWarnings("serial")
public abstract class IFDDataObjectCollection<T extends IFDDataObject<?>> extends IFDCollection<T> {

	protected IFDDataObjectCollection(String name, String type) throws IFDException {
		super(name, type);
	}
	
	@Override
	public boolean add(T t) {
		if (t == null) {
			System.out.println("IFDObject error null");
			return false;
		}
		if (contains(t)) {
			return false;
		}
		if (subtype == ObjectType.Unknown)
			subtype = t.getObjectType();
		else if (t.getObjectType() != subtype)
			subtype = ObjectType.Mixed;
		return super.add(t); // true
	}
	
	private Map<String, T> map = new HashMap<>();

	public T getDataObjectFor(String ifdPath, String path, String localName, String param, String value, String type, String mediaType)  throws IFDException {
		String keyValue = path + "::" + ifdPath;
		T sd = map.get(keyValue);
		if (sd == null) {
 			map.put(keyValue,  sd = newIFDDataObject(path, param, value, type));
 			add(sd);
		} else {
			sd.setPropertyValue(param, value);
		}
		sd.addRepresentation(ifdPath, localName, param, mediaType);
		return sd;
	}

	/**
	 * subclasses are responsible for delivering their own new IFDDataObject
	 * 
	 * @param path
	 * @param param
	 * @param value
	 * @param type
	 * @return
	 * @throws IFDException 
	 */
	protected abstract T newIFDDataObject(String path, String param, String value, String type) throws IFDException;
	
}
