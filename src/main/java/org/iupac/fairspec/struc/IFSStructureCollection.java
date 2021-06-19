package org.iupac.fairspec.struc;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

@SuppressWarnings("serial")
public class IFSStructureCollection extends IFSCollection<IFSStructure> {

	public IFSStructureCollection(String name) {
		super(name, ObjectType.StructureCollection);
	}
	
	public IFSStructureCollection(String name, IFSStructure structure) {
		super(name, ObjectType.Structure);
		addStructure(structure);
	}

	public void addStructure(IFSStructure s) {
		add(s);
	}

	private Map<String, IFSStructure> map = new HashMap<>();

	public IFSStructure getStructureFor(String path, String localName, String param, String value, String zipName) throws IFSException {
		String keyValue = param + ";" + value;
		IFSStructure sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFSStructure(path, param, value));
			add(sd);
		}
		if (param.indexOf(".param.") < 0)
			sd.getRepresentation(zipName, localName, true);
		return sd;
	}

}