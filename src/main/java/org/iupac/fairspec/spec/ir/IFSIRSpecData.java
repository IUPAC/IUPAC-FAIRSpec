package org.iupac.fairspec.spec.ir;

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
public class IFSIRSpecData extends IFSSpecData {



	{
		super.setProperties(new IFSProperty[] {
//				new IFSProperty("ir.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSIRSpecData(String name) {
		super(name);
	}

	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSIRSpecDataRepresentation(name, ref, obj);
	}	

}
