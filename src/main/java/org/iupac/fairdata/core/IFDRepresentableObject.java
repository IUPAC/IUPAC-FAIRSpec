package org.iupac.fairdata.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;

/**
 * A class implementing IFDRepresentableObject is expected to have one or more
 * distinctly different digital representations (byte sequences) that amount to
 * more than just metadata. These are the 2D or 3D MOL or SDF models, InChI or
 * SMILES strings representing chemical structure, and the experimental,
 * predicted, or simulated NMR or IR data associated with a chemical compound or
 * mixture of compounds.
 *
 * The representations themselves, in the form of subclasses of
 * IFDRepresentation, do not implement IFDObjectI. (They are not themselves
 * inherently lists of anything.) They could be any Java Object, but most likely
 * will be of the type String or byte[] (byte array). They represent the actual
 * "Digital Items" that might be found in a ZIP file or within a cloud-based or
 * local file system directory.
 * 
 * @author hansonr
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class IFDRepresentableObject<T extends IFDRepresentation> extends IFDObject<T> {

	/**
	 * Optional allowance for creating a new representation of this object type.
	 * This method should return null if it cannot process this request.
	 * @param ifdReference null here indicates an inline object
	 * @param object 
	 * @param len
	 * @param type TODO
	 * @param subtype TODO
	 * 
	 * @return
	 * @throws IFDException
	 */
	abstract protected IFDRepresentation newRepresentation(IFDReference ifdReference, Object object, long len,
			String type, String subtype);


	/**
	 * the root path to this object for its IFDReference
	 */
	protected String rootPath;

	/**
	 * index of source URL in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private IFDResource resource;

	/**
	 * a map of unique paths to specific representations used to ensure that all
	 * representations are to unique objects
	 */
	protected final Map<String, IFDRepresentation> map = new LinkedHashMap<>();

	public IFDRepresentableObject(String label, String type) {
		super(label, type);
	}

	public String getPath() {
		return rootPath;
	}

	public void setPath(String path) {
		rootPath = path;
	}

	/**
	 * A reference to the highest level in the collection 
	 * as defined by the finding aid.
	 */
	protected IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> parentCollection;

	
	/**
	 * Add a representation as long as it has not already been added.
	 * 
	 * @param originPath an origin label used to identify unique representations
	 * @param localName a localized label without / or |
	 * @param data TODO
	 * @param type
	 * @param mediaType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IFDRepresentation findOrAddRepresentation(String originPath, String localName, Object data, String type, String mediaType) {
		boolean isInline = (localName == null);
		String key = (!isInline ? localName : data instanceof byte[] ? new String((byte[]) data) : data.toString());
		IFDRepresentation rep = getRepresentation(key);
		if (rep == null) {
			rep = newRepresentation((isInline ? null : new IFDReference(originPath, rootPath, localName)), data, 0, type, mediaType);
			add((T) rep);
			map.put(rootPath + "::" + key, rep);
			if (!isInline)
				map.put(rootPath + "::" + originPath, rep);
		}
		return rep;
	}

	public IFDRepresentation getRepresentation(String key) {
		IFDRepresentation r = map.get(rootPath + "::" + key);
//		System.out.println(this.index + " getRep " + key + " " + r);	
		return r;
	}


	public void removeRepresentationFor(String localName) {
		for (int i = size(); --i >= 0;)
			if (localName.equals(get(i).getRef().getLocalName())) {
				remove(i);
				break;
			}
	}

	/**
	 * When it comes time for an association, we want to know what top-level collection
	 * is that contains this object.
	 * 
	 * @param c
	 */
	public void setParentCollection(IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> c) {
		parentCollection= c;		
	}
	
	public IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> getParentCollection() {
		return parentCollection;
	}
	
	abstract public String getObjectType();

	public void setResource(IFDResource resource) {
		this.resource = resource;
	}

	public IFDResource getResource() {
		return resource;
	}

	
	@Override
	protected void serializeTop(IFDSerializerI serializer) {
		super.serializeTop(serializer);
//		if (objectType != null && objectType != "Unknown")
//			serializer.addAttr("objectType", objectType);
	}

	@Override
	protected void serializeProps(IFDSerializerI serializer) {
		super.serializeProps(serializer);
		if (resource != null)
			serializer.addAttr("resourceID", resource.getID());
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		if (size() > 0) {
			serializer.addList("representations", this);
		}
	}


}
