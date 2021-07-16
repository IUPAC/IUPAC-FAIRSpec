package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecData;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;

import com.integratedgraphics.ifs.util.IFSDefaultVendorPlugin;
import com.integratedgraphics.ifs.vendor.mestrelab.MNovaMetadataReader.Param;

public class MestrelabIFSVendorPlugin extends IFSDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifs.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	private static Map<String, String> ifsMap = new HashMap<>();

	private Map<String, Object> params;

	private String fileName;	

	private int page = 0;

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
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		if (super.accept(extractor, fname, bytes)) {
			close();
		}
		MNovaMetadataReader reader;
		try {
			page = 0;
			params = null;
			reader = new MNovaMetadataReader(this, new ByteArrayInputStream(bytes));
			fileName = fname;
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
		return "Mestrelab";
	}

	@Override
	public String getDatasetType(String zipName) {
		return IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET;
	}

	// called from MNovaReader
	
	private boolean isJDF;

//	private boolean is2D;

	private String nuc1;

	private String nuc2;

	private double freq;

	public void addParam(String key, String val, Param param1, Param param2) {
		if (param1 != null)
			val = (param1.value == null || param1.value.length() == 0 ? param1.calc : param1.value);
		Object oval = null;
		if (val != null && val.length() > 0) {
			String key0 = key;
			val = val.trim();
			switch (key) {
//			case "NUC12":
//			if (is2D) {
//				int pt = val.indexOf(",");
//				if (pt >= 0) {
//					key = "N2";
//					val = val.substring(0, pt);
//					int i = 0;
//					while (i < val.length() && Character.isDigit(val.charAt(i)))
//						i++;
//					while (i < val.length() && !Character.isDigit(val.charAt(i)))
//						i++;
//					val = val.substring(i).trim();
//					nuc2 = val;
//				}
//			}
//			break;
//			case "DIM":
//			case "Experiment":
//				//is2D = val.equals("2D");
//				break;
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
			}
			if (oval == null)
				oval = val;
			params.put(key, oval);
			System.out.println("----------- page " + page + " " 
					+ key + " = " + oval + " was " + key0 + " " + param1 + (param2 == null ? "" : "/" + param2));
		}
	}

	void newPage() {
		addParams();
		System.out.println("------------ page " + ++page);
		params = new HashMap<>();
		return;
	}
	
	int getPage() {
		return page;
	}

	// private
	
	private void addParams() {
		if (params != null) {
			double f = getNominalFrequency(freq, nuc1);
			System.out.println("nom freq " + f + " for " + nuc1 + " " + nuc2 + " " + freq);
			params.put("SF", f);
			System.out.println(params);
		}
	}

	private void close() {
		addParams();
		System.out.println("MestreLabIFSVendorPlugin done " + page + " pages for " + fileName);
		page = 0;
		params = null;
		isJDF = false;//is2D = false;
		nuc1 = nuc2 = null;
		freq = 0;
		return;
	}


}