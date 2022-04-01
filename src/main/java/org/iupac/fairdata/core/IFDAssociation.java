package org.iupac.fairdata.core;

import org.iupac.fairdata.common.IFDException;

/**
 * An abstract class to handle generic N:N associations. 
 * For example, Structure-Data associations
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public  class IFDAssociation extends IFDCollection<IFDCollection<IFDObject<?>>> {

	@Override
	public Class<?>[] getObjectTypes() {
		return types;
	}

	protected Class<?>[] types;

	protected IFDAssociation(String name, String type, IFDCollection<IFDObject<?>> collection1, IFDCollection<IFDObject<?>> collection2) throws IFDException {
		super(name, type, 2, collection1, collection2);
		if (collection1 == null || collection2 == null)
			throw new IFDException("IFDAnalysis both collections must be non-null.");
		types = new Class<?>[] { collection1.getClass(), collection2.getClass() };
	}

	/**
	 * Check to see if this is a 1:N association for obj1
	 * 
	 * @param obj1
	 * @return true if 1:N association for obj1
	 */
	public boolean associates1ToN(IFDObject<?> obj1) {
		IFDCollection<IFDObject<?>> c1;
		return (size() == 2 && (c1 = get(0)).size() == 1 && c1.get(0) == obj1);
	}

	/**
	 * Check to see if this is an association between obj1 and obj2
	 * @param obj1
	 * @param obj2
	 * @return true if an association is found.
	 */
	public boolean associates(IFDObject<?> obj1, IFDObject<?> obj2) {
		return (get(1).indexOf(obj2) >= 0 && get(0).indexOf(obj1) >= 0);
	}

	/**
	 * Check to see if the first obj2 is the given object. No check for N:1 is made.
	 * 
	 * @param obj2
	 * 
	 * @return true if that is the case
	 */
	public boolean associatesToFirstObj2(IFDObject<?> obj2) {
		return (getFirstObj2() == obj2);
	}

	public IFDObject<?> getFirstObj1() {
		return get(0).get(0);
	}

	public IFDObject<?> getFirstObj2() {
		return get(1).get(0);
	}


}
