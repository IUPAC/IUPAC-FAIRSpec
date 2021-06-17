package org.iupac.fairspec.api;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.core.IFSObject;

/**
 * A plugin class that extends the ability to extract data and metadata from a
 * dataset.
 * 
 * After registering with IFSVendorPluginI.registerAdapter(IFSParamAdapterI),
 * IFSFindingAid will query the plugin for regex.Pattern values that it will
 * recognize for given ObjectType values.
 * 
 * During extraction, the IFSFindingAid class will then start checking the
 * adapter when it finds a possible match.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * IFSConst.Property fields with values. (Or, for that matter, do anything else
 * it wants, including create new files from the data.
 * 
 * @author hansonr
 *
 */
public interface IFSVendorPluginI {

	public static List<IFSVendorPluginI> vendorPlugins = new ArrayList<>();


	boolean register();

	String getRegex(IFSObjectI.ObjectType type);

	boolean accept(String fname, byte[] data);

	boolean populateProperties(IFSObject<?> object);

	static void registerIFSVendorPlugin(Class<? extends IFSVendorPluginI> adapter) {
		try {
			vendorPlugins.add(adapter.getDeclaredConstructor().newInstance());
			System.out.println("! IFSFindingAid vendorPlugin " + adapter + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
