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
		super(name, ObjectType.MSSpecData);
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSHRMSSpecDataRepresentation(name, ref, obj, len);
	}


}
