package org.iupac.fairspec.spec.raman;

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
public final class IFSRamanSpecData extends IFSSpecData {

	public static final String IFS_TYPE_SPEC_RAMAN = "spec.raman";
	
	public static final String IFS_PROP_SPEC_RAMAN_EXPT_LABEL = "IFS.property.spec.raman.expt.label";

	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSRamanSpecData.IFS_PROP_SPEC_RAMAN_EXPT_LABEL),
		});
	}
	
	public IFSRamanSpecData() throws IFSException {
		super(null, IFS_TYPE_SPEC_RAMAN);
	}
	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSRamanSpecDataRepresentation(ref, obj, len, type, subtype);

	}	

	@Override
	protected IFSSpecData newInstance() throws IFSException {
		return new IFSRamanSpecData();
	}



}
