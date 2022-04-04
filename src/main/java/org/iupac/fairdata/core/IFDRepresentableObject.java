package org.iupac.fairdata.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDException;

/**
 * A class implementing IFDRepresentableObject is expected to have one or more
 * distinctly different digital representations (byte sequences) that amount to
 * more than just metadata. These are the 2D or 3D MOL or SDF models, InChI or
 * SMILES strings representing chemical structure, and the experimental,
 * predicted, or simulated NMR or IR data associated with a chemical compound or
 * mixture of compounds.
 *
 * The representations themselves, in the form of subclasses of
 * IFDRepresentation, do not implement IFDObjectI. (They are not themselves
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
public abstract class IFDRepresentableObject<T extends IFDRepresentation> extends IFDObject<T> {

	public IFDRepresentableObject(String name, String type) {
		super(name, type);
	}

	protected final Map<String, IFDRepresentation> htReps = new LinkedHashMap<>();

	/**
	 * Add a representation as long as it has not already been added.
	 * 
	 * @param ifdPath an origin name used to identify unique representations
	 * @param localName a localized name without / or |
	 * @param type
	 * @param subtype
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IFDRepresentation addRepresentation(String ifdPath, String localName, String type, String subtype) {
		IFDRepresentation rep = getRepresentation(ifdPath);
		if (rep == null) {
			rep = newRepresentation(type, new IFDReference(ifdPath, localName, path), null, 0, type, subtype);
			add((T) rep);
			htReps.put(path + "::" + ifdPath, rep);
		}
		return rep;
	}

	public IFDRepresentation getRepresentation(String ifdPath) {
		return htReps.get(path + "::" + ifdPath);
	}

	/**
	 * Optional allowance for creating a new representation of this object type.
	 * This method should return null if it cannot process this request.
	 * 
	 * @param objectName
	 * @param ifdReference
	 * @param object
	 * @param len
	 * @param type TODO
	 * @param subtype TODO
	 * @return
	 * @throws IFDException
	 */
	abstract protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object,
			long len, String type, String subtype);

	public void removeRepresentationFor(String localName) {
		for (int i = size(); --i >= 0;)
			if (localName.equals(get(i).getRef().getLocalName())) {
				remove(i);
				break;
			}
	}

}
