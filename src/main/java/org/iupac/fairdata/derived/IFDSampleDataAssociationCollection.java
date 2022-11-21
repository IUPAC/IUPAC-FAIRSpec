package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.sample.IFDSample;

@SuppressWarnings({ "serial" })
public class IFDSampleDataAssociationCollection extends IFDAssociationCollection {

	public IFDSampleDataAssociationCollection(boolean byID) {
		super(null, null, byID);
	}

	public IFDSampleDataAssociation addAssociation(IFDSample sample, IFDDataObject data) throws IFDException {
		IFDSampleDataAssociation sda = (IFDSampleDataAssociation) getAssociationForSingleObj1(sample);
		if (sda == null) {
			add(sda = newAssociation(null, sample, data));
		} else if (!sda.getDataObjectCollection().contains(data)) {
			sda.getDataObjectCollection().add(data);
		}
		sda.setByID(byID);
		return sda;
	}

	protected IFDSampleDataAssociation newAssociation(String label, IFDSample sample, IFDDataObject data) throws IFDException {
			return new IFDSampleDataAssociation(null, sample, data);
	}

	public IFDObject<?> addAssociation(IFDSampleDataAssociation a) {
		add(a);
		a.setByID(byID);
		return a;
	}

	
	@Override
	protected String getDefaultName(int i) {
		return (i == 0 ? "samples" : "data");
	}


}