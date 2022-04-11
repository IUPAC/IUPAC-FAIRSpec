package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDObjectI;
import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;

/**
 * IFDObject is the base abstract superclass for all IFD Data Model metadata
 * objects.
 * 
 * IFDObject extends ArrayList so as to allow for storing and retrieving
 * multiple objects or representations with standard ArrayList methods.
 * 
 * An IFDObject can be initialized with just an arbitrary name or with a name
 * and a maximum count, or with a name, a maximum count, and an "immutable"
 * starting set that are not allowed to be set or removed or changed.
 * 
 * IFDObject and its subclasses implement the IFDObjectI interface and come in
 * two flavors: IFDRepresentableObject and IFDCollection.
 * 
 * IFDObject will self-serialize.
 * 
 * 
 * *** IFDRepresentableObject ***
 * 
 * IFDStructure, IFDDataObject, IFDSample, IFDStructureAnalysis,
 * IFDSampleAnalysis
 * 
 * A class extending IFDRepresentableObject is expected to have one or more
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
 * Note that there is no mechanism for IFDStructure, IFDSample, or IFDDataObject
 * objects to refer to each other. This is a key aspect of the IFD Data Model.
 * The model is based on the idea of a collection, and it is the IFDCollection
 * objects that do this referencing. This model allows for a relatively simple
 * and flexible packaging of objects, with their relationships identified only
 * in key metadata resources.
 * 
 * *** IFDCollection ***
 * 
 * IFDCollection objects are "pure metadata" and thus have no digital
 * representation outside of that role. They point to and associate IFDObject
 * instances. IFDCollection objects may be collections of IFDCollection objects.
 * This allows for a nesting of collections in meaningful ways. For example,
 * IFDStructureDataAssociation objects maintain two specific collections: one of
 * structures, and one of data objects.
 * 
 * To be sure, an IFDCollection could represent a Digital Object in the form of
 * a zip file (and usually does) or perhaps a finding aid. Nonetheless, this
 * does not make it "representable" in the sense that it is likely to have
 * multiple distinctly different representations that characterize
 * IFDRepresentableObject classes.
 * 
 * *** IFDAssociation ***
 * 
 * An IFDAssociation is an IFDCollection of IFDCollections that contains exactly
 * two IFDCollections -- "Collection 1" and "Collection 2". These may be
 * IFDStructure and IFDDataObject, for example. Like its superclass, an
 * IFDAssociation has no representations, only metadata.
 * 
 * *** IFDAnalysis ***
 * 
 * An IFDAnalysis is an IFDAssociation that also has an IFDRepressntableObject
 * that is an IFDAnalysisObject. This allows for metadata and associated data
 * that are specific to a more detailed analsysis, not just a simple
 * IFDAssociation.
 * 
 * *** core IFDObjectI classes ***
 * 
 * -- IFDStructure --
 * 
 * A chemically-related structural object, which may have several
 * representations, such as a 2D or 3D MOL file, an InChI or InChIKey, one or
 * more chemical names, one or more SMILES strings, or even just a PNG image of
 * a drawn structure. Each of these representations serves a purpose. Some are
 * more "interoperable" than others, but each in its own way may be more useful
 * in a given context.
 * 
 * -- IFDDataObject --
 * 
 * A data object references one or more Digital Objects that are what a
 * scientist would call "their data", such as a full vendor experiment dataset
 * (a Bruker NMR experiment), a PNG image of a spectrum, or a peaklist.
 * 
 * For the IUPAC FAIRSpec Project, we extend this class to IFDDataObject and its
 * subclasses (IFDNMRSpecData, IFDIRSpecData, etc.). We recognize, however, that
 * the system we are developing is not limited to spectroscopic data only, and
 * future implementations of this data model may subclass IFDDataObject in
 * completely different ways for completely different purposes.
 * 
 * -- IFDSample --
 * 
 * IFDSample corresponds to a specific physical sample that may or may not (yet)
 * have a chemical structure, spectroscopic data, or representations associated
 * with it. Typically associated with one or more IFDDataObjects; alternatively
 * associated with one or more IFDStructures, but such associations preferably
 * be associated indirectly, via an IFDDataObject, if that is available.
 * 
 * -- IFDStructureCollection and IFDDataObjectCollection --
 * 
 * These two classes each collect distinctly different structures and data,
 * respectively, that are related in some way. For instance, all the compounds
 * referred to in a publication, all the spectra for a publication, or all the
 * spectra relating to a specific compound. (Or, perhaps, all the compounds in a
 * mixture that are identified in an NMR spectrum.)
 * 
 * 
 * *** associative IFDObjectI classes ***
 *
 * The org/iupac/fairdata/derived package includes a number of more specific
 * types of associations, including: IFDStructureDataAssociation,
 * IFDSampleDataAssociation, IFDStructureDataAnalysis, and
 * IFDSampleDataAnalysis, along with their respective collections. These classes
 * associate specific structures, samples, and data to each other.
 * 
 * 
 * -- IFDStructureDataAssociation --
 * 
 * This class correlates one or more IFDStructure instances with one or more
 * IFDDataObject instances. It provides the "connecting links" between spectra
 * and structure.
 * 
 * -- IFDSampleDataAssociation --
 * 
 * This class correlates one IFDSample instance with one or more IFDDataObject
 * and IFDStructure instances. It provides a way of linking samples with their
 * associated molecular structure (if known) and spectra (if taken).
 * 
 * -- IFDStructureDataAnalysis --
 * 
 * The IFDStructureDataAnalysis class is intended to represent a detailed
 * correlation between chemical structure for a compound and its related
 * experimental or theoretical spectroscopic data. For instance, it might
 * correlate specific atoms or groups of atoms of a chemical structure with
 * specific signals in a spectrum or other sort of data object.
 *
 *
 * -- IFDFindingAid --
 *
 * The IFDFindingAid class is a master class for the organizing metadata in
 * relation to a collection. It is not a collection itself, and it has no
 * representations, though as an IFDObject, it can be serialized. This class
 * ultimately extends ArrayList, so all of the methods of that standard Java
 * class are allowed (add, put, replace, etc.)
 * 
 * The IFDFindingAid references the IFDCollectionSet "collection of
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
 * It is the IFDFindingAid that ultimately distinguishes the IUPAC FAIRSpec data
 * model from other models. It should contain all the information that forms the
 * basis of what the user sees. It should reveal information about the
 * collection that allows users to quickly determine whether data in this
 * collection are relevant to their interests or not. The IFDFindingAid could be
 * static -- a Digital Item within a repository collection -- or dynamically
 * created in response to a query.
 * 
 * 
 * @author hansonr
 *
 * @param <T> the class for items of the list - IFDRepresentations for
 *        DataObjects and Structures; relevant IFDObject types for
 *        IFDCollections
 */
