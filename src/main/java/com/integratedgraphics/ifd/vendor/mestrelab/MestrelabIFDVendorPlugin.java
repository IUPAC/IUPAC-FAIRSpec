package com.integratedgraphics.ifd.vendor.mestrelab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.ExtractorI;

import com.integratedgraphics.ifd.Extractor;
import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;
import com.integratedgraphics.ifd.vendor.mestrelab.MNovaMetadataReader.Param;

public class MestrelabIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.mestrelab.MestrelabIFDVendorPlugin.class);
	}

	private static Map<String, String> ifdMap = new HashMap<>();

	// called from MNovaReader

	private String mnovaVersion;

	private class Globals {
		
		private String pngcss;
		private boolean isJDF;
		private String nuc1;
		private double freq;
		private String origin;
		public int dim = 1;

		private String setOrigin(String val) {
			origin = FAIRSpecUtilities.rep(val, "\n", " ").trim();
			int pt = origin.indexOf(" ");
			if (pt >= 0)
				origin = origin.substring(0, pt);
			return origin;
		}
	}
	
	private Globals pageGlobals = new Globals();
	
	private Map<String, Object> params;

	private int page = 0;

	private String originPath;

	/**
	 * each page maintains its own set of data to pass back to the extractor
	 */
	private List<Map<String, Object>> pageList;

	static {
		String[] keys = { //
				"Pulse Sequence", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_PULSE_PROG"), //prop
				"Solvent", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_SOLVENT"), //prop
				"Probe", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_PROBE_TYPE"), //prop
				"Temperature", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_ABSOLUTE_TEMPERATURE"), //prop
				"DIM", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_DIM"), //prop
				"TITLE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_TITLE"), //prop
				"F1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_1"), //prop
				"F2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_2"), //prop
				"F3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_FREQ_3"), //prop
				"N1", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_1"), //prop
				"N2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_2"), //prop
				"N3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_EXPT_NUCL_3"), //prop
				"SF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_NOMINAL_FREQ"), //prop
				"TIMESTAMP", IFDConst.IFD_PROPERTY_DATAOBJECT_TIMESTAMP, //prop
		};

		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}

	public MestrelabIFDVendorPlugin() {
		paramRegex = "\\.mnova[^/]*$";
	}

	@Override
	public String accept(ExtractorI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		MNovaMetadataReader reader;
		try {
			page = 0;
			params = null;
			pageList = new ArrayList<>();
			reader = new MNovaMetadataReader(this, bytes);
			this.originPath = originPath;

			// extract structure files (CDX, CDXML, and MOL) and spectral metadata

			boolean haveMetadata = reader.process();
			if (page > 0 && haveMetadata) {
				// After processing the full file, we need to
				// send all the metadata for each spectrum page.
				finalizeParams();
				int nPages = pageList.size();
				boolean sendNewPage = (nPages > 1);
				for (int i = 0; i < nPages; i++) {
					Map<String, Object> params = pageList.get(i);
					reportVendor(); // really? Before start of pages? 
					for (Entry<String, Object> p : params.entrySet()) {
						String key = p.getKey();
						boolean isNewPage = key.equals(Extractor.NEW_PAGE_KEY);
						boolean isSpecialKey = key.startsWith("_");
						// the only special key we send 
						if (isSpecialKey ? key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)
								: sendNewPage || !isNewPage)
							report(key, p.getValue());
					}
				}
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
		return (pageGlobals.origin == null ? "" : pageGlobals.origin + "/") + "Mestrelab";
	}

	@Override
	public String processRepresentation(String originPath, byte[] bytes) {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}


	/**
	 * Handle the parameters coming from the reader. 
	 * 
	 * @param key
	 * @param oval
	 * @param param1
	 * @param param2
	 */
	void addParam(String key, Object oval, Param param1, Param param2) {
		if (param1 != null)
			oval = (param1.value == null || param1.value.length() == 0 ? param1.calc : param1.value);
		String key0 = key;
		String propName = null;
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
				case DefaultStructureHelper.PNG_FILE_DATA + ":css":
					pageGlobals.pngcss = val;
					return;
				case "Origin":
					oval = pageGlobals.setOrigin(val);
					break;
				case "Acquisition Date":
					// timestamp
					propName = "TIMESTAMP";
					oval = FAIRSpecUtilities.rep(val, "\n", " ").trim();
					break;
				case "Comment":
					propName = "TITLE";
					//$FALL-THROUGH$
				case "Author":
				case "Class":
				case "Experiment":
				case "Presaturation Frequency":
				case "Probe":
				case "Modification Date":
				case "Pulse Sequence":
				case "Site":
				case "Solvent":
				case "Title":
				default:
					oval = FAIRSpecUtilities.rep(val, "\n", " ").trim();
					break;
				case "Data File Name":
					pageGlobals.isJDF = (val.endsWith(".jdf"));
					return;
				case "Instrument":
				case "Spectrometer":
					break;
				case "Temperature":
					double d = Double.parseDouble(val);
					if (pageGlobals.isJDF) {
						// JDF temp is oC not K from MNOVA
						d += 273.15;
					}
					oval = Double.valueOf(d);
					break;
				case "Nucleus":
					key = "N1";
					pageGlobals.nuc1 = val;
					if (param2 != null) {
						params.put("N2", param2.value);
					}
					break;
				case "Purity":
				case "Spectrometer Frequency":
					key = "F1";
					pageGlobals.freq = Double.parseDouble(val);
					oval = Double.valueOf(pageGlobals.freq);
					if (param2 != null) {
						params.put("F2", Double.valueOf(param2.value));
					}
					break;
				case "Spectrum Quality":
					double q = Double.parseDouble(val);
					if (q != 0)
						oval = Double.valueOf(q);
					break;
				case "Pulse Width":
				case "Spectral Width":
				case "Receiver Gain":
				case "Relaxation Delay":
				case "Lowest Frequency":
				case "Acquisition Time":
					oval = Double.valueOf(Double.parseDouble(val));
					break;
				case "Spectral Size":
					oval = Integer.valueOf(Integer.parseInt(val));
					if (param2 != null)
						pageGlobals.dim = 2;
					break;
				case "Number of Scans":
				case "Acquired Size":
					oval = Integer.valueOf(Integer.parseInt(val));
					break;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if (oval == null)
			return;
		switch (key) {
		case DefaultStructureHelper.PNG_FILE_DATA:
			oval = new Object[] { oval, originPath + "#page" + page + ".png", pageGlobals.pngcss };
			break;
		case DefaultStructureHelper.CDX_FILE_DATA:
			oval = new Object[] { oval, originPath + "#page" + page + ".cdx", null };
			break;
		case DefaultStructureHelper.CDXML_FILE_DATA:
			oval = new Object[] { oval, originPath + "#page" + page + ".cdxml", null };
			break;
		case DefaultStructureHelper.MOL_FILE_DATA:
			oval = new Object[] { oval, originPath + "#page" + page + ".mol", null };
			break;
		}
		if (propName != null)
			params.put(ifdMap.get(propName), oval);
		params.put(key, oval);
		System.out.println("----------- MNova page " + page + " " + key + " = " + oval + " was " + key0 + " " + param1
				+ (param2 == null ? "" : "/ " + param2));
	}

	
	/**
	 * Each page in the document that has a spectum reports here
	 * that a page has started and that we need to track structures
	 * and metadata for a new (potential) association.
	 *  
	 * @param page
	 */
	void newPage(int page) {
		this.page = page;
		finalizeParams();
		// the reader will be filling in params
		params = new LinkedHashMap<>();
		params.put(Extractor.NEW_PAGE_KEY, "_page=" + page);
		pageGlobals = new Globals();
		pageList.add(params);
		System.out.println("MestrelabIFDVendor ------------ page " + page);
	}

	int getPage() {
		return page;
	}

	// private

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
		System.out.println("MestreLabIFDVendorPlugin done " + page + " pages for " + originPath 
				+ "\n=============================================\n");
		finalizeParams();
		page = 0;
	}

	private void finalizeParams() {
		if (params != null && pageGlobals.freq != 0) {
			int f = getNominalFrequency(pageGlobals.freq, pageGlobals.nuc1);
			params.put("SF", Double.valueOf(f));
			params.put("DIM", Integer.valueOf(pageGlobals.dim) + "D");
			params.put("mnovaVersion", mnovaVersion);
		} 
		params = null;
	}

	void setVersion(String mnovaVersion) {
		this.mnovaVersion = mnovaVersion;
	}

}