package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDSampleAssociationCollection;
import org.iupac.fairdata.common.IFDException;

/**
 * A collection of IFDSampleAssociation objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleSpecCollection extends IFDSampleAssociationCollection  {

	public IFDSampleSpecCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SampleSpecCollection);
	}
	
}