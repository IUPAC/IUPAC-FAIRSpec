package org.iupac.fairdata.extract;

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

	void addProperty(String key, Object val);

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
