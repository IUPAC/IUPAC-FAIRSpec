package org.iupac.fairdata.sample;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

/**
 * A collection of IFDSample objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleCollection extends IFDCollection<IFDSample> {

	public IFDSampleCollection(String name) throws IFDException {
		super(name, ObjectType.SampleCollection);
	}
	
	private Map<String, IFDSample> map = new HashMap<>();

	public IFDSample getSampleFor(String rootPath, String localName, String param, String value, String zipName, String mediaType) throws IFDException {
		String keyValue = param + ";" + value;
		IFDSample sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDSample(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.addRepresentation(zipName, localName, param, mediaType);
		return sd;
	}

}