package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializerI;

@SuppressWarnings("serial")
public class IFDFAIRDataCollection extends IFDCollection<IFDCollection<IFDObject<?>>> {

	{
		setProperties("IFD_PROP_FAIRDATA_COLLECTION_", null);
	}


	protected IFDFAIRDataCollection(String name) {
		this(name, null);
	}

	protected IFDFAIRDataCollection(String name, String type) {
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
			serializer.addAttrInt(name + "Count", c.size());
			serializer.addObject(name, c);
		}
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDCollection.class };
	}

}