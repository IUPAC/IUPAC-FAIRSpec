package org.iupac.fairdata.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDResource;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

/**
 * The IFDFAIRDataFindingAid class is a master class for the organizing metadata in relation to a collection. 
 * It is not a collection itself, and it has no representations, though as an IFDObject, it can 
 * be serialized. This class ultimately extends ArrayList, so all of the methods of
 * that standard Java class are allowed (add, put, replace, etc.)
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDFAIRDataFindingAid extends IFDObject<IFDObject<?>> {

	{
		setProperties("IFD_PROP_FAIRDATA_COLLECTION_", null);
	}

	protected IFDFAIRDataCollection collection;
	
	protected List<IFDResource> dataSources = new ArrayList<>();

	protected int currentSourceIndex = -1;

	private Map<String, Object> publicationInfo;

	private Date date = new Date();

	private String creator;

	public IFDFAIRDataFindingAid(String name, String type, String creator) {
		super(name, type);
		this.creator = creator;
		collection = new IFDFAIRDataCollection(IFDConst.IFD_FINDING_AID);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addCollection(IFDCollection c) {
		collection.add(c);
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
		return dataSources;
	}
	
	

	/**
	 * Add a source or return the index of a source if already present
	 * 
	 * @param ref the reference for this source
	 * @return index of the source if found or added
	 */
	public IFDResource addOrReturnSource(String ref) {
		IFDResource r;
		for (int i = dataSources.size(); --i >= 0;) {
			r = dataSources.get(i);
			if (r.getRef().equals(ref)) {
				return r;
			}
		}
		r = new IFDResource(ref, 0);
		currentSourceIndex = dataSources.size();
		dataSources.add(r);
		return r;
	}

	/**
	 * Set the current source to the indicated index.
	 * 
	 * @param index
	 * @return the indicated resource
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public IFDResource setCurrentSourceIndex(int index) {
		if (index < 0 || index >= dataSources.size())
			throw new ArrayIndexOutOfBoundsException();
		currentSourceIndex = index;
		return dataSources.get(index);
	}
	
	public int getCurrentSourceIndex() {
		return currentSourceIndex;
	}
	
	/**
	 * Set the byte length of the current source.
	 * 
	 * @param len
	 */
	public void setCurrentSourceLength(long len) {
		if (currentSourceIndex < 0)
			throw new ArrayIndexOutOfBoundsException();
		if (currentSourceIndex >= 0)
			dataSources.get(currentSourceIndex).setLength(len);
	}

	public void setPubInfo(Map<String, Object> pubInfo) {
		this.publicationInfo = pubInfo;
	}

	public Date getDate() {
		return date;
	}

	private boolean serializing;

	public void finalizeCollections(IFDSerializerI serializer) {
		collection.finalizeCollections(serializer);
	}

	public List<IFDResource> getDataSources() {
		return dataSources;
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		collection.finalizeCollections(serializer);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void serialize(IFDSerializerI serializer) {
		if (serializing) {
			serializeTop(serializer);
			serializer.addObject("created", date.toGMTString());
			if (getCreator() != null)
				serializer.addObject("createdBy", getCreator());
			if (publicationInfo != null)
				serializer.addObject("publicationInfo", publicationInfo);
			serializer.addObject("dataSources", dataSources);
			serializeProps(serializer);
			serializeList(serializer);
		} else {
			// addObject will call this method after wrapping
			serializing = true;
			serializer.addObject(IFDConst.IFD_FINDING_AID, this);
			serializing = false;
		}
	}

	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * [rootname]_IFD_PROP_FAIRDATA_COLLECTION.[ext] and optionally create an
	 * _IFD_collection.zip in that same directory.
	 * 
	 * @param targetDir  or null for no output
	 * @param rootName   a prefix root to add to the _IFD_PROP_FAIRDATA_COLLECTION.json
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

	@Override
	public void setPropertyValue(String name, Object value) {
		if (name.startsWith(IFDConst.IFD_FINDING_AID))
			super.setPropertyValue(name, value);
		else
			collection.setPropertyValue(name, value);		
	}
	
	@Override
	public Object getPropertyValue(String name) {
		if (name.startsWith(IFDConst.IFD_FINDING_AID))
			return super.getPropertyValue(name);
		return collection.getPropertyValue(name);
	}



}