package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;

/**
 * An class to handle generic collections of N:N associations. 
 * For example, Structure-Data associations.
 * 
 * Serialization and construction can be
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDAssociationCollection extends IFDCollection<IFDAssociation> {

	protected boolean byID;
	
	protected IFDAssociationCollection(String label, String type, boolean byID) {
		super(label, type);
		this.byID = byID;
	}
	
	/**
	 * Find the 1:N association in this collection that involves 
	 * a given obj1. For example, in SampleDataAssociations, where 
	 * we have one sample but multiple data objects, we know there 
	 * can be only one sample from which all these data objects derive.
	 * 
	 * @param obj1
	 * @return the found association or null
	 */
	public IFDAssociation getAssociationForSingleObj1(IFDObject<?> obj1) {

		for (IFDAssociation a : this) {
			if (a.associates1ToN(obj1))
				return a;
		}
		return null;
	}
	
	@Override
	public boolean add(IFDAssociation a) {
		a.setByID(byID);
		return super.add(a);
	}

	/**
	 * Look for an association containing obj2, optionally remove obj2 from that association, 
	 * and return the first obj1 associated with it.
	 * @param obj2
	 * @param andRemove
	 * @return obj1
	 */
	@SuppressWarnings("unchecked")
	public IFDObject<?> getFirstObj1ForObj2(IFDObject<?> obj2, boolean andRemove) {
		for (IFDAssociation a : this) {
			IFDCollection<? extends IFDObject<?>> c = a.get(1);
			int i = c.indexOf(obj2);
			if (i >= 0) {
				if (andRemove)
					c.remove(i);
				return a.getFirstObj1();
			}
		}
		return null;
	}

	public IFDAssociation getAssociationForSingleObj2(IFDObject<?> obj2) {
		for (IFDAssociation a : this) {
			if (a.associatesToFirstObj2(obj2))
				return a;
		}

		return null;
	}

	public IFDAssociation findAssociation(IFDObject<?> obj1, IFDObject<?> obj2) {
		for (IFDAssociation a : this) {
			if (a.associates(obj1, obj2))
				return a;
		}
		return null;
	}

	protected String getDefaultName(int i) {
		return null;
	}

	public void removeOrphanedAssociations() {
		for (int ia = size(); --ia >= 0;) {
			IFDAssociation a = get(ia);
			int arity = a.size();
			int nEmpty = 0;
			for (int i = 0; i < arity; i++) {
				IFDCollection<? extends IFDObject<?>> c = a.get(i);
				for (int j = c.size(); --j >= 0;) {
					if (c.get(j).getParentCollection() == null) {
						c.remove(j);
					}
				}
				if (c.size() == 0) {
					nEmpty++;
				}
			}
			if (nEmpty == arity)
				remove(ia);
		}
	}

	
	@Override
	protected void serializeTop(IFDSerializerI serializer) {
		if (size() == 0)
			return;
		super.serializeTop(serializer);
		IFDAssociation firstAssociation = get(0);
		int arity = firstAssociation.size();
		List<String> list = new ArrayList<>();
		// TODO should ensure these are all the same parent and that no null entries
		// exist.
		for (int i = 0; i < arity; i++) {
			IFDCollection<? extends IFDObject> c = firstAssociation.get(i);
			if (c.size() == 0) {
				String name = null;
				for (int n = size(); --n >= 0;) {
					c = get(n).get(i);					
					String id = (c.isEmpty() ? null : c.get(0).getParentCollection().getID());
					if (id != null) {
						name = id;
						break;
					}
				}
				list.add(name == null ? getDefaultName(i) : name);
			} else {
				IFDCollection<?> cp = (c == null ? null
						: c.get(0).getParentCollection());
				if (cp == null) {
					throw new NullPointerException("IFDAssociationCollection null or 0-length association");
				}
				list.add(cp.getID());
			}
		}
		if (byID) {
			for (int i = size(); --i >= 0;)
				get(i).setTypeList(list);
		} else {
			serializer.addObject("collections", list);
		}
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		if (size() == 0)
			return;
		if (byID) {
			Collections.sort(this);
		}
		super.serializeList(serializer);
	}
	
}
