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
	 * Set all indices for non-associations
	 * 
	 */
	protected void finalizeCollections() {
		for (int ic = 0; ic < size(); ic++) {
			IFDCollection<IFDObject<?>> c = get(ic);
			if (c == null || c.size() == 0 || c.get(0) instanceof IFDAssociation)
				continue;
			for (int i = c.size(); --i >= 0;)
				((IFDObject<?>) c.get(i)).setIndex(i);
		}
	}
	
	@Override
	public void serialize(IFDSerializerI serializer) {
		// serialize the associations last
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < size(); i++) {
				IFDCollection<IFDObject<?>> c = get(i);
				if (c == null || c.size() == 0)
					continue;
				if ((c.get(0) instanceof IFDAssociation) == (pass == 1)) {
					serializer.addObject(c.getName(), c);
				}
			}
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
			m.put("name", c.getName());
			m.put("type", c.getClass().getName());
			m.put("size", c.size());
			lst.add(m);
		}
		map.put("collections", lst);
	}

}