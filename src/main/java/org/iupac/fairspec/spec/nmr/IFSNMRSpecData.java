package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSNMRSpecData extends IFSSpecData {


	{
		super.setProperties(new IFSProperty[] {
				// TODO
				new IFSProperty("IFS.nmr.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
				new IFSProperty("IFS.nmr.nominalFreq1", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("IFS.nmr.nominalFreq2", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("IFS.nmr.nominalFreq3", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("IFS.nmr.nucleus1", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE), 
				new IFSProperty("IFS.nmr.nucleus2", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE),
				new IFSProperty("IFS.nmr.nucleus3", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE) 
		});
	}
	

	public IFSNMRSpecData(String name) {
		super(name, ObjectType.NMRSpecData);
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSNMRSpecDataRepresentation(name, ref, obj, len);

	}	
	
	public void setPropertyValue(String name, Object value) {
		if (this.name == null
				&& (name.equals("IFS.nmr.param.expt") || name.equals("IFS.nmr.representation.vender.dataset")))
			this.name = value.toString();
		super.setPropertyValue(name, value);
	}



}
