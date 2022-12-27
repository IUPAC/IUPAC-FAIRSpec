package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
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
public class IFDAssociation extends IFDCollection<IFDCollection<? extends IFDObject<?>>> implements Comparable<IFDAssociation> {

	protected boolean byID;

	int intID = -1;

	private List<String> typeList;

	/**
	 * From IFDAssociationCollection to pass on this information
	 * 
	 * @param b
	 */
	public void setByID(boolean b) {
		byID = b;
		checkIntID();
	}

		
	private void checkIntID() {
		if (byID && id != null && intID < 0) {
			intID = 0;
			for (int i = 0, n = id.length(); i < n; i++) {
				char c = id.charAt(i);
				if (c >= '0' && c <= '9') {
					intID = intID * 10 + ((int) (c - '0'));
				} else {
					intID = 0;
					break;
				}
			}
		}
	}


	@SafeVarargs
	protected IFDAssociation(String type, IFDCollection<IFDObject<?>>... collections) throws IFDException {
		super(null, type, collections);
		for (int i = 0; i < collections.length; i++) {
			if (collections[i] == null)
				throw new IFDException("IFDAssociation collections must be non-null.");
		}
	}

	@Override
	public void setID(String id) {
		super.setID(id);
		if (byID) {
			intID = -1;
			checkIntID();
		}
	}

	/**
	 * Check to see if this is a 1:N association for obj1
	 * 
	 * @param obj1
	 * @return true if 1:N association for obj1
	 */
	public boolean associates1ToN(IFDCollection<? extends IFDObject<?>> obj1) {
		IFDCollection<? extends IFDObject<?>> c1;
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
		return (get(0).size() > 0 ? get(0).get(0) : null);
	}

	public IFDObject<?> getFirstObj2() {
		return (get(1).size() > 0 ? get(1).get(0) : null);
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

	/**
	 * Set the type list ["structures", "spectra"] for serialization if byID.
	 * 
	 * @param list
	 */
	public void setTypeList(List<String> list) {
		typeList = list;
	}

	private Map<String, List<String>> getMyIDList() {
		Map<String, List<String>> list = new LinkedHashMap<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<? extends IFDObject<?>> c = getObject(i);
			list.put(typeList.get(i), c.getIDList());
		}
		return list;
	}
	
	private List<List<Integer>> getMyIndexList() {
		List<List<Integer>> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<? extends IFDObject<?>> c = getObject(i);
			list.add(c.getIndexList());
		}
		return list;
	}
	
	@Override
	public int compareTo(IFDAssociation o) {
		if (!byID || id == null || o.id == null)
			return Integer.compare(index, o.index);
		if (intID > 0 && o.intID > 0)
				return Integer.compare(intID, o.intID);
		return id.compareTo(o.getID());
	}

	@Override
	public void serializeList(IFDSerializerI serializer) {
		// this class should serialize as a raw list of lists, without {....}
		if (size() > 0)
			serializer.addObject("items", byID ? getMyIDList() : getMyIndexList());
	}
	
	@Override
	public String toString() {
		return super.toString().replace(']', ' ') + getMyIndexList() + " ]";
	}

}
