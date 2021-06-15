package org.iupac.fairspec.assoc;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.core.IFSCollection;

/**
 * The master class for a full collection, as from a publication or thesis or whatever.
 * This class ultimately extends ArrayList, so all of the methods of that class are allowed, 
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSFindingAid extends IFSCollection<IFSCollection<?>> {

	protected List<String> urls = new ArrayList<>();

	public IFSFindingAid(String name, ObjectType type, String sUrl) {
		super(name, type);
		urls.add(sUrl);
	}

	public List<String> getURLs() {
		return urls;
	}

	public void addUrl(String sUrl) {
		if (!urls.contains(sUrl))
			urls.add(sUrl);
	}


}