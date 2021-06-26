package org.iupac.fairspec.core;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSException;

@SuppressWarnings("serial")
public abstract class IFSDataObjectCollection<T extends IFSDataObject<?>> extends IFSCollection<T> {

	protected IFSDataObjectCollection(String name, ObjectType type) throws IFSException {
		super(name, type);
	}
	
	@Override
	public boolean add(T t) {
		if (t == null) {
			System.out.println("IFSObject error null");
			return false;
		}
		if (contains(t)) {
			return false;
		}
		if (subtype == ObjectType.Unknown)
			subtype = t.getType();
		else if (t.getType() != subtype)
			subtype = ObjectType.Mixed;
		return super.add(t); // true
	}
	
	private Map<String, T> map = new HashMap<>();

	public T getSpecDataFor(String path, String localName, String param, String value, String zipName, ObjectType type, String mediaType)  throws IFSException {
		String keyValue = path + "::" + zipName;
		T sd = map.get(keyValue);
		if (sd == null) {
 			map.put(keyValue,  sd = newIFSDataObject(path, param, value, type));
 			add(sd);
		} else {
			sd.setPropertyValue(param, value);
		}
		sd.getRepresentation(zipName, localName, true, param, mediaType);
		return sd;
	}

	/**
	 * subclasses are responsible for delivering their own new IFSDataObject
	 * 
	 * @param path
	 * @param param
	 * @param value
	 * @param type
	 * @return
	 * @throws IFSException 
	 */
	protected abstract T newIFSDataObject(String path, String param, String value, ObjectType type) throws IFSException;
	
}
