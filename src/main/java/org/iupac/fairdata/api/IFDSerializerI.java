package org.iupac.fairdata.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.iupac.fairdata.core.IFDFindingAid;

public interface IFDSerializerI {

	String serialize(IFDSerializableI obj);
	
	void openObject();

	void addAttr(String key, String val);

	void addAttrInt(String key, long ival);

	void addValue(Object val);

	void addObject(String string, Object oval);

	String closeObject();

	String getFileExt();

	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * [rootname]_IFD_PROP_FINDABLE_COLLECTION.[ext] and optionally create an _IFD_collection.zip
	 * in that same directory.
	 * 
	 * @param ifdFindingAid the requesting IFDFindingAid
	 * @param targetDir or null for no output
	 * @param rootName  a prefix root such as "acs.orglett.0c00572." to add to the _IFD_PROP_FINDABLE_COLLECTION.json (or.xml)
	 *                  finding aid created
	 * @param products  optionally, a list of directories containing the files referenced by the
	 *                  finding aid for creating the IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	String createSerialization(IFDFindingAid ifdFindingAid, File targetDir, String rootName, List<Object> products) throws IOException;
}
