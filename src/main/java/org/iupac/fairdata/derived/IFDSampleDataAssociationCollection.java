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

	public IFDSampleDataAssociationCollection(String name) {
		super(name, "Samples", null);
	}

	public IFDSampleDataAssociation addAssociation(String name, IFDSample struc, IFDDataObject data) throws IFDException {
		IFDSampleDataAssociation ssc = (IFDSampleDataAssociation) getAssociationForSingleObj1(struc);
		if (ssc == null) {
			add(ssc = newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			if (struc.getName() == null)
				struc.setPropertyValue(IFDConst.getProp("IFD_PROP_SAMPLE_LABEL"), name);
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDSampleDataAssociation newAssociation(String name, IFDSample sample, IFDDataObject data) throws IFDException {
			return new IFDSampleDataAssociation(name, sample, data);
	}

}