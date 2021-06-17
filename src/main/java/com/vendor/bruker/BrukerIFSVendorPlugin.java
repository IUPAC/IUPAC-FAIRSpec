package com.vendor.bruker;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSObjectI.ObjectType;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.core.IFSObject;

public class BrukerIFSVendorPlugin implements IFSVendorPluginI {
	
	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.bruker.BrukerIFSVendorPlugin.class);
	}
	
	public BrukerIFSVendorPlugin() {
		
	}


	@Override
	public boolean register() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getRegex(ObjectType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean populateProperties(IFSObject<?> object) {
		// TODO Auto-generated method stub
		return false;
	}


	
	
	
}