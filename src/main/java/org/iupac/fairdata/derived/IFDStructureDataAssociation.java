package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;

/**
 * A class to correlation one or more IFDStructure with one or more
 * IFDDataObject. Only two array items are allowed -- one IFDStructureCollection
 * and one IFDDataObjectCollection. 
 * 
 * The structures array may be empty, in which
 * case the association is being made just among spectra based on compound associations
 * because (perhaps) there are no structures in the collection.
 * 
 * Each of these collections allows for one or more item, resulting in a
 * one-to-one, many-to-one, one-to-many, or many-many associations.
 * 
 * This class does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDStructureDataAssociation extends IFDAssociation {
	
	private final static int ITEM_STRUC = 0;
	private final static int ITEM_DATA = 1;
	
	private final static String sdaPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_STRUCTUREDATA_ASSOCIATION_FLAG);

	@Override
	protected String getPropertyPrefix() {
		return sdaPrefix;
	}

	@SuppressWarnings("unchecked")
	public IFDStructureDataAssociation() throws IFDException {
		super(null, new IFDCollection[] { new IFDStructureCollection(), new IFDDataObjectCollection() });		
	}
	
	@SuppressWarnings("unchecked")
	public IFDStructureDataAssociation(IFDStructureCollection structureCollection, IFDDataObjectCollection dataCollection) throws IFDException {
		super(null, new IFDCollection[] { structureCollection, dataCollection });		
	}
	
	@SuppressWarnings("unchecked")
	public IFDStructureDataAssociation(IFDStructure structure, IFDDataObject data) throws IFDException {
		super(null, new IFDCollection[] { new IFDStructureCollection(structure), new IFDDataObjectCollection(data) });
	}

	public IFDStructureCollection getStructureCollection() {
		// coerce IFDStructureCollection. I do not know why this does not work directly
		return (IFDStructureCollection) (Object) get(ITEM_STRUC);
	}

	public IFDDataObjectCollection getDataObjectCollection() {
		return (IFDDataObjectCollection) (Object) get(ITEM_DATA);
	}

	public IFDStructure getStructure(int i) {
		return (IFDStructure) getStructureCollection().get(i);
	}

	public IFDDataObject getDataObject(int i) {
		return (IFDDataObject) getDataObjectCollection().get(i);
	}

	public boolean addStructure(IFDStructure struc) {
		return getStructureCollection().add(struc);
	}

	public boolean addDataObject(IFDDataObject data) {
		return getDataObjectCollection().add(data);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDStructureDataAssociation))
			return false;
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) o;
		return (ss.get(ITEM_STRUC).equals(get(ITEM_STRUC)) && ss.get(ITEM_DATA).equals(get(ITEM_DATA)));
	}


}