@SuppressWarnings("serial")
public abstract class IFDObject<T> extends ArrayList<T> implements IFDObjectI<T>, IFDSerializableI {

	abstract void serializeList(IFDSerializerI serializer);

	protected static int indexCount;

	/**
	 * a unique identifier for debugging
	 */
	protected int index;

	/**
	 * an arbitrary name given to provide some sort of context
	 */
	protected String name;

	/**
	 * an arbitrary identifier to provide some sort of context
	 */
	protected String id;

	/**
	 * known properties of this class, fully identified in terms of data type and
	 * units
	 * 
	 */
	protected final Map<String, IFDProperty> htProps = new TreeMap<>();

	/**
	 * generic properties that could be anything but are not in the list of known
	 * properties
	 */
	protected Map<String, Object> params = new TreeMap<>();

	/**
	 * the maximum number of items allowed in this list; may be 0
	 */
	private int maxCount;
	/**
	 * the minimum number of items allowed in this list, set by initializing it with
	 * a set of "fixed" items
	 */
	private int minCount;

	/**
	 * serialized
	 */
	protected String type;

	public IFDObject(String name, String type) {
		set(name, type, Integer.MAX_VALUE);
	}

	/**
	 * Set with an initial set, for an association. No element of the initial set
	 * can be null.
	 * 
	 * @param name
	 * @param type
	 * @param maxCount
	 * @param initialSet
	 * @throws IFDException if any element of a non-null initialSet is null
	 */
	@SuppressWarnings("unchecked")
	public IFDObject(String name, String type, int maxCount, T... initialSet) throws IFDException {
		set(name, type, maxCount);
		if (type != null)
			System.out.println("IFDObject type not null");
		if (initialSet == null) {
			minCount = 0;
		} else {
			minCount = initialSet.length;
			for (int i = 0; i < minCount; i++) {
				if (initialSet[i] == null)
					throw new IFDException("IFDObject initial set cannot be null");
				super.add(initialSet[i]);
			}
		}
	}

