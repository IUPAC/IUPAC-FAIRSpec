package org.iupac.fairdata.extract;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.jmol.api.JmolViewer;
import org.jmol.util.DefaultLogger;
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
	public String accept(ExtractorI extractor, String originPath, byte[] bytes) {
		this.extractor = extractor;
		return processRepresentation(originPath, bytes);
	}
	
	protected Viewer getJmolViewer() {
		if (jmolViewer == null) {
			System.out.println("IFDDefaultStructurePropertyManager initializing Jmol...");
			jmolViewer = (Viewer) JmolViewer.allocateViewer(null, null);
			jmolVersion = JmolViewer.getJmolVersionNoDate();
			// copy Jmol output to extractor.log
			org.jmol.util.Logger.setLogger(new DefaultLogger() {
				
				  protected String log(PrintStream out, int level, String txt, Throwable e) {
					  txt = super.log(out, level, txt, e);
					  if (txt != null)
						  extractor.log("!Jmol " + txt.trim());
					  return null;
				  }
				
			});
		}
		return jmolViewer;
	}

	String jmolVersion = null;

	public static final String PNG_FILE_DATA = "_struc.png";

	public static final String MOL_FILE_DATA = "_struc.mol";

	public static final String CDX_FILE_DATA = "_struc.cdx";

	public static final String CDXML_FILE_DATA = "_struc.cdxml";
	
	/**
	 * create associations using ID rather than index numbers
	 */
	
	public static final String STRUC_FILE_DATA_KEY = "_struc.";

	private static final String SMILES = IFDConst.getProp("IFD_REP_STRUCTURE_SMILES");
	private static final String STANDARD_INCHI = IFDConst.getProp("IFD_REP_STRUCTURE_STANDARD_INCHI");
	private static final String FIXEDH_INCHI = IFDConst.getProp("IFD_REP_STRUCTURE_FIXEDH_INCHI");
	private static final String INCHIKEY = IFDConst.getProp("IFD_PROPERTY_STRUCTURE_INCHIKEY");
	private static final String MOLECULAR_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE_MOLECULAR_FORMULA");
	private static final String EMPIRICAL_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE_EMPIRICAL_FORMULA");
	private static final String CELL_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE_CELL_FORMULA");

	@Override
	public String processRepresentation(String originPath, byte[] bytes) {
		String type = fileToType.get(originPath);
		if (type != null)
			return type;
		String ext = originPath.substring(originPath.lastIndexOf('.') + 1);
		type = getType(ext, bytes);
		String smiles = null, standardInchi = null, fixedhInchi = null, inchiKey = null, molecularFormula = null,
				cellFormula = null, empiricalFormula = null;
		boolean isCIF = ext.equals("cif");
		boolean isCDXML = !isCIF && ext.equals("cdxml");
		String note = null;
		if (isCIF || isCDXML || ext.equals("mol") || ext.equals("sdf") || ext.equals("cml")) {
			try {
				Viewer v = getJmolViewer();
				note = "generated from " + originPath + " by Jmol " + jmolVersion;
				String data = new String(bytes);
				String s = "set allowembeddedscripts false;load DATA \"model\"\n" + data
						+ "\nend \"model\" 1 FILTER 'no3D;noHydrogen'";
				v.scriptWait(s);
				if (isCIF) {
					if (v.getCurrentModelAuxInfo().containsKey("hasBonds")) {
						molecularFormula = v.evaluateExpression("{visible && configuration=1}.find('MF')").toString();
						bytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
					}
					s = "load DATA \"model\"\n" + data + "\nend \"model\" 1 packed";
					v.scriptWait(s);
					cellFormula = v.evaluateExpression("{visible && configuration=1}.find('CELLFORMULA')").toString();
					empiricalFormula = v.evaluateExpression("{visible && configuration=1}.find('CELLFORMULA', true)")
							.toString();
				} else {
					bytes = null;
					BS atoms = v.bsA();
					smiles = v.getSmiles(atoms);
					molecularFormula = v.evaluateExpression("{1.1 && configuration=1}.find('SMILES','MF')").toString();
					if (smiles == null || smiles.indexOf("Xx") >= 0) {
						extractor.log("! DefaultStructureHelper WARNING: SMILES could not be created for " + originPath
								+ " MF=" + molecularFormula);
						smiles = null;
					} else {
						standardInchi = v.getInchi(atoms, null, null);
						if (standardInchi == null) {
							extractor.log("! DefaultStructureHelper WARNING: InChI could not be created for "
									+ originPath + " MF=" + molecularFormula + " SMILES=" + smiles);
						} else {
							fixedhInchi = v.getInchi(atoms, null, "fixedh");
							inchiKey = v.getInchi(atoms, null, "key");
						}
					}
					// using SMILES here to get implicit H count
					if (isCDXML) {
						String mol2d = (String) v.evaluateExpression("write('MOL')");
						if (mol2d != null && mol2d.indexOf("2D") >= 0)
							extractor.addDeferredPropertyOrRepresentation(IFDConst.IFD_REP_STRUCTURE_MOL_2D,
									new Object[] { mol2d.getBytes(), originPath + ".mol" }, false,
									"chemical/x-mdl-molfile", note);
					}
				}
				boolean is3D = "3D".equals(v.getCurrentModelAuxInfo().get("dimension"));
				if (bytes == null && is3D)
					bytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
			} catch (Exception e) {
				extractor.log("!! Jmol error generating "
						+ (smiles == null ? "SMILES" : standardInchi == null ? "InChI" : "InChIKey"));
				jmolViewer = null;
				e.printStackTrace();
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
		} else {
			bytes = null;
		}
		if (bytes != null) {
			extractor.addDeferredPropertyOrRepresentation(IFDConst.IFD_REP_STRUCTURE_PNG,
					new Object[] { bytes, originPath + ".png" }, false, "image/png", note);
		}
		if (smiles != null) {
			extractor.addDeferredPropertyOrRepresentation(SMILES, smiles, true, "chemical/x-smiles", note);
		}
		if (standardInchi != null) {
			extractor.addDeferredPropertyOrRepresentation(STANDARD_INCHI, standardInchi, true, "chemical/x-inchi",
					note);
		}
		if (fixedhInchi != null) {
			extractor.addDeferredPropertyOrRepresentation(FIXEDH_INCHI, fixedhInchi, true, "chemical/x-inchi", note);
		}
		if (molecularFormula != null) {
			extractor.addDeferredPropertyOrRepresentation(MOLECULAR_FORMULA, molecularFormula, true, null, note);
		}
		if (empiricalFormula != null) {
			extractor.addDeferredPropertyOrRepresentation(EMPIRICAL_FORMULA, empiricalFormula, true, null, note);
		}
		if (cellFormula != null) {
			extractor.addDeferredPropertyOrRepresentation(CELL_FORMULA, cellFormula, true, null, note);
		}
		if (inchiKey != null) {
			extractor.addDeferredPropertyOrRepresentation(INCHIKEY, inchiKey, true, "chemical/x-inchikey", null);
		}
		fileToType.put(originPath, type);
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

