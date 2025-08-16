package org.iupac.fairdata.extract;

import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;

/**
 * Implemented by MetaDataExtractor and DOICrawler. Includes methods for
 * reporting version and source as well as getting an IFDFindingAid, and
 * logging. Also used for adding properties from a vender plugin or a structure
 * representation or its properties from the structure helper.
 * 
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 *
 */
public interface MetadataReceiverI {

	String getVersion();

	String getCodeSource();

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

	/**
	 * 
	 * @param key
	 * @param val
	 * @param isInLine
	 * @param mediaType
	 * @param note
	 * @param method
	 */
	void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInLine, String mediaType, String note, String method);

	void addProperty(String key, Object val);

	void setSpreadSheetMetadata(IFDObject<?> o, String param);

	boolean hasStructureFor(byte[] bytes);

}
