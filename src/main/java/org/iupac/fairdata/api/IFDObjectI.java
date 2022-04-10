package org.iupac.fairdata.api;

/**
 * The IFDObectI is the public interface for the IFDObject. 
 * 
 * See IFDObject for a detailed explanation of IFD objects.
 * 
 * 
 * @author hansonr
 *
 */
public interface IFDObjectI<T> {
	
	String getName();

	T getObject(int index);

	int getObjectCount();

}