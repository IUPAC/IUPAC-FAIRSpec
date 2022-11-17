package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;

/**
 * An class to handle generic collections of N:N associations. 
 * For example, Structure-Data associations
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
	public IFDAssociation getAssociationForSingleObj1(IFDRepresentableObject<? extends IFDRepresentation> obj1) {
		
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
	public IFDRepresentableObject<? extends IFDRepresentation> 
	getFirstObj1ForObj2(IFDRepresentableObject<? extends IFDRepresentation> obj2, boolean andRemove) {
		for (IFDAssociation a : this) {
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c = a.get(1);
			int i = c.indexOf(obj2);
			if (i >= 0) {
				if (andRemove)
					c.remove(i);
				return a.get(0).get(0);
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

	
	@Override
	protected void serializeTop(IFDSerializerI serializer) {
		if (size() == 0)
			return;
		super.serializeTop(serializer);
		IFDAssociation firstAssociation = get(0);
		int arity = firstAssociation.size();
		List<String> list = new ArrayList<>();
		// TODO should ensure these are all the same parent and that no null entries exist.
		for (int i = 0; i < arity; i++) {
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c = firstAssociation.get(i);
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> cp = (c == null || c.size() == 0 ? null : c.get(0).getParentCollection());
			if (cp == null) {
			  throw new NullPointerException("IFDAssociationCollection null or 0-length association");
			}
			list.add(cp.getID());
		}
		serializer.addAttrBoolean("byID", byID);
		serializer.addObject("collections", list);
	}

	public void removeOrphanedAssociations() {
		out: for (int ia = size(); --ia >= 0;) {
			IFDAssociation a = get(ia);
			int arity = a.size();
			for (int i = 0; i < arity; i++) {
				IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c = a.get(i);
				for (int j = c.size(); --j >= 0;) {
					if (c.get(j).getParentCollection() == null) {
						c.remove(j);
					}
				}
				if (c.size() == 0) {
					remove(ia);
					continue out;
				}
			}
		}
	}


	
}
