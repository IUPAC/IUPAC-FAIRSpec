package com.integratedgraphics.ifs.vendor;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifs.util.IFSDefaultVendorPlugin;

public abstract class NmrMLIFSVendorPlugin extends IFSDefaultVendorPlugin {

	private static Map<String, String> ifsMap = new HashMap<>();

	static {
		String[] keys = { //
				"DIM", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_DIM, //
				"F1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_1, //
				"F2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_2, //
				"F3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_3, //
				"N1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_1, //
				"N2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_2, //
				"N3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_3, //
				"PP", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_PULSE_PROG, //
				"SOLVENT", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_SOLVENT, //
				"SF", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, //
				"PROBE", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_PROBEID, //
				"TEMPERATURE", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_TEMPERATURE_K, //
		};

		for (int i = 0; i < keys.length;)
			ifsMap.put(keys[i++], keys[i++]);
	}
	
	/**
	 * Report the found property back to the IFSExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		addProperty(ifsMap.get(key), val);
	}

	protected void setParams(int dim, Acqu acq) {
		reportName();
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
