package org.iupac.fairdata.contrib;

import org.iupac.fairdata.core.IFDFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface ExtractorI {

	void addProperty(String param, Object val);

	IFDFindingAid getFindingAid();

	void log(String string);

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
