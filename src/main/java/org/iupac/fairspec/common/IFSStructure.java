package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;

@SuppressWarnings("serial")
public class IFSStructure extends IFSObject<IFSStructureRepresentation> implements IFSCoreObject {

	public IFSStructure(String name) {
		super(name, ObjectType.Structure);
		
	}

	public IFSStructure(String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Structure);
		if (param.equals("IFS.structure.param.compound.id"))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	protected IFSStructureRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSStructureRepresentation(name, ref, obj, len);

	}
	
	@Override
	public String toString() {
		return (name == null ? super.toString() : "[IFSStructure " + index + " " + name + "]");
	}

}
