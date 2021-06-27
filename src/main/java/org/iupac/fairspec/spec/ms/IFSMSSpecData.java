package org.iupac.fairspec.spec.ms;

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
public class IFSMSSpecData extends IFSSpecData {


	public static final String IFS_PROP_SPEC_MS_EXPT_ID    = "IFS.property.spec.ms.expt.id";


	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSMSSpecData.IFS_PROP_SPEC_MS_EXPT_ID),
		});
	}
	

	public IFSMSSpecData(String name) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.MSSpecData);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
