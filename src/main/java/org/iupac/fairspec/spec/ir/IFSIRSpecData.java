package org.iupac.fairspec.spec.ir;

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
public class IFSIRSpecData extends IFSSpecData {

	{
		setProperties(new IFSProperty[] {
				// TODO
//				new IFSProperty("ir.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSIRSpecData(String name) {
		super(name, ObjectType.IRSpecData);
	}

	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSIRSpecDataRepresentation(name, ref, obj, len);
	}	

}
