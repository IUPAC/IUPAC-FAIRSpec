package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;

/**
 * A collection of IFDSample objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleCollection extends IFDCollection<IFDObject<?>> {

	public IFDSampleCollection(String name) {
		super(name, null);
	}
	
	
	public IFDSampleCollection(String name, IFDSample sample) {
		this(name);
		add(sample);
	}

	
	public IFDSample getSampleFor(String rootPath, String localName, String param, String value, String zipName, String mediaType) {
		String keyValue = param + ";" + value;
		IFDSample sd = (IFDSample) map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDSample(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.addRepresentation(zipName, localName, param, mediaType);
		return sd;
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSample.class };
	}

}