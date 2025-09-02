package com.integratedgraphics.ifd.vendor.mestrelab;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.DefaultStructureHelper.StructureData;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.extractor.IFDExtractor;
import com.integratedgraphics.ifd.util.VendorUtils;
import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;
import com.integratedgraphics.ifd.vendor.mestrelab.MNovaMetadataReader.Param;

public class MestrelabIFDVendorPlugin extends NMRVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.mestrelab.MestrelabIFDVendorPlugin.class);
	}

	private static Map<String, String> ifdMap = new HashMap<>();

	// called from MNovaReader

	private String mnovaVersion;

	@SuppressWarnings("serial")
	private static class PageGlobals extends LinkedHashMap<String, Object> {
		
		private String pngcss; // For what??
		private boolean isJDF; 
		private String nuc1;
		private VendorUtils.DoubleString freq;
		private String origin;
		public int dim = 1;
		public long localTimeID;

		private String setOrigin(String val) {
			origin = FAIRSpecUtilities.rep(val, "\n", " ").trim();
			int pt = origin.indexOf(" ");
			if (pt >= 0)
				origin = origin.substring(0, pt);
			return origin;
		}
	}

	private PageGlobals pageGlobals;
	
	private int page = 0;

	private String originPath;

	/**
	 * each page maintains its own set of data to pass back to the extractor
	 */
	private List<PageGlobals> pageList;
	

	static {
		String[] keys = { //
				"Pulse Sequence", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_PULSE_PROGRAM"), //prop
				"Probe", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_PROBE_TYPE"), //prop
				"Temperature", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_THERMODYNAMIC_TEMPERATURE"), //prop
				"DIM", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_DIMENSION"), //prop
				"TITLE", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_TITLE"), //prop
				"Spectrometer Frequency", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_1"), //prop
				"Spectrometer Frequency2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_2"), //prop
				"Spectrometer Frequency3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_OFFSET_FREQ_3"), //prop
				"Nucleus", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_1"), //prop
				"Nucleus2", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_2"), //prop
				"Nucleus3", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_NUCL_3"), //prop
				"NF", getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_NOMINAL_FREQ"), //prop
				"TIMESTAMP", IFDConst.IFD_PROPERTY_DATAOBJECT_TIMESTAMP, //prop
		};

		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}

	public MestrelabIFDVendorPlugin() {
		paramRegex = "\\.mnova[^/]*$";
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		MNovaMetadataReader reader;
		try {
			page = 0;
			pageGlobals = null;
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
					PageGlobals pageGlobals = pageList.get(i);
					reportVendor(); // really? Before start of pages? 
					for (Entry<String, Object> p : pageGlobals.entrySet()) {
						String key = p.getKey();
						boolean isNewPage = key.equals(MetadataReceiverI.DeferredProperty.NEW_PAGE_KEY);
						boolean isSpecialKey = key.startsWith("_");
						// the only special key we send 
						if (isSpecialKey ? key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)
								: sendNewPage || !isNewPage)
							report(key, p.getValue());
						if (isNewPage && pageGlobals.localTimeID != 0) {
							setDataObjectLocalTimeID(pageGlobals.localTimeID);
							addSpecDateTimeIDs();
						}							
					}
				}
				close();
				report(MetadataReceiverI.DeferredProperty.NEW_PAGE_KEY, IFDProperty.NULL);
				return getVendorDataSetKey();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getVendorName() {
		return (pageGlobals == null || pageGlobals.origin == null ? "" : pageGlobals.origin + "/") + "Mestrelab";
	}

	@Override
	public String getVendorDataSetKey() {
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
					// unfortunately, this date is not GMT. 
					// Bruker and JDX from Mestrelab are GMT.
					// 2022-07-23T17:32:00
					// same as jdx .longdate (which Bruker does not include)
					// timestamp from longdate, truncated to the minute
					propName = "TIMESTAMP";
					oval = FAIRSpecUtilities.rep(val, "\n", " ").trim();
					System.out.println(">>" + originPath);
					System.out.println(">>" + oval);
					// 2022-01-25T00:55:28
					// have to use Z here, but it is local time
					pageGlobals.localTimeID = Instant.parse(oval.toString() + "Z").toEpochMilli() / 1000;
					break;
				case "Comment":
					propName = "TITLE";
					//$FALL-THROUGH$
				case "Author":
				case "Class":
				case "Presaturation Frequency":
				case "Probe":
				case "Modification Date":
				case "Pulse Sequence":
				case "Site":
				case "Title":
				case "Instrument":
				case "Spectrometer":
				case "Experiment":
				default:
					oval = FAIRSpecUtilities.rep(val, "\n", " ").trim();
					break;
				case "Solvent":
					oval = val = FAIRSpecUtilities.rep(val, "\n", " ").trim();
					reportSolvent(val);
					return;
				case "Purity":
					break;
				case "Data File Name":
					pageGlobals.isJDF = (val.endsWith(".jdf"));
					return;
				case "Temperature":
					float f = Float.parseFloat(val);
					if (pageGlobals.isJDF) {
						// JDF temp is oC not K from MNOVA
						val = "" + (f + 273.15);
					}
					oval = new VendorUtils.FloatString(val);
					break;
				case "Nucleus":
					pageGlobals.nuc1 = val;
					if (param2 != null) {
						pageGlobals.put("Nucleus2", param2.value);
					}
					break;
				case "Spectrometer Frequency":
					pageGlobals.freq = new VendorUtils.DoubleString(val);
					oval = pageGlobals.freq;
					if (param2 != null) {
						pageGlobals.put("Spectrometer Frequency2", new VendorUtils.DoubleString(param2.value));
					}
					break;
				case "Spectrum Quality":
					double q = Double.parseDouble(val);
					if (q != 0)
						oval = new VendorUtils.FloatString(val);
					break;
				case "Pulse Width":
				case "Spectral Width":
				case "Receiver Gain":
				case "Relaxation Delay":
					oval = new VendorUtils.FloatString(val);
					break;
				case "Acquisition Time":
					oval = new VendorUtils.FloatString(val);
					break;
				case "Lowest Frequency":
					oval = new VendorUtils.DoubleString(val);
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
		String structureType = null;
		switch (key) {
		case DefaultStructureHelper.PNG_FILE_DATA:
			structureType = ".png";
			break;
		case DefaultStructureHelper.CDX_FILE_DATA:
			structureType = ".cdx";
			break;
		case DefaultStructureHelper.CDXML_FILE_DATA:
			structureType = ".cdxml";
			break;
		case DefaultStructureHelper.MOL_FILE_DATA:
			structureType = ".mol";
			break;
		}
		if (structureType != null) {
			oval = new StructureData((byte[]) oval, originPath + "#page" + page + structureType, null, null,
					FAIRSpecUtilities.mediaTypeFromFileName(structureType), null);
		} else if (propName != null) {
			pageGlobals.put(ifdMap.get(propName), oval);
		}
		pageGlobals.put(key, oval);
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
		pageGlobals = new PageGlobals();
		pageGlobals.put(MetadataReceiverI.DeferredProperty.NEW_PAGE_KEY, "_page=" + page);
		pageList.add(pageGlobals);
		System.out.println("MestrelabIFDVendor ------------ page " + page);
	}

	int getPage() {
		return page;
	}

	// private

	/**
	 * Report the found property back to the IFDMetadataReceiverI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		boolean isDerived = key.startsWith("!");
		String k = ifdMap.get(isDerived ? key.substring(1) : key);
		// TODO? but not all keys are like this key = "MNova_" + key;
		if (k == null && key.equals("Page_Header")) {
			addProperty(IFDConst.IFD_PROPERTY_DESCRIPTION, val);
			addProperty(IFDExtractor.PAGE_ID_PROPERTY_SOURCE, val);
		}
		// SM and DIM are derived
		if (!isDerived)
			addProperty(key, val);
		if (k != null)
			addProperty(k, val);
	}

	private void close() {
		System.out.println("MestreLabIFDVendorPlugin done " + page + " pages for " + originPath 
				+ "\n=============================================\n");
		finalizeParams();
		page = 0;
	}

	private void finalizeParams() {
		if (pageGlobals != null && pageGlobals.freq != null) {
			int f = getNominalFrequency(pageGlobals.freq, pageGlobals.nuc1);
			pageGlobals.put("!NF", Integer.valueOf(f));
			pageGlobals.put("!DIM", pageGlobals.dim + "D");
			pageGlobals.put("mnovaVersion", mnovaVersion);
		} 
		pageGlobals = null;
	}

	void setVersion(String mnovaVersion) {
		this.mnovaVersion = mnovaVersion;
	}

	@Override
	public boolean isDerived() {
		// why this?
		return true;
	}

}