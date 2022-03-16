package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * An IFDSample represents a physical sample that optionally has one or the
 * other or both of associated IFDStructureCollection and
 * IFDDataObjectCollection. It may also have a representation. 
 * 
 * It is expected to
 * hold numerous IFDProperty values that describe the sample.
 * 
 * Multiple IFDStructures would indicate a mixture or, perhaps, an ambiguity
 * (which would be distinguished by its properties or subclass) Multiple
 * IFDDataObjects indicate multiple experiments all relating to this particular
 * sample.
 * 
 * Since IFDSample can exist with no structures and/or no data and/or no
 * representations, it can be considered a starting "holder class" that can be
 * added to during the process of sample analysis, before any structure is
 * known or any spectroscopy is carried out, for example.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSample extends IFDRepresentableObject<IFDSampleRepresentation> {

	public static final String IFD_PROP_SAMPLE_ID = "IFD.property.sample.id";

	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDSample.IFD_PROP_SAMPLE_ID),
				// could be MANY standard properties here
		});
	}
	
	public IFDSample(String name, String type) throws IFDException {
		super(name, type);
	}

	public IFDSample(String path, String param, String value) throws IFDException {
		super(param + ";" + value, ObjectType.Sample);
		setPath(path);
		if (param.equals(IFDSample.IFD_PROP_SAMPLE_ID))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object, long len,
			String type, String subtype) {
		return new IFDSampleRepresentation(ifdReference, object, len, type, subtype);
	}


}
