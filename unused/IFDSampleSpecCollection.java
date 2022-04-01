package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDSampleDataAssociation;
import org.iupac.fairdata.assoc.IFDSampleDataAssociationCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.sample.IFDSample;

/**
 * A collection of IFDSampleAssociation objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleSpecCollection extends IFDSampleDataAssociationCollection  {

	@Override
	public Class<?>[] getObjectTypes() {
		// TODO Auto-generated method stub
		return new Class<?>[] { IFDSample.class, IFDSpecData.class };
	}
	
	public IFDSampleSpecCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SampleSpecCollection, null);
	}

	@Override
	protected IFDSampleDataAssociation newAssociation(String name, IFDSample struc, IFDDataObject<?> data) {
		// TODO Auto-generated method stub
		return null;
	}

}