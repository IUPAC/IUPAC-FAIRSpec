package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.todo.IFDSampleDataAnalysisCollection;
import org.iupac.fairdata.todo.IFDSampleDataAssociationCollection;
import org.iupac.fairdata.todo.IFDStructureDataAnalysisCollection;
import org.iupac.fairdata.todo.IFDStructureDataAssociationCollection;

@SuppressWarnings("serial")
public abstract class IFDFAIRDataCollection extends IFDCollection<IFDCollection<IFDObject<?>>> {

	protected IFDFAIRDataCollection(String name) {
		super(name, null);
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

}