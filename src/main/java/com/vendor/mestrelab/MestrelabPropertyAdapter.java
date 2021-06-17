package com.vendor.mestrelab;

import org.iupac.fairspec.api.IFSObjectI.ObjectType;
import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.api.IFSPropertyAdapterI;
import org.iupac.fairspec.core.IFSObject;

public class MestrelabPropertyAdapter implements IFSPropertyAdapterI {

	static {
		IFSFindingAid.registerPropertyAdapter(com.vendor.mestrelab.MestrelabPropertyAdapter.class);
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