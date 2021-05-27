package org.iupac.fairspec.common;

import java.util.ArrayList;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings("serial")
public abstract class IFSObject<T> extends ArrayList<T> implements IFSApi {

	String name;	
	
	@Override
	public String getName() {
		return name;
	}

}