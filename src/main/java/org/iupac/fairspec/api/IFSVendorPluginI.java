package org.iupac.fairspec.api;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.core.IFSObject;

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
 * it wants, including create new files from the data.
 * 
 * @author hansonr
 *
 */
public interface IFSVendorPluginI {

	public static List<IFSVendorPluginI> vendorPlugins = new ArrayList<>();


	boolean register();

	String getRegex(IFSObjectI.ObjectType type);

	boolean accept(IFSFindingAid findingAid, String fname, byte[] data);

	boolean populat(IFSObject<?> object);

	static void registerIFSVendorPlugin(Class<? extends IFSVendorPluginI> adapter) {
		try {
			vendorPlugins.add(adapter.getDeclaredConstructor().newInstance());
			System.out.println("! IFSFindingAid vendorPlugin " + adapter + " registered");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
