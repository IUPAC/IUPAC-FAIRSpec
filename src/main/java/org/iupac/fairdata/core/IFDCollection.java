package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDRepresentation;

@SuppressWarnings("serial")
public abstract class IFDCollection<T extends IFDObject<?>> extends IFDObject<T> {

	protected IFDCollection(String name, String type) throws IFDException {
		super(name, type);
	}

	@SafeVarargs
	public IFDCollection(String name, String type, int n, T... initialSet) throws IFDException {
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
		if (!hasRepresentations && (t instanceof IFDRepresentableObject))
			hasRepresentations = true;
		return super.add(t);
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param ifdPath
	 * @return
	 */
	public IFDRepresentation getRepresentation(String ifdPath) {
		if (!hasRepresentations)
			return null;
		for (T c : this) {
			if (!(c instanceof IFDRepresentableObject))
				return null;
			IFDRepresentation r = ((IFDRepresentableObject<?>)c).getRepresentation(ifdPath);
			if (r != null)
				return r;
		}
		return null;
	}

}