package org.iupac.fairspec.spec.uvvis;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;
import org.iupac.fairspec.spec.IFSSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSUVVISSpecData extends IFSSpecData {


	public static final String IFS_PROP_SPEC_UVVIS_EXPT_ID = "IFS.property.spec.uvvis.expt.id";

	{
		super.setProperties(new IFSProperty[] {
				// TODO
//				new IFSProperty("IFS.spec.uvvis.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSUVVISSpecData() throws IFSException {
		super(null, "spec.uvvis");
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSUVISSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
