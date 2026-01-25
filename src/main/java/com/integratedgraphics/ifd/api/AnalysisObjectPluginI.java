package com.integratedgraphics.ifd.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.MetadataReceiverI;
import org.iupac.fairdata.extract.PropertyManagerI;

import com.integratedgraphics.extractor.IFDExtractorImpl;

/**
 * A class that implements AnalysisObjectPluginI extends the ability of an
 * IFDMetadataReceiverI class to extract data and metadata from an
 * IFDAnalysisObject representation.
 * 
 * After statically (i.e. automatically upon the first time an instance of the
 * class is created) registering with
 * AnalysisObjectPluginI.registerAdapter(IFDParamAdapterI), 
 * this will query
 * the plugin for regex.Pattern values that it will recognize for given
 * AnalysisObject values.
 * 
 * During extraction, the AnalysisObjectPluginI class will be checked every time
 * there is a file that gives a match in its name.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * property fields with values found in
 * org.iupac.fairdata.common.fairdata.properties (Or, for that matter, do
 * anything else it wants, including create new files from the data, since it
 * will have access to both the IFDMetadataReceiverI and AnalysisObjectPluginI
 * instances once it accepts.)
 * 
 * @author hansonr
 *
 */
public interface AnalysisObjectPluginI extends PropertyManagerI {
	
	public static List<AnalysisObjectPluginI> plugins = new ArrayList<>();

	public final static List<AnalysisObjectInfo> activeStandards = new ArrayList<AnalysisObjectInfo>();

	public static class AnalysisObjectInfo {
		
		final public AnalysisObjectPluginI Standard;
		final public int index;
		final public String vcache;

		private AnalysisObjectInfo(AnalysisObjectPluginI Standard, int index) {
			this.Standard = Standard;
			this.index = index;
			Standard.setIndex(index);
			String p = Standard.getParamRegex();
			vcache = (p == null ? null : "(?<param" + index + ">" + p + ")"); 
		}

	}

	static void init() {
		if (activeStandards.size() > 0)
			return;
		Map<String, Object> Standards = null;
		try {
			Standards = FAIRSpecUtilities.getJSONResource(IFDExtractorImpl.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		@SuppressWarnings("unchecked")
		List<Object> knownStandards = (List<Object>) Standards.get("knownAnalysisStandards");
		for (int i = 0, n = knownStandards.size(); i < n; i++) {
			String sv = (String) knownStandards.get(i);
			AnalysisObjectPluginI v;
			try {
				v = (AnalysisObjectPluginI) Class.forName(sv).getDeclaredConstructor().newInstance();
				if (v.isEnabled()) {
					addStandard(v);
				}
			} catch (Exception e) {
				System.err.println("! AnalysisObjectPluginI Trying to instatiation of " + sv + " failed.");
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * This method can be used to add a Standard if desired.
	 * 
	 * @param v
	 */
	public static void addStandard(AnalysisObjectPluginI v) {
		activeStandards.add(new AnalysisObjectInfo(v, activeStandards.size()));
		System.out.println("! AnalysisObjectPluginI Plugin " + v.getClass().getName() + " active");
	}

	/**
	 * Populate the activeStandards list.
	 * 
	 * @param plugin
	 */
	static void registerAnalysisPlugin(Class<? extends AnalysisObjectPluginI> plugin) {
		try {
			AnalysisObjectPluginI v = plugin.getDeclaredConstructor().newInstance();
			plugins.add(v);
			System.out.println("! AnalysisPluginI Plugin " + plugin + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	boolean isEnabled();

	String getAnalysisName();

	void initializeAnalysisObject(MetadataReceiverI extractor);
	
	int getIndex();
	
	void setIndex(int index);

	Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName);

}
