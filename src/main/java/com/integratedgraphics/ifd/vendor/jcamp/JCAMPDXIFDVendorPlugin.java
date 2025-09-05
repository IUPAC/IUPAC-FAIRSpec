package com.integratedgraphics.ifd.vendor.jcamp;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.DefaultVendorPlugin;

import jspecview.source.JDXReader;

public class JCAMPDXIFDVendorPlugin extends DefaultVendorPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_UNKNOWN_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_UNKNOWN.VENDOR_DATASET");
    protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UNKNOWN_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UNKNOWN.INSTR_MANUFACTURER_NAME");
    
	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	private DefaultVendorPlugin delegatedPlugin;
	
	protected String datasetKey;
	protected String vendorKey;

	private String jcampType;
	
	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
		datasetKey = IFD_REP_DATAOBJECT_FAIRSPEC_UNKNOWN_VENDOR_DATASET;
		vendorKey = IFD_PROPERTY_DATAOBJECT_FAIRSPEC_UNKNOWN_INSTR_MANUFACTURER_NAME;
		jcampType = null;
	}

	protected void setJCAMPType(String type) {
		jcampType = type;
		datasetKey = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_" + type + ".VENDOR_DATASET");
	    vendorKey = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_" + type + ".INSTR_MANUFACTURER_NAME");
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		if (jcampType != null)
	        return null;
		Map<String, String> map = null;
		try {
			map = JDXReader.getHeaderMap(new ByteArrayInputStream(bytes), null);
		} catch (Exception e) {
			// invalid format
			e.printStackTrace();
			return null;
		}
		String dataType = map.get("##DATATYPE");
		if (dataType == null)
			return null;
		switch (dataType) {
		case "NMR SPECTRUM":
			delegatedPlugin = newJCAMPDXPlugin("NMR", map);
			break;
		case "IR SPECTRUM":
			// nonstandard
			System.out.println("JCAMPDIX????'IR' SPECTRUM");
			//$FALL-THROUGH$
		case "INFRARED SPECTRUM":
			delegatedPlugin = newJCAMPDXPlugin("IR", map);
			break;
		case "MASS SPECTRUM":
			delegatedPlugin = newJCAMPDXPlugin("MS", map);
			break;
		case "UV/VIS SPECTRUM":
			delegatedPlugin = newJCAMPDXPlugin("UVVIS", map);
			break;
		case "RAMAN SPECTRUM":
			delegatedPlugin = newJCAMPDXPlugin("RAMAN", map);
			break;
	    default: 
			System.out.println("JCAMPDIX???? " + dataType);
			this.map = map;
		}
		if (delegatedPlugin != null)
			return delegatedPlugin.accept(extractor, originPath, bytes);
		return processJCAMP();
	}

	protected String processJCAMP() {
		// anything to do?
		return getVendorDataSetKey();
	}

	private DefaultVendorPlugin newJCAMPDXPlugin(String type, Map<String, String> map) {
		String className = JCAMPDXIFDVendorPlugin.class.getName();
		className = className.substring(0, className.lastIndexOf(".") + 1) + "JCAMPDX" + type + "Plugin";
		DefaultVendorPlugin o = null;
		try {
			o = (DefaultVendorPlugin) Class.forName(className).newInstance();
		} catch (Exception e) {
			System.out.println("JCAMPDXIFDPlugin class not found: " + className);
			o = this;
		}
		o.setMap(map);
		return o;
	}

	@Override
	public String getVendorName() {
		return (delegatedPlugin == null ? "JCAMP-DX" + (jcampType == null ? "" : "/" + jcampType) 
				: delegatedPlugin.getVendorName());
	}

	@Override
	public String getVendorDataSetKey() {
		return (delegatedPlugin == null ? datasetKey : delegatedPlugin.getVendorDataSetKey());
	}
	
	@Override
	public void reportVendor() {
		if (delegatedPlugin == null)
			addProperty(vendorKey, getVendorName());
		else 
			delegatedPlugin.reportVendor();
	}
	
	public static void main(String[] args) {
		// from mnova jdx and acqus good to the second
//    	System.out.println(new Date(1643072091L*1000).toGMTString());
//    	System.out.println(new Date(Date.UTC(2022-1900,01-1,25, 00,54,51)).toGMTString());
		try {
			Map<String, String> map = JDXReader.getHeaderMap(new FileInputStream("c:/temp/t.jdx"), null);
			for (String k : map.keySet())
				System.out.println(k + " = " + map.get(k));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

}