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
 * It maintains the key properties associated with a finding aid.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDFAIRDataCollection extends IFDCollection<IFDCollection<IFDObject<?>>> {

	{
		setProperties("IFD_PROP_FAIRDATA_COLLECTION_", null);
	}


	public IFDFAIRDataCollection(String name) {
		this(name, null);
	}

	public IFDFAIRDataCollection(String name, String type) {
		super(name, type);
	}

	/**
	 * 
	 * @param serializer
	 */
	protected void finalizeCollections(IFDSerializerI serializer) {
		for (int i = 0; i < size(); i++) {
			IFDCollection<IFDObject<?>> c = get(i);
			finalizeCollection(serializer, c.getName(), c);
		}
	}

	/**
	 * Reset all indices and optionally serialize this collection.
	 * 
	 * @param serializer
	 * @param name
	 * @param c
	 */
	private void finalizeCollection(IFDSerializerI serializer, String name, IFDCollection<?> c) {
		if (c == null || c.size() == 0)
			return;
		if (serializer == null) {
			// normalize indices
			for (int i = c.size(); --i >= 0;)
				((IFDObject<?>) c.get(i)).setIndex(i);
		} else {
			serializer.addObject(name, c);
		}
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDCollection.class };
	}

	public void getStatistics(Map<String, Object> map) {
		List<Map<String, Object>> lst = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			IFDCollection<?> c = get(i);
			if (c.size() == 0)
				continue;
			Map<String, Object> m = new TreeMap<>();
			m.put("collectionName", c.getName());
			m.put("collectionType", c.getClass().getName());
			m.put("collectionSize",  c.size());
			lst.add(m);
		}
		map.put("collections", lst);
	}

}