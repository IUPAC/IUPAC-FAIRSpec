package com.integratedgraphics.ifd.vendor.bruker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.extractor.ExtractorUtils.DoubleString;
import com.integratedgraphics.ifd.api.VendorPluginI;
import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;

import jspecview.source.JDXReader;

public class BrukerIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.bruker.BrukerIFDVendorPlugin.class);
	}

	// public final static String defaultRezipIgnorePattern = "\\.mnova$";

	private final static Map<String, String> ifdMap = new HashMap<>();

	static {
		// order here is not significant; keys without the JCAMP vendor prefix are
		// derived, not the value itself
		String[] keys = { //
				"DIM", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_DIMENSION"), //prop
				"##$SFO1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_1"), //prop
				"##$SFO2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_2"), //prop
				"##$SFO3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_3"), //prop
				"##$NUC1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_1"), //prop
				"##$NUC2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_2"), //prop
				"##$NUC3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_3"), //prop
				"##$EXP", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_ID"),
				"##$PULPROG", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_PULSE_PROGRAM"), //prop
				"##$TE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_THERMODYNAMIC_TEMPERATURE"), //prop
				"TITLE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_TITLE"), //prop
				"NF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_NOMINAL_FREQ"), //prop
				"PF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_PROTON_FREQ"), //prop
				"##$PROBHD", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_PROBE_TYPE"), //prop
				"TIMESTAMP", getProp("IFD_PROPERTY_DATAOBJECT.TIMESTAMP"), // prop
				"PROC_TIMESTAMP", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.PROC_TIMESTAMP"), // prop
		};
		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}

	private class Globals {
		
		/**
		 * 1D, 2D, ...; this value cannot be determined directly from parameters (I
		 * think)
		 */
		String dim;
		String nuc1, nuc2;
		String probeHead;
		String solvent;
		Stack<Object> originCache = new Stack<>();

		public void clear() {
			dim = null;
			nuc1 = nuc2 = null;
			probeHead = null;
			flushCache();
			originCache = new Stack<>();
		}

		public void flushCache() {
			if (originCache != null) {
				while (!originCache.isEmpty()) {
					readBrukerParameterFileJDX((String) originCache.remove(0), (byte[]) spec.originCache.remove(0), true);
				}
				originCache = null;
			}
		}


	}	
	
	private Globals spec;
	
	public BrukerIFDVendorPlugin() {
		super();
		spec = new Globals();
		// files of interest; procs is just for solvent
		// presence of acqu2s indicates a 2D experiment
		paramRegex += "|procs$|acqu2s$|acqus$|title$|audita.txt$|auditp.txt$";
		// rezip triggers for procs in a directory (1, 2, 3...) below a pdata directory,
		// such as pdata/1/procs. We do not add the "/" before pdata, because that could
		// be the| symbol, and that will be attached by IFDDefaultVendorPlugin in
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

	private static String IMAGE = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.SPECTRUM_IMAGE");
	private static String PDF = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.SPECTRUM_DOCUMENT");

	@Override
	public Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName) {
		boolean isImage = entryName.endsWith("thumb.png");
		String type = (entryName.endsWith(".pdf") ? PDF : isImage ? IMAGE : null);
		return (type == null ? null : new Object[] { type, (isImage ? Boolean.TRUE : null) });
	}

	@Override
	public boolean doRezipInclude(MetadataReceiverI extractor, String baseName, String entryName) {
		return !entryName.endsWith(".mnova");
	}

	@Override
	public boolean doExtract(String entryName) {
		return false;
	}

	@Override
	public void initializeDataSet(MetadataReceiverI extractor) {
		// we will need dim for setting 1D
		super.initializeDataSet(extractor);
		spec = new Globals();
	}

	/**
	 * called when rezip is complete
	 */
	@Override
	public void endDataSet() {
		// if we found an acqu2s file, then dim has been set to 2D already.
		// NUC2 will be set already, but that might just involve decoupling, which we
		// don't generally indicate. So here we remove the NUC2 property if this is a 1D
		// experiment.
		if (spec.dim == null) {
			report("DIM", "1D");
			report("##$NUC2", IFDProperty.NULL);
		}
		spec.clear();
		super.endDataSet();
	}

	/**
	 * We use jspecview.source.JDXReader (in Jmol.jar) to pull out the header as a
	 * map.
	 * 
	 */
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		return (readBrukerParameterFileJDX(originPath, bytes, false) ? getVendorDataSetKey() : null);
	} 

	/**
	 * reading title, procs, audita.txt, auditp.txt, acqus, acqu2s
	 * 
	 * @param originPath
	 * @param bytes
	 * @return
	 */
	private boolean readBrukerParameterFileJDX(String originPath, byte[] bytes, boolean isFlush) {

		boolean isAcqus = originPath.endsWith("acqus");
		if (!isAcqus && !isFlush && spec.originCache != null) {
			spec.originCache.push(originPath);
			spec.originCache.push(bytes);
			return true;
		}

		if (originPath.endsWith("title")) {
			report("TITLE", new String(bytes));
			return true;
		}
		Map<String, String> map = null;
		try {
			map = JDXReader.getHeaderMap(new ByteArrayInputStream(bytes), null);
		} catch (Exception e) {
			// invalid format
			e.printStackTrace();
			return false;
		}
		if (originPath.endsWith("procs")) {
			// this will be processed after acqus
			// solvent in procs overrides solvent in acqu or acqus
			// case acs.joc.0c0070|22567817 SREGLIST <13C.CDCl3> but ACQUS is acetone??
			// 2025.08.14 but SREGLST is just the scaling region file. ACQUS should be
			// correct
			Object solvent = getSolvent(map);
			if (solvent != null && spec.solvent == null) {
				System.out.println(
						"??Bruker ACQUS solvent not found -- using procs.SREGLIST " + solvent + " " + originPath);
				spec.solvent = (String) solvent;
//				reportSolvent(IFDProperty.NULL); // this will clear the
				reportSolvent(spec.solvent);
			}
		return true;
	}
		boolean isProc = false;
		if (originPath.indexOf("audita.txt") >= 0 || (isProc = originPath.indexOf("auditp.txt") >= 0)) {
			return processAudit(map, isProc);
		}
		if (isAcqus) {
			return processAcqus(map);
		}
		if (originPath.endsWith("acqu2s")) {
			report("DIM", spec.dim = "2D");
			return true;
		}
	    System.out.println("Bruker?? " + originPath);		
		return false;
	}

	private boolean processAudit(Map<String, String> map, boolean isProc) {
		String timestamp = map.get("##AUDITTRAIL");
		if (timestamp != null) {
			String[] data = timestamp.split("\\(");
			if (data.length > 1)
				data = data[data.length - 1].split("<");
			try {
				if (data.length > 1) {
					timestamp = data[1];
					timestamp = timestamp.substring(0, data[1].indexOf(">")).trim();
					int pt = Math.max(timestamp.indexOf("+"), timestamp.lastIndexOf("-"));
					String off = "";
					if (pt > 10) {
						off = "00000" + timestamp.substring(pt + 1);
						int len = off.length();
						off = timestamp.substring(pt, pt + 1) + off.substring(len - 4, len - 2) + ":"
								+ off.substring(len - 2);
					}
					timestamp = timestamp.substring(0, 10) + "T" + timestamp.substring(11, 19) + off;
					ZonedDateTime d = ZonedDateTime.parse(timestamp);
					addProperty(ifdMap.get(isProc ? "PROC_TIMESTAMP" : "TIMESTAMP"), d.toString());
				}
				return true;
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		return false;
	}

	private boolean processAcqus(Map<String, String> map) {
		String date = map.get("##$DATE");
		if (date != null)
			dataObjectLongID = Long.valueOf(date);
		if (spec.solvent == null) {
			// procs must have been processed already
			spec.solvent = processString(map, "##$SOLVENT", null);
		}
		// no need to close a ByteArrayInputStream
		int ndim = 0;
		// some of this can be decoupling, though.
		if ((spec.nuc1 == null ? (spec.nuc1 = processString(map, "##$NUC1", "off")) : spec.nuc1) != null)
			ndim = 1;
		if ((spec.nuc2 = processString(map, "##$NUC2", "off")) != null)
			ndim = 2;
		if (processString(map, "##$NUC3", "off") != null)
			ndim = 3;
		if (processString(map, "##$NUC4", "off") != null)
			ndim = 4;
		if (ndim == 0)
			return false;
		// offset frequencies BF1+O1
		report("##$SFO1", getDoubleValue(map, "##$SFO1"));
		if (ndim >= 2)
			report("##$SFO2", getDoubleValue(map, "##$SFO2"));
		if (ndim >= 3)
			report("##$SFO3", getDoubleValue(map, "##$SFO3"));
		DoubleString bf1 = new DoubleString((String) map.get("##$BF1"));
		DoubleString bf2 = new DoubleString((String) map.get("##$BF2"));
		report("PF", getProtonFrequency(bf1, spec.nuc1, bf2, spec.nuc2));
		report("NF", getNominalFrequency(bf1, spec.nuc1));
		report("##$TE", getDoubleValue(map, "##$TE"));
		processString(map, "##$PULPROG", null);
		processString(map, "##$EXP", null);
//		report("DIM", spec.dim = "1D");
		if (spec.probeHead == null) {
			spec.probeHead = processString(map, "##$PROBHD", null);
		}
		spec.flushCache();
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
		if (val == null || val.equals(ignore))
			return null;
		if (key.equals("##$SOLVENT")) {
		    reportSolvent(val);	
		} else {
			report(key, val);
		}
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
	 * Report the found property back to the IFDMetadataReceiverI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	private void report(String key, Object val) {
		addProperty(ifdMap.get(key), val);
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

	private static void test(String originPath) {
		VendorPluginI.init();
		System.out.println("====================" + originPath);
		try {
			String filename = new File(originPath).getAbsolutePath();
			byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			new BrukerIFDVendorPlugin().accept(null, filename, bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getVendorName() {
		return "Bruker";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

}