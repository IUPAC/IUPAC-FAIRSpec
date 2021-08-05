package org.iupac.fairspec.spec.ir;

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
public final class IFSIRSpecData extends IFSSpecData {

	public static final String IFS_TYPE_SPEC_IR = "spec.ir";
	

	public static final String IFS_PROP_SPEC_IR_EXPT_LABEL    = "IFS.property.spec.ir.expt.label";

	{
		setProperties(new IFSProperty[] {
				// TODO
				new IFSProperty(IFSIRSpecData.IFS_PROP_SPEC_IR_EXPT_LABEL),
		});
	}
	
	public IFSIRSpecData() throws IFSException {
		super(null, IFS_TYPE_SPEC_IR);
	}

	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSIRSpecDataRepresentation(ref, obj, len, type, subtype);
	}	

}
