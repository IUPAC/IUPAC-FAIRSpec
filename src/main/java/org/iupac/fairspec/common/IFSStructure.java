package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;

@SuppressWarnings("serial")
public class IFSStructure extends IFSObject<IFSStructureRepresentation> implements IFSCoreObject {

	@Override
	public ObjectType getObjectType() {
		return ObjectType.Structure;
	}

}
