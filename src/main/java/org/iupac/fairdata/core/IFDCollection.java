package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;

@SuppressWarnings("serial")
public abstract class IFDCollection<T extends IFDObject<?>> extends IFDObject<T> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_COLLECTION_FLAG);
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	protected IFDCollection(String label, String type) {
		super(label, type);
	}

	@SafeVarargs
	public IFDCollection(String label, String type, T... initialSet) throws IFDException {
		super(label, type, initialSet.length, initialSet);
	}

	public List<String> getIDList() {
		List<String> list = new ArrayList<>();
		for (T o : this) {
			String id = o.getID();
			list.add(id == null ? "" + o.getIndex() : id);
		}
		return list;
	}

	public List<Integer> getIndexList() {
		List<Integer> list = new ArrayList<>();
		for (T c : this) {
			list.add(c.getIndex());
		}
		return list;
	}
	
	private boolean hasRepresentations = false;

	/**
	 * Does not allow duplicates.
	 */
	@Override
	public boolean add(T t) {
		if (t == null || contains(t))
			return false;
		if (!hasRepresentations && (t instanceof IFDRepresentableObject))
			hasRepresentations = true;
		return super.add(t);
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param localizedPath
	 * @return
	 */
	public IFDRepresentation getRepresentation(String resourceID, String localizedPath) {
		if (!hasRepresentations)
			return null;
		for (T c : this) {
			if (!(c instanceof IFDRepresentableObject))
				continue;
			IFDRepresentation r = ((IFDRepresentableObject<?>)c).getRepresentation(resourceID, localizedPath);
			if (r != null)
				return r;
		}
		return null;
	}
	
	protected Map<String, T> map = new HashMap<>();
	private boolean haveCommonClass;

	public T getPath(String path) {
		return map.get(path);
	}
	
	/**
	 * Add the object, checking to see that the indicated path has not been
	 * registered and that the object is not already present.
	 * 
	 * @param path
	 * @param sd
	 * @return sd or null if not added
	 */
	public T addWithPath(String path, T sd) {
		if (path != null && map.get(path) != null) {
			return null;
		}
		map.put(path,  sd);
		return (add(sd) ? sd : null);
	}
	
	public void replaceObject(T data, T newData) {
		remove(data);
		add(newData);
	}

	public T getObjectByLabel(String label) {
		for (int i = size(); --i >= 0;) {
			T o = get(i);
			if (label.equals(o.getLabel())) 
				return o;
		}
		return null;
	}

	@Override
	protected void serializeTop(IFDSerializerI serializer) {
		haveCommonClass = false;
		super.serializeTop(serializer);
		Class<?> commonClass = null;
		for (int i = size(); --i >= 0;) {
			Class<?> c = get(i).getClass();
			if (commonClass == null)
				commonClass = c;
			if (c != commonClass) {
				commonClass = null;
				break;
			}
		}
		if (commonClass != null) {
			haveCommonClass = true;
			serializeClass(serializer, commonClass, "itemType");
			for (int i = size(); --i >= 0;) {
				get(i).setSerializeType(false);
			}
		}
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		if (size() > 0) {
			boolean byid = serializer.isByID();
			int i;
			for (i = size(); --i >= 0;) {
				if (get(i).getID() == null) {
					break;
				}
			}
			if (i >= 0) {
				for (i = size(); --i >= 0;) {
					get(i).setID("" + (i + 1));
				}
			}
			
			serializer.addCollection(byid ? "itemsByID" : "items", this, byid);
		}
		if (haveCommonClass) {
			for (int i = size(); --i >= 0;) {
				get(i).setSerializeType(true);
			}
		}
	}

	public T getObjectByID(String value) {
		for (int i = size(); --i >= 0;) {
			T v = get(i);
			if (value.equals(v.getID()))
					return v;
		}
		return null;
	}

	public void removeInvalidData() {
		for (int i = size(); --i >=0;) {
			IFDObject<?> o = get(i);
			if (!o.isValid())
				remove(i);
		}
	}

}