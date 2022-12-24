package org.iupac.fairdata.dataobject;

import java.util.Map;

import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDRepresentableObject;

@SuppressWarnings("serial")
public class IFDDataObjectCollection extends IFDCollection<IFDRepresentableObject<IFDDataObjectRepresentation>> {

	public IFDDataObjectCollection(IFDDataObject data) {
		this();
		add(data);
	}

	public IFDDataObjectCollection(String label, String type) {
		super(label, type);
	}
	
	public IFDDataObjectCollection() {
		super(null, null);
	}
	
	/**
	 * Replace a data object with a cloned version that has a new ID.
	 * 
	 * @param data
	 * @param idExtension
	 * @return
	 */
	public IFDDataObject cloneData(IFDDataObject data, String newID) {
		IFDDataObject newData = data.clone();
		if (newID != null)
			newData.setID((data.getID() == null ? "" : data.getID()) + newID);
		data.setValid(false);
		newData.setValid(true);
		Map<String, IFDProperty> props = newData.getProperties();
		System.out.println(newID + " DO??? " + data.getProperties().size() + " " + props.size());
		add(newData);
		return newData;
	}

}
