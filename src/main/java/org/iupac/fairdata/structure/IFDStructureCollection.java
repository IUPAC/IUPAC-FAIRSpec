package org.iupac.fairdata.struc;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

@SuppressWarnings("serial")
public class IFDStructureCollection extends IFDCollection<IFDStructure> {

	public IFDStructureCollection(String name) throws IFDException {
		super(name, ObjectType.StructureCollection);
	}
	
	public IFDStructureCollection(String name, IFDStructure structure) throws IFDException {
		super(name, ObjectType.Structure);
		addStructure(structure);
	}

	public void addStructure(IFDStructure s) {
		add(s);
	}

	private Map<String, IFDStructure> map = new HashMap<>();

	public IFDStructure getStructureFor(String rootPath, String localName, String param, String value, String ifdPath, String mediaType) throws IFDException {
		String keyValue = param + ";" + value;
		IFDStructure sd = map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDStructure(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.addRepresentation(ifdPath, localName, param, mediaType);
		return sd;
	}

}