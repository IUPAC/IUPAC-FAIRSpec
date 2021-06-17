package org.iupac.fairspec.api;

public interface IFSSerializableI {

	void serialize(IFSSerializerI serializer);

	String getSerializedType();

}
