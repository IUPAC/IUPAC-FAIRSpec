package com.integratedgraphics.ifd.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.MetadataReceiverI;
import org.iupac.fairdata.extract.PropertyManagerI;

import com.integratedgraphics.extractor.IFDExtractorMain;

/**
 * A class that implements DataObjectVendorPluginI extends the ability of an
 * IFDMetadataReceiverI class to extract data and metadata from an IFDDataObject
 * representation.
 * 
 * After statically (i.e. automatically upon the first time an instance of the
 * class is created) registering with
 * DataObjectVendorPluginI.registerDataObjectVendorPlugin(DataObjectVendorPluginI), 
 * this class will query
 * the plugin for regex.Pattern values that it will recognize for given
 * ObjectType values.
 * 
 * During extraction, the DataObjectVendorPluginI class will be checked every time
 * there is a file that gives a match in its name.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * property fields with values found in
 * org.iupac.fairdata.common.fairdata.properties (Or, for that matter, do
 * anything else it wants, including create new files from the data, since it
 * will have access to both the IFDMetadataReceiverI and DataObjectVendorPluginI
 * instances once it accepts.)
 * 
 * @author hansonr
 *
 */
public interface DataObjectVendorPluginI extends PropertyManagerI {
	
	public static List<DataObjectVendorPluginI> plugins = new ArrayList<>();

	public final static List<DataObjectVendorInfo> activeVendors = new ArrayList<DataObjectVendorInfo>();

	public static class DataObjectVendorInfo {
		
		final public DataObjectVendorPluginI vendor;
		final public int index;
		final public String vrezip;
		final public String vcache;

		private DataObjectVendorInfo(DataObjectVendorPluginI vendor, int index) {
			this.vendor = vendor;
			this.index = index;
			vendor.setIndex(index);
			String p = vendor.getRezipRegex();
			vrezip = (p == null ? null : "(?<rezip" + index + ">" + p + ")");
			p = vendor.getParamRegex();
			vcache = (p == null ? null : "(?<param" + index + ">" + p + ")"); 
		}

	}

	static void init() {
		if (activeVendors.size() > 0)
			return;
		Map<String, Object> vendors = null;
		try {
			vendors = FAIRSpecUtilities.getJSONResource(IFDExtractorMain.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		@SuppressWarnings("unchecked")
		List<Object> knownVendors = (List<Object>) vendors.get("knownDataObjectVendors");
		for (int i = 0, n = knownVendors.size(); i < n; i++) {
			String sv = (String) knownVendors.get(i);
			DataObjectVendorPluginI v;
			try {
				v = (DataObjectVendorPluginI) Class.forName(sv).getDeclaredConstructor().newInstance();
				if (v.isEnabled()) {
					addVendor(v);
				}
			} catch (Exception e) {
				System.err.println("! DataObjectVendorPluginI Trying to instatiation of " + sv + " failed.");
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * This method can be used to add a vendor if desired.
	 * 
	 * @param v
	 */
	public static void addVendor(DataObjectVendorPluginI v) {
		activeVendors.add(new DataObjectVendorInfo(v, activeVendors.size()));
		System.out.println("! DataObjectVendorPluginI vendorPlugin " + v.getClass().getName() + " active");
	}

	/**
	 * Populate the activeVendors list.
	 * 
	 * @param plugin
	 */
	static void registerDataObjectVendorPlugin(Class<? extends DataObjectVendorPluginI> plugin) {
		try {
			DataObjectVendorPluginI v = plugin.getDeclaredConstructor().newInstance();
			plugins.add(v);
			System.out.println("! DataObjectVendorPluginI vendorPlugin " + plugin + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	boolean isEnabled();

	String getVendorName();

	String getRezipRegex();

	String getRezipPrefix(String dirname, byte[] bytes);

	void initializeDataSet(MetadataReceiverI extractor);
	
	boolean doRezipInclude(MetadataReceiverI extractor, String zipfileName, String entryName);

	void endDataSet();

	int getIndex();
	
	void setIndex(int index);

	Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName);

}
