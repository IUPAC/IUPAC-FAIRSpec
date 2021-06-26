package org.iupac.fairspec.sample;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

/**
 * A collection of IFSSample objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSSampleCollection extends IFSCollection<IFSSample> {

	public IFSSampleCollection(String name) throws IFSException {
		super(name, ObjectType.SampleCollection);
	}
	
	private Map<String, IFSSample> map = new HashMap<>();

	public IFSSample getSampleFor(String path, String localName, String param, String value, String zipName, String mediaType) throws IFSException {
		String keyValue = param + ";" + value;
		IFSSample sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFSSample(path, param, value));
			add(sd);
		}
		if (IFSConst.isRepresentation(param))
			sd.getRepresentation(zipName, localName, true, param, mediaType);
		return sd;
	}

}