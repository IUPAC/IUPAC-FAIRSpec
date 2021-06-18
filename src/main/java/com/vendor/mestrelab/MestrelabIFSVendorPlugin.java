package com.vendor.mestrelab;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.core.IFSObject;

public class MestrelabIFSVendorPlugin implements IFSVendorPluginI {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.mestrelab.MestrelabIFSVendorPlugin.class);
	}
	
	public MestrelabIFSVendorPlugin() {
		
	}


	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getRezipRegex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getParamRegex() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getRezipPrefix(String dirname) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean doRezipInclude(String entryName) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void startRezip(IFSExtractorI extractor) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void endRezip() {
		// TODO Auto-generated method stub
		
	}
	
	
}