package org.iupac.fairdata.data.ir;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.core.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class IFDIRSpecData extends IFDDataObject {

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
	protected IFDDataObjectRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDIRSpecDataRepresentation(ref, obj, len, type, subtype);
	}	

	@Override
	protected IFDDataObject newInstance() throws IFDException {
		IFDIRSpecData d = new IFDIRSpecData();
		// add manuf name etc here.
		return d;

	}

}
