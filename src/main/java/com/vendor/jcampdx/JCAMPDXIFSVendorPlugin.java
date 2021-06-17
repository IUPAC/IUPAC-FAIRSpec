package com.vendor.jcampdx;

import org.iupac.fairspec.api.IFSObjectI.ObjectType;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.core.IFSObject;

public class JCAMPDXIFSVendorPlugin implements IFSVendorPluginI {

	static {
		IFSVendorPluginI.registerIFSVendorPlugin(com.vendor.jcampdx.JCAMPDXIFSVendorPlugin.class);
	}
	
	public JCAMPDXIFSVendorPlugin() {
		
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
	public boolean accept(IFSFindingAid findingAid, String fname, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean populateProperties(IFSObject<?> object) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
}