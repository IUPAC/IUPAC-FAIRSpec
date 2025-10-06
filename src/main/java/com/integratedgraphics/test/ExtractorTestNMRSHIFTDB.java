package com.integratedgraphics.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.extractor.FindingAidCreator;
import com.integratedgraphics.extractor.IFDExtractor;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * 
 * Just modify the first few parameters in main and run this as a Java file.
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTestNMRSHIFTDB extends FindingAidCreator {

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/test/ExtractorTestNMRShiftDB.java";
	protected static final String version = "9.1.0-beta+2025.09.21";

	public static void main(String[] args) {
		String dir = "c:/temp/iupac/nmrshiftdb/";
		scrapeNMRShiftDB(dir, "20182362");
////		String ifdExtractFile = dir + "IFD-extract-procter.json";
////		String localSourceArchive = dir + "Procter/";
////		String targetDir = dir + "icl-procter";
//
//		String ifdExtractFile = dir + "IFD_extract.json";
//		String localSourceArchive = dir + "zip";
//		String targetDir = dir + "site";//null;//dir + "icl-procter-2025-08";
//
//		String flags = null;
//
//		new IFDExtractor().runExtraction(ifdExtractFile, localSourceArchive, targetDir, null, flags);
	}

	private final static String mainTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/portal/js_pane/P-Results?nmrshiftdbaction=showDetailsFromHome&molNumber=MOLNUMBER&showoutertab=0_1";
	private final static String specImageTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/images/spectra/original/$SPECID.png";
	private final static String moleculeImageTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/images/molecules/large/$MOLNUMBER_$SPECID.jpeg";

	private final static String molDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$MOLNUMBER.$FORMAT?spectrumid=$SPECID&nmrshiftdbaction=exportmol&shownumbers=false&format=$FORMAT";
	private final static String specDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$FILENAME?spectrumid=$SPECID&nmrshiftdbaction=exportspec&format=$FORMAT";

	private static void scrapeNMRShiftDB(String dir, String string) {
		String molNumber = "20182362";
		File filesDir = new File(dir + molNumber, "files");
		File dataDir = new File(dir + molNumber, "data");
		try {
			String source = mainTemplate.replaceFirst("MOLNUMBER", molNumber);
			String data = getSourceData(source, filesDir, "index.html", true);
			getSpectra(molNumber, source, filesDir, dataDir, data);
			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void getSpectra(String molnumber, String source, File filesDir, File molDataDir, String data) throws IOException {
		String[] spectra = data.split("a name=\"spectrum");
		System.out.println(data.indexOf("double"));
		for (int i = 1; i < spectra.length; i++) {
			String spec = spectra[i];
			String name = spec.substring(0, spec.indexOf('"'));
			File specdir = new File(filesDir, name);
			File specDataDir = new File(molDataDir, name);			
			String imageSource = specImageTemplate.replace("$SPECID", name);
			getSourceData(imageSource, specDataDir, name + ".png", false);
			imageSource = moleculeImageTemplate.replace("$MOLNUMBER", molnumber).replace("$SPECID", name);
			getSourceData(imageSource, molDataDir, molnumber + ".jpeg", false);
			getTabData(molnumber, source, molDataDir, specdir, specDataDir, spec, name);
		}
	}

	private static void getTabData(String molnumber, String source, File molDataDir, File specFileDir, File specDataDir, String spec, String specID) throws IOException {
		String[] lines = spec.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int pt = line.indexOf("&tab=");
			if (pt >= 0) {
				String tab = line.substring(pt + 1, line.indexOf("#", pt));
				String data = getSourceData(source + "&" + tab, specFileDir, tab.replace('=', '_') + ".htm", true);
				data = getSpecData(data, specID);
				if (tab.endsWith("_1")) {
					getAdditionalData(molnumber, source, molDataDir, specDataDir, data, specID);
				} else if (tab.endsWith("_2")) {
					getDownloadData(molnumber, source, molDataDir, specDataDir, data, specID);
				}
			}
		}
	}
	
	private static String getSpecData(String data, String specID) {
		String[] spectra = data.split("a name=\"spectrum");
		for (int i = 1; i < spectra.length; i++) {
			String spec = spectra[i];
			String name = spec.substring(0, spec.indexOf('"'));
			if (name.equals(specID))
				return spec;
		}
		return null;
	}

	private static void getAdditionalData(String molnumber, String source, File molDataDir, File specDataDir, String spec, String specID) throws IOException {
		String[] tables = spec.split("<table");
		for (int i = 0; i < tables.length; i++) {
			String table = tables[i];
			if (table.indexOf("http://www.blueobelisk.org/chemistryblogs") >= 0) {
				int pt = table.indexOf(specID);
				Map<String, String> molData = new TreeMap<String, String>();
				extractMoleculeMetadata(molnumber, molDataDir, table.substring(0, pt), molData);
				writeMetadata(molDataDir, molData);
				Map<String, String> specData = new TreeMap<String, String>();
				extractSpectraMetadata(molnumber, specDataDir, table.substring(pt), specID, specData);
				writeMetadata(specDataDir, specData);				
				return;
			}
		}
	}

	private static void getDownloadData(String molnumber, String source, File molDataDir, File specDataDir, String data, String specID) throws IOException {
		String[] forms = data.split("<form");
		for (int i = 1; i < forms.length; i++) {
			String form = forms[i];
			String[] options = getHTMLOptions(form);
			if (form.indexOf("\"exportspec") >= 0) {
				int pt1 = data.indexOf("var filename");//="40018612_13C.";
				int pt = data.indexOf('"', pt1) + 1;
				pt1 = data.indexOf('"', pt);
			    String fname = data.substring(pt, pt1);
			    if (fname.length() < 0)
			    	continue;
				for (int j = 1; j < options.length; j++) {
					String filename = fname;
					String opt = options[j];
					switch (opt) {
					case "rawdata":
						filename += "zip";
						break;
					case "pdforig":
						filename += "pdf";
						break;
					case "jcamppeaks":
						filename += "jcamp";
						break;
					case "asciitable":
						filename += "tab";
						break;
					case "originalimage":
						filename += "png";
						break;
					case "svg":
					case "jpeg":
					case "png":
					case "tiff":
					case "txt":
					case "cml":
					default:
						filename += opt;
						break;
					}
					String url = specDownloadTemplate
							.replace("$SPECID", specID)
							.replace("$FILENAME", filename)
							.replace("$FORMAT", opt);
					getSourceData(url, specDataDir, filename, false);
				}
//				private final static String specDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$FILENAME?spectrumid=$SPECID&nmrshiftdbaction=exportspec&lastsearchtype=test";
//				  }else{
//				    filename=filename+document.exportspec23.format.value;
//				  }
				
			} else if (form.indexOf("\"molexport") >= 0) {
				
//				<form action="" method="post" target="_new" name="molexport92">
//				Get this molecule as 
//				<select name="format" size="1">
//					<option value="svg">svg</option>
//					<option value="jpeg">jpeg</option>
//					<option value="png">png</option>
//					<option value="tiff">tiff</option>
//					<option value="mdl">mdl molfile</option>
//			    <option value="cml">cml code</option>
//				</select>
//				 file 
//				<input type="submit" value="Request" onClick="exportMol92()">
//			</form>
				String filename = null;
				for (int j = 1; j < options.length; j++) {
					String opt = options[j];
					switch (opt) {
					case "svg":
					case "jpeg":
					case "png":
					case "tiff":
					case "mdl":
					case "cml":
						break;
					default:
						continue;
					}
					filename = molnumber + "." + opt;
					String url = molDownloadTemplate.replace("$MOLNUMBER", molnumber).replace("$SPECID", specID).replace("$FORMAT", opt);
					getSourceData(url, molDataDir, filename, false);
				}
			}
		}
	}

	private static String[] getHTMLOptions(String form) {
		String[] opts = form.split("<option");
		for (int j = 1; j < opts.length; j++) {
			int pt = opts[j].indexOf("value=\"");
			opts[j] = opts[j].substring(pt + 7, opts[j].indexOf("\"", pt + 7));
		}
		return opts;
	}

	private static void writeMetadata(File dataDir, Map<String, String> map) throws IOException {
		if (map.isEmpty())
			return;
		String s = "";
		for (Entry<String, String> e : map.entrySet()) {
			s += e.getKey() + "=" + e.getValue() + "\n";
		}
		File f = new File(dataDir, "metadata.properties");
		FAIRSpecUtilities.writeBytesToFile(s.getBytes(), f);
	}

	private static void extractMoleculeMetadata(String molnumber, File molDataDir, String data, Map<String, String> molData) {
		String[] rows = data.split("<tr");
		for (int i = 2; i < rows.length; i++) {
			String row = rows[i];
			String[] cells = row.split("<td");
			if (cells.length > 2) {
				String key = htmlValue(cells[1]);
				switch (key) {
				case "Additional information":
					continue;
				}
				String val = htmlValue(cells[2]);
				if (val.indexOf("<ul>") >= 0) {
					// chemical name(s)
					String[] list = val.split("<li");
					for (int j = 1; j < list.length; j++) {
						
						String item = list[j];
						int pt = item.indexOf('>');
						item = item.substring(pt + 1, item.indexOf("</li>"));
						pt = item.lastIndexOf('(');
						key = item.substring(pt + 1, item.lastIndexOf(')')).trim();
						pt = item.indexOf('\'');
						int pt1 = item.indexOf(pt < 0 ? '(' : '\'', pt + 1);
						val = item.substring(pt + 1, pt1).trim();
						addKeyValue(key, val, molData);
					}
					continue;
				} else if (val.length() == 0 || val.indexOf("<a ") >= 0) {
					System.out.println("skipping " + key + " " +  val);
					continue;
				}
				addKeyValue(key, val, molData);
			}
		}
	}

	private static void extractSpectraMetadata(String molnumber, File specDataDir, String data, String specID, Map<String, String> specData) {
		String[] rows = data.split("<tr");
		for (int i = 1; i < rows.length; i++) {
			String row = rows[i];
			String[] cells = row.split("<td");
			if (cells.length > 2) {
				String key = htmlValue(cells[1]);
				String val = htmlValue(cells[2]);
				addKeyValue(key, val, specData);
			}
		}
	}

	private static void addKeyValue(String key, String val, Map<String, String> map) {
		if (val.length() > 0) {
			System.out.println(key + "=" + val);
			map.put(key, val);
		}
	}

	private static String htmlValue(String s) {
		String cell = s.substring(s.indexOf('>') + 1);
		cell = cell.substring(0, cell.lastIndexOf("</td"));
		cell = cell.replace("<sub>", " ");
		cell = cell.replace("</sub>", " ");
		cell = cell.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
		int pt;
		while ((pt = cell.indexOf("document.write(")) >= 0) {
			int pt1 = cell.indexOf("<a href");
			if (pt1 > 0 && pt1 < pt) {
				cell = cell.substring(0, pt1) + cell.substring(pt + 15);
				char c = cell.charAt(pt1);
				pt = cell.indexOf(c, pt1 + 1) + 1;
				pt1 = cell.indexOf("(truncated)", pt);
				pt1 = cell.indexOf('(', pt1 + 1);
				cell = cell.substring(0, pt) + (
						pt1 < 0 
						? "" 
								: cell.substring(pt1));
			}
			// ><span class="chem:smiles"><a href="javascript:window.open('about:blank',
			// '_blank',
			// 'width=1000,height=100,menubar=no,toolbar=no,status=no,location=no').document.write('[H]OC1([H])C([H])(C([H])(C([H])([H])[H])C([H])([H])[H])C([H])([H])C([H])([H])C([H])(C1([H])[H])C([H])([H])[H]')">[H]OC1([H])C([H])(C([H])(C([H]...
			// (truncated)</a></span> (SMILES)</li>
			// ><a href="javascript:window.open('about:blank', '_blank',
			// 'width=1000,height=100,menubar=no,toolbar=no,status=no,location=no').document.write('InChI=1/C10H20O/c1-7(2)9-5-4-8(3)6-10(9)11/h7-11H,4-6H2,1-3H3/t8-,9+,10-/m1/s1')">InChI=1/C10H20O/c1-7(2)9-5-4-8...
			// (truncated)</a> (InChI with fixed H layer)</li> </ul>
			while ((pt = cell.indexOf("<span")) >= 0) {
				pt1 = cell.indexOf('>', pt);
				cell = cell.substring(0, pt) + cell.substring(pt1 + 1);
			}
			cell = cell.replaceAll("</span>", "");
		}
		return cell;
	}

	private static String getSourceData(String source, File filesDir, String fileName, boolean returnString) throws IOException {
		System.out.println("checking " + source);
		File localFile = new File(filesDir, fileName);
		if (localFile.exists()) {
			return (returnString ? FAIRSpecUtilities.getFileStringData(localFile) : null);
		} 
		filesDir.mkdirs();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] bytes = FAIRSpecUtilities.getURLBytes(source);
		if (bytes.length == 0) {
			System.out.println("NO DATA");
			return null;
		}
		FAIRSpecUtilities.putToFile(bytes, localFile);
		System.out.println("saved " + bytes.length + " bytes to " + localFile);
		return (returnString ? new String(bytes) : null);
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getCodeSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addProperty(String key, Object val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDeferredPropertyOrRepresentation(DeferredProperty p) {
		// TODO Auto-generated method stub
		
	}

}