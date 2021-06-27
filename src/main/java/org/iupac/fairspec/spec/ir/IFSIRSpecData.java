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
public class IFSIRSpecData extends IFSSpecData {

	public static final String IFS_PROP_SPEC_IR_EXPT_ID    = "IFS.property.spec.ir.expt.id";

	{
		setProperties(new IFSProperty[] {
				// TODO
				new IFSProperty(IFSIRSpecData.IFS_PROP_SPEC_IR_EXPT_ID),
		});
	}
	
	public IFSIRSpecData(String name) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.IRSpecData);
	}

	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSIRSpecDataRepresentation(ref, obj, len, type, subtype);
	}	

}
