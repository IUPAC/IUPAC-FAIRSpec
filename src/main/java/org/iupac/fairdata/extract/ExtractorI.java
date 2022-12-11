package org.iupac.fairdata.extract;

import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface ExtractorI {

	IFDFindingAid getFindingAid();

	/**
	 * A simple logger that accepts messages and processes them based on 
	 * their initial few characters:
	 * 
	 * "!!" error to System.err and log FileOutputStream
	 * 
	 * "! " warning to System.err and log FileOutputStream
	 * 
	 * "!" but not error or warning to System.out and log FileOutputStream
	 * 
	 * anything else just to FileOutputStream
	 * 
	 * @param string
	 */
	void log(String string);

	void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInLine, String mediaType, String note);

	void addProperty(String key, Object val);

	void setNewObjectMetadata(IFDObject<?> o, String param);


//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
