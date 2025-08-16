package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst.PROPERTY_TYPE;
import org.iupac.fairdata.common.IFDConst.PROPERTY_UNIT;

public class IFDProperty implements IFDSerializableI {

	private final String name;
	private final PROPERTY_TYPE dataType;
	private final PROPERTY_UNIT units;
	
	private Object value;
	private String source;

	/**
	 * non-null value to represent null; saved as null
	 */
	public static final String NULL = "\1";

	public IFDProperty(String name, Object value, PROPERTY_TYPE dataType, PROPERTY_UNIT units) {
		this.name = name;
		this.dataType = dataType;
		this.units = units;
		setValue(value);
	}

	public IFDProperty(String name, Object value, PROPERTY_TYPE dataType) {
		this(name, value, dataType, PROPERTY_UNIT.NONE);
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

	public IFDProperty getInherited(String newName) {
		return new IFDProperty(newName, null, dataType, units);
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
		// NOTE: This method is not used, as IFDObject
		// creates a TreeMap instead.  
//		if (value == null || value == "")
//			return;
//		serializer.addAttr("name", name);
//		serializer.addAttr("type", dataType.toString());
//		serializer.addAttr("units", units.toString());
//		serializer.addValue(value);
	}
	
	@Override
	public String toString() {
		return (value == null ? "" : "[IFDProp " + name + "=" + value + "]");
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public String getSource() {
		return source;
	}

	public void setValue(Object val) {
		value = (val == NULL ? null : val);
		source = null;
	}
}
