package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
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
public final class IFSNMRSpecData extends IFSSpecData {

	public static final String IFS_TYPE_SPEC_NMR = "spec.nmr";
	
	public static final String IFS_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME = "IFS.property.spec.nmr.instr.manufacturer.name";
	public static final String IFS_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL      = "IFS.property.spec.nmr.instr.freq.nominal";
	public static final String IFS_PROP_SPEC_NMR_INSTR_PROBEID           = "IFS.property.spec.nmr.instr.probe.id";
	public static final String IFS_PROP_SPEC_NMR_EXPT_DIM        = "IFS.property.spec.nmr.expt.dim";
	public static final String IFS_PROP_SPEC_NMR_EXPT_FREQ_1     = "IFS.property.spec.nmr.expt.freq.1";
	public static final String IFS_PROP_SPEC_NMR_EXPT_FREQ_2     = "IFS.property.spec.nmr.expt.freq.2";
	public static final String IFS_PROP_SPEC_NMR_EXPT_FREQ_3     = "IFS.property.spec.nmr.expt.freq.3";
	public static final String IFS_PROP_SPEC_NMR_EXPT_NUCL_1     = "IFS.property.spec.nmr.expt.nucl.1";
	public static final String IFS_PROP_SPEC_NMR_EXPT_NUCL_2     = "IFS.property.spec.nmr.expt.nucl.2";
	public static final String IFS_PROP_SPEC_NMR_EXPT_NUCL_3     = "IFS.property.spec.nmr.expt.nucl.3";
	public static final String IFS_PROP_SPEC_NMR_EXPT_PULSE_PROG = "IFS.property.spec.nmr.expt.pulse.prog";
	public static final String IFS_PROP_SPEC_NMR_EXPT_SOLVENT    = "IFS.property.spec.nmr.expt.solvent";
	public static final String IFS_PROP_SPEC_NMR_EXPT_TEMPERATURE_K= "IFS.property.spec.nmr.expt.temperature.K";
	public static final String IFS_PROP_SPEC_NMR_EXPT_ID   = "IFS.property.spec.nmr.expt.id";
	final public static String[][] nmrSolvents = {{"1,1,2,2-tetrachloroethane-d2","Cl2CDCDCl2","C([2H])(Cl)Cl)([2H])(Cl)Cl"},
	{"acetone-d6","CD3COCD3","[2H]C([2H])([2H])C(=O)C([2H])([2H])[2H]"},
	{"acetonitrile-d3","CD3CN","[2H]C([2H])([2H])C#N"},
	{"chloroform-d","CDCl3","[2H]C(Cl)(Cl)Cl"},
	{"deuterium oxide","D2O","[2H]O[2H]"},
	{"dichloromethane-d2","CD2Cl2","[2H]C([2H])(Cl)Cl"},
	{"dimethyl sulfoxide-d6","CD3SOCD3;d6-DMSO","[2H]C([2H])([2H])S(=O)C([2H])([2H])[2H]"},
	{"ethanol-d6","CD3CD2OD","[2H]C([2H])([2H])C([2H])([2H])O[2H]"},
	{"methanol-d4","CD3COD; MeOD","[2H]C([2H])([2H])O[2H]"},
	{"nitrobenzene-d5","C6D5NO2","[2H]C1=C([2H])C([2H])=C(C([2H])=C1[2H])[N+]([O-])=O"},
	{"nitromethane-d3","CD3NO2","[2H]C([2H])([2H])[N+]([O-])=O"},
	{"pyridine-d5","C5D5N","[2H]C1=NC([2H])=C([2H])C([2H])=C1[2H]"},
	{"toluene-d8","C6D5CD3","[2H]C([2H])([2H])C1=C([2H])C([2H])=C([2H])C([2H])=C1[2H]"},
	{"benzene-d6","C6D6","C1([2H])=C([2H])C([2H])=C([2H])C([2H])=C1[2H]"}};


	{
		super.setProperties(new IFSProperty[] {
				
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNIT.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_PROBEID),
				
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_ID),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_DIM, IFSConst.PROPERTY_TYPE.INT),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_PULSE_PROG),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_SOLVENT),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_1, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNIT.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_2, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNIT.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_3, IFSConst.PROPERTY_TYPE.INT, IFSConst.PROPERTY_UNIT.MHZ),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_1, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNIT.NONE), 
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_2, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNIT.NONE),
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_3, IFSConst.PROPERTY_TYPE.NUCL, IFSConst.PROPERTY_UNIT.NONE), 
				new IFSProperty(IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_TEMPERATURE_K, IFSConst.PROPERTY_TYPE.FLOAT, IFSConst.PROPERTY_UNIT.KELVIN) 
		});
	}
	
	public IFSNMRSpecData() throws IFSException {
		super(null, IFS_TYPE_SPEC_NMR);
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
