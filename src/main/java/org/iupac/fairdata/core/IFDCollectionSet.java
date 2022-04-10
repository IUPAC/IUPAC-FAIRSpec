package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDSerializerI;

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

	{
		setProperties("IFD_PROP_COLLECTIONSET_", null);
	}

	public IFDCollectionSet(String name) {
		this(name, null);
	}

	public IFDCollectionSet(String name, String type) {
		super(name, type);
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
					o.setIndex(i);
					// coerced to collection of IFDRepresentableObject
					o.setParentCollection(
							(IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>) (Object) c);
				}
			}
		}
	}
	
	@Override
	public void serialize(IFDSerializerI serializer) {
		// two passes so that we serialize the associations last
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < size(); i++) {
				IFDCollection<IFDObject<?>> c = get(i);
				if (c == null)
					continue;
				if ((c.size() > 0 && c.get(0) instanceof IFDAssociation) == (pass == 1)) {
					if (pass == 1 || c.size() > 0)
					serializer.addObject(c.getName(), c);
				}
			}
		}
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDCollection.class };
	}

	public void getContents(Map<String, Object> map) {
		List<Map<String, Object>> lst = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<?> c = get(i);
			if (c.size() == 0)
				continue;
			Map<String, Object> m = new TreeMap<>();
			m.put("name", c.getName());
			IFDObject.addTypes(c.getClass(), m);
			m.put("count", c.size());
			lst.add(m);
		}		
		map.put("collections", lst);
	}

}