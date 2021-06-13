package org.iupac.fairspec.spec;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSDataObjectCollection;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.spec.ir.IFSIRSpecData;
import org.iupac.fairspec.spec.ms.IFSMSSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.raman.IFSRamanSpecData;

@SuppressWarnings("serial")
public class IFSSpecDataCollection extends IFSDataObjectCollection<IFSSpecData> {

	private final ObjectType dataType;


	public IFSSpecDataCollection(String name, ObjectType dataType) {
		super(name, ObjectType.SpecDataCollection);
		this.dataType = dataType;
	}

	
	public IFSSpecDataCollection(String name, IFSSpecData data) {
		super(name, ObjectType.SpecDataCollection);
		dataType = data.getObjectType();
		addSpecData(data);
	}


	public void addSpecData(IFSSpecData data) {
		super.add(data);
	}
	
	public ObjectType getDataType() {
		return dataType;
	}

	private Map<String, IFSSpecData> map = new HashMap<>();

	public IFSSpecData getSpecDataFor(String param, String value, String objectFile, ObjectType type) throws IFSException {
		String keyValue = objectFile;
		IFSSpecData sd = map.get(keyValue);
		if (sd == null) {
 			map.put(keyValue,  sd = newIFSSpecData(param, value, type));
 			add(sd);
		} else {
			sd.setPropertyValue(param, value);
		}
		sd.getRepresentation(objectFile);
		return sd;
	}


	public static IFSSpecData newIFSSpecData(String param, String value, ObjectType type) throws IFSException {
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
		sd.setPropertyValue(param, value);
		return sd;
	}
}