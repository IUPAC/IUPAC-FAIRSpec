package com.vendor.bruker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.iupac.fairspec.util.IFSDefaultVendorPlugin;

import javajs.util.Rdr;
import jspecview.source.JDXReader;

public class BrukerIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.vendor.bruker.BrukerIFSVendorPlugin.class);
	}

	// public final static String defaultRezipIgnorePattern = "\\.mnova$";

	private final static Map<String, String> ifsMap = new HashMap<>();

	static {
		// order here is not significant; keys without the JCAMP vendor prefix are
		// derived, not the value itself
		String[] keys = { //
				"DIM", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_DIM, //
				"##$BF1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_1, //
				"##$BF2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_2, //
				"##$BF3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_3, //
				"##$NUC1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_1, //
				"##$NUC2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_2, //
				"##$NUC3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_3, //
				"##$PULPROG", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_PULSE_PROG, //
				"##$TE", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_TEMPERATURE_K, //
				"SOLVENT", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_SOLVENT, //
				"SF", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, //
				"##$PROBHD", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_PROBEID, //
		};
		for (int i = 0; i < keys.length;)
			ifsMap.put(keys[i++], keys[i++]);
	}

	/**
	 * 1D, 2D, ...; this value cannot be determined directly from parameters (I
	 * think)
	 */
	private String dim;

	public BrukerIFSVendorPlugin() {
		// files of interest; procs is just for solvent
		// presence of acqu2s indicates a 2D experiment
		paramRegex = "acqus$|acqu2s$|procs$";
		// rezip triggers for procs in a directory (1, 2, 3...) below a pdata directory,
		// such as pdata/1/procs. We do not add the "/" before pdata, because that could
		// be the| symbol, and that will be attached by IFSDefaultVendorPlugin in
		// super.getRezipRegex()
		rezipRegex = "pdata/[^/]+/procs$";
	}

	/**
	 * Require an unsigned integer, and if that is not there, replace the directory
	 * name with "1".
	 */
	@Override
	public String getRezipPrefix(String dirName) {
		return (isUnsignedInteger(dirName) ? null : "1");
	}

	/**
	 * .mnova files will be extracted by the mestrelab plugin. They should not be
	 * left in the Bruker dataset.
	 */

	@Override
	public boolean doRezipInclude(String baseName, String entryName) {
		if (entryName.endsWith(".pdf"))
			addProperty(IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_SPECTRUM_PDF, baseName + entryName);
		else if (entryName.endsWith("thumb.png"))
			addProperty(IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_SPECTRUM_IMAGE, baseName + entryName);
		return !entryName.endsWith(".mnova");
	}

	@Override
	public boolean doExtract(String entryName) {
		return false;
	}

	@Override
	public void startRezip(IFSExtractorI extractor) {
		// we will need dim for setting 1D
		super.startRezip(extractor);
		dim = null;
	}

	@Override
	public void endRezip() {
		// if we found an acqu2s file, then dim has been set to 2D already.
		// NUC2 will be set already, but that might just involve decoupling, which we
		// don't generally indicate. So here we remove the NUC2 property if this is a 1D
		// experiment.
		if (dim == null) {
			report("DIM", "1D");
			report("##$NUC2", null);
		}
		dim = null;
		super.endRezip();
	}

	/**
	 * We use jspecview.source.JDXReader (in Jmol.jar) to pull out the header as a
	 * map.
	 * 
	 */
	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		super.accept(extractor, fname, bytes);
		Map<String, String> map = null;
		try {
			map = JDXReader.getHeaderMap(new ByteArrayInputStream(bytes), null);
			// System.out.println(map.toString().replace(',', '\n'));
		} catch (Exception e) {
			// invalid format
			e.printStackTrace();
			return false;
		}
		if (fname.indexOf("procs") >= 0) {
			report("SOLVENT", getSolvent(map));
			return true;
		}
		// no need to close a ByteArrayInputStream
		int ndim = 0;
		String nuc1;
		if ((nuc1 = processString(map, "##$NUC1", "off")) != null)
			ndim++;
		if ((processString(map, "##$NUC2", "off")) != null)
			ndim++;
		if (processString(map, "##$NUC3", "off") != null)
			ndim++;
		if (processString(map, "##$NUC4", "off") != null)
			ndim++;
		if (ndim == 0)
			return false;
		double freq1 = getDoubleValue(map, "##$BF1");
		if (ndim >= 2)
			getDoubleValue(map, "##$BF2");
		if (ndim >= 3)
			getDoubleValue(map, "##$BF3");
		if (ndim >= 4)
			getDoubleValue(map, "##$BF4");
		report("##$TE", getDoubleValue(map, "##$TE"));
		processString(map, "##$PULPROG", null);
		if (fname.endsWith("acqu2s")) {
			report("DIM", dim = "2D");
		}
		report("SF", getNominalFrequency(freq1, nuc1));
		processString(map, "##$PROBHD", null);
		if (extractor != null)
			this.extractor = null;
		return true;
	}

	/**
	 * Looking here for &lt;nucl.solvent&gt; in ##$SREGLST
	 * 
	 * @param map
	 * @return "CDCl3" or "DMSO", for example
	 */
	private Object getSolvent(Map<String, String> map) {
		String nuc_solv = getBrukerString(map, "##$SREGLST");
		int pt;
		return (nuc_solv != null && (pt = nuc_solv.indexOf(".")) >= 0 ? nuc_solv.substring(pt + 1) : null);
	}

	/**
	 * Extract a Bruker &lt;xxxx&gt; string
	 * 
	 * @param map
	 * @param key
	 * @param ignore a value such as "off" to disregard
	 * @return null if not found or empty
	 */
	private String processString(Map<String, String> map, String key, String ignore) {
		String val = getBrukerString(map, key);
		if (val != null && !val.equals(ignore))
			report(key, val);
		return val;
	}

	/**
	 * remove &lt; and &gt; from the string
	 * 
	 * @param map
	 * @param key
	 * @return null if the value is not of the form &lt;xxx&gt; or is empty &lt;&gt;
	 */
	private static String getBrukerString(Map<String, String> map, String key) {
		return getDelimitedString(map, key, '<', '>');
	}

	/**
	 * Report the found property back to the IFSExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	private void report(String key, Object val) {
		addProperty(ifsMap.get(key), val);
	}

////////// testing ///////////

	public static void main(String[] args) {
		test("test/cosy/acqus");
		test("test/cosy/acqu2s");
		test("test/cosy/procs");
		test("test/cosy/proc2s");
		test("test/13c/procs");
		test("test/13c/acqus");

	}

	private static void test(String fname) {
		IFSVendorPluginI.init();
		System.out.println("====================" + fname);
		try {
			String filename = new File(fname).getAbsolutePath();
			byte[] bytes = Rdr.getLimitedStreamBytes(new FileInputStream(filename), -1);
			new BrukerIFSVendorPlugin().accept(null, filename, bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getVendorName() {
		return "Bruker";
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

}