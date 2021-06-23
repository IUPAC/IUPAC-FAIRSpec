package org.iupac.fairspec.spec.raman;

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
public class IFSRamanSpecData extends IFSSpecData {

	public static final String IFS_PROP_SPEC_RAMAN_EXPT_ID = "IFS.property.spec.raman.expt.id";

	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSRamanSpecData.IFS_PROP_SPEC_RAMAN_EXPT_ID),
		});
	}
	
	public IFSRamanSpecData(String name) {
		super(name, ObjectType.RAMANSpecData);
	}
	
	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSRamanSpecDataRepresentation(ref, obj, len, type, subtype);

	}	




}
