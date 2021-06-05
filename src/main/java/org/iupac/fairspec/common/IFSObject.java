package org.iupac.fairspec.common;

import java.util.ArrayList;

import org.iupac.fairspec.api.IFSObjectApi;

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
public abstract class IFSObject<T> extends ArrayList<T> implements IFSObjectApi<T> {

	String name;	
	
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
		return "[" + getClass().getName() + " " + name + " n=" + size() + "]";
	}
}