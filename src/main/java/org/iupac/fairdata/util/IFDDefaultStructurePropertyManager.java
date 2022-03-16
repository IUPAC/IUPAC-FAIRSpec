package org.iupac.fairdata.util;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.api.IFDExtractorI;
import org.iupac.fairdata.api.IFDPropertyManagerI;
import org.iupac.fairdata.struc.IFDStructure;
import org.iupac.fairdata.struc.IFDStructureRepresentation;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;

import javajs.util.BS;

/**
 * A class to handle the extraction of structure objects and metadata related to
 * structures. This class leverages Jmol for these purposes.
 * 
 * @author hansonr
 *
 */
public class IFDDefaultStructurePropertyManager implements IFDPropertyManagerI {

	/**
	 * the associated extractor
	 */
	private IFDExtractorI extractor;

	private static Viewer jmolViewer;

	/**
	 * A class to process structures using Jmol methods to extract and discover
	 * properties of a model.
	 * 
	 */

	public IFDDefaultStructurePropertyManager(IFDExtractorI extractor) {
		this.extractor = extractor;
	}

	@Override
	public String getParamRegex() {
		return "(?<struc>(?<mol>\\.mol$|\\.sdf$)|(?<cdx>\\.cdx$|\\.cdxml$))";
	}

	@Override
	public boolean doExtract(String entryName) {
		return true;
	}

	private static Map<String, String> fileToType = new HashMap<>();

	@Override
	public String accept(IFDExtractorI extractor, String ifdPath, byte[] bytes) {
		return processRepresentation(ifdPath, bytes);
	}
	
	protected Viewer getJmolViewer() {
		if (jmolViewer == null) {
			System.out.println("IFDDefaultStructurePropertyManager initializing Jmol...");
			jmolViewer = (Viewer) JmolViewer.allocateViewer(null, null);
		}
		return jmolViewer;
	}

	@Override
	public String processRepresentation(String ifdPath, byte[] bytes) {
		String type = fileToType.get(ifdPath);
		if (type != null)
			return type;
		String ext = ifdPath.substring(ifdPath.lastIndexOf('.') + 1);
		type = getType(ext, bytes);
		String smiles = null, inchi = null, inchiKey = null;
		if (ext.equals("mol") || ext.equals("sdf")) {
			try {
				Viewer v = getJmolViewer();
				v.loadInline(new String(bytes));
				BS atoms = v.bsA();
				smiles = v.getSmiles(atoms);
				inchi = v.getInchi(atoms, null, null);
				inchiKey = v.getInchi(atoms, null, "key");
			} catch (Exception e) {
				System.err.println("!! Jmol error generating " + (smiles == null ? "SMILES" : inchi == null ? "InChI" : "InChIKey"));
				jmolViewer = null;
				e.printStackTrace();
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
		}
		if (smiles != null) {
			IFDPropertyManagerI.addProperty(extractor, IFDStructure.IFD_PROP_STRUC_SMILES, smiles);
		}
		if (inchi != null) {
			IFDPropertyManagerI.addProperty(extractor, IFDStructure.IFD_PROP_STRUC_INCHI, inchi);
		}
		if (inchiKey != null) {
			IFDPropertyManagerI.addProperty(extractor, IFDStructure.IFD_PROP_STRUC_INCHIKEY, inchiKey);
		}
		fileToType.put(ifdPath, type);
		return type;
	}

	public static String getType(String ext, byte[] bytes) {
		switch (ext) {
		case "png":
			return IFDStructureRepresentation.IFD_STRUCTURE_REP_PNG;
		case "mol":
			return (isMol2D(bytes) ? IFDStructureRepresentation.IFD_STRUCTURE_REP_MOL_2D : IFDStructureRepresentation.IFD_STRUCTURE_REP_MOL);
		case "sdf":
			return (isMol2D(bytes) ? IFDStructureRepresentation.IFD_STRUCTURE_REP_SDF_2D : IFDStructureRepresentation.IFD_STRUCTURE_REP_SDF);
		case "cdx":
			return IFDStructureRepresentation.IFD_STRUCTURE_REP_CDX;
		case "cdxml":
			return IFDStructureRepresentation.IFD_STRUCTURE_REP_CDXML;
		default:
			return ext.toUpperCase();
		}
	}

	private static boolean isMol2D(byte[] bytes) {
		int line = 0;
		int linept = 0;
		for (int i = 0; i < bytes.length; i++) {
			switch (bytes[i]) {
			case 0xD:
				if (bytes[i + 1] == 0xA) {
					continue;
				}
				//$FALL-THROUGH$
			case 0xA:
				if (++line == 2) {
					// GSMACCS-II10169115362D
					// 0.........1.........2
					// 0123456789012345678901
					int ptdim = linept + 20;
					return (i > ptdim + 1 && bytes[ptdim] == '2' && bytes[ptdim+1] == 'D');
				}
				linept = i + 1;
				break;
			}
		}
		return false;
	}
	
//	static {
//		System.out.println(isMol2D(("https://files.rcsb.org/download/1pgb.pdb\n" + 
//				"__Jmol-14_07301921552D 1   1.00000     0.00000     0\n" + 
//				"Jmol version 14.29.46  2019-06-03 12:50 EXTRACT: ({215:225 394:404})\n" + 
//				"").getBytes()));
//		System.out.println(isMol2D(("https://files.rcsb.org/download/1pgb.pdb\n" + 
//				"__Jmol-14_07301921553D 1   1.00000     0.00000     0\n" + 
//				"Jmol version 14.29.46  2019-06-03 12:50 EXTRACT: ({215:225 394:404})\n" + 
//				"").getBytes()));
//		System.out.println(isMol2D(("https://files.rcsb.org/download/1pgb.pdb\n" + 
//				"__Jmol-14_07301921552D").getBytes()));
//		System.out.println(isMol2D(("https://files.rcsb.org/download/1pgb.pdb\n" + 
//				"__Jmol-14_07301921552").getBytes()));
//	}

}

