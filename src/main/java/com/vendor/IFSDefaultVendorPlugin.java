package com.vendor;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;

/**
 * An abstract class that sets methods only used by Bruker for extract and
 * rezipping involving Bruker directories and files.
 * 
 * @author hansonr
 *
 */
public abstract class IFSDefaultVendorPlugin implements IFSVendorPluginI {

	@Override
	public boolean doExtract(String entryName) {
		return true;
	}
	
	@Override
	public String getRezipRegex() {
		// N/A in default
		return null;
	}

	@Override
	public String getRezipPrefix(String dirname) {
		// N/A in default
		return null;
	}

	@Override
	public void startRezip(IFSExtractorI extractor) {
		// N/A in default
	}

	@Override
	public boolean doRezipInclude(String entryName) {
		// N/A in default
		return false;
	}

	@Override
	public void endRezip() {
		// N/A in default
	}


}
