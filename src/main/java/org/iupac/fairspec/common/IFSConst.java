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



		
}
