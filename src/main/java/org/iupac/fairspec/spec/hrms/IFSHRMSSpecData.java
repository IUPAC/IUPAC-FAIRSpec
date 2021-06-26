package org.iupac.fairspec.spec.hrms;

import org.iupac.fairspec.common.IFSException;
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
public class IFSHRMSSpecData extends IFSSpecData {


	public static final String IFS_PROP_SPEC_HRMS_EXPT_ID  = "IFS.property.spec.hrms.expt.id";


	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSHRMSSpecData.IFS_PROP_SPEC_HRMS_EXPT_ID),
		});
	}
	

	public IFSHRMSSpecData(String name) throws IFSException {
		super(name, ObjectType.HRMSSpecData);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSHRMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
