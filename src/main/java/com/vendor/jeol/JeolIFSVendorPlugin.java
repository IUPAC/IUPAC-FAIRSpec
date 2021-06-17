package com.vendor.jeol;

import org.iupac.fairspec.api.IFSObjectI.ObjectType;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.core.IFSObject;

public class JeolIFSVendorPlugin implements IFSVendorPluginI {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.jeol.JeolIFSVendorPlugin.class);
	}

	public JeolIFSVendorPlugin() {
		
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
	public boolean accept(String fname, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean populateProperties(IFSObject<?> object) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
}