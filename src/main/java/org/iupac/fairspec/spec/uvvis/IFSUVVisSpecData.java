package org.iupac.fairspec.spec.uvvis;

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
public class IFSUVVisSpecData extends IFSSpecData {


	public IFSUVVisSpecData(String name, ObjectType type) {
		super(name, type);
	}


	{
		super.setProperties(new IFSProperty[] {
				// TODO
//				new IFSProperty("IFS.spec.uvvis.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	

	public IFSUVVisSpecData(String name) {
		super(name, ObjectType.UVVisSpecData);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSUVVisSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
