package org.iupac.fairdata.extract;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDProperty;
import org.jmol.api.JmolViewer;
import org.jmol.util.DefaultLogger;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.interfaces.IAtomContainer;

//import com.actelion.research.chem.AbstractDepictor;
//import com.actelion.research.chem.SmilesParser;
//import com.actelion.research.chem.StereoMolecule;
//import com.actelion.research.gui.JStructureView;
//import com.actelion.research.gui.generic.GenericRectangle;

import javajs.util.BS;
import javajs.util.Base64;
import swingjs.CDK;

/**
 * A class to handle the extraction of structure objects and metadata related to
 * structures. This class leverages Jmol for these purposes. It may be extended.
 * 
 * @author hansonr
 *
 */
public class DefaultStructureHelper implements PropertyManagerI {

	private static final String defaultStructureFilePattern = IFDConst.getProp("IFD_DEFAULT_STRUCTURE_FILE_PATTERN");;

	/**
	 * the associated extractor
	 */
	private MetadataReceiverI extractor;

	private Viewer jmolViewer;

	/**
	 * A class to process structures using Jmol methods to extract and discover
	 * properties of a model.
	 * 
	 */

	public DefaultStructureHelper(MetadataReceiverI extractor) {
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

	private boolean createRepresentation;

	/**
	 */
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
System.out.println("DFH " + bytes.length + " " + originPath);
		this.extractor = extractor;
		createRepresentation = (extractor != null); 
		return processStructureRepresentation(originPath, bytes, null, null, createRepresentation, false);
	}
	
