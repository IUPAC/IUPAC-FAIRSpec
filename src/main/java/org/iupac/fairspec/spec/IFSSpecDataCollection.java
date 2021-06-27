package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.spec.hrms.IFSHRMSSpecData;
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

	public IFSSpecDataCollection(String name) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.SpecDataCollection);
	}
	
	public IFSSpecDataCollection(String name, IFSSpecData data) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.SpecDataCollection);
		addSpecData(data);
	}

	public boolean addSpecData(IFSSpecData data) {
		return super.add(data);
	}
	
	public String getDataType() {
		return subtype;
	}

	public final static String 
	// IFS spec core and collections
	NMRSpecData = "NMRSpecData", 
	IRSpecData = "IRSpecData", 
	MSSpecData = "SpecData", 
	HRMSSpecData = "HRMSSpecData", 
	RAMANSpecData = "SRAMANpecData", 
	UVVisSpecData = "UVVisSpecData";

	@Override
	public IFSSpecData newIFSDataObject(String path, String param, String value, String type) throws IFSException {
		IFSSpecData sd = null;
		try {
			switch (type) {
			case NMRSpecData:
				sd = new IFSNMRSpecData(null);
				break;
			case IRSpecData:
				sd = new IFSIRSpecData(null);
				break;
			case RAMANSpecData:
				sd = new IFSRamanSpecData(null);
				break;
			case HRMSSpecData:
				sd = new IFSHRMSSpecData(null);
				break;
			case MSSpecData:
				sd = new IFSMSSpecData(null);
				break;
			default:
				// throw the unrecognized type exception
				break;
			}
		} catch (IFSException e) {
			// not attainable
		}
		if (sd == null)
			throw new IFSException("Unrecognized IFSSpecData type " + type);
		sd.setPath(path);
		sd.setPropertyValue(param, value);
		return sd;
	}

}