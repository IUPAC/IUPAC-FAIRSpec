package org.iupac.fairspec.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.iupac.fairspec.api.IFSObjectAPI;
import org.iupac.fairspec.api.IFSObjectAPI.ObjectType;

/**
 * IFSObject extends ArrayList so as to allow for storing and retrieving
 * multiple objects or representations with standard set/get methods.
 * 
 * It can be initialized with just an arbitrary name or with a name and a
 * maximum count, or with a name, a maximum count, and an "immutable" starting
 * set that are not allowed to be set or removed or changed.
 * 
 * See IFSObjectApi for more information about this class.
 * 
 * @author hansonr
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class IFSObject<T> extends ArrayList<T> implements IFSObjectAPI<T>, Cloneable {

	public final static String REP_TYPE_UNKNOWN = "unknown";

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

	/**
	 * Optional allowance for creating a new representation of this object type.
	 * This method should be overridden to throw an IFSException of no representations are allowed.
	 * 
	 * @param objectName
	 * @param ifsReference
	 * @param object
	 * @param len
	 * @return
	 * @throws IFSException
	 */
	abstract protected T newRepresentation(String objectName, IFSReference ifsReference, Object object, long len)
			throws IFSException;

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

	public void setPropertyValue(String name, Object value) throws IFSException {
		IFSProperty p = htProps.get(name);
		if (p == null) {
			params.put(name, value);
			return;
		}
		htProps.put(name, p.getClone(value));
	}

	public Object getPropertyValue(String name) throws IFSException {
		return params.get(name);
	}

	public String getID() {
		return id;
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

	protected final Map<String, T> htReps = new HashMap<>();

	public T getRepresentation(String objectName) throws IFSException {
		T rep = htReps.get(objectName);
		if (rep == null) {
			rep = newRepresentation(REP_TYPE_UNKNOWN, new IFSReference(objectName), null, 0);
			add(rep);
			htReps.put(objectName, rep);
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


}