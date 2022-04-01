package org.iupac.fairdata.derived;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * A class to correlate one or more IFDSample with one or more
 * IFDDataObject. Only two array items are required -- one IFDSampleCollection
 * and one IFDDataObjectCollection.
 * 
 * Each of these collections allows for one or more item, resulting in a
 * one-to-one, many-to-one, one-to-many, or many-many associations.
 * 
 * An object that does not itself allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleDataAssociation extends IFDAssociation {
	
	private final static int ITEM_SAMPLE = 0;
	private final static int ITEM_DATA = 1;
	
	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] {
			IFDSampleCollection.class, IFDDataObjectCollection.class
		};
	}
	
	public IFDSampleDataAssociation(String name, IFDSample sample, IFDDataObject data) throws IFDException {
		super(name, null, new IFDSampleCollection("samples", sample), new IFDDataObjectCollection("data", data));
	}


	public IFDSampleDataAssociation(String name, String type, IFDSampleCollection sampleCollection, IFDCollection<IFDObject<?>> dataCollection) throws IFDException {
		super(name, type, sampleCollection, dataCollection);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDSampleDataAssociation))
			return false;
		IFDSampleDataAssociation ss = (IFDSampleDataAssociation) o;
		return (ss.get(ITEM_SAMPLE).equals(get(ITEM_SAMPLE)) && ss.get(ITEM_DATA).equals(get(ITEM_DATA)));
	}

	public IFDSampleCollection getSampleCollection() {
		return (IFDSampleCollection) get(ITEM_SAMPLE);
	}

	public IFDCollection<IFDObject<?>> getDataObjectCollection() {
		return (IFDCollection<IFDObject<?>>) get(ITEM_DATA);
	}

	public IFDSample getSample(int i) {
		return (IFDSample) getSampleCollection().get(i);
	}

	public IFDDataObject getDataObject(int i) {
		return (IFDDataObject) getDataObjectCollection().get(i);
	}

	public IFDSample getFirstSample() {
		return (IFDSample) getSampleCollection().get(0);
	}

	public IFDDataObject getFirstDataObject() {
		return (IFDDataObject) getDataObjectCollection().get(0);
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.addObject("obj1", getSampleCollection().getIndexList());
		serializer.addObject("obj2", getDataObjectCollection().getIndexList());
	}
	
	protected boolean addSample(IFDSample s) {
		return getSampleCollection().add(s);
	}

	protected boolean addDataObject(IFDDataObject data) {
		return getDataObjectCollection().add(data);
	}



}
