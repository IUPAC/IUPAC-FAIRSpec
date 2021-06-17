package org.iupac.fairspec.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.api.IFSAbstractObjectI;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

@SuppressWarnings("serial")
public abstract class IFSCollection<T extends IFSObject<?>> extends IFSObject<T> implements IFSAbstractObjectI {

	protected IFSCollection(String name, ObjectType type) {
		super(name, type);
	}

	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len) {
		return null;
	}
	
	public List<Integer> getIndexList() {
		List<Integer> list = new ArrayList<>();
		for (T c : this) {
			list.add(c.getIndex());
		}
		return list;
	}

	/**
	 * Find a representation in one of the items of a collection
	 * @param zipName
	 * @return
	 */
	public IFSRepresentation getRepresentation(String zipName) {
		for (T c : this) {
			IFSRepresentation r = c.getRepresentation(zipName, null, false);
			if (r != null)
				return r;
		}
		return null;
	}


	





}