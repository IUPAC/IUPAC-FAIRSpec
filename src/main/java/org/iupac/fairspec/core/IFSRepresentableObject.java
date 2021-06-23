package org.iupac.fairspec.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

@SuppressWarnings("serial")
public abstract class IFSRepresentableObject<T extends IFSRepresentation> extends IFSObject<T> {

	public IFSRepresentableObject(String name, ObjectType type) {
		super(name, type);
	}

	protected final Map<String, IFSRepresentation> htReps = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public IFSRepresentation getRepresentation(String zipName, String localName, boolean createNew, String type, String subtype) {
		String key = path + "::" + zipName;
		IFSRepresentation rep = htReps.get(key);
		if (rep == null && createNew) {
			rep = newRepresentation(type, new IFSReference(zipName, localName, path), null, 0, type, subtype);
			add((T) rep);
			htReps.put(key, rep);
		}
		return rep;
	}

	/**
	 * Optional allowance for creating a new representation of this object type.
	 * This method should return null if it cannot process this request.
	 * 
	 * @param objectName
	 * @param ifsReference
	 * @param object
	 * @param len
	 * @param type TODO
	 * @param subtype TODO
	 * @return
	 * @throws IFSException
	 */
	abstract protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object,
			long len, String type, String subtype);

	public void removeRepresentationFor(String localName) {
		for (int i = size(); --i >= 0;)
			if (localName.equals(get(i).getRef().getLocalName())) {
				remove(i);
				break;
			}
	}

}