	protected Viewer getJmolViewer() {
		if (jmolViewer == null) {
			System.out.println("IFDDefaultStructurePropertyManager initializing Jmol...");
			jmolViewer = (Viewer) JmolViewer.allocateViewer(null, null);
			jmolVersion = JmolViewer.getJmolVersionNoDate();
			// route Jmol logging to extractor.log
			org.jmol.util.Logger.setLogger(new DefaultLogger() {
				
				  @Override
				protected String log(PrintStream out, int level, String txt, Throwable e) {
//					  txt = super.log(null, level, txt, e);
					  if (txt != null)
						  extractor.log("!Jmol " + txt.trim());
					  return txt;
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

	public static final String CIF_FILE_DATA = "_struc.cif";

	public static final String CML_FILE_DATA = "_struc.cml";


	/**
	 * create associations using ID rather than index numbers
	 */
	
	public static final String STRUC_FILE_DATA_KEY = "_struc.";

	private static final String SMILES = IFDConst.getProp("IFD_REP_STRUCTURE.SMILES");
	private static final String STANDARD_INCHI = IFDConst.getProp("IFD_REP_STRUCTURE.STANDARD_INCHI");
	private static final String FIXEDH_INCHI = IFDConst.getProp("IFD_REP_STRUCTURE.FIXEDH_INCHI");
	private static final String INCHIKEY = IFDConst.getProp("IFD_PROPERTY_STRUCTURE.INCHIKEY");
	private static final String MOLECULAR_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE.MOLECULAR_FORMULA");
	private static final String EMPIRICAL_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE.EMPIRICAL_FORMULA");
	private static final String CELL_FORMULA = IFDConst.getProp("IFD_PROPERTY_STRUCTURE.CELL_FORMULA");

	@Override
	public String getVendorDataSetKey() {
		return null; // n/a
	}

	public String processStructureRepresentation(String originPath, byte[] bytes, String type, String standardInChI,
			boolean isEmbedded, boolean returnInChI) {
		if (type == null) {
			type = fileToType.get(originPath);
			if (type != null)
				return (returnInChI ? null : type);
		}
		String ext = originPath.substring(originPath.lastIndexOf('.') + 1);
		if (type == null)
			type = getType(ext, bytes, true);
		String smiles = null, fixedhInchi = null, inchiKey = null, molecularFormula = null, cellFormula = null,
				empiricalFormula = null;
		boolean isCIF = ext.equals("cif");
		boolean isCDXML = !isCIF && ext.equals("cdxml");
		boolean isCDX = !isCIF && ext.equals("cdx");
		boolean isCML = ext.equals("cml");
		String note = null, warning = null;
		// We use Jmol for reading all files and creating standard and hydrogen-only InChI and InChIKeys
		
		// We use CDK for creating the images and converting SMILES to "canonical" smiles and to InChI
		
		if (isCIF || isCDX || isCDXML || isCML || ext.equals("mol") || ext.equals("sdf")) {
			try {
				String data = (isCDX ? ";base64," + Base64.getBase64(bytes) : new String(bytes));
				//System.out.println(data);
				Viewer v = getJmolViewer();
				String s = "set allowembeddedscripts false;load DATA \"model\"\n" + data
						+ "\nend \"model\" 1 FILTER 'no3D;noHydrogen'";
				s = v.scriptWait(s);
				int pt = s.indexOf("Warning:");
				warning = (pt >= 0 ? s.substring(pt, s.indexOf("\\n", pt)) : null);
				BS atoms = v.bsA();
				if (standardInChI == null)
					standardInChI = v.getInchi(atoms, null, null);
				if (standardInChI == null)
					extractor.log("! InChI could not be created for " + originPath);
				if (isEmbedded) {
					// add the representation
					String stype = (isCIF ? CIF_FILE_DATA
							: isCDXML ? CDXML_FILE_DATA
									: isCDX ? CDX_FILE_DATA : isCML ? CML_FILE_DATA : MOL_FILE_DATA);
					// We use noaromatic here because we want a
					// target SMILES, not a substructure smiles.
					// Targets with aromatic atoms must match aromatic atoms exactly
					extractor
							.addDeferredPropertyOrRepresentation(stype,
									getDeferredObject(bytes, originPath, type, standardInChI, IFDConst.getMediaTypesForExtension(ext)),
									false, null, warning, "Helper.procRep");
					return (returnInChI ? standardInChI : stype);
				}
				note = (warning == null ? "generated from " + originPath + " by Jmol " + jmolVersion : warning);
				boolean is2D = "2D".equals(v.getCurrentModelAuxInfo().get("dimension"));
				if (isCIF) {
					if (v.getCurrentModelAuxInfo().containsKey("hasBonds")) {
						v.scriptWait("configuration 1;display selected;set zshade;rotate best;refresh");
						molecularFormula = v.evaluateExpression("{visible && configuration=1}.find('MF')").toString();
						bytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
					}
					v.scriptWait("load DATA \"model\"\n" + data
							+ "\nend \"model\" 1 packed;configuration 1;display selected;set zshade;rotate best");

					cellFormula = v.evaluateExpression("{visible && configuration=1 && within(unitcell)}.find('CELLFORMULA')").toString();
					empiricalFormula = v.evaluateExpression("{visible && configuration=1 && within(unitcell)}.find('CELLFORMULA', true)")
							.toString();
				} else {
					bytes = null;
					// We use noaromatic here because we want a
					// target SMILES, not a substructure smiles.
					// Targets with aromatic atoms must match aromatic atoms exactly
					if (warning == null) {
						smiles = v.getSmilesOpt(atoms, 0, 0, 0, "/noaromatic/");
						molecularFormula = v.evaluateExpression("{1.1 && configuration=1}.find('SMILES','MF')")
								.toString();
					}
					if (smiles == null || smiles.indexOf("Xx") >= 0) {
						extractor.log("! DefaultStructureHelper WARNING: SMILES could not be created for " + originPath
								+ " MF=" + molecularFormula);
						smiles = null;
						molecularFormula = null;
					} else {
						if (standardInChI == null) {
							extractor.log("! DefaultStructureHelper WARNING: InChI could not be created for "
									+ originPath);
						} else {
							fixedhInchi = v.getInchi(atoms, null, "fixedh");
							inchiKey = v.getInchi(atoms, null, "key");
						}
					}
					// using SMILES here to get implicit H count
					String mol2d = null;
					if (isCDXML || isCDX) {
						// issue here is that these may be quite unviewable.
						mol2d = (String) v.evaluateExpression("write('MOL')");
						if (mol2d != null && mol2d.indexOf("2D") >= 0)
							extractor.addDeferredPropertyOrRepresentation(IFDConst.IFD_REP_STRUCTURE_MOL_2D,
									getDeferredObject(mol2d.getBytes(), originPath + ".mol" , null, standardInChI, null), false,
									"chemical/x-mdl-molfile", note, null);
					}
//					if (is2D) {
//						JMEJmol jme = (JMEJmol) org.jmol.api.Interface.getInterface("jme.JMEJmol", v, "FAIRSpec");
//						jme.options("headless");
//						jme.readMolFile(mol2d == null ? data : mol2d);
//						bytes = jme.toBorderedPNG(null, 10, 10);
//					}
					if (is2D && smiles != null) {
						IAtomContainer mol = CDK.getCDKMoleculeFromSmiles(smiles);
						smiles = getCDKSmiles(smiles, mol);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						writeCDKMoleculePNG(mol, bos);
						bytes = bos.toByteArray();
					}
				}
				if (bytes == null && !is2D) {
					bytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
				}
			} catch (Exception e) {
				extractor.log("!! Jmol error generating "
						+ (smiles == null ? "SMILES" : standardInChI == null ? "InChI" : "InChIKey") + " for "
						+ originPath);
				jmolViewer = null;
				e.printStackTrace();
			}
			if (standardInChI != null && !"?".equals(standardInChI)) {
				extractor.addDeferredPropertyOrRepresentation(STANDARD_INCHI, standardInChI, true, "chemical/x-inchi",
						note, null);
				if (fixedhInchi != null) {
					extractor.addDeferredPropertyOrRepresentation(FIXEDH_INCHI, fixedhInchi, true, "chemical/x-inchi",
							note, null);
				}
				if (inchiKey != null) {
					extractor.addDeferredPropertyOrRepresentation(INCHIKEY, inchiKey, true, "chemical/x-inchikey", null,
							null);
				}
			} else {
				standardInChI = null;
			}
			if (smiles != null) {
				extractor.addDeferredPropertyOrRepresentation(SMILES, smiles, true, "chemical/x-smiles", note, null);
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
			if (bytes != null) {
				extractor.addDeferredPropertyOrRepresentation(IFDConst.IFD_REP_STRUCTURE_PNG,
						getDeferredObject( bytes, originPath + ".png", IFDConst.IFD_REP_STRUCTURE_PNG, standardInChI, "image/png"),
						false, "image/png", note, null);
			}
			if (molecularFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(MOLECULAR_FORMULA, molecularFormula, true, null, note,
						null);
			}
			if (empiricalFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(EMPIRICAL_FORMULA, empiricalFormula, true, null, note,
						null);
			}
			if (cellFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(CELL_FORMULA, cellFormula, true, null, note, null);
			}
		}
		fileToType.put(originPath, type);
		return (returnInChI ? standardInChI : type);
	}
	
	private static String getCDKSmiles(String smiles, IAtomContainer mol) throws IOException {
		return CDK.getSmilesFromCDKMolecule(mol);
	}
		
	private static void writeCDKMoleculePNG(IAtomContainer mol, OutputStream os) throws IOException {
		BufferedImage bi = CDK.getImageFromCDKMolecule(mol, false);
		ImageIO.write(bi, "png", os);
	}

//	private static void writeOCLMoleculePNG(String smiles, OutputStream os) throws IOException {	
//		StereoMolecule mol = new SmilesParser().parseMolecule(smiles); 
//		GenericRectangle rect = mol.getBounds(null);
//		int w = (int) (rect.getWidth() * 30);
//		int h = (int) (rect.getHeight()/rect.getWidth()* w);
//		int mode = AbstractDepictor.cDModeSuppressCIPParity | AbstractDepictor.cDModeSuppressESR
//				| AbstractDepictor.cDModeSuppressChiralText;
//		JStructureView mArea = new JStructureView(mol);
//		mArea.setDisplayMode(mode);
//		mArea.setSize(w, h);
//		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//		mArea.paint(bi.getGraphics());
//		ImageIO.write(bi, "png", os);
//	}

	/**
	 * 
	 * @param bytes
	 * @param originPath
	 * @param type
	 * @param standardInChI
	 * @param mediaType
	 * @return
	 */
	private static Object[] getDeferredObject(byte[] bytes, String originPath, String type, String standardInChI,
			String mediaType) {
		return new Object[] { bytes, originPath, type, 
				(standardInChI == null ? "?" : standardInChI), mediaType};
	}

	/**
	 * These would need updating for structures. 
	 * 
	 * @param ext
	 * @param bytes
	 * @return
	 */
	public static String getType(String ext, byte[] bytes, boolean allowNone) {
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
			return (allowNone ? ext.toUpperCase() : null);
		}
	}

	private static boolean isMol2D(byte[] bytes) {
		if (bytes == null)
			return false;
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

	@Override
	public boolean isDerived() {
		return false;
	}

}

