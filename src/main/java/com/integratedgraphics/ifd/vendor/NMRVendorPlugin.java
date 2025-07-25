package com.integratedgraphics.ifd.vendor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;

import jspecview.source.JDXDataObject;

public abstract class NMRVendorPlugin extends DefaultVendorPlugin {
	
	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.VENDOR_DATASET");
    final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_MANUFACTURER_NAME");
	private static Map<String, List<String>> solventMap;
	private static List<String> solventKeyList;

    protected NMRVendorPlugin() {
    	super();
    }
    
    @Override
	protected void reportVendor() {
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
	 * Get the "nominal" spectrometer frequency -- 300, 400, 500, etc. -- from the frequency and identity of the nucleus
	 * @param freq
	 * @param nuc if null, just do the century cleaning
	 * @return
	 */
	public static int getNominalFrequency(double freq, String nuc) {
		return JDXDataObject.getNominalSpecFreq(nuc, freq);
	}

	/**
	 * Get the full-precision spectrometer frequency -- 300.012303 -- from the frequency and identity of the nucleus
	 * @param freq
	 * @param nuc must not be null
	 * @return
	 */
	public static double getProtonFrequency(double freq, String nuc) {
		if (nuc == null)
			return Double.NaN;
		return JDXDataObject.getProtonFreq(nuc, freq);
	}

	final static String nmrSolvent = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.EXPT_SOLVENT");
	
	/**
	 * Add InChI, InChIKey, and common_name 
	 * based on * prefix in the tab file does the adding.
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
			for (int i = 0; i < solventKeyList.size(); i+= 2) {
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
			for (int i = 0; i < lst.size(); i+= 2) {
				String key = nmrSolvent + "_" + lst.get(i).substring(1);
				String val = lst.get(i + 1);
				addProperty(key, val);
			}
		}
	}



}
