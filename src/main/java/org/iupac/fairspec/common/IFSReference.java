package org.iupac.fairspec.common;

import java.util.ArrayList;

import org.iupac.fairspec.api.IFSObjectApi;
import org.iupac.fairspec.api.IFSObjectApi.ObjectType;

/**
 * An IFSReference object allows for saving a String or other form of reference. (But for now, just a String.)
 *  
 * @author hansonr

 */
public class IFSReference {

	private String ref;
	
	public IFSReference(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
	
}