package com.vendor.bruker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.core.IFSObject;

import javajs.util.Rdr;
import jspecview.source.JDXDataObject;
import jspecview.source.JDXReader;

public class BrukerIFSVendorPlugin implements IFSVendorPluginI {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.bruker.BrukerIFSVendorPlugin.class);
	}

	private IFSExtractorI extractor;
	private String dim;

	public BrukerIFSVendorPlugin() {

	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getRezipRegex() {
		return "^(?<path>.+(?:/|\\|)(?<dir>[^/]+)(?:/|\\|))pdata/[^/]+/procs$";
	}

	@Override
	public String getParamRegex() {
		return "acqus$|acqu2s$";
	}

	@Override
	public String getRezipPrefix(String dirName) {
		return (isNumeric(dirName) ? null : "1");
	}

	@Override
	public boolean doRezipInclude(String entryName) {
		return !entryName.endsWith(".mnova");
	}

	//	public final static String defaultRezipIgnorePattern = "\\.mnova$";

	
	private final static Map<String, String> ifsMap = new HashMap<>();
	
	static {
		String[] keys = {
				"##$PULPROG", IFSConst.IFS_SPEC_NMR_PULSE_PROG,
				"##$BF1", IFSConst.IFS_SPEC_NMR_FREQ_1,
				"##$BF2", IFSConst.IFS_SPEC_NMR_FREQ_2,
				"##$BF3", IFSConst.IFS_SPEC_NMR_FREQ_3,
				"##$BF4", IFSConst.IFS_SPEC_NMR_FREQ_4,
				"##$NUC1", IFSConst.IFS_SPEC_NMR_NUCL_1,
				"##$NUC2", IFSConst.IFS_SPEC_NMR_NUCL_2,
				"##$NUC3", IFSConst.IFS_SPEC_NMR_NUCL_3,
				"##$NUC4", IFSConst.IFS_SPEC_NMR_NUCL_4,
				"DIM", IFSConst.IFS_SPEC_NMR_EXPT_DIM,
				"SF", IFSConst.IFS_SPEC_NMR_NOMINAL_SPECTROMETER_FREQ,

				
		};
		for (int i = 0; i < keys.length;)
			ifsMap.put(keys[i++], keys[i++]);
	}
	

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		if (extractor != null)
			this.extractor = extractor;
		Map<String, String> map = null;
		try {
			map = JDXReader.getHeaderMap(new ByteArrayInputStream(bytes), null);
		} catch (Exception e) {
			// invalid format
			e.printStackTrace();
			return false;
		}
		// no need to close a ByteArrayInputStream
//		System.out.println(map.toString().replace(',', '\n'));
		int ndim = 0;
		String nuc1, nuc2;
		if ((nuc1 = processString(map, "##$NUC1", "<off>")) != null)
			ndim++;
		if ((nuc2 = processString(map, "##$NUC2", "<off>")) != null)
			ndim++;
		if (processString(map, "##$NUC3", "<off>") != null)
			ndim++;
		if (processString(map, "##$NUC4", "<off>") != null)
			ndim++;
		if (ndim == 0)
			return false;
		double freq1 = processFreq(map, "##$BF1");
		if (ndim >= 2)
			processFreq(map, "##$BF2");
		if (ndim >= 3)
			processFreq(map, "##$BF3");
		if (ndim >= 4)
			processFreq(map, "##$BF4");
		processString(map, "##$PULPROG", null);
		if (fname.endsWith("acqu2s")) {
			report("DIM", dim = "2D");
		}
		report("SF", getNominalSpectrometerFrequency(nuc1, freq1));
		if (extractor != null)
			this.extractor = null;
		return true;
	}

	private int getNominalSpectrometerFrequency(String nuc1, double freq1) {
		return JDXDataObject.getNominalSpecFreq(nuc1, freq1);
	}

	private String processString(Map<String, String> map, String key, String ignore) {
		String nuc = map.get(key);
		if (nuc == null || nuc.equals(ignore) || nuc.length() < 3)
			return null;
		nuc = nuc.substring(1, nuc.length() - 1); // remove <  >
		if (nuc.length() > 0)
			report(key, nuc);
		return nuc;
	}

	private double processFreq(Map<String, String> map, String key) {
		String f = map.get(key);
		if (f == null)
			return Double.NaN;
		double freq = Double.valueOf(f);
//		report(ifsMap.get(key), freq);
		return freq;
	}

	private void report(String key, Object val) {
		key = ifsMap.get(key);
		System.out.println(key + " = " + val);
		if (extractor != null)
			extractor.addParam(key, val);
	}

	/**
	 * Only allow numerical top directories.
	 * 
	 * @param s
	 * @return
	 */
	private static boolean isNumeric(String s) {
		// I just don't like to fire exceptions.
		for (int i = s.length(); --i >= 0;)
			if (!Character.isDigit(s.charAt(i)))
				return false;
		return true;
	}

	@Override
	public void startRezip(IFSExtractorI extractor) {
		this.extractor = extractor;
		dim = null;
	}

	@Override
	public void endRezip() {
		if (dim == null) {
			report("DIM", "1D");
			report("##$NUC2", null);
		}
		dim = null;
		extractor = null;
	}

	
////////// testing ///////////
	
	public static void main(String[] args) {
		test("test/cosy/acqus");
		test("test/cosy/acqu2s");
//		test("test/cosy/procs");
//		test("test/cosy/proc2s");
//		test("test/13c/procs");
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



}