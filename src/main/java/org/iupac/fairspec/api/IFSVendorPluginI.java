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
public interface IFSVendorPluginI extends IFSPropertyManagerI {

	public static List<IFSVendorPluginI> vendorPlugins = new ArrayList<>();

	// TODO These should be in a config file
	public final static String[] knownVendors = {
			"com.integratedgraphics.ifs.vendor.bruker.BrukerIFSVendorPlugin",
			"com.integratedgraphics.ifs.vendor.jcamp.JCAMPDXIFSVendorPlugin",
			"com.integratedgraphics.ifs.vendor.jeol.JeolIFSVendorPlugin",
			"com.integratedgraphics.ifs.vendor.mestrelab.MestrelabIFSVendorPlugin",
			"com.integratedgraphics.ifs.vendor.varian.VarianIFSVendorPlugin",
		};

	public final static List<VendorInfo> activeVendors = new ArrayList<VendorInfo>();

	public static class VendorInfo {
		
		final public IFSVendorPluginI vendor;
		final public int index;
		final public String vrezip;
		final public String vcache;

		private VendorInfo(IFSVendorPluginI vendor, int index) {
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
	 * @param plugin
	 */
	static void registerIFSVendorPlugin(Class<? extends IFSVendorPluginI> plugin) {
		try {
			IFSVendorPluginI v = plugin.getDeclaredConstructor().newInstance();
			vendorPlugins.add(v);
			System.out.println("! IFSVendorPluginI vendorPlugin " + plugin + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	boolean isEnabled();

	String getVendorName();

	String getRezipRegex();

	String getRezipPrefix(String dirname);

	void startRezip(IFSExtractorI extractor);
	
	boolean doRezipInclude(String zipfileName, String entryName);

	void endRezip();

	int getIndex();
	
	void setIndex(int index);

}
