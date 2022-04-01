package org.iupac.fairdata.structure;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.dataobject.IFDDataObject;

@SuppressWarnings("serial")
public class IFDStructureCollection extends IFDCollection<IFDObject<?>> {

	
	@Override
	public Class<?>[] getObjectTypes() {
		// TODO Auto-generated method stub
		return new Class<?>[] { IFDStructure.class };
	}
	

	public IFDStructureCollection(String name) {
		super(name, null);
	}
	
	public IFDStructureCollection(String name, IFDStructure structure) {
		this(name);
		add(structure);
	}

	@Override
	public IFDStructure get(int i) {
		return (IFDStructure) super.get(i);
	}
	
	public boolean add(IFDStructure t) {
		if (contains(t)) {
			return false;
		}
		return super.add(t);
	}

	@Override
	public boolean add(IFDObject<?> t) {
		System.out.println("IFDStructure error: " + t);
		return false;
	}
	
	public IFDStructure getStructureFor(String rootPath, String localName, String param, String value, String ifdPath, String mediaType) {
		String keyValue = param + ";" + value;
		IFDStructure sd = (IFDStructure) map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDStructure(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.addRepresentation(ifdPath, localName, param, mediaType);
		return (IFDStructure) sd;
	}

}