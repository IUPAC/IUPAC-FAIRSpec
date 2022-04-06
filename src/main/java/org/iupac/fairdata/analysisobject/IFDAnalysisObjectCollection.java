package org.iupac.fairdata.analysis;

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

	public IFDAnalysisObjectCollection(String name) {
		super(name, null);
	}
	
	
	public IFDAnalysisObjectCollection(String name, IFDAnalysisObject ao) {
		this(name);
		add(ao);
	}

	
	public IFDAnalysisObject getAnalysisFor(String rootPath, String localName, String param, String value, String zipName, String mediaType) {
		String keyValue = param + ";" + value;
		IFDAnalysisObject ao = (IFDAnalysisObject) map.get(keyValue);
		if (ao == null) {
			map.put(keyValue,  ao = new IFDAnalysisObject(rootPath, param, value));
			add(ao);
		}
		if (IFDConst.isRepresentation(param))
			ao.addRepresentation(zipName, localName, param, mediaType);
		return ao;
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDAnalysisObject.class };
	}

}