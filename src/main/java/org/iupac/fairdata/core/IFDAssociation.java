package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;

/**
 * An class to handle generic N:N associations (for example, Structure-Data
 * associations). Objects of the collection must be representable, as they are
 * going to be referenced by collection type and index only.
 *
 * The collection size is fixed to the number of collections passed to it in its
 * constructor.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDAssociation extends IFDCollection<IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_ASSOCIATION_FLAG); 
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	@SafeVarargs
	protected IFDAssociation(String type, IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>... collections) throws IFDException {
		super(null, type, collections);
		for (int i = 0; i < collections.length; i++) {
			if (collections[i] == null)
				throw new IFDException("IFDAssociation collections must be non-null.");
		}
	}

	/**
	 * Check to see if this is a 1:N association for obj1
	 * 
	 * @param obj1
	 * @return true if 1:N association for obj1
	 */
	public boolean associates1ToN(IFDRepresentableObject<? extends IFDRepresentation> obj1) {
		IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c1;
		return (size() >= 2 && (c1 = get(0)).size() == 1 && c1.get(0) == obj1);
	}

	/**
	 * Check to see if this is an association between obj1 and obj2
	 * @param obj1
	 * @param obj2
	 * @return true if an association is found.
	 */
	public boolean associates(IFDObject<?> obj1, IFDObject<?> obj2) {
		return (size() >= 2 && get(1).indexOf(obj2) >= 0 && get(0).indexOf(obj1) >= 0);
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

	/**
	 * Get the first object of an indexed collection
	 * @param collectionIndex
	 * @return
	 * @throws IFDException if index is out of bounds
	 */
	public IFDObject<?> getFirstObj(int collectionIndex) throws IFDException {
		if (collectionIndex < 0 || collectionIndex >= size())
			throw new IFDException("IFDAssociation collectionIndex must be in the range 0 to " + (size() - 1));			
		return get(collectionIndex).get(0);
	}

	@Override
	public void serializeList(IFDSerializerI serializer) {
		if (size() == 0)
			return;
		// this class should serialize as a raw list of lists, without {....}
		List<List<Integer>> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c = getObject(i);
			list.add(c.getIndexList());
		}
		serializer.addList("items", list);
	}
	
}
