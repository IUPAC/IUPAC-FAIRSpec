package com.integratedgraphics.ifd.vendor;

import java.util.HashMap;
import java.util.Map;

import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.util.DefaultVendorPlugin;

public abstract class NmrMLIFDVendorPlugin extends DefaultVendorPlugin {

	private static Map<String, String> ifdMap = new HashMap<>();

	static {
		String[] keys = { //
				"DIM", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_DIM"), //prop
				"F1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_1"), //prop
				"F2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_2"), //prop
				"F3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_3"), //prop
				"N1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_1"), //prop
				"N2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_2"), //prop
				"N3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_3"), //prop
				"PP", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_PULSE_PROG"), //prop
				"SOLVENT", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_SOLVENT"), //prop
				"SF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_FREQ_NOMINAL"), //prop
				"PROBE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_PROBE_TYPE"), //prop
				"TEMPERATURE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE"), //prop
		};

		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}
	
	/**
	 * Report the found property back to the IFDExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		addProperty(ifdMap.get(key), val);
	}

	protected void setParams(int dim, Acqu acq) {
		reportVendor();
		report("DIM", dim);
		double freq = acq.getTransmiterFreq();
		report("F1", freq);
 		String nuc = fixNucleus(acq.getObservedNucleus());
		report("N1", nuc);
		report("SF", getNominalFrequency(freq, nuc));
		report("SOLVENT", fixSolvent(acq.getSolvent()));
		report("TEMPERATURE", acq.getTemperature());
		report("PP", acq.getPulseProgram());
		report("PROBE", acq.getProbehead());
	}

}
