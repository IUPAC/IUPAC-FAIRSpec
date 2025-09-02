package com.integratedgraphics.ifd.vendor.jcamp;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.DefaultVendorPlugin;

import jspecview.source.JDXReader;

public class JCAMPDXIFDVendorPlugin extends DefaultVendorPlugin {

	interface JCAMPPlugin {
		void setMap(Map<String, String> map);
	}
	
	private DefaultVendorPlugin delegatedPlugin;
	
	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.class);
	}

	public JCAMPDXIFDVendorPlugin() {
		paramRegex = "\\.jdx$|\\.dx$";
	}

	protected Map<String, String> map;
	
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
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
	    	return null;
		}
		return delegatedPlugin.accept(extractor, originPath, bytes);
	}

	private DefaultVendorPlugin newJCAMPDXPlugin(String type, Map<String, String> map) {
		String className = JCAMPDXIFDVendorPlugin.class.getName();
		className = className.substring(0, className.lastIndexOf(".") + 1) + "JCAMPDX" + type + "Plugin";
		try {
			DefaultVendorPlugin o = (DefaultVendorPlugin) Class.forName(className).newInstance();
			((JCAMPPlugin) o).setMap(map);
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getVendorName() {
		return (delegatedPlugin == this ? "JCAMP-DX" : delegatedPlugin.getVendorName());
	}

	@Override
	public String getVendorDataSetKey() {
		return (delegatedPlugin == null ? null : delegatedPlugin.getVendorDataSetKey());
	}
	
	@Override
	public void reportVendor() {
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