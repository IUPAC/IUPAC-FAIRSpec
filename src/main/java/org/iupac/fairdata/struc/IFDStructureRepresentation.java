package org.iupac.fairdata.struc;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

public class IFDStructureRepresentation extends IFDRepresentation {

	public static final String IFD_STRUCTURE_REP_MOL      = "IFD.representation.struc.mol"; 
	public static final String IFD_STRUCTURE_REP_MOL_2D   = "IFD.representation.struc.mol.2d"; 
	public static final String IFD_STRUCTURE_REP_MOL_3D   = "IFD.representation.struc.mol.3d"; 
	public static final String IFD_STRUCTURE_REP_SDF      = "IFD.representation.struc.sdf";
	public static final String IFD_STRUCTURE_REP_SDF_2D   = "IFD.representation.struc.sdf.2d";
	public static final String IFD_STRUCTURE_REP_SDF_3D   = "IFD.representation.struc.sdf.3d";
	public static final String IFD_STRUCTURE_REP_CDX      = "IFD.representation.struc.cdx";
	public static final String IFD_STRUCTURE_REP_CDXML    = "IFD.representation.struc.cdxml";
	public static final String IFD_STRUCTURE_REP_PNG      = "IFD.representation.struc.png"; 
	public static final String IFD_STRUCTURE_REP_UNKNOWN  = "IFD.representation.struc.unknown";

	private final static String[] repNames = new String[] {
			IFD_STRUCTURE_REP_MOL, 
			IFD_STRUCTURE_REP_MOL_2D, 
			IFD_STRUCTURE_REP_MOL_3D, 
			IFD_STRUCTURE_REP_SDF,
			IFD_STRUCTURE_REP_CDX,
			IFD_STRUCTURE_REP_CDXML,
			IFD_STRUCTURE_REP_UNKNOWN
	};

	public static String[] getRepnames() {
		return repNames;
	}

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
