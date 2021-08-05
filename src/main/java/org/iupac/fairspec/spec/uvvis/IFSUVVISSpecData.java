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


	public static final String IFS_TYPE_SPEC_UVVIS = "spec.uvvis";
	

	public static final String IFS_PROP_SPEC_UVVIS_EXPT_LABEL = "IFS.property.spec.uvvis.expt.label";

	{
		super.setProperties(new IFSProperty[] {
				// TODO
//				new IFSProperty("IFS.spec.uvvis.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
		});
	}
	
	public IFSUVVISSpecData() throws IFSException {
		super(null, IFS_TYPE_SPEC_UVVIS);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSUVVISSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
