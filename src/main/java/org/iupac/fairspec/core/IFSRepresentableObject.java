package org.iupac.fairspec.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

/**
 * A class implementing IFSRepresentableObject is expected to have one or more
 * distinctly different digital representations (byte sequences) that amount to
 * more than just metadata. These are the 2D or 3D MOL or SDF models, InChI or
 * SMILES strings representing chemical structure, and the experimental,
 * predicted, or simulated NMR or IR data associated with a chemical compound or
 * mixture of compounds.
 *
 * The representations themselves, in the form of subclasses of
 * IFSRepresentation, do not implement IFSObjectI. (They are not themselves
 * inherently lists of anything.) They could be any Java Object, but most likely
 * will be of the type String or byte[] (byte array). They represent the actual
 * "Digital Items" that might be found in a ZIP file or within a cloud-based or
 * local file system directory.
 * 
 * @author hansonr
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class IFSRepresentableObject<T extends IFSRepresentation> extends IFSObject<T> {

	public IFSRepresentableObject(String name, String type) throws IFSException {
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
