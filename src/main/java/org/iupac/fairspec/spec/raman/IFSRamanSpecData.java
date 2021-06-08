package org.iupac.fairspec.spec.raman;

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
public class IFSRamanSpecData extends IFSSpecData {



	{
		super.setProperties(new IFSProperty[] {
//				new IFSProperty("raman.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSRamanSpecData(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSRamanSpecDataRepresentation(name, ref, obj);

	}	




}
