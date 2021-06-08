package org.iupac.fairspec.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.iupac.fairspec.api.IFSObjectAPI;

/**
 * IFSObject extends ArrayList so as to allow for storing and retrieving
 * multiple objects or representations with standard set/get methods.
 * 
 * See IFSObjectApi for more information about this class.
 * 
 * @author hansonr
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class IFSObject<T> extends ArrayList<T> implements IFSObjectAPI<T>, Cloneable{

	public String REP_TYPE_UNKNOWN = "unknown";

	protected static int indexCount;
	
	protected int index;
	
	protected String name;	
	
	protected String id;

	protected final Map<String, IFSProperty> htProps = new Hashtable<>();
	private IFSProperty[] properties;
	
	public void setProperties(IFSProperty[] properties) {
		this.properties = properties;
		for (int i = properties.length; --i >= 0;)
			htProps.put(properties[i].getName(), properties[i]);				
	}

	public IFSProperty[] getProperties() {
		return properties;
	}
	
	protected Map<String, Object> params = new HashMap<>();
	
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

	public IFSObject (String name) {
		this.name = name;
		this.index = indexCount++;
		
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
	public int getObjectCount() {
		return size();
	}

	
	public Object clone() {
		IFSObject<?> c = (IFSObject<?>) super.clone();
		c.name = name;
		return c;		
	}
	
	public String toString() {
		return "[" + getClass().getSimpleName() + " " + index + " " + name + " n=" + size() + "]";
	}

	abstract protected T newRepresentation(String objectName, IFSReference ifsReference, Object object);
	
	protected final Map<String, T> htReps = new HashMap<>();
	
	public T getRepresentation(String objectName) {
		T rep = htReps.get(objectName);
		if (rep == null) {
			rep = newRepresentation(REP_TYPE_UNKNOWN, new IFSReference(objectName), null);
			add(rep);
			htReps.put(objectName, rep);
		}
		return rep;
	}

}