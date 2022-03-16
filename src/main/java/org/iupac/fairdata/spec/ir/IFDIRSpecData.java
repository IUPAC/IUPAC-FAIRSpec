package org.iupac.fairdata.spec.ir;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.spec.IFDSpecData;
import org.iupac.fairdata.spec.IFDSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class IFDIRSpecData extends IFDSpecData {

	public static final String IFD_TYPE_SPEC_IR = "spec.ir";
	

	public static final String IFD_PROP_SPEC_IR_EXPT_LABEL    = "IFD.property.spec.ir.expt.label";

	{
		setProperties(new IFDProperty[] {
				// TODO
				new IFDProperty(IFDIRSpecData.IFD_PROP_SPEC_IR_EXPT_LABEL),
		});
	}
	
	public IFDIRSpecData() throws IFDException {
		super(null, IFD_TYPE_SPEC_IR);
	}

	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDIRSpecDataRepresentation(ref, obj, len, type, subtype);
	}	

	@Override
	protected IFDSpecData newInstance() throws IFDException {
		IFDIRSpecData d = new IFDIRSpecData();
		// add manuf name etc here.
		return d;

	}

}
