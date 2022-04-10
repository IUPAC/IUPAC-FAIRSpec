package org.iupac.fairdata.extract;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
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
public class DefaultStructureHelper implements PropertyManagerI {

	/**
	 * the associated extractor
	 */
	private ExtractorI extractor;

	private static Viewer jmolViewer;

	/**
	 * A class to process structures using Jmol methods to extract and discover
	 * properties of a model.
	 * 
	 */

	public DefaultStructureHelper(ExtractorI extractor) {
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
	public String accept(ExtractorI extractor, String ifdPath, byte[] bytes) {
		this.extractor = extractor;
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
				if (inchi == null) {
				  	extractor.log("!! InChI could not be created for " + ifdPath);
				} else {
					inchiKey = v.getInchi(atoms, null, "key");
				}
			} catch (Exception e) {
				extractor.log("!! Jmol error generating " + (smiles == null ? "SMILES" : inchi == null ? "InChI" : "InChIKey"));
				jmolViewer = null;
				e.printStackTrace();
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
		}
		if (smiles != null) {
			extractor.addPropertyOrRepresentation(IFDConst.getProp("IFD_REP_STRUCTURE_SMILES"), smiles, true, "chemical/x-smiles");
		}
		if (inchi != null) {
				extractor.addPropertyOrRepresentation(IFDConst.getProp("IFD_REP_STRUCTURE_INCHI"), inchi, true, "chemical/x-inchi");
		}
		if (inchiKey != null) {
				extractor.addPropertyOrRepresentation(IFDConst.getProp("IFD_PROP_STRUCTURE_INCHIKEY"), inchiKey, true, "chemical/x-inchikey");
		}
		fileToType.put(ifdPath, type);
		return type;
	}

	public static String mediaTypeFromName(String fname) {
		int pt = Math.max(fname.lastIndexOf('/'), fname.lastIndexOf('.'));
		return (fname.endsWith(".zip") ? "application/zip"
				: fname.endsWith(".pdf") ? "application/pdf" 
				: fname.endsWith(".png") ? "image/png"
				: fname.endsWith(".inchi") ? "chemical/x-inchi"
				: fname.endsWith(".smiles") ? "chemical/x-smiles"
				: fname.endsWith(".smi") ? "chemical/x-smiles"
				: fname.endsWith(".cdx") ? "chemical/x-cdx (ChemDraw CDX)"
				: fname.endsWith(".cdxml") ? "chemical/x-cdxml (ChemDraw XML)"
					// see https://en.wikipedia.org/wiki/Chemical_file_format
				: fname.endsWith(".mol") ? "chemical/x-mdl-molfile"
				: fname.endsWith(".sdf") ? "chemical/x-mdl-sdfile"
				: fname.endsWith(".txt") || fname.endsWith(".log") || fname.endsWith(".out") ? "text/plain"
				: fname.endsWith(".inchi") ? "chemical/x-inchi"
				: fname.endsWith(".smiles") || fname.endsWith(".smi") ? "chemical/x-daylight-smiles"
				: fname.endsWith(".jpf") ? "application/octet-stream (JEOL)"
				: fname.endsWith(".mnova") ? "application/octet-stream (MNOVA)"
				: pt >= 0 ? "?" + fname.substring(pt) : "?");
	}

	public static String getType(String ext, byte[] bytes) {
		// TODO -- generalize this
		switch (ext) {
		case "png":
			return IFDConst.IFD_REP_STRUCTURE_PNG;
		case "mol":
			return (isMol2D(bytes) ? IFDConst.IFD_REP_STRUCTURE_MOL_2D : IFDConst.IFD_REP_STRUCTURE_MOL);
		case "sdf":
			return (isMol2D(bytes) ? IFDConst.IFD_REP_STRUCTURE_SDF_2D : IFDConst.IFD_REP_STRUCTURE_SDF);
		case "cdx":
			return IFDConst.IFD_REP_STRUCTURE_CDX;
		case "cdxml":
			return IFDConst.IFD_REP_STRUCTURE_CDXML;
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

