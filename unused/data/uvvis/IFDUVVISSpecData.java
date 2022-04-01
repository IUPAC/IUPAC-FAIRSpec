package org.iupac.fairdata.spec.uvvis;

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
public class IFDUVVISSpecData extends IFDSpecData {


	public static final String IFD_TYPE_SPEC_UVVIS = "spec.uvvis";
	

	public static final String IFD_PROP_SPEC_UVVIS_EXPT_LABEL = "IFD.property.spec.uvvis.expt.label";

	{
		super.setProperties(new IFDProperty[] {
				// TODO
//				new IFDProperty("IFD.spec.uvvis.dimension", IFDConst.PROPERTY_TYPE.INT, IFDConst.UNITS.NONE),
		});
	}
	
	public IFDUVVISSpecData() throws IFDException {
		super(null, IFD_TYPE_SPEC_UVVIS);
	}


	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDUVVISSpecDataRepresentation(ref, obj, len, type, subtype);
	}
	
	@Override
	protected IFDSpecData newInstance() throws IFDException {
		return new IFDUVVISSpecData();
	}



}
