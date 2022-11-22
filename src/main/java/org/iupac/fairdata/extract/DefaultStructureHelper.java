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

	private static final String defaultStructureFilePattern = IFDConst.getProp("IFD_DEFAULT_STRUCTURE_FILE_PATTERN");;

	/**
	 * the associated extractor
	 */
	private ExtractorI extractor;

	private Viewer jmolViewer;

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
		return defaultStructureFilePattern;
	}

	@Override
	public boolean doExtract(String entryName) {
		return true;
	}

	private Map<String, String> fileToType = new HashMap<>();

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
		if (ext.equals("mol") || ext.equals("sdf") || ext.equals("cml")) {
			try {
				Viewer v = getJmolViewer();
				v.loadInline(new String(bytes));
				BS atoms = v.bsA();
				smiles = v.getSmiles(atoms);
				inchi = v.getInchi(atoms, null, null);
				if (inchi == null) {
				  	extractor.log("! DefaultStructureHelper WARNING: InChI could not be created for " + ifdPath);
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
				extractor.addPropertyOrRepresentation(IFDConst.getProp("IFD_PROPERTY_STRUCTURE_INCHIKEY"), inchiKey, true, "chemical/x-inchikey");
		}
		fileToType.put(ifdPath, type);
		return type;
	}

	/**
	 * These would need updating for structures. 
	 * 
	 * @param ext
	 * @param bytes
	 * @return
	 */
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
		case "cif":
			return IFDConst.IFD_REP_STRUCTURE_CIF;
		case "cml":
			return IFDConst.IFD_REP_STRUCTURE_CML;
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

