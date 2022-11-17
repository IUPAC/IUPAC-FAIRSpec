package org.iupac.fairdata.api;

import java.util.List;

public interface IFDSerializerI {

	String serialize(IFDSerializableI obj);
	
	void openObject();

	void addAttr(String key, String val);

	void addAttrInt(String key, long ival);

	void addAttrBoolean(String key, boolean value);

	void addValue(Object val);

	void addObject(String string, Object oval);

	String closeObject();

	String getFileExt();

	void addList(String key, List<?> value);

}
