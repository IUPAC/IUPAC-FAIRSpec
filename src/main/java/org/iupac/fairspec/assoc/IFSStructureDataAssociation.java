package org.iupac.fairspec.assoc;

import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructure;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * A class to correlation one or more IFSStructure with one or more
 * IFSDataObject. Only two array items are allowed -- one IFSStructureCollection
 * and one IFSDataObjectCollection.
 * 
 * Each of these collections allows for one or more item, resulting in a
 * one-to-one, many-to-one, one-to-many, or many-many associations.
 * 
 * An abstract object that does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSStructureDataAssociation extends IFSCollection<IFSCollection<?>> {
	
	public IFSStructureDataAssociation(String name, String type, IFSStructureCollection structureCollection, IFSDataObjectCollection<?> dataCollection) throws IFSException {
		super(name, type, 2, structureCollection, dataCollection);
		if (dataCollection == null || structureCollection == null)
			throw new IFSException("IFSSample constructure must provide IFSStructureCollection and IFSDataCollection");
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSStructureDataAssociation))
			return false;
		IFSStructureDataAssociation ss = (IFSStructureDataAssociation) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFSStructureCollection getStructureCollection() {
		return (IFSStructureCollection) get(0);
	}

	@SuppressWarnings("unchecked")
	public IFSDataObjectCollection<IFSDataObject<?>> getDataObjectCollection() {
		return (IFSDataObjectCollection<IFSDataObject<?>>) get(1);
	}

	public IFSStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	public IFSDataObject<?> getDataObject(int i) {
		return (IFSDataObject<?>) getDataObjectCollection().get(i);
	}

	public IFSStructure getFirstStructure() {
		return getStructureCollection().get(0);
	}

	public IFSDataObject<?> getFirstDataObject() {
		return (IFSDataObject<?>) getDataObjectCollection().get(0);
	}

	@Override
	protected void serializeList(IFSSerializerI serializer) {
		serializer.addObject("struc", getStructureCollection().getIndexList());
		serializer.addObject("data", getDataObjectCollection().getIndexList());
	}
	
	protected boolean addStructure(IFSStructure struc) {
		return getStructureCollection().add(struc);
	}

	protected boolean addDataObject(IFSDataObject<?> data) {
		return getDataObjectCollection().add(data);
	}



}
