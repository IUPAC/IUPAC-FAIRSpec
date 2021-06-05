package org.iupac.fairspec.common;

import org.iupac.fairspec.common.IFSConst.PROPERTY_TYPE;
import org.iupac.fairspec.common.IFSConst.UNITS;

public class IFSProperty {

	private final String name;
	private final PROPERTY_TYPE type;
	private final UNITS units;
	
	private Object value;

	public IFSProperty(String name, PROPERTY_TYPE type, UNITS units) {
		this.name = name;
		this.type = type;
		this.units = units;
	}

	public String getName() {
		return name;
	}

	public UNITS getUnits() {
		return units;
	}
	
	public PROPERTY_TYPE getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	
}
