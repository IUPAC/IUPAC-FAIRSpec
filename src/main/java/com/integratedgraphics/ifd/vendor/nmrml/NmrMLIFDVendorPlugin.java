package com.integratedgraphics.ifd.vendor.nmrml;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;

public abstract class NmrMLIFDVendorPlugin extends NMRVendorPlugin {

	private static Map<String, String> ifdMap = new HashMap<>();

	static {
		String[] keys = { //
				"DIM", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_DIMENSION"), //prop
				"F1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_1"), //prop
				"F2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_2"), //prop
				"F3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_3"), //prop
				"N1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_1"), //prop
				"N2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_2"), //prop
				"N3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_3"), //prop
				"PP", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_PULSE_PROGRAM"), //prop
				"NF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_NOMINAL_FREQ"), //prop
				"PROBE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_PROBE_TYPE"), //prop
				"TEMPERATURE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_ABSOLUTE_TEMPERATURE"), //prop
				"TIMESTAMP", IFDConst.IFD_PROPERTY_DATAOBJECT_TIMESTAMP,
				"TITLE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_TITLE"), //prop
		};

		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}
	
	/**
	 * Report the found property back to the IFDMetadataReceiverI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		addProperty(ifdMap.get(key), val);
	}

	protected void setParams(NmrMLHeader header, Acqu acq) {
		reportVendor();
		report("DIM", header.getDimension() + "D");
		double freq = acq.getTransmiterFreq();
		report("F1", freq);
 		String nuc = fixNucleus(acq.getObservedNucleus());
		report("N1", nuc);
		report("NF", getNominalFrequency(freq, nuc));
		reportSolvent(acq.getSolvent());
		report("TEMPERATURE", acq.getTemperature());
		report("PP", acq.getPulseProgram());
		report("PROBE", acq.getProbehead());
		report("TIMESTAMP", header.getCreationTime());
		addProperty("Comment", header.getComment());
		addProperty("Title", header.getTitle());
		report("TITLE", header.getTitle());
	}

}
