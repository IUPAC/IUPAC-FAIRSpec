package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;

@SuppressWarnings({ "serial" })
public class IFDSampleDataAssociationCollection extends IFDAssociationCollection {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSampleCollection.class, IFDDataObjectCollection.class };
	}

	public IFDSampleDataAssociationCollection(String name, String type) {
		super(name, type);
	}

	public IFDSampleDataAssociationCollection(String name) {
		this(name, null);
	}

	public IFDSampleDataAssociation addAssociation(String name, IFDSample sample, IFDDataObject data) throws IFDException {
		IFDSampleDataAssociation ssc = (IFDSampleDataAssociation) getAssociationForSingleObj1(sample);
		if (ssc == null) {
			add(ssc = newAssociation(name, sample, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			if (sample.getName() == null)
				sample.setPropertyValue(IFDConst.getProp("IFD_PROP_SAMPLE_LABEL"), name);
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDSampleDataAssociation newAssociation(String name, IFDSample sample, IFDDataObject data) throws IFDException {
			return new IFDSampleDataAssociation(name, sample, data);
	}

}