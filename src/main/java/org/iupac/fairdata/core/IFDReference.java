package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * An IFDReference object allows for saving a String or other form of reference.
 * (But for now, just a String.) It is intended to represent an actual file, not
 * a property.
 * 
 * @author hansonr
 * 
 */
public class IFDReference implements IFDSerializableI {

	static int test;
	
	/**
	 * Origin object; typically a ZIP file label; 
	 * but possibly a remote archive path
	 * 
	 * toString() will be used for serialization
	 */
	private Object originPath;
	
	/**
	 * root path to this file
	 */
	private final String localDir;

	/**
	 * source URL item in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private final String resourceID;
	
	/**
	 * label of this file
	 */
	private final String localName;

	private String url;
	
	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
		System.out.println("IFDREF " + index + " " + localName + " " + url);
	}

	private String doi;

	private int index;
	
	public String getDOI() {
		return doi;
	}

	public void setDOI(String doi) {
		this.doi = doi;
	}



	public IFDReference() {
		this(null, null, null, null);
	}

	/**
	 * 
	 * @param resourceID provides the resource path, ultimately
	 * @param originPath the full original path, if it exists, or where it is derived from
	 * @param localDir without closing "/"
	 * @param localName
	 */
	public IFDReference(String resourceID, Object originPath, String localDir, String localName) {
		this.index = ++test;
		System.out.println("IFDREF. " + index + " " + localName);
		this.resourceID = resourceID;
		this.originPath = originPath;
		this.localDir = localDir;
		this.localName = localName;
	}

	public Object getOriginPath() {
		return originPath;
	}

	public void setOriginPath(Object path) {
		originPath = path;
	}

	public String getResourceID() {
		return resourceID;
	}

	public String getlocalDir() {
		return localDir;
	}

	public String getLocalPath() {
		return (localDir == null ? "" : localDir + "/") + localName;
	}
	
	public String getLocalName() {
		return localName;
	}

	@Override
	public String toString() {
		return "[IFDReference " + test + " " + (localDir == null ? "" : localDir + "::") + originPath + " :as::" + localName + " url:" + url + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		System.out.println("IFDRef " + index + " " + localName + " " + url);
		IFDObject.serializeClass(serializer, getClass(), null);
		if (resourceID != null)
			serializer.addAttr("resourceID", resourceID);
		if (doi != null)
			serializer.addAttr("doi", doi);
		if (url != null)
			serializer.addAttr("url", url);
		if (originPath != null && !originPath.equals(doi) && !originPath.equals(url))
			serializer.addAttr("originPath", originPath.toString());
		if (localName != null) {
			if (localDir == null) {
				serializer.addAttr("localName", localName);
			} else {
				serializer.addAttr("localPath", getLocalPath());
				// TODO: Could add #page=" to origin; localPath is null?
			}
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}
	
	

}