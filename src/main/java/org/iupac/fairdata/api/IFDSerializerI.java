package org.iupac.fairdata.api;

import java.util.List;

import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;

public interface IFDSerializerI {

	void addAttr(String key, String val);

	void addAttrBoolean(String key, boolean value);

	void addAttrInt(String key, long ival);

	void addCollection(String key, IFDCollection<? extends IFDObject<?>> collection, boolean byID);

	void addList(String key, List<?> value);

	void addObject(String key, Object oval);

	void addValue(Object val);

	String closeObject();

	String getFileExt();

	boolean isByID();

	void openObject();
	
	String serialize(IFDSerializableI obj);

	void setByID(boolean tf);


}
