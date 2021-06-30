package org.iupac.fairspec.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;

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
	
	
	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty(IFSConst.IFS_FINDINGAID_DATA_LICENSE_NAME),
				new IFSProperty(IFSConst.IFS_FINDINGAID_DATA_LICENSE_URI),
		});
	}

	protected List<Resource> sources = new ArrayList<>();

	private Map<String, Object> pubInfo;
	
	protected int currentSourceIndex;

	private Date date = new Date();

	private Resource myResource = new Resource(null, 0);
	
	public static class Resource implements IFSSerializableI {
		private String ref;
		private long len;

		Resource(String ref, long length) {
			this.ref = ref;
			this.len = length;
		}

		@Override
		public void serialize(IFSSerializerI serializer) {
			if (ref != null)
				serializer.addAttr("ref", ref);
			if (len > 0)
				serializer.addAttrInt("len", len);
		}

		@Override
		public String getSerializedType() {
			return "resource";
		}
		
		@Override
		public String toString() {
			return "[Resource " + ref + " len " + len + "]";
		}
	}
	public IFSFindingAid(String name, String type, String sUrl) throws IFSException {
		super(name, type);
		sources.add(new Resource(sUrl, 0));
	}

	public List<Resource> getSources() {
		return sources;
	}

	public int addSource(String ref) {
		for (int i = sources.size(); --i >= 0;) {
			if (sources.get(i).ref.equals(ref)) {
				return i;
			}
		}
		sources.add(new Resource(ref, 0));
		return currentSourceIndex = sources.size() - 1;
	}

	public void setResource(String name, long len) {
		myResource.ref = name;
		myResource.len = len;
	}
	
	public void setCurrentURLLength(long len) {
		if (currentSourceIndex < 0)
			return;
		sources.get(currentSourceIndex).len = len;
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
			if (myResource.len > 0) {
				if (myResource.ref != null)
					serializer.addAttr("collectionRef", myResource.ref);
				if (myResource.len != 0)
					serializer.addAttrInt("collectionLen", myResource.len);
			}
			serializer.addObject("created", date.toGMTString());
			if (pubInfo != null)
				serializer.addObject("pubInfo", pubInfo);
			serializer.addObject("sources", sources);
			serializeProps(serializer);
			serializeList(serializer);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject(IFSConst.IFS_FINDINGAID, this);
			serializing = false;
		}
	}

}