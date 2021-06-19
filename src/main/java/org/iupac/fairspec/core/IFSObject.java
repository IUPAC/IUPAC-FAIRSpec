package org.iupac.fairspec.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

/**
 * 
 * IFSObject extends ArrayList so as to allow for storing and retrieving
 * multiple objects or representations with standard ArrayList methods.
 * 
 * It can be initialized with just an arbitrary name or with a name and a
 * maximum count, or with a name, a maximum count, and an "immutable" starting
 * set that are not allowed to be set or removed or changed.
 * 
 * IFSObject and its subclasses implement the IFSObjectI interface and come in
 * two flavors: IFSRepresentableObjectI and IFSAbstractObjectI.
 * 
 * 
 * *** IFSRepresentableObjectI ***
 * 
 * IFSStructure and IFSDataObject
 * 
 * A class implementing IFSRepresentableObjectI is expected to have one or more
 * distinctly different digital representations (byte sequences) that amount to
 * more than just metadata. These are the 2D or 3D MOL or SDF models, InChI or
 * SMILES strings representing chemical structure, and the experimental,
 * predicted, or simulated NMR or IR data associated with a chemical compound or
 * mixture of compounds.
 * 
 * The representations themselves, in the form of IFSStructureRepresentation and
 * IFSDataObjectRepresentation, do not implement IFSObjectI. (They are not
 * themselves lists of anything.) They could be any Java Object, but most likely
 * will be of the type String or byte[] (byte array). They represent the actual
 * "Digital Items" that might be found in a ZIP file or within a cloud-based or
 * local file system directory.
 * 
 * 
 * *** IFSAbstractObjectI ***
 * 
 * IFSCollection and IFSStructureDataAssociation
 * 
 * These classes are "pure metadata" and thus have no digital representation
 * outside of that role. They point to and associate IFSObject instances.
 * IFSCollection objects in particular may be collections of IFSCollection
 * object. This allows for a nesting of collections in meaningful ways.
 * Similarly, IFSStructureDataAssociation objects maintain two specific
 * collections, one of structures, and one of data objects.
 * 
 * To be sure, an IFSCollection could describe a Digital Object in the form of a
 * zip file (and usually does). Nonetheless, this does not make it
 * "representable" in the sense that it is likely to have multiple distinctly
 * different representations that characterize IFSRepresentableObjectI classes.
 * 
 * 
 * *** core IFSObjectI classes ***
 * 
 * -- IFSStructure --
 * 
 * A chemically-related structural object, which may have several
 * representations, such as a 2D or 3D MOL file, an InChI or InChIKey, one or
 * more chemical names, one or more SMILES strings, or even just a PNG image of
 * a drawn structure. Each of these representations serves a purpose. Some are
 * more "interoperable" than others, but each in its own way may be more useful
 * in a given context.
 * 
 * -- IFSDataObject --
 * 
 * A data object references one or more Digital Objects that are what a
 * scientist would call "their data", such as a full vendor experiment dataset
 * (a Bruker NMR experiment), a PNG image of a spectrum, or a peaklist.
 * 
 * For the IUPAC FAIRSpec Project, we extend this class to IFSSpecData and its
 * subclasses (IFSNMRSpecData, IFSIRSpecData, etc.). We recognize, however, that
 * the system we are developing is not limited to spectroscopic data only, and
 * future implementations of this data model may subclass IFSDataObject in
 * completely different ways for completely different purposes.
 * 
 * -- IFSStructureCollection and IFSDataObjectCollection --
 * 
 * These two classes each collect distinctly different structures and data,
 * respectively, that are related in some way. For instance, all the compounds
 * referred to in a publication, all the spectra for a publication, or all the
 * spectra relating to a specific compound. (Or, perhaps, all the compounds in a
 * mixture that are identified in an NMR spectrum.)
 * 
 * 
 * *** associative IFSObjectI classes ***
 *
 * The org/iupac/fairspec/assoc package includes three higher-level abstract
 * IFSObjectI classes: IFSStructureDataAssociation, IFSAnalysis (a subclass of
 * IFSStrctureDataAssociation), and IFSFindingAid (a subclass of IFSCollection).
 * 
 * 
 * -- IFSStructureDataAssociation --
 * 
 * This class correlates one or more IFSStructure instances with one or more
 * IFSDataObject instances. It provides the "connecting links" between spectra
 * and structure.
 * 
 * 
 * -- IFSAnalysis --
 * 
 * The IFSAnalysis class represents a more detailed correlation between chemical
 * structure and its related experimental or theoretical data. For instance, it
 * might correlate specific atoms or groups of atoms of a chemical structure
 * with specific signals in a spectrum or other sort of data object.
 *
 *
 * -- IFSFindingAid --
 *
 * In general, an overall collection will contain a "master" collection metadata
 * object, the IFSFindingAid. The IFSFindingAid is a special "collection of
 * collections," providing:
 * 
 * 1) metadata relating to the entire collection. For example, a publication or
 * thesis.
 * 
 * 2) high-level access to lower-level metadata. For example, a list of compound
 * names or key spectroscopy data characteristics such as NMR nucleus (1H, 13C,
 * 31P) and spectrometer nominal frequency (300 MHz, 800 MHz) or type of IR
 * analysis (such as ATR).
 * 
 * 3) pointers to finding aids for subcollections, each of which which may be a
 * pointer to one or more additional finding aids.
 * 
 * It is the IFSFindingAid that ultimately distinguishes the IUPAC FAIRSpec data
 * model from other models. It should contain all the information that forms the
 * basis of what the user sees. It should reveal information about the
 * collection that allows users to quickly determine whether data in this
 * collection are relevant to their interests or not. The IFSFindingAid could be
 * static -- a Digital Item within a repository collection -- or dynamically
 * created in response to a query.
 * 
 * 
 * @author hansonr
 *
 * @param <T> the class for items of the list
 */
