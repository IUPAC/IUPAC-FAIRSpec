package org.iupac.fairspec.struc;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

@SuppressWarnings("serial")
public class IFSStructureCollection extends IFSCollection<IFSStructure> {

	public IFSStructureCollection(String name) throws IFSException {
		super(name, ObjectType.StructureCollection);
	}
	
	public IFSStructureCollection(String name, IFSStructure structure) throws IFSException {
		super(name, ObjectType.Structure);
		addStructure(structure);
	}

	public void addStructure(IFSStructure s) {
		add(s);
	}

	private Map<String, IFSStructure> map = new HashMap<>();

	public IFSStructure getStructureFor(String path, String localName, String param, String value, String zipName, String mediaType) throws IFSException {
		String keyValue = param + ";" + value;
		IFSStructure sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFSStructure(path, param, value));
			add(sd);
		}
		if (IFSConst.isRepresentation(param))
			sd.getRepresentation(zipName, localName, true, param, mediaType);
		return sd;
	}

}