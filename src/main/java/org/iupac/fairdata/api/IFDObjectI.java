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
	
	String getDescription();
	
	String getID();
	
	String getIDorIndex();

	String getLabel();
	
	String getNote();
	
	T getObject(int index);
	
	int getObjectCount();
	
	String getTimestamp();
	
	void setDescription(String description);
	

	void setID(String id);

	void setLabel(String label);

	void setNote(String note);

	void setTimestamp(String timestamp);

	String getDOI();

	void setDOI(String doi);

	String getURL();

	void setURL(String url);


}