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
	private boolean byID = true; // making byID the default for IFDCollectionSet
	
	@Override
	protected String getIFDPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDCollectionSet(String label) {
		this(label, null);
	}

	public IFDCollectionSet(String label, String type) {
		super(label, type);
		setProperties(propertyPrefix, null);
	}
	
	public void setById(boolean b) {
		byID = b;
	}

	/**
	 * Set all indices for IDFRepresentableObject collections to be sequential, and
	 * and set each object's collectionSet field to the top-level collection
	 * containing it. Also add URI and DOI references if available 
	 * 
	 * @param htURLReferences  a JSON-derived map containing "cmpd", "file", and either "doi" or "url" links
	 */
	@SuppressWarnings("unchecked")
	protected void finalizeCollections(Map<String, Map<String, Object>> htURLReferences) {
		for (int ic = 0; ic < size(); ic++) {
			IFDCollection<?> c = get(ic);
			if (c == null || c.size() == 0)
				continue;
			if (c instanceof IFDAssociationCollection) {
				((IFDAssociationCollection) c).removeOrphanedAssociations();
			} else {
				// check representatableObjects
				for (int i = c.size(); --i >= 0;) {
					IFDRepresentableObject<?> o = (IFDRepresentableObject<?>) c.get(i);
					if (o.size() == 0 && !o.allowEmpty()) {
						System.out.println("IFDC removing " + i + " " + o + " from " + c);
						c.remove(i);
						o.setValid(false);
					}
				}
				// set index finally; set parent colletion; set URL/DOI from reference map
				for (int i = c.size(); --i >= 0;) {
					IFDRepresentableObject<?> o = (IFDRepresentableObject<?>) c.get(i);
					o.setIndex(i);
					// coerced to collection of IFDRepresentableObject
					o.setParentCollection(
							(IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>) (Object) c);
					if (htURLReferences != null) {
						o.setRepresentationDOIandURLs(htURLReferences);
					}
				}
			}
		}
	}
	
	@Override
	public void serializeTop(IFDSerializerI serializer) {
		super.serializeTop(serializer);
		if (byID)
			serializer.addAttrBoolean("byID", true);
	}

	@Override
	public String getID() {
		return null; // no need for this in finding aid
	}

	@Override
	public void serializeList(IFDSerializerI serializer) {
		IFDCollectionSet list = new IFDCollectionSet(null);
		for (int i = 0; i < size(); i++) {
			IFDCollection<IFDObject<?>> c = get(i);
			if (c == null || c.size() == 0)
				continue;
			list.add(c);
		}
		if (list.size() > 0)
			serializer.addCollection((byID ? "itemsByID" : "items"), list, byID);
	}

	public void getContents(Map<String, Object> map) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<?> c = get(i);
			if (c.size() == 0)
				continue;
			Map<String, Object> m = new TreeMap<>();
			m.put("id", c.getIDorIndex());
			getTypeAndExtends(c.getClass(), m);
			m.put("count", c.size());
			list.add(m);
		}		
		if (list.size() > 0)
			map.put("collections", list);
	}

}