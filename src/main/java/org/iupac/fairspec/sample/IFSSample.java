package org.iupac.fairspec.sample;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSRepresentableObject;

/**
 * An IFSSample represents a physical sample that optionally has one or the
 * other or both of associated IFSStructureCollection and
 * IFSDataObjectCollection. It may also have a representation. 
 * 
 * It is expected to
 * hold numerous IFSProperty values that describe the sample.
 * 
 * Multiple IFSStructures would indicate a mixture or, perhaps, an ambiguity
 * (which would be distinguished by its properties or subclass) Multiple
 * IFSDataObjects indicate multiple experiments all relating to this particular
 * sample.
 * 
 * Since IFSSample can exist with no structures and/or no data and/or no
 * representations, it can be considered a starting "holder class" that can be
 * added to during the process of sample analysis, before any structure is
 * known or any spectroscopy is carried out, for example.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSample extends IFSRepresentableObject<IFSSampleRepresentation> {

	public static final String IFS_PROP_SAMPLE_ID = "IFS.property.sample.id";

	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSSample.IFS_PROP_SAMPLE_ID),
				// could be MANY standard properties here
		});
	}
	
	public IFSSample(String name, String type) throws IFSException {
		super(name, type);
	}

	public IFSSample(String path, String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Sample);
		setPath(path);
		if (param.equals(IFSSample.IFS_PROP_SAMPLE_ID))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len,
			String type, String subtype) {
		return new IFSSampleRepresentation(ifsReference, object, len, type, subtype);
	}


}
