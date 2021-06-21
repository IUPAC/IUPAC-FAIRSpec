package org.iupac.fairspec.common;

public class IFSConst {
	
	public static final String IFS_FAIRSpec_version = "0.0.1-2021_06_18";

	public enum PROPERTY_TYPE { INT, FLOAT, STRING, NUCL, OBJ };
	
	public enum PROPERTY_UNITS { NONE, MHZ };
	
	

	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = "(MACOSX)|(desktop\\.ini)|(\\.DS_Store)";

	/**
	 * 	the files we want extracted -- just PDF and PNG here; all others are taken care of by
	 *  individual IFSVendorPluginI classes
	 */
	public static final String defaultCachePattern = ""
			+ "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
			;
 	
	public static boolean isRepresentation(String param) {
		return (param.indexOf(".representation.") >= 0);
	}

	// These lists will grow substantially. Those that are not just strings have their definitions declared in 
	// the specified classes:
	

	// core.IFStructure
	
	public static final String IFS_STRUCTURE_PROP_COMPOUND_ID  = "IFS.structure.property.compound.id";
	public static final String IFS_STRUCTURE_PROP_SMILES       = "IFS.structure.property.smiles";
	public static final String IFS_STRUCTURE_PROP_INCHI        = "IFS.structure.property.inchi";
	public static final String IFS_STRUCTURE_PROP_INCHIKEY     = "IFS.structure.property.inchikey";
	

	
	// core.IFSNMRSpecData
	
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
