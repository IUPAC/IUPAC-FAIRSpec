package org.iupac.fairspec.core;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSException;

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

	public IFSStructure getStructureFor(String path, String param, String value, String objectFile) throws IFSException {
		String keyValue = param + ";" + value;
		IFSStructure sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFSStructure(path, param, value));
			add(sd);
		}
		sd.getRepresentation(param.indexOf(".param.") >= 0 ? "param" : objectFile);
		return sd;
	}

}