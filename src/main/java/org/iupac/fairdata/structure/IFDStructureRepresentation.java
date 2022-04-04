package org.iupac.fairdata.structure;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;

public class IFDStructureRepresentation extends IFDRepresentation {


//	private static String[] repNames;
//	
//	public static String[] getRepnames() {
//		return (repNames == null ? (repNames = IFDConst.getPropertiesAsArray("IFD_REP_STRUCTURE_", null)) : repNames);
//	}
//
	/**
	 * 
	 * @param ref
	 * @param data
	 * @param len
	 * @param type
	 * @param subtype
	 */
	public IFDStructureRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

	/**
	 * Allow for transfer from a temporary generic representation to a structure representation.
	 * 
	 * @param rep
	 */
	public IFDStructureRepresentation(IFDRepresentation rep) {
		super(rep);
	}

}
