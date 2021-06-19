package org.iupac.fairspec.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A clas that implemends IFSVendorPluginI extends the ability of an
 * IFSExtractorI class to extract data and metadata from a dataset.
 * 
 * After statically (i.e. automatically upon the first time an instance of the
 * class is created) registering with
 * IFSVendorPluginI.registerAdapter(IFSParamAdapterI), IFSExporterI will query
 * the plugin for regex.Pattern values that it will recognize for given
 * ObjectType values.
 * 
 * During extraction, the IFSVendorPluginI class will be checked every time
 * there is a file that gives a match in its name.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * IFSConst.Property fields with values. (Or, for that matter, do anything else
 * it wants, including create new files from the data, since it will have access
 * to both the IFSExtractorI and IFSVendorPluginI instances once it accepts.)
 * 
 * @author hansonr
 *
 */
public interface IFSVendorPluginI {

	public static List<IFSVendorPluginI> vendorPlugins = new ArrayList<>();

	// TODO These should be in a config file
	public final static String[] knownVendors = {
			"com.vendor.bruker.BrukerIFSVendorPlugin",
			"com.vendor.jcamp.JCAMPDXIFSVendorPlugin",
			"com.vendor.jeol.JeolIFSVendorPlugin",
			"com.vendor.mestrelab.MestrelabIFSVendorPlugin",
			"com.vendor.varian.VarianIFSVendorPlugin",
		};

	public final static List<VendorInfo> activeVendors = new ArrayList<VendorInfo>();

	public static class VendorInfo {
		
		public IFSVendorPluginI vendor;
		public int index;
		public String vrezip;
		public String vcache;

		private VendorInfo(IFSVendorPluginI vendor, int index) {
			this.vendor = vendor;
			this.index = index;
			String p = vendor.getRezipRegex();
			if (p != null)
				vrezip = "(?<rezip" + index + ">" + p + ")";
			p = vendor.getParamRegex();
			if (p != null)
				vcache = "(?<param" + index + ">" + p + ")"; 
		}
		
	}

	static void init() {
		if (activeVendors.size() > 0)
			return;
		for (int i = 0, n = IFSVendorPluginI.knownVendors.length; i < n; i++) {
			String sv = IFSVendorPluginI.knownVendors[i];
			IFSVendorPluginI v;
			try {
				v = (IFSVendorPluginI) Class.forName(sv).getDeclaredConstructor().newInstance();
				if (v.isEnabled()) {
					activeVendors.add(new VendorInfo(v, activeVendors.size()));
					System.out.println("! IFSVendorPluginI vendorPlugin " + sv + " active");
				}
			} catch (Exception e) {
				System.err.println("! IFSVendorPluginI Trying to instatiation of " + sv + " failed.");
				e.printStackTrace(System.err);
			}

		}
	}
	
	/**
	 * Populate the activeVendors list.
	 * 
	 * @param adapter
	 */
	static void registerIFSVendorPlugin(Class<? extends IFSVendorPluginI> adapter) {
		try {
			IFSVendorPluginI v = adapter.getDeclaredConstructor().newInstance();
			vendorPlugins.add(v);
			System.out.println("! IFSVendorPluginI vendorPlugin " + adapter + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	boolean isEnabled();

	String getParamRegex();

	String getRezipRegex();

	String getRezipPrefix(String dirname);

	void startRezip(IFSExtractorI extractor);
	
	boolean doRezipInclude(String entryName);

	boolean accept(IFSExtractorI extractor, String fname, byte[] bytes);

	void endRezip();

	boolean doExtract(String entryName);
	
	


}
