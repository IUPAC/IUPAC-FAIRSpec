package org.iupac.fairspec.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.common.IFSRepresentation;

@SuppressWarnings("serial")
public abstract class IFSCollection<T extends IFSObject<?>> extends IFSAbstractObject<T> {

	protected IFSCollection(String name, ObjectType type) {
		super(name, type);
	}

	public List<Integer> getIndexList() {
		List<Integer> list = new ArrayList<>();
		for (T c : this) {
			list.add(c.getIndex());
		}
		return list;
	}
	
	private boolean hasRepresentations = false;

	@Override
	public boolean add(T t) {
		if (!hasRepresentations && (t instanceof IFSRepresentableObject))
			hasRepresentations = true;
		return super.add(t);
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param zipName
	 * @return
	 */
	public IFSRepresentation getRepresentation(String zipName) {
		if (!hasRepresentations)
			return null;
		for (T c : this) {
			if (!(c instanceof IFSRepresentableObject))
				return null;
			IFSRepresentation r = ((IFSRepresentableObject<?>)c).getRepresentation(zipName, null, false, null, null);
			if (r != null)
				return r;
		}
		return null;
	}

}