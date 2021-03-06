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
	
	String getLabel();
	
	void setLabel(String label);
	
	String getID();
	
	void setID(String id);
	
	String getNote();
	
	void setNote(String note);
	
	String getDescription();
	
	void setDescription(String description);
	

	T getObject(int index);

	int getObjectCount();

}