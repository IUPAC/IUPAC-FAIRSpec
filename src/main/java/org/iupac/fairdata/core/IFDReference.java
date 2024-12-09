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
	 * source URL item in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private final String resourceID;
	
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
	 * label of this file
	 */
	private final String localName;

	private String url;
	
	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	private String doi;

	private int index;

	private String insituExt;
	
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
		this.resourceID = resourceID;
		setOriginPath(originPath);
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

	/**
	 * Two possibilities:
	 * 
	 * origin path contains "|" and so is internal to a zip file:
	 * 
	 * keep the data; treat normally.
	 * 
	 * otherwise:
	 * 
	 * keep the data only if the localName is null or the localName's last three
	 * chars are not the same as the origin path's (indicating a derived file, for
	 * example, xxx.cdxml and xxx.cdxml.mol)
	 * 
	 * to be run only just before serialization
	 * 
	 * @return true if data can be cleared
	 */
	public boolean checkInSitu() {
		if (originPath == null || localName == null)
			return false;
		String op = originPath.toString();
		if (op.indexOf("|") >= 0) {
			insituExt = "";
			return false;
		}
		int pt = localName.lastIndexOf(".");
		String ext = (pt >= 0 ? localName.substring(pt) : "");
		if (pt >= 0 && !op.endsWith(ext)) {
			insituExt = ext;			
	 		return false;
		}
		// we can just display the local data
		insituExt = "";
		return true;
	}
	
	@Override
	public String toString() {
		return "[IFDReference " + index + " " + (localDir == null ? "" : localDir + "::") + originPath + ">as>" + localName + " url:" + url +  " doi:" + doi + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		// System.out.println("IFDRef " + index + " " + localName + " " + url);
		IFDObject.serializeClass(serializer, getClass(), null);
		if (resourceID != null)
			serializer.addAttr("resourceID", resourceID);
		if (doi != null)
			serializer.addAttr("doi", doi);
		if (url != null)
			serializer.addAttr("url", url);
		if (insituExt != null) {
			serializer.addAttr("localPath", originPath + insituExt);
		} else {
			if (originPath != null && !originPath.equals(doi) && !originPath.equals(url))
				serializer.addAttr("originPath", originPath.toString());
			if (localName != null) {
				if (url != null || doi != null || localDir == null) {
					serializer.addAttr("localName", localName);
				} else {
					serializer.addAttr("localPath", getLocalPath());
					// TODO: Could add #page=" to origin; localPath is null?
				}
			}
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}


}