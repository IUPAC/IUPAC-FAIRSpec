package org.iupac.fairspec.spec.hrms;

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


	{
		super.setProperties(new IFSProperty[] {
		});
	}
	

	public IFSHRMSSpecData(String name) {
		super(name, ObjectType.HRMSSpecData);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSHRMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
