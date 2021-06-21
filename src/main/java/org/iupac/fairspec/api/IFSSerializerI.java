package org.iupac.fairspec.api;

public interface IFSSerializerI {

	String serialize(IFSSerializableI obj);
	
	void openObject();

	void addAttr(String key, String val);

	void addAttrInt(String key, int ival);

	void addValue(Object val);

	void addObject(String string, Object oval);

	String closeObject();
}
