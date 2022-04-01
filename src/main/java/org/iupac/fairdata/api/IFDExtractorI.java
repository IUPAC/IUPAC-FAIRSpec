package org.iupac.fairdata.api;

import org.iupac.fairdata.core.IFDFAIRDataFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFDExtractorI {

	void addProperty(String param, Object val);

	IFDFAIRDataFindingAid getFindingAid();

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
