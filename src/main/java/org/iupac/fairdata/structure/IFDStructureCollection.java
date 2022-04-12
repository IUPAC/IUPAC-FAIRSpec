package org.iupac.fairdata.structure;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;

@SuppressWarnings("serial")
public class IFDStructureCollection extends IFDCollection<IFDStructure> {

	public IFDStructureCollection() {
		super(null, null);
	}
	
	public IFDStructureCollection(IFDStructure structure) {
		this();
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
		String keyValue = param + ";" + ifdPath;
		IFDStructure sd = (IFDStructure) (IFDRepresentableObject<? extends IFDRepresentation>) map.get(keyValue);
		if (sd == null) {
			map.put(keyValue,  sd = new IFDStructure(rootPath, param, value));
			add(sd);
		}
		if (IFDConst.isRepresentation(param))
			sd.findOrAddRepresentation(ifdPath, localName, null, param, mediaType);
		return (IFDStructure) sd;
	}

}