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

	/**
	 * Origin object; typically a ZIP file label; toString() will be used for serialization
	 */
	private final Object originPath;
	
	/**
	 * root path to this file
	 */
	private final String localDir;

	/**
	 * source URL in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private final String resourceID;
	
	/**
	 * label of this file
	 */
	private final String localName;
	
	/**
	 * persistent identifier to this representation
	 */
	private final String pid;

	/**
	 * 
	 * @param resourceID provides the resource path, ultimately
	 * @param originPath the full original path, if it exists, or where it is derived from
	 * @param localDir without closing "/"
	 * @param localName
	 */
	public IFDReference(String resourceID, Object originPath, String localDir, String localName, String pid) {
		this.resourceID = resourceID;
		this.originPath = originPath;
		this.localDir = localDir;
		this.localName = localName;
		this.pid = pid;
	}

	public Object getOriginPath() {
		return originPath;
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

	public String getPID() {
		return pid;
	}
	
	@Override
	public String toString() {
		return "[IFDReference " + (localDir == null ? "" : localDir + "::") + originPath + " :as::" + localName + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		IFDObject.serializeClass(serializer, getClass(), null);
		if (resourceID != null)
			serializer.addAttr("resourceID", resourceID);
		if (pid != null)
			serializer.addAttr("pid", pid);
		if (originPath != null)
			serializer.addAttr("originPath", originPath.toString());
		if (localName != null) {
			String s = getLocalPath();
			serializer.addAttr("path", s);
			// TODO: Could add #page=" to origin; localPath is null?
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}

}