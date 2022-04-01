package org.iupac.fairdata.api;

import org.iupac.fairdata.core.IFDFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFDExtractorI {

	void addProperty(String param, Object val);

	IFDFindingAid getFindingAid();

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
