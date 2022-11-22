package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;

/**
 * A class representing an overall "collection of collections" IUPAC FAIRData
 * Collection, which is associated with an IUPAC FAIRData Finding Aid.
 * 
 * This class also maintains some of the key properties associated of a finding aid.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDCollectionSet extends IFDCollection<IFDCollection<IFDObject<?>>> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_COLLECTIONSET_FLAG);
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDCollectionSet(String label) {
		this(label, null);
	}

	public IFDCollectionSet(String label, String type) {
		super(label, type);
		setProperties(propertyPrefix, null);
	}

	/**
	 * Set all indices for IDFRepresentableObject collections to be sequential, and
	 * and set each object's collectionSet field to the top-level collection containing it
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected void finalizeCollections() {
		for (int ic = 0; ic < size(); ic++) {
			IFDCollection<?> c = get(ic);
			if (c == null || c.size() == 0)
				continue;
			if (c instanceof IFDAssociationCollection) {
					((IFDAssociationCollection) c).removeOrphanedAssociations();
			} else {
				for (int i = c.size(); --i >= 0;) {
					IFDRepresentableObject<?> o = (IFDRepresentableObject<?>) c.get(i);
					if (o.size() == 0) {
						c.remove(i);
						o.invalidate();
					}
				}
				for (int i = c.size(); --i >= 0;) {
					IFDRepresentableObject<?> o = (IFDRepresentableObject<?>) c.get(i);
					o.setIndex(i);
					// coerced to collection of IFDRepresentableObject
					o.setParentCollection(
							(IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>) (Object) c);
				}
			}
		}
	}
	
	@Override
	public void serializeList(IFDSerializerI serializer) {
		List<IFDCollection<?>> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<?> c = get(i);
			if (c == null || c.size() == 0)
				continue;
			list.add(c);
		}
		if (list.size() > 0)
			serializer.addList("items", list);
	}

	public void getContents(Map<String, Object> map) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<?> c = get(i);
			if (c.size() == 0)
				continue;
			Map<String, Object> m = new TreeMap<>();
			m.put("id", c.getID());
			getTypeAndExtends(c.getClass(), m);
			m.put("count", c.size());
			list.add(m);
		}		
		if (list.size() > 0)
			map.put("collections", list);
	}

}