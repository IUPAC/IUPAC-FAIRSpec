package org.iupac.fairdata.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;

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

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_FINDINGAID_FLAG);
	
	@Override
	protected String getIFDPropertyPrefix() {
		return propertyPrefix;
	}

	protected IFDCollectionSet collectionSet;
	
	protected List<Map<String, Object>> relatedItems;

	protected List<IFDResource> resources = new ArrayList<>();

	protected Date date = new Date();

	protected String creator;

	protected boolean serializing;

	/**
	 * ISO-8601
	 */
	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	
	static {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public IFDFindingAid(String label, String type, String creator, IFDCollectionSet collection) throws IFDException {
		super(label, type, 1, (collection == null ? new IFDCollectionSet(null) : collection));
		this.creator = creator;
		collectionSet = (IFDCollectionSet) get(0);
	}

	public void setCollectionSet(IFDCollectionSet set) {
		set(0, collectionSet = set);
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
	public IFDResource addOrSetResource(String ref, String rootPath) {
		IFDResource r;
		for (int i = resources.size(); --i >= 0;) {
			r = resources.get(i);
			if (r.getRef().equals(ref)) {
				return r;
			}
		}
		r = new IFDResource(ref, rootPath, "" + (resources.size() + 1), 0);
		resources.add(r);
		return r;
	}

	public List<IFDResource> getResources() {
		return resources;
	}

	public Date getDate() {
		return date;
	}

	public void finalizeCollectionSet(Map<String, Map<String, Object>> htURLReferences) {
		collectionSet.finalizeCollections(htURLReferences);
	}

	@Override
	public IFDProperty setPropertyValue(String label, Object value) {
		if (label.startsWith(IFDConst.IFD_FINDINGAID_FLAG))
			return super.setPropertyValue(label, value);
		return collectionSet.setPropertyValue(label, value);
	}
	
	@Override
	public Object getPropertyValue(String label) {
		if (label.startsWith(IFDConst.IFD_FINDINGAID_FLAG))
			return super.getPropertyValue(label);
		return collectionSet.getPropertyValue(label);
	}

	public String getVersion() {
		return IFDConst.getVersion();
	}

	protected Map<String, Object> getContentsMap(Map<String, Object> map) {
		if (relatedItems != null && relatedItems.size() > 0)
			map.put("relatedCount", Integer.valueOf(relatedItems.size()));
		map.put("resourceCount", Integer.valueOf(resources.size()));
		collectionSet.getContents(map);
		return map;
	}
	
	public void setRelatedTo(List<Map<String, Object>> citationMap) {
		relatedItems = citationMap;
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.serialize(collectionSet);
		collectionSet.serialize(serializer);
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (serializing) {
			serializeTop(serializer);
			serializer.addObject("version", getVersion());
			serializer.addObject("created", df.format(date));
			if (getCreator() != null)
				serializer.addObject("createdBy", getCreator());
			serializer.addObject("contents", getContentsMap(new TreeMap<>()));
			if (relatedItems != null)
				serializer.addObject("relatedItems", relatedItems);
			Object o = resources;
			if (serializer.isByID()) {
				Map<String, IFDResource> map = new LinkedHashMap<>();
				for (int i = 0; i < resources.size(); i++) {
					IFDResource r = resources.get(i);
					map.put(r.getID(), r);
				}
				o = map;				
			}
			serializer.addObject("resources", o);
			serializeProps(serializer);
			if (collectionSet != null)
				serializer.addObject("collectionSet", collectionSet);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject(IFDConst.IFD_FINDINGAID, this);
			serializing = false;
		}
	}

}