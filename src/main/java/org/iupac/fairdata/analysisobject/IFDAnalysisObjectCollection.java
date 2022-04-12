package org.iupac.fairdata.analysisobject;

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
public class IFDAnalysisObjectCollection extends IFDCollection<IFDRepresentableObject<IFDAnalysisObjectRepresentation>> {

	public IFDAnalysisObjectCollection() {
		super(null, null);
	}
	
	
	public IFDAnalysisObjectCollection(IFDAnalysisObject ao) {
		this();
		add(ao);
	}

	
	public IFDAnalysisObject getAnalysisFor(String rootPath, String localName, String param, String value, String zipName, String mediaType) {
		// UNTESTED
		String keyValue = param + ";" + value;
		IFDAnalysisObject ao = (IFDAnalysisObject) map.get(keyValue);
		if (ao == null) {
			map.put(keyValue,  ao = new IFDAnalysisObject(rootPath, param, value));
			add(ao);
		}
		if (IFDConst.isRepresentation(param))
			ao.findOrAddRepresentation(zipName, localName, null, param, mediaType);
		return ao;
	}

}