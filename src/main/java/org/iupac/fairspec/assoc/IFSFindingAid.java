package org.iupac.fairspec.assoc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.iupac.fairspec.api.IFSSerializerI;
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

	private Map<String, Object> pubInfo;
	
	@SuppressWarnings("deprecation")

	protected int currentUrlIndex;

	private Date date = new Date();

	public IFSFindingAid(String name, ObjectType type, String sUrl) {
		super(name, type);
		urls.add(sUrl);
	}

	public List<String> getURLs() {
		return urls;
	}

	public int addUrl(String sUrl) {
		int urlIndex = urls.indexOf(sUrl);
		if (urlIndex < 0) {
			urls.add(sUrl);
			currentUrlIndex = urlIndex = urls.size() - 1;
		}
		return urlIndex;
	}

	public void setPubInfo(Map<String, Object> pubInfo) {
		this.pubInfo = pubInfo;
	}

	
	public Date getDate() {
		return date;
	}
	private boolean serializing;

	@SuppressWarnings("deprecation")
	@Override
	public void serialize(IFSSerializerI serializer) {
		if (serializing) {
			serializeTop(serializer);
			serializer.addObject("created", date.toGMTString());
			serializer.addObject("pubInfo", pubInfo);
			serializer.addObject("urls", urls);
			serializeProps(serializer);
			serializeList(serializer);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject("IFS.finding.aid", this);
			serializing = false;
		}
	}
	
}