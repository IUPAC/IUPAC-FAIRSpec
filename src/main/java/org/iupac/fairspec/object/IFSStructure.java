package org.iupac.fairspec.object;

import org.iupac.fairspec.api.IFSCoreObjectI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSStructureRepresentation;

@SuppressWarnings("serial")
public class IFSStructure extends IFSObject<IFSStructureRepresentation> implements IFSCoreObjectI {

	public IFSStructure(String name) {
		super(name, ObjectType.Structure);
		
	}

	public IFSStructure(String path, String param, String value) throws IFSException {
		super(param + ";" + value, ObjectType.Structure);
		setPath(path);
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
