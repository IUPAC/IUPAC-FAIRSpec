package org.iupac.fairdata.spec.hrms;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.spec.IFDSpecData;
import org.iupac.fairdata.spec.IFDSpecDataRepresentation;

/**
 * A final class for high-resolution mass spec data.
 * It is final because it is created by reflection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class IFDHRMSSpecData extends IFDSpecData {

	public static final String IFD_TYPE_SPEC_HRMS = "spec.hrms";


	public static final String IFD_PROP_SPEC_HRMS_EXPT_LABEL  = "IFD.property.spec.hrms.expt.label";


	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDHRMSSpecData.IFD_PROP_SPEC_HRMS_EXPT_LABEL),
		});
	}
	

	public IFDHRMSSpecData() throws IFDException {
		super(null, IFD_TYPE_SPEC_HRMS);
	}


	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDHRMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


	@Override
	protected IFDSpecData newInstance() throws IFDException {
		return new IFDHRMSSpecData();
	}


}
