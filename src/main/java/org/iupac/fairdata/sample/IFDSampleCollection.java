package org.iupac.fairdata.sample;

import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * A collection of IFDSample objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleCollection extends IFDCollection<IFDRepresentableObject<IFDSampleRepresentation>> {

	public IFDSampleCollection() {
		super(null, null);
	}
	
	public IFDSampleCollection(IFDSample sample) {
		this();
		add(sample);
	}
	
	

}