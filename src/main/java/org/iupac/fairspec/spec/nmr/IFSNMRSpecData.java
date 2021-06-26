package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
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
public class IFSNMRSpecData extends IFSSpecData {

	public static final String IFS_SPEC_NMR_INSTR_MANUFACTURER_NAME = "IFS.spec.nmr.instr.manufacturer.name";
	public static final String IFS_SPEC_NMR_INSTR_FREQ_NOMINAL      = "IFS.spec.nmr.instr.freq.nominal";
	public static final String IFS_SPEC_NMR_INSTR_PROBEID           = "IFS.spec.nmr.instr.probe.id";
	public static final String IFS_SPEC_NMR_EXPT_DIM        = "IFS.spec.nmr.expt.dim";
	public static final String IFS_SPEC_NMR_EXPT_FREQ_1     = "IFS.spec.nmr.expt.freq.1";
	public static final String IFS_SPEC_NMR_EXPT_FREQ_2     = "IFS.spec.nmr.expt.freq.2";
	public static final String IFS_SPEC_NMR_EXPT_FREQ_3     = "IFS.spec.nmr.expt.freq.3";
	public static final String IFS_SPEC_NMR_EXPT_FREQ_4     = "IFS.spec.nmr.expt.freq.4";
	public static final String IFS_SPEC_NMR_EXPT_NUCL_1     = "IFS.spec.nmr.expt.nucl.1";
	public static final String IFS_SPEC_NMR_EXPT_NUCL_2     = "IFS.spec.nmr.expt.nucl.2";
	public static final String IFS_SPEC_NMR_EXPT_NUCL_3     = "IFS.spec.nmr.expt.nucl.3";
	public static final String IFS_SPEC_NMR_EXPT_NUCL_4     = "IFS.spec.nmr.expt.nucl.4";
	public static final String IFS_SPEC_NMR_EXPT_PULSE_PROG = "IFS.spec.nmr.expt.pulse.prog";
	public static final String IFS_SPEC_NMR_EXPT_SOLVENT    = "IFS.spec.nmr.expt.solvent";
	public static final String IFS_PROP_SPEC_NMR_EXPT_ID   = "IFS.property.spec.nmr.expt.id";


	{
		super.setProperties(new IFSProperty[] {
				
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_INSTR_MANUFACTURER_NAME),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_INSTR_FREQ_NOMINAL, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_INSTR_PROBEID),
				
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_ID),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_DIM, IFSConst.PROPERTY_TYPE.INT),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_PULSE_PROG),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_SOLVENT),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_FREQ_1, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_FREQ_2, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_FREQ_3, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNITS.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_NUCL_1, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE), 
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_NUCL_2, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE),
				new IFSProperty(IFSNMRSpecData.IFS_SPEC_NMR_EXPT_NUCL_3, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNITS.NONE) 
		});
	}
	

	public IFSNMRSpecData(String name) throws IFSException {
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
	
	@Override
	public void setPropertyValue(String name, Object value) {
		super.setPropertyValue(name, value);
	}


	@Override
	public String toString() {
		return super.toString();
	}

}
