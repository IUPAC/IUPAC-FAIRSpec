package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
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
public class IFSNMRSpecData extends IFSSpecData {

	{
		super.setProperties(new IFSProperty[] {
				
				new IFSProperty(IFSConst.IFS_SPEC_NMR_INSTR_MANUFACTURER_NAME),

				new IFSProperty(IFSConst.IFS_SPEC_NMR_INSTR_FREQ_NOMINAL, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_INSTR_PROBEID),
							
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_DIM, IFSConst.PROPERTY_TYPE.INT),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_PULSE_PROG),

				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_SOLVENT),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_FREQ_1, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_FREQ_2, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_FREQ_3, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_NUCL_1, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE), 
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_NUCL_2, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE),
				new IFSProperty(IFSConst.IFS_SPEC_NMR_EXPT_NUCL_3, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE) 
		});
	}
	

	public IFSNMRSpecData(String name) {
		super(name, ObjectType.NMRSpecData);
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	protected IFSSpecDataRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len, String type, String subtype) {
		return new IFSNMRSpecDataRepresentation(ref, obj, len, type, subtype);

	}	
	
	public void setPropertyValue(String name, Object value) {
		if (this.name == null
				&& (name.equals("IFS.spec.nmr.property.expt") || name.equals("IFS.spec.nmr.representation.vendor.dataset")))
			this.name = value.toString();
		super.setPropertyValue(name, value);
	}


	public String toString() {
		return super.toString();
	}

}
