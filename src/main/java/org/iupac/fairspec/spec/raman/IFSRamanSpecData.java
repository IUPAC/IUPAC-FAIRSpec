package org.iupac.fairspec.spec.raman;

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
public class IFSRamanSpecData extends IFSSpecData {

	{
		super.setProperties(new IFSProperty[] {
				// TODO
//				new IFSProperty("raman.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSRamanSpecData(String name) {
		super(name, ObjectType.RAMANSpecData);
	}
	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSRamanSpecDataRepresentation(name, ref, obj, len);

	}	




}
