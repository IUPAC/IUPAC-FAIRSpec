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
import org.iupac.fairdata.extract.MetadataReceiverI.DeferredProperty;
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

	private static boolean generate3D = false;
	
	public static class StructureData {
		public int id;
		public byte[] bytes;
		public byte[] pngBytes;
		public String originPath;
		public String type;
		public String standardInChI;
		public String fixedHInChI;
		public String mediaType;
		public String cssData;
		public String molData;
		public byte[] cdxData;
		public String mol2dData;
		public String smiles;
		public String molecularFormula;
		public String warning;
		public String note;
		public String cellFormula;
		public String empiricalFormula;
		public String inchiKey;
		
		
		@Override
		public String toString() {
			return "[SD " + bytes.length + " " + originPath + " " + mediaType + " " + standardInChI + "]";
		}

		public StructureData(int id, byte[] bytes, String originPath, String type, String standardInChI, String mediaType, String cssData) {
			this.bytes = bytes;
			this.id = id;
			this.originPath = originPath;
			this.type = type;
			this.standardInChI = standardInChI;
			this.mediaType = mediaType;
			this.cssData = cssData;
		}

	}
	
	static int nid = 0;
	/**
	 * the associated extractor
	 */
	private MetadataReceiverI extractor;

	@Override
	public void setExtractor(MetadataReceiverI extractor) {
		this.extractor = extractor;
	}

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

	/**
	 * Q: Why does extractor==null (in Phase 2c) negate "embedded"?
	 */
	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		this.extractor = extractor;
		return processStructureRepresentation(null, originPath, bytes, (extractor != null));
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

	private StructureData currentStructureData;

	/**
	 * create associations using ID rather than index numbers
	 */
	
	public static final String STRUC_FILE_DATA_KEY = "_struc.";

	public static final String PNG_FILE_DATA = STRUC_FILE_DATA_KEY + "png";

	public static final String MOL_FILE_DATA = STRUC_FILE_DATA_KEY + "mol";

	public static final String XYZ_FILE_DATA = STRUC_FILE_DATA_KEY + "xyz";

	public static final String CDX_FILE_DATA = STRUC_FILE_DATA_KEY + "cdx";

	public static final String CDXML_FILE_DATA = STRUC_FILE_DATA_KEY + "cdxml";

	public static final String CIF_FILE_DATA = STRUC_FILE_DATA_KEY + "cif";

	public static final String CML_FILE_DATA = STRUC_FILE_DATA_KEY + "cml";


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

	private String processStructureRepresentation(StructureData sd, String originPath, byte[] bytes,
			boolean isEmbedded) {
		String type = (sd == null ? null : sd.type);
		if (type == null) {
			type = fileToType.get(originPath);
			if (type != null)
				return type;
		}
		if (sd != null) {
			originPath = sd.originPath;
			bytes = sd.bytes;
		}
		String ext = originPath.substring(originPath.lastIndexOf('.') + 1);
		if (type == null)
			type = getType(ext, bytes, true);
		boolean isCIF = ext.equals("cif");
		boolean isCDXML = !isCIF && ext.equals("cdxml");
		boolean isCDX = !isCIF && ext.equals("cdx");
		boolean isCML = ext.equals("cml");
		// We use Jmol for reading all files and creating standard and hydrogen-only
		// InChI and InChIKeys

		// We use CDK for creating the images and converting SMILES to "canonical"
		// smiles and to InChI
		boolean isMol = false;
		if (isCIF || isCDX || isCDXML || isCML || (isMol = ext.equals("mol") || ext.equals("sdf"))) {
			try {
				Viewer v = getJmolViewer();
				String data = (isCDX ? ";base64," + Base64.getBase64(bytes) : new String(bytes));
				// System.out.println(data);
				// sd may come from MNova
				if (sd == null || sd != currentStructureData) {
					sd = getDeferredStructureData(++nid, bytes, originPath, type, null,
							IFDConst.getMediaTypesForExtension(ext));
					System.out.println("Jmol for " + originPath);
					// this first load will not create a 3D structure and will not adda any H atoms
					// it is for a test of 2D; it is NOT suitabble for InChI if this is a 2D no-H
					// model
					String s = "set allowembeddedscripts false;load DATA \"model\"\n" + data
							+ "\nend \"model\" 1 FILTER 'no3D;noHydrogen'";
					s = v.scriptWait(s);
					int pt = s.indexOf("Warning:");
					sd.warning = (pt >= 0 ? s.substring(pt, s.indexOf("\\n", pt)) : null);
				}
				BS atoms = v.bsA();

				boolean hasBonds;
				if (isCIF) {
					hasBonds = v.getCurrentModelAuxInfo().containsKey("hasBonds");
					sd.standardInChI = "?";
				} else {
					hasBonds = true;
					if (sd.warning == null && sd.smiles == null) {
						sd.smiles = v.getSmilesOpt(atoms, 0, 0, 0, "/noaromatic/");
						sd.molecularFormula = v.evaluateExpression("{1.1 && configuration=1}.find('SMILES','MF')")
								.toString();
					}
					if (sd.smiles == null || sd.smiles.indexOf("Xx") >= 0) {
						extractor.log("! DefaultStructureHelper WARNING: SMILES could not be created for " + originPath
								+ " MF=" + sd.molecularFormula);
						sd.smiles = null;
						sd.molecularFormula = null;
					}
				}

				boolean is2D = "2D".equals(v.getCurrentModelAuxInfo().get("dimension"));
				// We use noaromatic here because we want a
				// target SMILES, not a substructure smiles.
				// Targets with aromatic atoms must match aromatic atoms exactly
				
				if (sd.mol2dData == null) {
					if (isCDXML || isCDX) {
						// issue here is that these may be quite unviewable.
						String molData = (String) v.evaluateExpression("write('MOL')");
						if (molData != null && molData.indexOf("2D") >= 0)
							sd.mol2dData = molData;
					} else if (isMol) {
						if (is2D)
							sd.mol2dData = new String(bytes);
					}
				}
				
				IAtomContainer mol = null;
				
				if (sd.pngBytes == null) {
					if (isCIF) {
						if (hasBonds) {
							v.scriptWait("configuration 1;display selected;set zshade;rotate best;refresh");
							sd.molecularFormula = v.evaluateExpression("{visible && configuration=1}.find('MF')")
									.toString();
							sd.pngBytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
						}
						v.scriptWait("load DATA \"model\"\n" + data
								+ "\nend \"model\" 1 packed;configuration 1;display selected;set zshade;rotate best");
						sd.cellFormula = v
								.evaluateExpression(
										"{visible && configuration=1 && within(unitcell)}.find('CELLFORMULA')")
								.toString();
						sd.empiricalFormula = v
								.evaluateExpression(
										"{visible && configuration=1 && within(unitcell)}.find('CELLFORMULA', true)")
								.toString();
						if (sd.pngBytes == null) {
							sd.pngBytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);							
						}
					} else if (sd.smiles != null) {
						mol = getCDKSmiles(sd, mol);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						writeCDKMoleculePNG(mol, bos);
						sd.pngBytes = bos.toByteArray();
					} else if (!is2D) {
						sd.pngBytes = v.getImageAsBytes("png", 500, 500, -1, new String[1]);
					}
				}


				if (sd.smiles != null && sd.standardInChI == null) {
					if (getCDKInchi(mol, sd) == null) {
						sd.standardInChI = "?";
						extractor.log("! DefaultStructureHelper WARNING: InChI could not be created for " + originPath);
					};
				}

				if (sd.molData == null && generate3D) {
					if (is2D && generate3D) {
						// generate 3D model
						String s = "set allowembeddedscripts false;load DATA \"model\"\n" + data + "\nend \"model\" 1";
						v.scriptWait(s);
						is2D = false;
						atoms = v.bsA();
					}
					if (isCDXML || isCDX) {
						// issue here is that these may be quite unviewable.
						String molData = (String) v.evaluateExpression("write('MOL')");
						if (molData != null && molData.indexOf("2D") < 0)
							sd.molData = molData;
					} else if (isMol) {
						if (!is2D)
							sd.molData = new String(bytes);
					}
				}

				if (isEmbedded) {
					// add the representation from an accept
					String stype = (isCIF ? CIF_FILE_DATA
							: isCDXML ? CDXML_FILE_DATA
									: isCDX ? CDX_FILE_DATA : isCML ? CML_FILE_DATA : MOL_FILE_DATA);
					extractor.addDeferredPropertyOrRepresentation(
							DeferredProperty.newStructureRep(stype, sd, false, null, sd.warning));
					return stype;
				}

			
			} catch (Exception e) {
				extractor.log("!! Jmol error generating "
						+ (sd.smiles == null ? "SMILES" : sd.standardInChI == null ? "InChI" : "InChIKey") + " for "
						+ originPath);
				jmolViewer = null;
				currentStructureData = null;
				e.printStackTrace();
			}
			// add additional representations
			sd.note = (sd.warning == null ? "generated from " + originPath + " by Jmol-SwingJS " + jmolVersion : sd.warning);
			if (sd.mol2dData != null) {
				extractor.addDeferredPropertyOrRepresentation(DeferredProperty.newStructureRep(
						IFDConst.IFD_REP_STRUCTURE_MOL_2D, getDeferredStructureData(++nid, sd.mol2dData.getBytes(),
								originPath + ".2d.mol", null, sd.standardInChI, null),
						false, "chemical/x-mdl-molfile", sd.note));
			}
			if (sd.molData != null) {
				extractor.addDeferredPropertyOrRepresentation(DeferredProperty.newStructureRep(
						IFDConst.IFD_REP_STRUCTURE_MOL, getDeferredStructureData(++nid, sd.mol2dData.getBytes(),
								originPath + ".mol", null, sd.standardInChI, null),
						false, "chemical/x-mdl-molfile", sd.note));
			}
			String note1 = (sd.warning == null ? sd.note + "/CDK-SwingJS" : sd.note);
			if (sd.standardInChI != null && !"?".equals(sd.standardInChI)) {
				extractor.addDeferredPropertyOrRepresentation(DeferredProperty.newStructureRep(STANDARD_INCHI,
						sd.standardInChI, true, "chemical/x-inchi", note1));
				if (sd.fixedHInChI != null) {
					extractor.addDeferredPropertyOrRepresentation(DeferredProperty.newStructureRep(FIXEDH_INCHI,
							sd.fixedHInChI, true, "chemical/x-inchi", note1));
				}
				if (sd.inchiKey != null) {
					extractor.addDeferredPropertyOrRepresentation(
							DeferredProperty.newStructureRep(INCHIKEY, sd.inchiKey, true, "chemical/x-inchikey", note1));
				}
			}
			if (sd.smiles != null) {
				extractor.addDeferredPropertyOrRepresentation(
						DeferredProperty.newStructureRep(SMILES, sd.smiles, true, "chemical/x-smiles", note1));
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
			if (sd.pngBytes != null) {
				extractor.addDeferredPropertyOrRepresentation(
						DeferredProperty.newStructureRep(IFDConst.IFD_REP_STRUCTURE_PNG,
								getDeferredStructureData(nid++, sd.pngBytes, originPath + ".png",
										IFDConst.IFD_REP_STRUCTURE_PNG, sd.standardInChI, "image/png"),
								false, "image/png", (sd.mol2dData == null ? sd.note : note1)));
			}
			if (sd.molecularFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(
						DeferredProperty.newStructureRep(MOLECULAR_FORMULA, sd.molecularFormula, true, null, sd.note));
			}
			if (sd.empiricalFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(
						DeferredProperty.newStructureRep(EMPIRICAL_FORMULA, sd.empiricalFormula, true, null, sd.note));
			}
			if (sd.cellFormula != null) {
				extractor.addDeferredPropertyOrRepresentation(
						DeferredProperty.newStructureRep(CELL_FORMULA, sd.cellFormula, true, null, sd.note));
			}
		}
		fileToType.put(originPath, type);
		return type;
	}

	private String getCDKInchi(IAtomContainer mol, StructureData sd) {
		try {
			if (mol == null)
				mol = CDK.getCDKMoleculeFromSmiles(sd.smiles);
			String s = CDK.getInChIFromCDKMolecule(mol, null);
			if (s != null) {
				sd.standardInChI = s;
				sd.inchiKey = CDK.getInChIKey(mol, null);
				sd.fixedHInChI = CDK.getInChIFromCDKMolecule(mol, "fixedh");
			}
			return s;
		} catch (Exception e) {
		}
		return null;
	}

	private static IAtomContainer getCDKSmiles(StructureData sd, IAtomContainer mol) throws IOException {
		try {
			if (mol == null)
				mol = CDK.getCDKMoleculeFromSmiles(sd.smiles);
			sd.smiles = CDK.getSmilesFromCDKMolecule(mol);
			return mol;
		} catch (Exception e) {
		}
		return null;
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
	private StructureData getDeferredStructureData(int id, byte[] bytes, String originPath, String type, String standardInChI,
			String mediaType) {
		currentStructureData = new StructureData(id, bytes, originPath, type, 
				standardInChI, mediaType, null);
		return currentStructureData;
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

	public void processStructureData(StructureData sd) {
		processStructureRepresentation(sd, null, null, false);
	}

}

