package com.integratedgraphics.ifd.vendor;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;

import com.integratedgraphics.ifd.util.VendorUtils;

import jspecview.source.JDXDataObject;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp(
			"IFD_REP_DATAOBJECT_FAIRSPEC_NMR.VENDOR_DATASET");
	protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp(
			"IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_MANUFACTURER_NAME");

	private static Map<String, List<String>> solventMap;
	private static List<String> solventKeyList;
	protected Long dataObjectGMTTimeID, dataObjectLocalTimeID;
	private String dataObjectLocalTimeOffset;

	protected NMRVendorPlugin() {
		super();
	}

	@Override
	public void endDataSet() {
		dataObjectLocalTimeID = dataObjectGMTTimeID = null;
		dataObjectLocalTimeOffset = null;
		super.endDataSet();
	}
	
	@Override
	public void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}

	private static Pattern nucPat = Pattern.compile("(^[^\\d]*)(\\d+)([^\\d]*)$");

	public static String fixNucleus(String nuc) {
		Matcher m = nucPat.matcher(nuc);
		if (m.find()) {
			String sn = m.group(2);
			String el = m.group(1);
			el = (el.length() == 0 ? m.group(3) : el);
			if (el.length() > 2) {
				int an = org.jmol.util.Elements.elementNumberFromName(el);
				el = org.jmol.util.Elements.elementSymbolFromNumber(an);
				if (el != null)
					nuc = el;
			} else {
				nuc = el;
			}
			if (nuc.length() == 2) {
				nuc = nuc.substring(0, 1).toUpperCase() + nuc.substring(1).toLowerCase();
			}
			nuc = sn + nuc;
		}
		return nuc;
	}

	/**
	 * Get the "nominal" spectrometer frequency -- 300, 400, 500, etc. -- from the
	 * frequency and identity of the nucleus
	 * 
	 * @param freq
	 * @param nuc  if null, just do the century cleaning
	 * @return
	 */
	public static int getNominalFrequency(VendorUtils.DoubleString freq, String nuc) {
		return JDXDataObject.getNominalSpecFreq(nuc, freq.value());
	}

	/**
	 * Get the full-precision spectrometer frequency -- 300.012303 -- from the
	 * frequency and identity of the nucleus
	 * 
	 * @param freq
	 * @param nuc  must not be null
	 * @return
	 */
	public static VendorUtils.DoubleString getProtonFrequency(VendorUtils.DoubleString freq1, String nuc1,
			VendorUtils.DoubleString freq2, String nuc2) {
		if (nuc1 == null)
			return null;
		if ("1H".equals(nuc1))
			return freq1;
		if ("1H".equals(nuc2))
			return freq2;
		return new VendorUtils.DoubleString("" + JDXDataObject.getProtonFreq(nuc1, freq1.value()));
	}

	final static String nmrSolvent = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_SOLVENT");

	/**
	 * Add InChI, InChIKey, and common_name based on * prefix in the tab file does
	 * the adding.
	 * 
	 * @param solvent
	 */
	protected void reportSolvent(String solvent) {
		if (solventMap == null) {
			solventMap = FAIRSpecUtilities.getNMRSolventMap();
			solventKeyList = solventMap.get("cdcl3");
		}
		if (solvent.equals(IFDProperty.NULL)) {
			addProperty(nmrSolvent, solvent);
			for (int i = 0; i < solventKeyList.size(); i += 2) {
				addProperty(nmrSolvent + "_" + solventKeyList.get(i).substring(1), solvent);
			}
			return;
		}
		addProperty(nmrSolvent, solvent);
		int pt = solvent.indexOf("_"); // as in Bruker MeOD_SPE
		if (pt > 0)
			solvent = solvent.substring(0, pt);
		List<String> lst = solventMap.get(solvent.toLowerCase());
		if (lst != null) {
			for (int i = 0; i < lst.size(); i += 2) {
				String key = nmrSolvent + "_" + lst.get(i).substring(1);
				String val = lst.get(i + 1);
				addProperty(key, val);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static String timeToString(long dateTimeSec) {
		return new Date(dateTimeSec * 1000).toGMTString().replace("GMT", "");
	}

	protected void setDataObjectGMTTimeID(long dateTimeSec) {
		System.out.println(">>> dateGMT " + dateTimeSec + " is " + timeToString(dateTimeSec));
		dataObjectGMTTimeID = Long.valueOf(dateTimeSec);
	}

	protected void setDataObjectLocalTimeID(long dateTimeSec) {
		System.out.println(">>> dateLOC " + dateTimeSec + " is " + timeToString(dateTimeSec));
		dataObjectLocalTimeID = Long.valueOf(dateTimeSec);
	}

	/**
	 * +0100, for example
	 * 
	 * @param offset
	 */
	protected void setDataObjectLocalTimeOffset(String offset) {
		System.out.println(">>> dateOffset " + offset);
		dataObjectLocalTimeOffset = offset;
	}

//	static {
//		System.out.println(new Date("2023/07/09 15:50:10 GMT").toInstant().toEpochMilli());
//		System.out.println(new Date("07/09/2023 15:50:10 GMT").toGMTString());
//		System.out.println(new Date(new Date("2023/07/09 15:50:10 GMT").toInstant().toEpochMilli()).toGMTString());
//	}
	@SuppressWarnings("deprecation")
	protected void setDataObjectTimeID(String date, boolean isGMT) {
		if (date == null)
			return;
		try {
			long l;
			if (date.indexOf("/") >= 0) {
				int pt = date.indexOf('.');
				if (pt >= 0) { // remove .000 ms
					date = date.substring(0, pt);
				}
				l = new Date(date + " GMT").toInstant().toEpochMilli() / 1000;
			} else {
				l = Long.parseLong(date);
			}
			if (isGMT) {
				setDataObjectGMTTimeID(l);
			} else {
				setDataObjectLocalTimeID(l);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void addSpecDateTimeIDs() {
		setDateIDs();
		if (dataObjectGMTTimeID != null) {
			addProperty(FAIRSpecExtractorHelper.TIMESTAMP_GMT + "_" + getVendorName(), dataObjectGMTTimeID + "=" + timeToString(dataObjectGMTTimeID));
		}
		if (dataObjectLocalTimeID != null) {
			addProperty(FAIRSpecExtractorHelper.TIMESTAMP_LOCAL + "_" + getVendorName(), dataObjectLocalTimeID + "=" + timeToString(dataObjectLocalTimeID));
		}
	}

	protected void setDateIDs() {
		if (dataObjectLocalTimeOffset == null)
			return;
		int offset = Integer.parseInt(dataObjectLocalTimeOffset.replace('+', ' ').replace("30", "50").trim()) * 36;
		// dataObjectLongID;
		// +0100 => 100 > 3600 s
		if (dataObjectGMTTimeID == null && dataObjectLocalTimeID != null) {
			dataObjectGMTTimeID = new Long((dataObjectLocalTimeID - offset));
		}
		if (dataObjectLocalTimeID == null && dataObjectGMTTimeID != null) {
			dataObjectLocalTimeID = new Long((dataObjectGMTTimeID + offset));
		}		
		System.out.println(">>>" + originPath + " " + dataObjectGMTTimeID + " " + dataObjectLocalTimeID);
	}

}
