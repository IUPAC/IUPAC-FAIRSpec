package org.iupac.fairspec.spec;

import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.spec.ir.IFSIRSpecData;
import org.iupac.fairspec.spec.ms.IFSMSSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.raman.IFSRamanSpecData;

/**
 * A collection of IFSSpecData objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSpecDataCollection extends IFSDataObjectCollection<IFSSpecData> {

	public IFSSpecDataCollection(String name) {
		super(name, ObjectType.SpecDataCollection);
	}
	
	public IFSSpecDataCollection(String name, IFSSpecData data) {
		super(name, ObjectType.SpecDataCollection);
		addSpecData(data);
	}

	public void addSpecData(IFSSpecData data) {
		super.add(data);
	}
	
	public ObjectType getDataType() {
		return dataType;
	}

	public IFSSpecData newIFSDataObject(String path, String param, String value, ObjectType type) {
		IFSSpecData sd;
		switch(type) {
		case IRSpecData:
			sd = new IFSIRSpecData(null);
			break;
		case MSSpecData:
			sd = new IFSMSSpecData(null);
			break;
		case NMRSpecData:
			sd = new IFSNMRSpecData(null);
			break;
		case RAMANSpecData:
			sd = new IFSRamanSpecData(null);
			break;
		default:
			return null;	
		}
		sd.setPath(path);
		sd.setPropertyValue(param, value);
		return sd;
	}

}