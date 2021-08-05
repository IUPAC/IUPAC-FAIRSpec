package org.iupac.fairspec.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSRepresentation;

@SuppressWarnings("serial")
public abstract class IFSCollection<T extends IFSObject<?>> extends IFSObject<T> {

	protected IFSCollection(String name, String type) throws IFSException {
		super(name, type);
	}

	@SafeVarargs
	public IFSCollection(String name, String type, int n, T... initialSet) throws IFSException {
		super(name, type, n, initialSet);
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
		if (t != null && contains(t))
			return false;
		if (!hasRepresentations && (t instanceof IFSRepresentableObject))
			hasRepresentations = true;
		return super.add(t);
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param ifsPath
	 * @return
	 */
	public IFSRepresentation getRepresentation(String ifsPath) {
		if (!hasRepresentations)
			return null;
		for (T c : this) {
			if (!(c instanceof IFSRepresentableObject))
				return null;
			IFSRepresentation r = ((IFSRepresentableObject<?>)c).getRepresentation(ifsPath);
			if (r != null)
				return r;
		}
		return null;
	}

}