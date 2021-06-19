package org.iupac.fairspec.assoc;

import org.iupac.fairspec.api.IFSAbstractObjectI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.data.IFSDataObject;
import org.iupac.fairspec.data.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructure;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * An class to correlation one or more IFSStructure with one or more
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
public abstract class IFSStructureDataAssociation extends IFSObject<IFSObject<?>> implements IFSAbstractObjectI {
	
	public IFSStructureDataAssociation(String name, ObjectType type, IFSStructureCollection structureCollection, IFSDataObjectCollection<?> dataCollection) {
		super(name, type, 2, structureCollection, dataCollection);
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
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len) {
		return null;
	}

	
	protected void serializeList(IFSSerializerI serializer) {
		serializer.addObject("struc", getStructureCollection().getIndexList());
		serializer.addObject("data", getDataObjectCollection().getIndexList());
	}



}
