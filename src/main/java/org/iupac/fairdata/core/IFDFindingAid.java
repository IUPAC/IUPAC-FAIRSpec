package org.iupac.fairdata.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

/**
 * The IFDFindingAid class is a master class for the organizing metadata
 * in relation to a collection. It is not a collection itself, though it
 * maintains an IFDCollectionSet, and it has no representations, though as
 * an IFDObject, it can be serialized. This class ultimately extends ArrayList,
 * so all of the methods of that standard Java class are allowed (add, get,
 * delete, replace, etc.)
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDFindingAid extends IFDObject<IFDObject<?>> {

	{
		setProperties("IFD_PROP_FAIRDATA__", null);
	}

	protected IFDCollectionSet collectionSet;
	
	protected List<IFDResource> resources = new ArrayList<>();

	protected Date date = new Date();

	protected String creator;

	protected boolean serializing;

	public IFDFindingAid(String name, String type, String creator, IFDCollectionSet collection) throws IFDException {
		super(name, type, 1, (collection == null ? new IFDCollectionSet(null) : collection));
		this.creator = creator;
		collectionSet = (IFDCollectionSet) get(0);
	}

	public IFDCollectionSet getCollectionSet() {
		return collectionSet;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addCollection(IFDCollection c) {
		collectionSet.add(c);
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	
	/**
	 * Get the list of sources
	 * 
	 * @return
	 */
	public List<IFDResource> getSources() {
		return resources;
	}
	
	

	/**
	 * Add or find a resource based on its reference.
	 * 
	 * @param ref the reference for this resource
	 * @return index of the resource found or added
	 */
	public IFDResource addOrSetResource(String ref) {
		IFDResource r;
		for (int i = resources.size(); --i >= 0;) {
			r = resources.get(i);
			if (r.getRef().equals(ref)) {
				return r;
			}
		}
		r = new IFDResource(ref, resources.size(), 0);
		resources.add(r);
		return r;
	}

	public Date getDate() {
		return date;
	}

	public void finalizeCollectionSet() {
		collectionSet.finalizeCollections();
	}

	public List<IFDResource> getResources() {
		return resources;
	}

	@Override
	public void setPropertyValue(String name, Object value) {
		if (name.startsWith(IFDConst.IFD_FINDING_AID))
			super.setPropertyValue(name, value);
		else
			collectionSet.setPropertyValue(name, value);		
	}
	
	@Override
	public Object getPropertyValue(String name) {
		if (name.startsWith(IFDConst.IFD_FINDING_AID))
			return super.getPropertyValue(name);
		return collectionSet.getPropertyValue(name);
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		collectionSet.serialize(serializer);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void serialize(IFDSerializerI serializer) {
		if (serializing) {
			serializeTop(serializer);
			serializer.addObject("version", getVersion());
			serializer.addObject("created", date.toGMTString());
			if (getCreator() != null)
				serializer.addObject("createdBy", getCreator());
			serializer.addObject("statistics", getStatistics(new TreeMap<>()));
			addCitations(serializer);
			serializer.addObject("resources", resources);
			serializeProps(serializer);
			serializer.addObject("collection", collectionSet);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject(IFDConst.IFD_FINDING_AID, this);
			serializing = false;
		}
	}

	protected void addCitations(IFDSerializerI serializer) {
		// for subclasses
	}

	public String getVersion() {
		return IFDConst.getVersion();
	}

	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * [rootname]_IFD_PROP_COLLECTIONSET.[ext] and optionally create an
	 * _IFD_collection.zip in that same directory.
	 * 
	 * @param targetDir  or null for no output
	 * @param rootName   a prefix root to add to the _IFD_PROP_COLLECTIONSET.json
	 *                   (or.xml) finding aid created
	 * @param products   optionally, a list of directories containing the files
	 *                   referenced by the finding aid for creating the
	 *                   IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	public String createSerialization(File targetDir, String rootName, List<Object> products, IFDSerializerI serializer)
			throws IOException {
		if (serializer == null)
			serializer = new IFDDefaultJSONSerializer();
		return serializer.createSerialization(this, targetDir, rootName, products);
	}

	protected Map<String, Object> getStatistics(Map<String, Object> map) {
		map.put("resources", Integer.valueOf(resources.size()));
		collectionSet.getStatistics(map);
		return map;
	}
	


}