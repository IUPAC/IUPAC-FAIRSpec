package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;
import org.iupac.fairspec.struc.IFSStructure;

import com.integratedgraphics.ifs.util.IFSDefaultVendorPlugin;
import com.integratedgraphics.ifs.vendor.mestrelab.MNovaMetadataReader.Param;

public class MestrelabIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	private static Map<String, String> ifsMap = new HashMap<>();

	private Map<String, Object> params;

	private int page = 0;

	private String fullFileName;	

	private String zipName;

	static {
		String[] keys = { //
				"Pulse Sequence", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_PULSE_PROG, //
				"Solvent", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_SOLVENT, //
				"Probe", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_PROBEID, //
				"Temperature", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_TEMPERATURE_K, //
				"Experiment", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_DIM, //
				"F1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_1, //
				"F2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_2, //
				"F3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_FREQ_3, //
				"N1", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_1, //
				"N2", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_2, //
				"N3", IFSNMRSpecData.IFS_PROP_SPEC_NMR_EXPT_NUCL_3, //
				"SF", IFSNMRSpecData.IFS_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, //
		};

		for (int i = 0; i < keys.length;)
			ifsMap.put(keys[i++], keys[i++]);
	}
	
	public MestrelabIFSVendorPlugin() {
		paramRegex = "\\.mnova[^/]*$";
	}
	
	@Override
	public boolean accept(IFSExtractorI extractor, String fname, String zipName, byte[] bytes) {
		super.accept(extractor, fname, zipName, bytes);
		MNovaMetadataReader reader;
		try {
			page = 0;
			params = null;
			reader = new MNovaMetadataReader(this, new ByteArrayInputStream(bytes));
			fullFileName = fname;
			this.zipName = zipName;
			//extractor.registerFileVendor(zipName, this);
			if (reader.process()) {
				addParams();
				close();
				return true;
			}
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String getVendorName() {
		String origin = (String) (params == null ? null : params.get("Origin"));
		return "Mestrelab"  + (origin == null ? "": "/" + origin);
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

	// called from MNovaReader
	
	private boolean isJDF;

	private String nuc1;

	private String nuc2;

	private double freq;

	private String mnovaVersion;

	public void addParam(String key, String val, Param param1, Param param2) {
		if (param1 != null)
			val = (param1.value == null || param1.value.length() == 0 ? param1.calc : param1.value);
		Object oval = null;
		if (val != null && val.length() > 0) {
			String key0 = key;
			val = val.trim();
			switch (key) {
			case "Data File Name":
				isJDF = (val.endsWith(".jdf"));
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
				oval =  Double.valueOf(freq);
				if (param2 != null) {
					params.put("F2", Double.valueOf(param2.value));
				}
				break;
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
			
			if (oval == null)
				oval = val;
			params.put(key, oval);
			System.out.println("----------- page " + page + " " 
					+ key + " = " + oval + " was " + key0 + " " + param1 + (param2 == null ? "" : "/ " + param2));
		}
	}

	List<Map<String, Object>> pageList = new ArrayList<>();
	
	void newPage() {
		clearParams();
		params = new HashMap<>();
		params.put("_fileName", zipName);
		params.put("mnovaVersion", mnovaVersion);
		pageList.add(params);
		System.out.println("------------ page " + ++page);
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
		if (pageList.size() == 0)
			return;
		clearParams();
		if (pageList.size() == 1) {
			processSpec(pageList.get(0));
			params = null;
			return;
		}
		System.out.println("?? multiple spectra in MNova file");
	}

	private void processSpec(Map<String, Object> params) {
		this.params = params;
		reportName();
			for (Entry<String, Object> p: params.entrySet()) {
				String key = p.getKey();
				if (!key.startsWith("_"))
					report(key, p.getValue());				
			}
//		extractor.registerFileVendor(zipName, null);
	}

	/**
	 * Report the found property back to the IFSExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	protected void report(String key, Object val) {
		String k = ifsMap.get(key);
		addProperty(k == null ? key : k, val);
	}

	private void close() {
		System.out.println("MestreLabIFSVendorPlugin done " + page + " pages for " + fullFileName);
		clearParams();
		page = 0;
	}
	
	private void clearParams() {
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

//	@Override
//	public void processVendorFile(String zipName) {
//		for (int i = pageList.size(); --i >= 0;) {
//			Map<String, Object> p = pageList.get(i);
//			if (p.get("_fileName").equals(zipName)) {
//				processSpec(p);
//			}
//		}
//	}
	
	
}