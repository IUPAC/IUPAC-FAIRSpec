package org.iupac.fairdata.spec.nmr;

import org.iupac.fairdata.common.IFDConst;
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
public final class IFDNMRSpecData extends IFDSpecData {

	public static final String IFD_TYPE_SPEC_NMR = "spec.nmr";
	
	public static final String IFD_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME = "IFD.property.spec.nmr.instr.manufacturer.name";
	public static final String IFD_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL      = "IFD.property.spec.nmr.instr.freq.nominal";
	public static final String IFD_PROP_SPEC_NMR_INSTR_PROBE_TYPE        = "IFD.property.spec.nmr.instr.probe.type";
	public static final String IFD_PROP_SPEC_NMR_EXPT_DIM        = "IFD.property.spec.nmr.expt.dim";
	public static final String IFD_PROP_SPEC_NMR_EXPT_FREQ_1     = "IFD.property.spec.nmr.expt.freq.1";
	public static final String IFD_PROP_SPEC_NMR_EXPT_FREQ_2     = "IFD.property.spec.nmr.expt.freq.2";
	public static final String IFD_PROP_SPEC_NMR_EXPT_FREQ_3     = "IFD.property.spec.nmr.expt.freq.3";
	public static final String IFD_PROP_SPEC_NMR_EXPT_NUCL_1     = "IFD.property.spec.nmr.expt.nucl.1";
	public static final String IFD_PROP_SPEC_NMR_EXPT_NUCL_2     = "IFD.property.spec.nmr.expt.nucl.2";
	public static final String IFD_PROP_SPEC_NMR_EXPT_NUCL_3     = "IFD.property.spec.nmr.expt.nucl.3";
	public static final String IFD_PROP_SPEC_NMR_EXPT_PULSE_PROG = "IFD.property.spec.nmr.expt.pulse.prog";
	public static final String IFD_PROP_SPEC_NMR_EXPT_SOLVENT    = "IFD.property.spec.nmr.expt.solvent";
	public static final String IFD_PROP_SPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE= "IFD.property.spec.nmr.expt.temperature.absolute";
	public static final String IFD_PROP_SPEC_NMR_EXPT_LABEL   = "IFD.property.spec.nmr.expt.label";
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
		super.setProperties(new IFDProperty[] {
				
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, IFDConst.PROPERTY_TYPE.INT, IFDConst.PROPERTY_UNIT.MHZ),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_PROBE_TYPE),
				
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_LABEL),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_DIM, IFDConst.PROPERTY_TYPE.INT),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_PULSE_PROG),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_SOLVENT),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_1, IFDConst.PROPERTY_TYPE.INT, IFDConst.PROPERTY_UNIT.MHZ),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_2, IFDConst.PROPERTY_TYPE.INT, IFDConst.PROPERTY_UNIT.MHZ),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_3, IFDConst.PROPERTY_TYPE.INT, IFDConst.PROPERTY_UNIT.MHZ),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_1, IFDConst.PROPERTY_TYPE.NUCL, IFDConst.PROPERTY_UNIT.NONE), 
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_2, IFDConst.PROPERTY_TYPE.NUCL, IFDConst.PROPERTY_UNIT.NONE),
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_3, IFDConst.PROPERTY_TYPE.NUCL, IFDConst.PROPERTY_UNIT.NONE), 
				new IFDProperty(IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE, IFDConst.PROPERTY_TYPE.FLOAT, IFDConst.PROPERTY_UNIT.KELVIN) 
		});
	}
	
	public IFDNMRSpecData() throws IFDException {
		super(null, IFD_TYPE_SPEC_NMR);
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDNMRSpecDataRepresentation(ref, obj, len, type, subtype);

	}	
	
	@Override
	public void setPropertyValue(String name, Object value) {
		super.setPropertyValue(name, value);
	}


	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	protected IFDSpecData newInstance() throws IFDException {
		IFDSpecData d = new IFDNMRSpecData();
		d.setPropertyValue(IFD_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME, getPropertyValue(IFD_PROP_SPEC_NMR_INSTR_MANUFACTURER_NAME));
		return d;
	}

}