@SuppressWarnings("serial")
public abstract class IFSObject<T> extends ArrayList<T> implements IFSObjectI<T>, Cloneable, IFSSerializableI {

	public final static String REP_TYPE_UNKNOWN = "unknown";

	protected static int indexCount;

	/**
	 * a unique identifier for debugging
	 */
	protected int index;

	/**
	 * index of source URL in the IFSFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private int urlIndex = -1;

	/**
	 * an arbitrary name given to provide some sort of context
	 */
	protected String name;

	/**
	 * an arbitrary identifier to provide some sort of context
	 */
	protected String id;

	/**
	 * an arbitrary path to provide some sort of context
	 */
	private String path;

	/**
	 * known properties of this class, fully identified in terms of data type and
	 * units
	 * 
	 */
	protected final Map<String, IFSProperty> htProps = new Hashtable<>();

	/**
	 * generic properties that could be anything but are not in the list of known
	 * properties
	 */
	protected Map<String, Object> params = new HashMap<>();

	/**
	 * the maximum number of items allowed in this list; may be 0
	 */
	private final int maxCount;

	/**
	 * the minimum number of items allowed in this list, set by initializing it with
	 * a set of "fixed" items
	 */
	private final int minCount;

	protected final ObjectType type;

	protected ObjectType subtype = ObjectType.Unknown;

