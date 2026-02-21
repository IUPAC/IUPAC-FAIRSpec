package com.integratedgraphics.ifd.dataobject;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDAttribute;
import org.iupac.fairdata.core.IFDProperty;

import jspecview.source.JDXDataObject;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp(
			"IFD_REP_DATAOBJECT_FAIRSPEC_NMR.VENDOR_DATASET");

	protected static String IMAGE = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.SPECTRUM_IMAGE");
	protected static String PDF = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.SPECTRUM_DOCUMENT");

	private static Map<String, List<String>> solventMap;
	private static List<String> solventKeyList;
	protected Long dataObjectGMTTimestamp, dataObjectLocalTimestamp;
	private String dataObjectLocalTimeOffset;

	/**
	 *  default false
	 * 
	 */
	protected boolean haveFID;
	
	protected NMRVendorPlugin() {
		super();
	}

	@Override
	public void endDataSet() {
		dataObjectLocalTimestamp = dataObjectGMTTimestamp = null;
		dataObjectLocalTimeOffset = null;
		super.endDataSet();
	}
	
	@Override
	public void reportVendor() {
		addProperty(IFDConst.IFD_PROPERTY_DATAOBJECT_INSTR_MANUFACTURER_NAME, getVendorName());
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
	public static int getNominalFrequency(IFDAttribute.DoubleString freq, String nuc) {
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
	public static IFDAttribute.DoubleString getProtonFrequency(IFDAttribute.DoubleString freq1, String nuc1,
			IFDAttribute.DoubleString freq2, String nuc2) {
		if (nuc1 == null)
			return null;
		if ("1H".equals(nuc1))
			return freq1;
		if ("1H".equals(nuc2))
			return freq2;
		return new IFDAttribute.DoubleString("" + JDXDataObject.getProtonFreq(nuc1, freq1.value()));
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
		//System.out.println(">>> dateGMT " + dateTimeSec + " is " + timeToString(dateTimeSec));
		dataObjectGMTTimestamp = Long.valueOf(dateTimeSec);
	}

	protected void setDataObjectLocalTimeID(long dateTimeSec) {
		//System.out.println(">>> dateLOC " + dateTimeSec + " is " + timeToString(dateTimeSec));
		dataObjectLocalTimestamp = Long.valueOf(dateTimeSec);
	}

	/**
	 * +0100, for example
	 * 
	 * @param offset
	 */
	protected void setDataObjectLocalTimeOffset(String offset) {
		//System.out.println(">>> dateOffset " + offset);
		dataObjectLocalTimeOffset = offset;
	}

//	static {
//		System.out.println(new Date("2023/07/09 15:50:10 GMT").toInstant().toEpochMilli());
//		System.out.println(new Date("07/09/2023 15:50:10 GMT").toGMTString());
//		System.out.println(new Date(new Date("2023/07/09 15:50:10 GMT").toInstant().toEpochMilli()).toGMTString());
//	}
	/**
	 * date may be long or some sort of date. We will save the long number for GMT timestamp if we can get it
	 * @param date
	 * @param isGMT
	 */
	@SuppressWarnings("deprecation")
	protected void setDataObjectTimestamp(String date, boolean isGMT) {
		if (date == null)
			return;
		try {
			long l;
			int pt = date.indexOf('.');
			if (pt >= 0) { // remove .000 ms
				date = date.substring(0, pt);
			}
			if (date.indexOf("/") >= 0) {
				l = new Date(date + " GMT").toInstant().toEpochMilli() / 1000;
			} else if (date.indexOf("T") >= 0){
				if (!date.endsWith("Z"))
					date += "Z";
				l = Instant.parse(date).toEpochMilli() / 1000;				
			} else { 
				l = Long.parseLong(date);
			}
			if (isGMT) {
				setDataObjectGMTTimeID(l);
			} else {
				setDataObjectLocalTimeID(l);
			}
			System.out.println("NVP " + l + " " + Instant.ofEpochSecond(l));
		} catch (Exception e) {
			System.err.println("NMRVendorPlugin could not parse " + date + " (ignored) should be of form YYYY/MM/DD [HH:MM:SS[.SSSS] [±UUUU]]");
		}
	}

	protected void addSpecDateTimes() {
		setDateIDs();
		long timestamp = 0;
		if (dataObjectLocalTimestamp != null) {
			addProperty(FAIRSpecExtractorHelper.TIME_LOCAL + "_" + getVendorName(), timeToString(dataObjectLocalTimestamp));
			timestamp = dataObjectLocalTimestamp;
		}
		if (dataObjectGMTTimestamp != null) {
			addProperty(FAIRSpecExtractorHelper.DATE_TIME_GMT + "_" + getVendorName(), timeToString(dataObjectGMTTimestamp));
			timestamp = dataObjectGMTTimestamp;
		}
		if (timestamp > 0) {
			addProperty(IFDConst.IFD_PROPERTY_DATAOBJECT_EXPT_TIMESTAMP, Long.valueOf(timestamp));
		}
	}

	protected void setDateIDs() {
		if (dataObjectLocalTimeOffset == null)
			return;
		try {
		int offset = Integer.parseInt(dataObjectLocalTimeOffset.replace('+', ' ').replace("30", "50").trim()) * 36;
		// dataObjectLongID;
		// +0100 => 100 > 3600 s
		if (dataObjectGMTTimestamp == null && dataObjectLocalTimestamp != null) {
			dataObjectGMTTimestamp = new Long((dataObjectLocalTimestamp - offset));
		}
		if (dataObjectLocalTimestamp == null && dataObjectGMTTimestamp != null) {
			dataObjectLocalTimestamp = new Long((dataObjectGMTTimestamp + offset));
		}		
		//System.out.println(">>>" + originPath + " " + dataObjectGMTTimeID + " " + dataObjectLocalTimeID);
		} catch (Exception e) {
			System.err.println("NMRVEndorPlugin could not parse offset " + dataObjectLocalTimeOffset);
		}
	}

}
