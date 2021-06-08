package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;

@SuppressWarnings("serial")
public class IFSStructure extends IFSObject<IFSStructureRepresentation> implements IFSCoreObject {

	public IFSStructure(String name) {
		super(name);
		
	}

	public IFSStructure(String param, String value) throws IFSException {
		super(param + ";" + value);
		if (param.equals("IFS.structure.param.compound.id"))
			name = value;
		setPropertyValue(param, value);
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.Structure;
	}

	@Override
	protected IFSStructureRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSStructureRepresentation(name, ref, obj);

	}

}
