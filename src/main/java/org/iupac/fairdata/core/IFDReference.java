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
	private String localDir;

	/**
	 * label of this file
	 */
	private String localName;

	private String url;
	
	private String doi;

	private int index;

	private String insituExt;
	
	public IFDReference() {
		this(null, null, null, null);
	}

	/**
	 * 
	 * @param resourceID provides the resource path, ultimately
	 * @param originPath the full original path, if it exists, or where it is derived from
	 * @param localDir without closing "/"; may be null if this resource is data-only
	 * @param localName
	 */
	public IFDReference(String resourceID, Object originPath, String localDir, String localName) {
		this.index = ++test;
		this.resourceID = resourceID;
		setOriginPath(originPath);
		this.localDir = localDir;
		this.localName = localName;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getDOI() {
		return doi;
	}

	public void setDOI(String doi) {
		this.doi = doi;
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

	private String getLocalPath() {
		return (localDir == null ? getLocalName() : localDir + "/" + localName);
	}
	
	public String getLocalName() {
		return localName;
	}

	/**
	 * The -insitu flag is being used. This means that any derived files, including
	 * ones found within zip files in the collection have no actual file existing.
	 * 
	 * Two possibilities:
	 * 
	 * origin path contains "|" and so is internal to a zip file:
	 * 
	 * keep the data; treat normally.
	 * 
	 * otherwise:
	 * 
	 * keep the data only if the localName is null or the localName's extension is
	 * not the same as the origin path's (indicating a derived file, for example,
	 * xxx.cdxml and xxx.cdxml.mol)
	 * 
	 * to be run only just before serialization
	 * 
	 * @return true if data can be cleared
	 */
	boolean checkInSitu(boolean hasData) {
		if (originPath == null || localName == null)
			return false;
		String op = originPath.toString();
		int pt = localName.lastIndexOf(".");
		String ext = (pt >= 0 ? localName.substring(pt) : "");
		if (pt >= 0 && !op.endsWith(ext)) {
			// will necessarily have data
			if (!hasData)
				System.err.println("IFDReference insitu but has no data: " + op);
			insituExt = ext;
			localDir = null;
			return false;
		}
		insituExt = "";
		if (op.indexOf("|") < 0) {
			// we can just display the local data using the origin path;
			// if there is data, delete it
			return true;
		}
		// within a ZIP file. Had better be data!
		if (!hasData)
			System.err.println("IFDReference insitu but has no data: " + op);
		// clear the local directory so that "localName" is given in the finding aid
		localDir = null;
		// set the localName to be the origin path
		localName = originPath + "";
		return false;
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
			if (localDir == null) {
				serializer.addAttr("localName", localName);
			} else {
				serializer.addAttr("localPath", originPath + "");
			}
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

	@Override
	public String toString() {
		return "[IFDReference " + index + " " 
		+ (localDir == null ? "" : localDir + "::") 
		+ (url == null && doi == null ? originPath : "") 
		+ ">as>" + localName 
		+ (url == null ? "" : " url:" + url) 
		+ (doi == null ? "" : " doi:" + doi) 
		+ "]";
	}


}