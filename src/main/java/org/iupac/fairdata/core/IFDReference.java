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
	private final String rootPath;

	/**
	 * source URL in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private String resourceID;

	/**
	 * label of this file
	 */
	private String localName;
	
	public IFDReference(String resourceID, Object originPath, String localRoot, String localName) {
		if (!resourceID.equals("1") && originPath != null && originPath.toString().indexOf("png") >= 0)
			System.out.println("IFDRef ??? ");
		this.originPath = originPath;
		this.rootPath = localRoot;
		this.localName = localName;
		this.resourceID = resourceID;
	}

	public Object getOriginPath() {
		return originPath;
	}

	public String getResourceID() {
		return resourceID;
	}

	public String getRootPath() {
		return rootPath;
	}
	
	public String getLocalPath() {
		return (rootPath == null ? "" : rootPath + "/") + localName;
	}
	
	public String getLocalName() {
		return localName;
	}

	@Override
	public String toString() {
		return "[IFDReference " + (rootPath == null ? "" : rootPath + "::") + originPath + " :as::" + localName + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		IFDObject.serializeClass(serializer, getClass(), null);
		if (resourceID != null) {
			serializer.addAttr("resourceID", resourceID);
		}
		if (originPath != null)
			serializer.addAttr("originPath", originPath.toString());
		if (localName != null) {
			serializer.addAttr("path", getLocalPath());
			// TODO: Could add #page=" to origin; localPath is null?
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}

}