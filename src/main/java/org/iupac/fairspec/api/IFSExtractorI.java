package org.iupac.fairspec.api;

import org.iupac.fairspec.spec.IFSSpecDataFindingAid;

import com.integratedgraphics.ifs.api.IFSVendorPluginI;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFSExtractorI {

	void addProperty(String param, Object val);

	IFSSpecDataFindingAid getFindingAid();

//	void registerFileVendor(String fname, IFSVendorPluginI ifsVendorPluginI);

}
