package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDConst;
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

	public IFDSampleCollection(String label) {
		super(label, null);
	}
	
	
	public IFDSampleCollection(String label, IFDSample sample) {
		this(label);
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
			sd.findOrAddRepresentation(zipName, localName, null, param, mediaType);
		return sd;
	}

}