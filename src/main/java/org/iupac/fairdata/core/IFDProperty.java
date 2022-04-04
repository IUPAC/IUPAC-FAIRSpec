package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDConst.PROPERTY_TYPE;
import org.iupac.fairdata.common.IFDConst.PROPERTY_UNIT;

public class IFDProperty implements IFDSerializableI {

	private final String name;
	private final PROPERTY_TYPE dataType;
	private final PROPERTY_UNIT units;
	
	private Object value;

	public IFDProperty(String name, Object value, PROPERTY_TYPE dataType, PROPERTY_UNIT units) {
		this.name = name;
		this.dataType = dataType;
		this.units = units;
		this.value = value;
	}

	public IFDProperty(String name, PROPERTY_TYPE dataType, PROPERTY_UNIT units) {
		this(name, null, dataType, units);
	}
	
	public IFDProperty(String name, PROPERTY_TYPE dataType) {
		this(name, null, dataType, PROPERTY_UNIT.NONE);
	}

	public IFDProperty(String name) {
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

	public IFDProperty getClone(Object value) {
		return new IFDProperty(name, value, dataType, units);
	}
	
	@Override
	public String getSerializedType() {
		return "IFDProperty";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (value == null)
			return;
		serializer.addAttr("name", name);
		serializer.addAttr("type", dataType.toString());
		serializer.addAttr("units", units.toString());
		serializer.addValue(value);
	}
	
	@Override
	public String toString() {
		return (value == null ? "" : "[IFDProp " + name + "=" + value + "]");
	}
}