	/**
	 * Optional allowance for creating a new representation of this object type.
	 * This method should return null if it cannot process this request.
	 * 
	 * @param objectName
	 * @param ifsReference
	 * @param object
	 * @param len
	 * @return
	 * @throws IFSException
	 */
	abstract protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object,
			long len);

	@SuppressWarnings("unchecked")
	public IFSObject(String name, ObjectType type) {
		this(name, type, Integer.MAX_VALUE);
	}

	@SuppressWarnings("unchecked")
	public IFSObject(String name, ObjectType type, int maxCount, T... initialSet) {
		this.name = name;
		this.type = type;
		this.maxCount = maxCount;
		this.index = indexCount++;
		if (initialSet == null) {
			minCount = 0;
		} else {
			minCount = initialSet.length;
			for (int i = 0; i < minCount; i++)
				super.add(initialSet[i]);
		}
	}

	/**
	 * Generally set only by subclasses during construction, these IFSProperty
	 * definitions are added to the htProps list.
	 * 
	 * @param properties
	 */
	protected void setProperties(IFSProperty[] properties) {
		for (int i = properties.length; --i >= 0;)
			htProps.put(properties[i].getName(), properties[i]);
	}

	public Map<String, IFSProperty> getProperties() {
		return htProps;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setPropertyValue(String name, Object value) {
		// check for .representation., which is not stored in the object.
		if (name.indexOf(".representation.") >= 0)
			return;
		IFSProperty p = htProps.get(name);
		if (p == null) {
			if (value == null)
				params.remove(name);
			else
				params.put(name, value);
			return;
		}
		htProps.put(name, p.getClone(value));
	}

	public Object getPropertyValue(String name) {
		IFSProperty p = htProps.get(name);
		return (p == null ? params.get(name) : p.getValue());
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUrlIndex(int urlIndex) {
		this.urlIndex = urlIndex;
	}

	public int getUrlIndex() {
		return urlIndex;
	}

	public String getID() {
		return (id == null ? "" + index : id);
	}

	public void setID(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public T getObject(int index) {
		return get(index);
	}

	@Override
	public ObjectType getObjectType() {
		return type;
	}

	@Override
	public int getObjectCount() {
		return size();
	}

	protected final Map<String, IFSRepresentation> htReps = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public IFSRepresentation getRepresentation(String zipName, String localName, boolean createNew) {
		IFSRepresentation rep = htReps.get(path + "::" + zipName);
		if (rep == null && createNew) {
			rep = newRepresentation(REP_TYPE_UNKNOWN, new IFSReference(zipName, localName, path), null, 0);
			add((T) rep);
			htReps.put(path + "::" + zipName, rep);
		}
		return rep;
	}

	@Override
	public T remove(int index) {
		checkRange(index);
		return super.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		int i = super.indexOf(o);
		if (i < 0)
			return false;
		checkRange(i);
		return super.remove(o);
	}

	@Override
	public T set(int index, T c) {
		checkRange(index);
		return super.set(index, c);
	}

	protected void setSafely(int index, T c) {
		super.set(index, c);
	}

	private void checkRange(int index) {
		if (index < minCount)
			throw new IndexOutOfBoundsException("operation not allowed for index < " + minCount);
		if (index > maxCount)
			throw new IndexOutOfBoundsException("operation not allowed for index > " + maxCount);
	}

	public Object clone() {
		IFSObject<?> c = (IFSObject<?>) super.clone();
		return c;
	}

	public ObjectType getType() {
		return type;
	}

	public String toString() {
		return "[" + getClass().getSimpleName() + " " + index + " " + " size=" + size() + "]";
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 
	 * @return true if any property value is not null
	 */
	public boolean haveProperties() {
		for (IFSProperty p : htProps.values()) {
			if (p.getValue() != null)
				return true;
		}
		return false;
	}

	@Override
	public String getSerializedType() {
		return type.toString();
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		serializeTop(serializer);
		serializeProps(serializer);
		serializeList(serializer);
	}

	protected void serializeTop(IFSSerializerI serializer) {
		serializer.addAttr("type", getSerializedType());
		if (subtype != null && subtype != ObjectType.Unknown)
			serializer.addAttr("subtype", subtype.toString());
		serializer.addAttr("name", getName());
		serializer.addAttr("id", getID());
	}

	protected void serializeProps(IFSSerializerI serializer) {
		if (urlIndex >= 0)
			serializer.addAttrInt("urlIndex", urlIndex);
		serializer.addAttr("path", getPath());
		if (haveProperties())
			serializer.addObject("properties", getProperties());
		if (getParams().size() > 0)
			serializer.addObject("params", getParams());
	}

	protected void serializeList(IFSSerializerI serializer) {
		if (size() > 0) {
			serializer.addAttrInt("listCount", size());
			List<T> list = new ArrayList<T>();
			for (int i = 0, n = size(); i < n; i++)
				list.add(get(i));
			serializer.addObject("list", list);
		}
	}

}