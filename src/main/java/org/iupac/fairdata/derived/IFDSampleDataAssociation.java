package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
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
 * This object is not representable itself.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleDataAssociation extends IFDAssociation {
	
	private final static int ITEM_SAMPLE = 0;
	private final static int ITEM_DATA = 1;
	
	private final static String sdaPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_SAMPLEDATA_ASSOCIATION_FLAG);

	@Override
	protected String getPropertyPrefix() {
		return sdaPrefix;
	}

	@SuppressWarnings("unchecked")
	public IFDSampleDataAssociation() throws IFDException {
		super(null, new IFDCollection[] { new IFDSampleCollection(), new IFDDataObjectCollection() });		
	}
	
	@SuppressWarnings("unchecked")
	public IFDSampleDataAssociation(String type, IFDSample sample, IFDDataObject data) throws IFDException {
		super(null, new IFDCollection[] { new IFDSampleCollection(sample), new IFDDataObjectCollection(data) });
	}


	@SuppressWarnings("unchecked")
	public IFDSampleDataAssociation(String type, IFDSampleCollection sampleCollection, IFDDataObjectCollection dataCollection) throws IFDException {
		super(type, new IFDCollection[] { sampleCollection, dataCollection });
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDSampleDataAssociation))
			return false;
		IFDSampleDataAssociation ss = (IFDSampleDataAssociation) o;
		return (ss.get(ITEM_SAMPLE).equals(get(ITEM_SAMPLE)) && ss.get(ITEM_DATA).equals(get(ITEM_DATA)));
	}

	public IFDSampleCollection getSampleCollection() {
		// for whatever reason, we have to coerce this 
		return (IFDSampleCollection) (Object) get(ITEM_SAMPLE);
	}

	public IFDDataObjectCollection getDataObjectCollection() {
		// for whatever reason, we have to coerce this 
		return (IFDDataObjectCollection) (Object) get(ITEM_DATA);
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

	public boolean addSample(IFDSample s) {
		return getSampleCollection().add(s);
	}

	public boolean addDataObject(IFDDataObject data) {
		return getDataObjectCollection().add(data);
	}



}
