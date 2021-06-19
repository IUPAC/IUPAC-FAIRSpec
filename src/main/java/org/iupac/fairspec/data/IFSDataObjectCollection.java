package org.iupac.fairspec.data;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSRepresentableObjectI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

@SuppressWarnings("serial")
public abstract class IFSDataObjectCollection<T extends IFSDataObject<?>> extends IFSCollection<T> {
	
	private boolean hasRepresentations = true;

	protected IFSDataObjectCollection(String name, ObjectType type) {
		super(name, type);
	}
	
	public boolean add(T t) {
		// not allowing for widely mixed types here.
		if (hasRepresentations && !(t instanceof IFSRepresentableObjectI))
				hasRepresentations = false;
		if (subtype == ObjectType.Unknown)
			subtype = t.getType();
		else if (t.getType() != subtype)
			subtype = ObjectType.Mixed;
		return super.add(t);
	}
	
	private Map<String, T> map = new HashMap<>();

	public T getSpecDataFor(String path, String localName, String param, String value, String objectFile, ObjectType type)  throws IFSException {
		String keyValue = path + "::" + objectFile;
		T sd = map.get(keyValue);
		if (sd == null) {
 			map.put(keyValue,  sd = newIFSDataObject(path, param, value, type));
 			add(sd);
		} else {
			sd.setPropertyValue(param, value);
		}
		sd.getRepresentation(objectFile, localName, true);
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
	 */
	protected abstract T newIFSDataObject(String path, String param, String value, ObjectType type);
	
	
	
}
