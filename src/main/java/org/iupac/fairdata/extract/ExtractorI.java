package org.iupac.fairdata.contrib;

import org.iupac.fairdata.core.IFDFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface ExtractorI {

	IFDFindingAid getFindingAid();

	void log(String string);

	void addPropertyOrRepresentation(String key, Object val, boolean isInLine, String mediaType);

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
