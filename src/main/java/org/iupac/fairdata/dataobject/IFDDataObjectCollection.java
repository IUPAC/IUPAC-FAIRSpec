package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.structure.IFDStructure;

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
	
	@Override
	public boolean add(IFDRepresentableObject<IFDDataObjectRepresentation> s) {
		return super.add(s);
	}


	
	/**
	 * Replace a data object with a cloned version that has a new ID.
	 * 
	 * @param data
	 * @param idExtension
	 * @return
	 */
	public IFDDataObject cloneData(IFDDataObject data, String newID, boolean andReplace) {
		IFDDataObject newData = data.clone();
		if (newID != null)
			newData.setID((data.getID() == null ? "" : data.getID()) + newID);
		if (andReplace) {
			data.setValid(false);
		}
		newData.setValid(true);
		newData.getProperties();
		add(newData);
		return newData;
	}

}
