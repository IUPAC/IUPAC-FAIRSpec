package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSSpecData;
import org.iupac.fairspec.common.IFSSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSNMRSpecData extends IFSSpecData {


	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty("nmr.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
				new IFSProperty("nmr.nominalFreq1", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nominalFreq2", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nominalFreq3", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nucleus1", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE), 
				new IFSProperty("nmr.nucleus2", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE),
				new IFSProperty("nmr.nucleus3", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE) 
		});
	}
	

	public IFSNMRSpecData(String name) {
		super(name);
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSNMRSpecDataRepresentation(name, ref, obj);

	}	
	
	public void setPropertyValue(String name, Object value) throws IFSException {
		IFSProperty p = htProps.get(name);
		if (p == null) {
			params.put(name, value);
			if (this.name == null
					&& (name.equals("IFS.nmr.param.expt") || name.equals("IFS.nmr.representation.vender.dataset")))
				this.name = value.toString();
			return;
		}
		htProps.put(name, p.getClone(value));
	}



}
