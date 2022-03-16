package com.integratedgraphics.ifd.vendor;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.spec.nmr.IFDNMRSpecData;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.util.IFDDefaultVendorPlugin;

public abstract class NmrMLIFDVendorPlugin extends IFDDefaultVendorPlugin {

	private static Map<String, String> ifdMap = new HashMap<>();

	static {
		String[] keys = { //
				"DIM", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_DIM, //
				"F1", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_1, //
				"F2", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_2, //
				"F3", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_3, //
				"N1", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_1, //
				"N2", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_2, //
				"N3", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_3, //
				"PP", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_PULSE_PROG, //
				"SOLVENT", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_SOLVENT, //
				"SF", IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, //
				"PROBE", IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_PROBE_TYPE, //
				"TEMPERATURE", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE, //
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
