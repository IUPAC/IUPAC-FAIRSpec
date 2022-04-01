package org.iupac.fairdata.todo;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;

/**
 * A class to correlation one or more IFDStructure with one or more
 * IFDDataObject. Only two array items are allowed -- one IFDStructureCollection
 * and one IFDDataObjectCollection.
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
public class IFDStructureDataAssociation extends IFDAssociation {
	
	private final static int ITEM_STRUC = 0;
	private final static int ITEM_DATA = 1;
	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] {
			IFDStructureCollection.class, IFDDataObjectCollection.class
		};
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IFDStructureDataAssociation(String name, String type
			, IFDStructureCollection structureCollection
			, IFDDataObjectCollection dataCollection) throws IFDException {
		super(name, type, structureCollection, (IFDCollection<IFDObject<?>>) dataCollection);
		if (dataCollection == null || structureCollection == null)
			throw new IFDException("IFDSample constructure must provide IFDStructureCollection and IFDDataCollection");
	}
	
	public IFDStructureDataAssociation(String name, IFDStructure structure, IFDDataObject data) {
		super(name, null, new IFDStructureCollection("structures", structure), new IFDDataObjectCollection("data", data));
	}

	public IFDStructureDataAssociation(String name, IFDStructureCollection structureCollection, IFDDataObjectCollection dataCollection) {
		super(name, null, structureCollection, dataCollection);		
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDStructureDataAssociation))
			return false;
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) o;
		return (ss.get(ITEM_STRUC).equals(get(ITEM_STRUC)) && ss.get(ITEM_DATA).equals(get(ITEM_DATA)));
	}

	public IFDStructureCollection getStructureCollection() {
		return (IFDStructureCollection) get(ITEM_STRUC);
	}

	public IFDCollection<IFDObject<?>> getDataObjectCollection() {
		return (IFDCollection<IFDObject<?>>) get(ITEM_DATA);
	}

	public IFDStructure getStructure(int i) {
		return (IFDStructure) getStructureCollection().get(i);
	}

	public IFDDataObject getDataObject(int i) {
		return (IFDDataObject) getDataObjectCollection().get(i);
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.addObject("obj1", getStructureCollection().getIndexList());
		serializer.addObject("obj2", getDataObjectCollection().getIndexList());
	}
	
	protected boolean addStructure(IFDStructure struc) {
		return getStructureCollection().add(struc);
	}

	protected boolean addDataObject(IFDDataObject data) {
		return getDataObjectCollection().add(data);
	}
	

}
