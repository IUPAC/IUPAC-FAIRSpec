package org.iupac.fairdata.api;

/**
 * The IFDObectI is the public interface for the IFDObject. Note that IFDObject extends
 * ArrayList, so all the methods of ArrayList are also inherited.
 * 
 * See IFDObject for a detailed explanation of IFD objects.
 * 
 * 
 * @author hansonr
 *
 */
public interface IFDObjectI<T> {
	
		// IFDCollection, IFDDataObject, IFDDataObjectCollection, and IFDFindingAid
		// are all abstract and so do not express their own ObjectType

	String getName();

	T getObject(int index);

	int getObjectCount();

	String getObjectType();

}