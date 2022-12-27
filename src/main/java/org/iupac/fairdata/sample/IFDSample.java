package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;

/**
 * An IFDSample represents a physical sample that optionally has one or the
 * other or both of associated IFDStructureCollection and
 * IFDDataObjectCollection. It may also have a representation.
 * 
 * It is expected to hold numerous IFDProperty values that describe the sample.
 * 
 * Multiple IFDStructures would indicate a mixture or, perhaps, an ambiguity
 * (which would be distinguished by its properties or subclass) Multiple
 * IFDDataObjects indicate multiple experiments all relating to this particular
 * sample.
 * 
 * Since IFDSample can exist with no structures and/or no data and/or no
 * representations, it can be considered a starting "holder class" that can be
 * added to during the process of sample analysis, before any structure is known
 * or any spectroscopy is carried out, for example.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSample extends IFDRepresentableObject<IFDSampleRepresentation> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_SAMPLE_FLAG);
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDSample() {
		super(null, null);
		setProperties(propertyPrefix, null);
	}

	@Override
	protected IFDRepresentation newRepresentation(IFDReference ifdReference, Object object, long len, String type,
			String subtype) {
		return new IFDSampleRepresentation(ifdReference, object, len, type, subtype);
	}

}
