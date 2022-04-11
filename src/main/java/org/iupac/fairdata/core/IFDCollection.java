package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;

@SuppressWarnings("serial")
public abstract class IFDCollection<T extends IFDObject<?>> extends IFDObject<T> {

	protected IFDCollection(String label, String type) {
		super(label, type);
	}

	@SafeVarargs
	public IFDCollection(String label, String type, T... initialSet) throws IFDException {
		super(label, type, initialSet.length, initialSet);
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
		//System.out.println("IFDCollection " + this + " adding " + t);
		return super.add(t);
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param originPath
	 * @return
	 */
	public IFDRepresentation getRepresentation(String originPath) {
		if (!hasRepresentations)
			return null;
		for (T c : this) {
			if (!(c instanceof IFDRepresentableObject))
				continue;
			IFDRepresentation r = ((IFDRepresentableObject<?>)c).getRepresentation(originPath);
			if (r != null)
				return r;
		}
		return null;
	}
	
	protected Map<String, T> map = new HashMap<>();

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
	protected void serializeList(IFDSerializerI serializer) {
		if (size() > 0) {
			serializer.addList("items", this);
		}
	}



}