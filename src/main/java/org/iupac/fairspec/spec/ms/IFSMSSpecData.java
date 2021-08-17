package org.iupac.fairspec.spec.ms;

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
public final class IFSMSSpecData extends IFSSpecData {


	public static final String IFS_PROP_SPEC_MS_EXPT_LABEL    = "IFS.property.spec.ms.expt.label";


	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSMSSpecData.IFS_PROP_SPEC_MS_EXPT_LABEL),
		});
	}
	

	public IFSMSSpecData() throws IFSException {
		super(null, "spec.ms");
	}

	@Override
	protected IFSSpecData newInstance() throws IFSException {
		return new IFSMSSpecData();
	}


	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
