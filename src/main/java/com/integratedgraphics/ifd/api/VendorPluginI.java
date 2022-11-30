package com.integratedgraphics.ifd.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.ExtractorI;
import org.iupac.fairdata.extract.PropertyManagerI;

import com.integratedgraphics.ifd.Extractor;

/**
 * A clas that implements IFDVendorPluginI extends the ability of an
 * IFDExtractorI class to extract data and metadata from a dataset.
 * 
 * After statically (i.e. automatically upon the first time an instance of the
 * class is created) registering with
 * IFDVendorPluginI.registerAdapter(IFDParamAdapterI), IFDExporterI will query
 * the plugin for regex.Pattern values that it will recognize for given
 * ObjectType values.
 * 
 * During extraction, the IFDVendorPluginI class will be checked every time
 * there is a file that gives a match in its name.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * property fields with values found in
 * org.iupac.fairdata.common.fairdata.properties (Or, for that matter, do
 * anything else it wants, including create new files from the data, since it
 * will have access to both the IFDExtractorI and IFDVendorPluginI instances
 * once it accepts.)
 * 
 * @author hansonr
 *
 */
public interface VendorPluginI extends PropertyManagerI {
	
	public static List<VendorPluginI> vendorPlugins = new ArrayList<>();

	public final static List<VendorInfo> activeVendors = new ArrayList<VendorInfo>();

	public static class VendorInfo {
		
		final public VendorPluginI vendor;
		final public int index;
		final public String vrezip;
		final public String vcache;

		private VendorInfo(VendorPluginI vendor, int index) {
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
			vendors = FAIRSpecUtilities.getJSONResource(Extractor.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		@SuppressWarnings("unchecked")
		List<Object> knownVendors = (List<Object>) vendors.get("knownVendors");
		for (int i = 0, n = knownVendors.size(); i < n; i++) {
			String sv = (String) knownVendors.get(i);
			VendorPluginI v;
			try {
				v = (VendorPluginI) Class.forName(sv).getDeclaredConstructor().newInstance();
				if (v.isEnabled()) {
					addVendor(v);
				}
			} catch (Exception e) {
				System.err.println("! IFDVendorPluginI Trying to instatiation of " + sv + " failed.");
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * This method can be used to add a vendor if desired.
	 * 
	 * @param v
	 */
	public static void addVendor(VendorPluginI v) {
		activeVendors.add(new VendorInfo(v, activeVendors.size()));
		System.out.println("! IFDVendorPluginI vendorPlugin " + v.getClass().getName() + " active");
	}

	/**
	 * Populate the activeVendors list.
	 * 
	 * @param plugin
	 */
	static void registerIFDVendorPlugin(Class<? extends VendorPluginI> plugin) {
		try {
			VendorPluginI v = plugin.getDeclaredConstructor().newInstance();
			vendorPlugins.add(v);
			System.out.println("! IFDVendorPluginI vendorPlugin " + plugin + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	boolean isEnabled();

	String getVendorName();

	String getRezipRegex();

	String getRezipPrefix(String dirname);

	void startRezip(ExtractorI extractor);
	
	boolean doRezipInclude(ExtractorI extractor, String zipfileName, String entryName);

	void endRezip();

	int getIndex();
	
	void setIndex(int index);

	void checkExtract(ExtractorI extractor, String baseName, String entryName);

//	void processVendorFile(String zipName);

}
