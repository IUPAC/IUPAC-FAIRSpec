package org.iupac.fairspec.struc;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

public class IFSStructureRepresentation extends IFSRepresentation {

	public static final String IFS_STRUCTURE_REP_MOL      = "IFS.representation.struc.mol"; 
	public static final String IFS_STRUCTURE_REP_MOL_2D   = "IFS.representation.struc.mol.2d"; 
	public static final String IFS_STRUCTURE_REP_MOL_3D   = "IFS.representation.struc.mol.3d"; 
	public static final String IFS_STRUCTURE_REP_SDF      = "IFS.representation.struc.sdf";
	public static final String IFS_STRUCTURE_REP_SDF_2D   = "IFS.representation.struc.sdf.2d";
	public static final String IFS_STRUCTURE_REP_SDF_3D   = "IFS.representation.struc.sdf.3d";
	public static final String IFS_STRUCTURE_REP_CDX      = "IFS.representation.struc.cdx";
	public static final String IFS_STRUCTURE_REP_CDXML    = "IFS.representation.struc.cdxml";
	public static final String IFS_STRUCTURE_REP_PNG      = "IFS.representation.struc.png"; 
	public static final String IFS_STRUCTURE_REP_UNKNOWN  = "IFS.representation.struc.unknown";

	private final static String[] repNames = new String[] {
			IFS_STRUCTURE_REP_MOL, 
			IFS_STRUCTURE_REP_MOL_2D, 
			IFS_STRUCTURE_REP_MOL_3D, 
			IFS_STRUCTURE_REP_SDF,
			IFS_STRUCTURE_REP_CDX,
			IFS_STRUCTURE_REP_CDXML,
			IFS_STRUCTURE_REP_UNKNOWN
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
	public IFSStructureRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

	/**
	 * Allow for transfer from a temporary generic representation to a structure representation.
	 * 
	 * @param rep
	 */
	public IFSStructureRepresentation(IFSRepresentation rep) {
		super(rep);
	}

}
