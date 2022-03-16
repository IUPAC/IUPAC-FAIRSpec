package org.iupac.fairdata.api;

public interface IFDSerializableI {

	void serialize(IFDSerializerI serializer);

	String getSerializedType();

}
