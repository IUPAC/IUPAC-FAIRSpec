package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDDataObjectCollection extends IFDCollection<IFDRepresentableObject<IFDDataObjectRepresentation>> {

	public IFDDataObjectCollection(IFDDataObject data) {
		this();
		add(data);
	}

	public IFDDataObjectCollection(String label) {
		super(label, null);
	}

	public IFDDataObjectCollection(String label, String type) {
		super(label, type);
	}
	
	public IFDDataObjectCollection() {
		super(null, null);
	}
	
	public IFDDataObject findObject(String path, String originPath) {
		String keyValue = path + "::" + originPath;
		return (IFDDataObject) map.get(keyValue);
	}

	public void addObject(String rootPath, String originPath, IFDDataObject sd) {
		String keyValue = rootPath + "::" + originPath;
		map.put(keyValue, sd);
		add(sd);
	} 
	
	/**
	 * Replace a data object with a cloned version that has a new ID.
	 * 
	 * @param data
	 * @param idExtension
	 * @return
	 */
	public IFDDataObject cloneData(IFDDataObject data, String newID) {
		IFDDataObject newData = (IFDDataObject) data.clone();
		newData.setID(newID);
		remove(data);
		add(newData);
		return newData;
	}

}
