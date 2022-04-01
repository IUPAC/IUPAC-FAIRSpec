package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDSampleStructureAssociationCollection;
import org.iupac.fairdata.common.IFDException;

/**
 * A collection of IFDSampleAssociation objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleSpecCollection extends IFDSampleStructureAssociationCollection  {

	public IFDSampleSpecCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SampleSpecCollection);
	}
	
}