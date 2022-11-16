package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.sample.IFDSample;

@SuppressWarnings({ "serial" })
public class IFDSampleDataAssociationCollection extends IFDAssociationCollection {

	public IFDSampleDataAssociationCollection() {
		super(null, null);
	}

	public IFDSampleDataAssociation addAssociation(IFDSample sample, IFDDataObject data) throws IFDException {
		IFDSampleDataAssociation ssc = (IFDSampleDataAssociation) getAssociationForSingleObj1(sample);
		if (ssc == null) {
			add(ssc = newAssociation(null, sample, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDSampleDataAssociation newAssociation(String label, IFDSample sample, IFDDataObject data) throws IFDException {
			return new IFDSampleDataAssociation(null, sample, data);
	}

	public IFDObject<?> addAssociation(IFDSampleDataAssociation a) {
		add(a);
		return a;
	}

}