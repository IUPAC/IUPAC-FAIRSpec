package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSConst.PROPERTY_TYPE;
import org.iupac.fairspec.common.IFSConst.PROPERTY_UNIT;

public class IFSProperty implements IFSSerializableI {

	private final String name;
	private final PROPERTY_TYPE dataType;
	private final PROPERTY_UNIT units;
	
	private Object value;

	public IFSProperty(String name, Object value, PROPERTY_TYPE dataType, PROPERTY_UNIT units) {
		this.name = name;
		this.dataType = dataType;
		this.units = units;
		this.value = value;
	}

	public IFSProperty(String name, PROPERTY_TYPE dataType, PROPERTY_UNIT units) {
		this(name, null, dataType, units);
	}
	
	public IFSProperty(String name, PROPERTY_TYPE dataType) {
		this(name, null, dataType, PROPERTY_UNIT.NONE);
	}

	public IFSProperty(String name) {
		this(name, null, PROPERTY_TYPE.STRING, PROPERTY_UNIT.NONE);
	}


	public String getName() {
		return name;
	}

	public PROPERTY_UNIT getUnits() {
		return units;
	}
	
	public PROPERTY_TYPE getType() {
		return dataType;
	}

	public Object getValue() {
		return value;
	}

	public IFSProperty getClone(Object value) {
		return new IFSProperty(name, value, dataType, units);
	}
	
	@Override
	public String getSerializedType() {
		return "IFSProperty";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		if (value == null)
			return;
		serializer.addAttr("name", name);
		serializer.addAttr("type", dataType.toString());
		serializer.addAttr("units", units.toString());
		serializer.addValue(value);
	}
}
