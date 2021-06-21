package org.iupac.fairspec.common;

import java.util.Map;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSConst.PROPERTY_TYPE;
import org.iupac.fairspec.common.IFSConst.PROPERTY_UNITS;

public class IFSProperty implements IFSSerializableI {

	private final String name;
	private final PROPERTY_TYPE type;
	private final PROPERTY_UNITS units;
	
	private Object value;

	public IFSProperty(String name, PROPERTY_TYPE type, PROPERTY_UNITS units) {
		this.name = name;
		this.type = type;
		this.units = units;
	}

	public IFSProperty(String name, PROPERTY_TYPE type) {
		this(name, type, PROPERTY_UNITS.NONE);
	}

	public IFSProperty(String name) {
		this(name, PROPERTY_TYPE.STRING, PROPERTY_UNITS.NONE);
	}


	public String getName() {
		return name;
	}

	public PROPERTY_UNITS getUnits() {
		return units;
	}
	
	public PROPERTY_TYPE getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public IFSProperty getClone(Object value) {
		IFSProperty p = new IFSProperty(name, type, units);
		p.value = value;
		return p;
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
		serializer.addAttr("type", type.toString());
		serializer.addAttr("units", units.toString());
		serializer.addValue(value);
	}
}
