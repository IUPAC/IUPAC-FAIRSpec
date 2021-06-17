package org.iupac.fairspec.api;

import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.core.IFSObject;

/**
 * A plugin class that implements IFSPropertyAdapterI can take parameter data and
 * fill IUPAC FAIRSpec standardized parameter property fields.
 * 
 * After registering with IFSFindingAid's static method
 * registerAdapter(IFSParamAdapterI), IFSFindingAid will query the plugin for
 * regex.Pattern values that it will recognize for given ObjectType values.
 * 
 * By accepting a data block either by reading its bytes or by recognizing a
 * file name, the plugin will be offered the opportunity to populate standard
 * IFSConst.Property fields with values.
 * 
 * @author hansonr
 *
 */
public interface IFSPropertyAdapterI {
	
	boolean register();
	
	String getRegex(IFSObjectI.ObjectType type);
	
	boolean accept(String fname, byte[] data);
	
	boolean populateProperties(IFSObject<?> object);
	
}
