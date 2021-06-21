package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

public class IFSStructureRepresentation extends IFSRepresentation {

	public static final String IFS_STRUCTURE_REP_MOL      = "IFS.structure.representation.mol"; 
	public static final String IFS_STRUCTURE_REP_MOL_2D   = "IFS.structure.representation.mol.2d"; 
	public static final String IFS_STRUCTURE_REP_MOL_3D   = "IFS.structure.representation.mol.3d"; 
	public static final String IFS_STRUCTURE_REP_SDF      = "IFS.structure.representation.sdf";
	public static final String IFS_STRUCTURE_REP_SDF_2D   = "IFS.structure.representation.sdf.2d";
	public static final String IFS_STRUCTURE_REP_SDF_3D   = "IFS.structure.representation.sdf.3d";
	public static final String IFS_STRUCTURE_REP_CDF      = "IFS.structure.representation.cdf";
	public static final String IFS_STRUCTURE_REP_CDXML    = "IFS.structure.representation.cdxml";
	public static final String IFS_STRUCTURE_REP_UNKNOWN  = "IFS.structure.representation.unkown";

	private final static String[] repNames = new String[] {
			IFS_STRUCTURE_REP_MOL, 
			IFS_STRUCTURE_REP_MOL_2D, 
			IFS_STRUCTURE_REP_MOL_3D, 
			IFS_STRUCTURE_REP_SDF,
			IFS_STRUCTURE_REP_CDF,
			IFS_STRUCTURE_REP_CDXML,
			IFS_STRUCTURE_REP_UNKNOWN
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSStructureRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
