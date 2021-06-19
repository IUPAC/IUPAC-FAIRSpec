package org.iupac.fairspec.common;

public class IFSConst {
	
	public final static String IFS_FAIRSpec_version = "0.0.1-2021_06_18";
	
	

	public enum PROPERTY_TYPE { INT, NUCL, OBJ };
	
	public enum PROPERTY_UNITS { NONE, MHZ };
	
	
	
	/**
	 * 	the files we want extracted -- no $ for cdx, as that could be cdxml
	 */
	public final static String defaultCachePattern = ""
			+ "\\.pdf$|\\.png$|"
			+ "\\.mol$|\\.sdf$|\\.cdx$|\\.cdxml$"
//			+ "\\.log$|\\.out$|\\.txt$|"// maybe put these into JSON only? 
			;

	
	
	/**
	 * Not sure about this one -- the idea is that we found a .mnova file within a 
	 * directory that needs to be rezipped. Ignore this as not valid?
	 */
 	

	public static final String IFS_SPEC_NMR_FREQ_1 = "IFS.spec.nmr.freq.1";
	public static final String IFS_SPEC_NMR_FREQ_2 = "IFS.spec.nmr.freq.2";
	public static final String IFS_SPEC_NMR_FREQ_3 = "IFS.spec.nmr.freq.3";
	public static final String IFS_SPEC_NMR_FREQ_4 = "IFS.spec.nmr.freq.4";

	public static final String IFS_SPEC_NMR_NUCL_1 = "IFS.spec.nmr.nucl.1";
	public static final String IFS_SPEC_NMR_NUCL_2 = "IFS.spec.nmr.nucl.2";
	public static final String IFS_SPEC_NMR_NUCL_3 = "IFS.spec.nmr.nucl.3";
	public static final String IFS_SPEC_NMR_NUCL_4 = "IFS.spec.nmr.nucl.4";
	public static final String IFS_SPEC_NMR_NOMINAL_SPECTROMETER_FREQ = "IFS.spec.nmr.freq.nominal";
	public static final String IFS_SPEC_NMR_EXPT_DIM = "IFS.spec.nmr.expt.dim";
	public static final String IFS_SPEC_NMR_EXPT_PULSE_PROG = "IFS.spec.nmr.expt.pulseprog";

		
}
