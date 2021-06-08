package org.iupac.fairspec.spec.ms;

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
public class IFSMSSpecData extends IFSSpecData {


	{
		super.setProperties(new IFSProperty[] {
//				new IFSProperty("MS.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	

	public IFSMSSpecData(String name) {
		super(name);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSMSSpecDataRepresentation(name, ref, obj);
	}


}
