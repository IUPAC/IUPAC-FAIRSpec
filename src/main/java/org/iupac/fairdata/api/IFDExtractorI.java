package org.iupac.fairdata.api;

import org.iupac.fairdata.spec.IFDSpecDataFindingAid;

import com.integratedgraphics.ifd.api.IFDVendorPluginI;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFDExtractorI {

	void addProperty(String param, Object val);

	IFDSpecDataFindingAid getFindingAid();

//	void registerFileVendor(String fname, IFDVendorPluginI ifdVendorPluginI);

}
