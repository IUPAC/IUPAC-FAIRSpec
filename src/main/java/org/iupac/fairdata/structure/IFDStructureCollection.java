package org.iupac.fairdata.structure;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;

@SuppressWarnings("serial")
public class IFDStructureCollection extends IFDCollection<IFDStructure> {

	
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
	
	@Override
	public boolean add(IFDStructure t) {
		if (contains(t)) {
			return false;
		}
		return super.add(t);
	}

	public IFDStructure getStructureFor(String rootPath, String localName, String param, String value, String ifdPath, String mediaType) {
		String keyValue = param + ";" + value;
		IFDStructure sd = (IFDStructure) (IFDRepresentableObject<? extends IFDRepresentation>) map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDStructure(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.addRepresentation(ifdPath, localName, param, mediaType);
		return (IFDStructure) sd;
	}

}