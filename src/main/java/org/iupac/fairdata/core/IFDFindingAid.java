package org.iupac.fairdata.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

/**
 * The master class for a full collection, as from a publication or thesis or whatever.
 * This class ultimately extends ArrayList, so all of the methods of that class are allowed, 
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDFindingAid extends IFDCollection<IFDCollection<?>> {
	
	
	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_ID),
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_REF),
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_LEN),
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_SOURCE_PUBLICATION_URI),
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_DATA_LICENSE_NAME),
				new IFDProperty(IFDConst.IFD_PROP_COLLECTION_DATA_LICENSE_URI),
		});
	}

	protected List<Resource> sources = new ArrayList<>();

	private Map<String, Object> pubInfo;
	
	protected int currentSourceIndex;

	private Date date = new Date();

	private String creator;

	/**
	 * A simple reference/length holder.
	 * 
	 * @author hansonr
	 *
	 */
	public static class Resource implements IFDSerializableI {
		private String ref;
		private long len;

		Resource(String ref, long length) {
			this.ref = ref;
			this.len = length;
		}

		@Override
		public void serialize(IFDSerializerI serializer) {
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
	public IFDFindingAid(String name, String type, String creator) throws IFDException {
		super(name, type);
		this.creator = creator;
	}

	/**
	 * Get the list of sources
	 * @return
	 */
	public List<Resource> getSources() {
		return sources;
	}

	/**
	 * Add a source or return the index of a source if already present
	 * @param ref the reference for this source
	 * @return index of the source if found or added
	 */
	public int addSource(String ref) {
		for (int i = sources.size(); --i >= 0;) {
			if (sources.get(i).ref.equals(ref)) {
				return i;
			}
		}
		sources.add(new Resource(ref, 0));
		return currentSourceIndex = sources.size() - 1;
	}

	public void setCurrentURLLength(long len) {
		if (currentSourceIndex >= 0)
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
	public void serialize(IFDSerializerI serializer) {
		if (serializing) {
			serializeTop(serializer);
			serializer.addObject("created", date.toGMTString());
			if (creator != null)
				serializer.addObject("createdBy", creator);
			if (pubInfo != null)
				serializer.addObject("pubInfo", pubInfo);
			serializer.addObject("sources", sources);
			serializeProps(serializer);
			serializeList(serializer);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject(IFDConst.IFD_FINDINGAID, this);
			serializing = false;
		}
	}
	
	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * [rootname]_IFD_PROP_COLLECTION.[ext] and optionally create an _IFD_collection.zip
	 * in that same directory.
	 * 
	 * @param targetDir or null for no output
	 * @param rootName  a prefix root to add to the _IFD_PROP_COLLECTION.json (or.xml)
	 *                  finding aid created
	 * @param products  optionally, a list of directories containing the files referenced by the
	 *                  finding aid for creating the IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	public String createSerialization(File targetDir, String rootName, List<Object> products, IFDSerializerI serializer) throws IOException {
		if (serializer == null)
			serializer = new IFDDefaultJSONSerializer();
		return serializer.createSerialization(this, targetDir, rootName, products);
	}
}