	private void set(String name, String type, int maxCount) {
		this.name = name;
		if (type == null)
			type = this.getClass().getName();
		this.type = type;
		this.maxCount = maxCount;
		this.index = indexCount++;
	}

	/**
	 * Set the minimum count of list objects such that an attempt to remove one of
	 * these objects will result in an IFDException.
	 * 
	 * @param n
	 */
	protected void setMinCount(int n) {
		minCount = n;
	}

	/**
	 * Generally set only by subclasses during construction, these IFDProperty
	 * definitions are added to the htProps list.
	 * 
	 * @param key
	 */
	protected void setProperties(String key, String notKey) {
		IFDConst.setProperties(htProps, key, notKey);
	}

	public Map<String, IFDProperty> getProperties() {
		return htProps;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setPropertyValue(String name, Object value) {
		// check for .representation., which is not stored in the object.
		if (IFDConst.isRepresentation(name))
			return;
		if (IFDConst.isLabel(name))
			this.name = value.toString();
		IFDProperty p = IFDConst.getIFDProperty(htProps, name);
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
		IFDProperty p = htProps.get(name);
		return (p == null ? params.get(name) : p.getValue());
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public String getID() {
		return id;//(id == null ? "" + index : id);
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
	public int getObjectCount() {
		return size();
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

	/**
	 * Subclasses can bypass the minimum/maximum restrictions.
	 * 
	 * @param index
	 * @param c
	 */
	protected void setSafely(int index, T c) {
		super.set(index, c);
	}

	private void checkRange(int index) {
		if (index < minCount)
			throw new IndexOutOfBoundsException("operation not allowed for index < " + minCount);
		if (index > maxCount)
			throw new IndexOutOfBoundsException("operation not allowed for index > " + maxCount);
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + " " + index + " " + " size=" + size() + "]";
	}

	/**
	 * 
	 * @return true if any property value is not null
	 */
	public boolean haveProperties() {
		for (IFDProperty p : htProps.values()) {
			if (p.getValue() != null)
				return true;
		}
		return false;
	}

	@Override
	public String getSerializedType() {
		return type;
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		serializeTop(serializer);
		serializeProps(serializer);
		serializeList(serializer);
	}

	protected void serializeTop(IFDSerializerI serializer) {
		serializeClass(serializer, getClass(), type);
		serializer.addAttr("name", getName());
		serializer.addAttr("id", getID());
	}

	static void serializeClass(IFDSerializerI serializer, Class<?> c, String stype) {
		serializer.addAttr("type", stype == null ? c.getName() : stype);
		if (serializeExtended(c).length() > 0)
			serializer.addAttr("typeExtends", serializeExtended(c));
	}

	public static void addTypes(Class<?> c, Map<String, Object> m) {
		String ctype = c.getName();
		m.put("type", ctype);
		String stype = serializeExtended(c);
		if (!stype.equals(ctype))
			m.put("typeExtends", stype);
	}

	private static Map<String, String> htExtendedTypes = new Hashtable<>();

	private static String serializeExtended(Class<?> t) {
		String name = t.getName();
		String et = htExtendedTypes.get(name);
		String n = name;
		if (et == null) {
			et = "";
			String sep = "";
			while ((t = t.getSuperclass()) != Object.class && (n = t.getName()).indexOf("IFDObject") < 0) {
				et += sep + n;
				sep = ";";
				if (n.startsWith("org.iupac.fairdata.core"))
					break;
			}
			htExtendedTypes.put(name, et);
		}
		return et;
	}

	protected void serializeProps(IFDSerializerI serializer) {
		if (haveProperties()) {
			// general serialization does not write out units
			Map<String, Object> map = new TreeMap<>();
			for (Entry<String, IFDProperty> e : htProps.entrySet()) {
				Object val = e.getValue().getValue();
				if (val != null)
					map.put(e.getKey(), val);
			}
			serializer.addObject("properties", map);
		}
		if (getParams().size() > 0)
			serializer.addObject("parameters", getParams());
	}

	@Override
	public boolean equals(Object o) {
		return (o == this);
//		if (!super.equals(o))
//			return false;
//		IFDObject<?> c = (IFDObject<?>) o;
//		return name.equals(c.getName());
	}

}