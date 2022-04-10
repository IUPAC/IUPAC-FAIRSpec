package com.integratedgraphics.ifd.vendor.mestrelab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.extract.ExtractorI;

import com.integratedgraphics.ifd.Extractor;
import com.integratedgraphics.ifd.util.DefaultVendorPlugin;
import com.integratedgraphics.ifd.vendor.mestrelab.MNovaMetadataReader.Param;

import javajs.util.PT;

public class MestrelabIFDVendorPlugin extends DefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.mestrelab.MestrelabIFDVendorPlugin.class);
	}

	private static Map<String, String> ifdMap = new HashMap<>();

	private Map<String, Object> params;

	private int page = 0;

	private String ifdPath;

	static {
		String[] keys = { //
				"Pulse Sequence", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_PULSE_PROG"), //prop
				"Solvent", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_SOLVENT"), //prop
				"Probe", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_INSTR_PROBE_TYPE"), //prop
				"Temperature", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE"), //prop
				"Experiment", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_DIM"), //prop
				"Title", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_TITLE"), //prop
				"F1", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_1"), //prop
				"F2", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_2"), //prop
				"F3", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_3"), //prop
				"N1", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_1"), //prop
				"N2", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_2"), //prop
				"N3", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_3"), //prop
				"SF", getProp("IFD_PROP_DATAOBJECT_FAIRSPEC_NMR_INSTR_FREQ_NOMINAL"), //prop
		};

		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}

	public MestrelabIFDVendorPlugin() {
		paramRegex = "\\.mnova[^/]*$";
	}

	@Override
	public String accept(ExtractorI extractor, String ifdPath, byte[] bytes) {
		super.accept(extractor, ifdPath, bytes);
		MNovaMetadataReader reader;
		try {
			page = 0;
			params = null;
			pageList = new ArrayList<>();
			reader = new MNovaMetadataReader(this, bytes);
			this.ifdPath = ifdPath;

			// extractor.registerFileVendor(zipName, this);
			if (reader.process()) {
				addParams();
				close();
				return processRepresentation(null, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getVendorName() {
		return (origin == null ? "" : origin + "/") + "Mestrelab";
	}

	@Override
	public String processRepresentation(String ifdPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

	// called from MNovaReader

	private boolean isJDF;

	private String nuc1;

	private String nuc2;

	private double freq;

	private String mnovaVersion;

	private String pngcss;

	private String origin;

	public void addParam(String key, Object oval, Param param1, Param param2) {
		if (param1 != null)
			oval = (param1.value == null || param1.value.length() == 0 ? param1.calc : param1.value);
		String key0 = key;
		if (oval instanceof String) {
			String val = (String) oval;
			if (oval == null || val.length() == 0 || val.charAt(0) == '=') {
				return;
			}
			oval = val = val.trim();
			try {
				switch (key) {
				case "Owner":
					// skipping
					return;
				case Extractor.PNG_FILE_DATA + ":css":
					pngcss = val;
					return;
				case "Origin":
					oval = origin = PT.rep(val, "\n", " ").trim();
					int pt = origin.indexOf(" ");
					if (pt >= 0)
						origin = origin.substring(0, pt);
					break;
				case "Acquisition Date":
				case "Author":
				case "Comment":
				case "Experiment":
				case "Modification Date":
				case "Pulse Sequence":
				case "Site":
				case "Solvent":
				case "Title":
				case "Class":
				case "Presaturation Frequency":
				case "Probe":
				default:
					oval = PT.rep(val, "\n", " ").trim();
					break;
				case "Data File Name":
					isJDF = (val.endsWith(".jdf"));
					return;
				case "Instrument":
				case "Spectrometer":
					break;
				case "Temperature":
					double d = Double.parseDouble(val);
					if (isJDF) {
						// JDF temp is oC not K from MNOVA
						d += 273.15;
					}
					oval = Double.valueOf(d);
					break;
				case "Nucleus":
					key = "N1";
					nuc1 = val;
					if (param2 != null) {
						params.put("N2", param2.value);
					}
					break;
				case "Spectrometer Frequency":
					key = "F1";
					freq = Double.parseDouble(val);
					oval = Double.valueOf(freq);
					if (param2 != null) {
						params.put("F2", Double.valueOf(param2.value));
					}
					break;
				case "Pulse Width":
				case "Spectral Width":
				case "Receiver Gain":
				case "Purity":
				case "Relaxation Delay":
				case "Spectrum Quality":
				case "Lowest Frequency":
				case "Acquisition Time":
					oval = Double.valueOf(Double.parseDouble(val));
					break;
				case "Number of Scans":
				case "Acquired Size":
				case "Spectral Size":
					oval = Integer.valueOf(Integer.parseInt(val));
					break;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if (oval != null) {
			switch (key) {
			case Extractor.PNG_FILE_DATA:
				oval = new Object[] { oval, ifdPath + "#page" + page + ".png", pngcss };
				break;
			case Extractor.CDX_FILE_DATA:
				oval = new Object[] { oval, ifdPath + "#page" + page + ".cdx", null };
				break;
			case Extractor.MOL_FILE_DATA:
				oval = new Object[] { oval, ifdPath + "#page" + page + ".mol", null };
				break;
			}
			params.put(key, oval);
			System.out.println("----------- page " + page + " " + key + " = " + oval + " was " + key0 + " " + param1
					+ (param2 == null ? "" : "/ " + param2));
		}
	}

	List<Map<String, Object>> pageList;

	void newPage(int page) {
		this.page = page;
		finalizeParams();
		params = new LinkedHashMap<>();
		params.put(Extractor.NEW_FAIRSPEC_KEY, "_page=" + page);
		params.put("mnovaVersion", mnovaVersion);
		origin = null;
		pageList.add(params);
		System.out.println("MestrelabIFDVendor ------------ page " + page);
		return;
	}

	int getPage() {
		return page;
	}

	// private

	/**
	 * 
	 */
	private void addParams() {
		int nPages = pageList.size();
		if (nPages == 0)
			return;
		finalizeParams();
		boolean sendNewPage = (nPages > 1);
		for (int i = 0; i < nPages; i++) {
			processSpec(pageList.get(i), sendNewPage);
		}
	}

	private void processSpec(Map<String, Object> params, boolean sendNewPage) {
		reportVendor();
		for (Entry<String, Object> p : params.entrySet()) {
			String key = p.getKey();
			if (key.startsWith("_") ? key.startsWith(Extractor.STRUC_FILE_DATA_KEY)
					: sendNewPage || !key.equals(Extractor.NEW_FAIRSPEC_KEY))
				report(key, p.getValue());
		}
	}

	/**
	 * Report the found property back to the IFDExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		String k = ifdMap.get(key);
		addProperty(k == null ? key : k, val);
	}

	private void close() {
		System.out.println("MestreLabIFDVendorPlugin done " + page + " pages for " + ifdPath 
				+ "\n=============================================\n");
		finalizeParams();
		page = 0;
	}

	private void finalizeParams() {
		if (params != null && freq != 0) {
			double f = getNominalFrequency(freq, nuc1);
			System.out.println("nom freq " + f + " for " + nuc1 + " " + nuc2 + " " + freq);
			params.put("SF", f);
		}
		params = null;
		isJDF = false;
		freq = 0;
		nuc1 = nuc2 = null;
		mnovaVersion = null;
	}

	void setVersion(String mnovaVersion) {
		this.mnovaVersion = mnovaVersion;
	}

}