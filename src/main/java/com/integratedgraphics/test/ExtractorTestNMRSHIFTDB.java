package com.integratedgraphics.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import com.integratedgraphics.extractor.FindingAidCreator;
import com.integratedgraphics.extractor.IFDExtractorMain;

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

	private final static String base = "https://nmrshiftdb.nmr.uni-koeln.de/";
	private final static String mainTemplate = base + "portal/js_pane/P-Results?nmrshiftdbaction=showDetailsFromHome&molNumber=MOLNUMBER&showoutertab=0_1";
	private final static String specImageTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/images/spectra/original/$SPECID.png";
	private final static String moleculeImageTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/images/molecules/large/$MOLNUMBER_$SPECID.jpeg";

	private final static String molDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$MOLNUMBER.$FORMAT?spectrumid=$SPECID&nmrshiftdbaction=exportmol&shownumbers=false&format=$FORMAT";
	private final static String specDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$FILENAME?spectrumid=$SPECID&nmrshiftdbaction=exportspec&format=$FORMAT";
	private final static String datasetDownloadTemplate = "https://nmrshiftdb.nmr.uni-koeln.de/download/NmrshiftdbServlet/$FILENAME?spectrumid=$SPECID&nmrshiftdbaction=exportdataset&format=$FORMAT";

	private static void scrapeNMRShiftDB(String dir, String string) {
		String molNumber = "20182362";
		File filesDir = new File(dir + molNumber, "files");
		File dataDir = new File(dir + molNumber, "data");
		Map<String, String> molProperties;
		try {
			String source = mainTemplate.replaceFirst("MOLNUMBER", molNumber);
			String data = getSourceData(source, filesDir, "index.html", null, true);
			molProperties = getSpectra(molNumber, source, filesDir, dataDir, data);
		    String firstSpecID = molProperties.remove("#SPECID");
		    int pt = data.indexOf("highlightTitleStyleClass");
		    String tabName = null;
		    if (pt > 0) {
		    	molProperties.put("IFD.property.colllectionset.name",  molNumber + "-" + tabName);
		    	tabName = getField(data, pt, ">", "<");
		    }    	    		
		    pt = data.indexOf("DOI for this dataset:");
		    if (pt > 0) {
		    	pt = data.lastIndexOf("<a href", pt);
		    	getDataSetData(molNumber, dataDir, data.substring(pt), firstSpecID, molProperties);
		    }

			writeMetadata(dataDir, molProperties);
			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void getDataSetData(String molNumber, File molDataDir, String data, String firstSpecID, Map<String, String> molProperties) throws IOException {
    	//   <a href="molecule/20182362/dataset/Classics+in+Spectroscopy_Chloroform-D1+%28CDCl3%29" 
		//     onclick="return showlink('molecule/20182362/dataset/Classics+in+Spectroscopy_Chloroform-D1+%28CDCl3%29');">
		//     Copy dataset link</a><br>		    	
		//   DOI for this dataset: 10.18716/nmrshiftdb2/20182362/classics_in_spectroscopy_cdcl3
		//   <br>...
		String href = getField(data, 0, "\"", "\"");
		String doi = getField(data, 0, "DOI for this dataset:", "<");
		System.out.println(href + "==" + doi);
		if (href != null) {
			href = base + href;
			molProperties.put("IFD.property.collectionset.url", href);
		}
		if (doi != null)
			molProperties.put("IFD.property.collectionset.doi", doi);
		getDownloadData(molNumber, molDataDir, null, data, firstSpecID);

	}

	private static String getField(String data, int i, String s0, String s1) {
		boolean isLast = (i < 0);
		i = (i < 0 ? data.lastIndexOf(s0) :	data.indexOf(s0, i));
		if (i < 0)
			return null;
		String s = data.substring(i + s0.length());
		i = (isLast ? s.lastIndexOf(s1) : s.indexOf(s1));
		s = (i < 0 ? null : s.substring(0, i).trim());
		return s;
	}

	private static Map<String, String> getSpectra(String molnumber, String source, File filesDir, File molDataDir, String data) throws IOException {
		
		
		
		String[] spectra = data.split("a name=\"spectrum");
		System.out.println(data.indexOf("double"));
		Map<String, String> molProperties = null;
		for (int i = 1; i < spectra.length; i++) {
			String spec = spectra[i];
			String specID = spec.substring(0, spec.indexOf('"'));
			File specdir = new File(filesDir, specID);
			File specDataDir = new File(molDataDir, specID);			
			String imageSource = specImageTemplate.replace("$SPECID", specID);
			getSourceData(imageSource, specDataDir, specID + ".png", null, false);
			imageSource = moleculeImageTemplate.replace("$MOLNUMBER", molnumber).replace("$SPECID", specID);
			getSourceData(imageSource, molDataDir, molnumber + ".jpeg", null, false);
			Map<String, String> m = getTabData(molnumber, source, molDataDir, specdir, specDataDir, spec, specID);
			if (molProperties == null) {
				molProperties = m;
				molProperties.put("#SPECID", specID);
			}
		}
		return molProperties;
	}

	private static Map<String, String> getTabData(String molnumber, String source, File molDataDir, File specFileDir, File specDataDir, String spec, String specID) throws IOException {
		String[] lines = spec.split("\n");
		Map<String, String> molData = null;
		for (int i = 0; i < lines.length; i++) {
			String tab = getField(lines[i], 0, "&tab=", "#");
			if (tab != null) {
				String data = getSourceData(source + "&tab=" + tab, specFileDir, "tab_" + tab + ".htm", null, true);
				data = getSpecData(data, specID);
				if (tab.endsWith("_1")) {
					molData = getAdditionalData(molnumber, molDataDir, specDataDir, data, specID);
				} else if (tab.endsWith("_2")) {
					getDownloadData(molnumber, molDataDir, specDataDir, data, specID);
				}
			}
		}
		return molData;
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

	private static Map<String, String> getAdditionalData(String molnumber, File molDataDir, File specDataDir, String spec, String specID) throws IOException {
		String[] tables = spec.split("<table");
		for (int i = 0; i < tables.length; i++) {
			String table = tables[i];
			if (table.indexOf("http://www.blueobelisk.org/chemistryblogs") >= 0) {
				int pt = table.indexOf(specID);
				Map<String, String> molData = new TreeMap<String, String>();
				extractMoleculeMetadata(molnumber, molDataDir, table.substring(0, pt), molData);
				Map<String, String> specData = new TreeMap<String, String>();
				extractSpectraMetadata(molnumber, specDataDir, table.substring(pt), specID, specData);
				writeMetadata(specDataDir, specData);				
				return molData;
			}
		}
		return null;
	}

	private static void getDownloadData(String molnumber, File molDataDir, File specDataDir, String data,
			String specID) throws IOException {
		String fname = getField(data, 0, "var filename=\"", "\"");
		String[] forms = data.split("<form");
		for (int i = 1; i < forms.length; i++) {
			String form = forms[i];
			String[] options = getHTMLOptions(form);
			if (form.indexOf("\"exportspec") >= 0) {
				if (fname == null)
					continue;
				//<option value="rawdata">Raw data</option>
				//<option value="svg">svg</option>
				//<option value="jpeg">jpeg</option>
				//<option value="png">png</option>
				//<option value="tiff">tiff</option>
				//<option value="jcamppeaks">jcamp-dx (peaks only)</option>
				//<option value="txt">ascii table</option>
				//<option value="cml">cml code</option>
				//<option value="originalimage">image (original spectrum, png)</option>				
				for (int j = 1; j < options.length; j++) {
					String filename = fname;
					String opt = options[j];
					String ext = null;
					switch (opt) {
					case "rawdata":
						ext = "zip";
						break;
					case "pdforig":
						ext = "pdf";
						break;
					case "jcamppeaks":
						ext = "jcamp";
						break;
					case "asciitable":
						ext = "tab";
						break;
					case "originalimage":
						ext = "png";
						break;
					case "svg":
					case "jpeg":
					case "png":
					case "tiff":
					case "txt":
					case "cml":
					default:
						ext = opt;
						break;
					}
					filename += ext;
					String url = specDownloadTemplate.replace("$SPECID", specID).replace("$FILENAME", filename)
							.replace("$FORMAT", opt);
					getSourceData(url, specDataDir, filename, null, false);
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
					String filename = molnumber + "." + opt;
					String url = molDownloadTemplate
							.replace("$MOLNUMBER", molnumber)
							.replace("$SPECID", specID)
							.replace("$FORMAT", opt);
					getSourceData(url, molDataDir, filename, null, false);
				}
			} else if (form.indexOf("\"datasetexport") >= 0) {
//				<option value="nmredata.zip">NMR record</option>
//				<option value="nmredata.sd">NMReDATA</option>
//				<option value="cml">cml</option>
//				<option value="pdf">Report</option>
				String hack = null;
				for (int j = 1; j < options.length; j++) {
					String opt = options[j];
					switch (opt) {
					case "nmredata.sd":
						hack = "nmredata";
						break;
					case "nmredata.zip":
					case "cml":
					case "pdf":
						break;
					default:
						continue;
					}
					String filename = molnumber + ".dataset." + opt;
					String url = datasetDownloadTemplate
							.replace("$FILENAME", filename)
							.replace("$SPECID", specID)
							.replace("$FORMAT", opt);
					getSourceData(url, molDataDir, filename, "nmredata", false);
				}
			}
		}
	}

	private static String[] getHTMLOptions(String form) {
		String[] opts = form.split("<option");
		for (int j = 1; j < opts.length; j++) {
			opts[j] = getField(opts[j], 0, "value=\"", "\"");
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
						String item = getField(list[j], 0, ">", "</li>");
						key = getField(item, -1, "(", ")");
						key = checkHack("key", key);
						val = getField(list[j], 0, "'", "'");
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

	private static String checkHack(String hack, String s) {
		int pt = -1;
		switch (hack) {
		case "inchi":
//			capitalization issue
//			... (truncated)</a> (INChI)
			if (s.equalsIgnoreCase("inchi"))
				s = "InChI";
			break;
		case "nmredata":			

//			duplication of value
//			> <NMREDATA_2D_1H_NJ_1H#2>
//			Spectrum_Location=https://nmrshiftdb.nmr.uni-koeln.de/portal/../download/NmrshiftdbServlet/40018635_J-resolved.zip?spectrumid=40018635&nmrshiftdbaction=exportspec&format=rawdatadownload/NmrshiftdbServlet/40018633_H,H-NOESY.zip?spectrumid=40018633&nmrshiftdbaction=exportspec&format=rawdata\
			while ((pt = s.indexOf("rawdatadownload", pt + 1)) > 0) {
				int pt0 = s.lastIndexOf("download", pt);
				s = s.substring(0, pt0) + s.substring(pt + 7);
				String specid = getField(s, pt0, "spectrumid=", "&");
				pt = s.indexOf('\\', pt);
				s = s.substring(0, pt) + "\\\nSpectrum_location=./" + specid + s.substring(pt);
			}
			//$FALL-THROUGH$
		case "up-directory":
			// 
			while ((pt = s.indexOf("/../")) > 0) {
				int pt0 = s.lastIndexOf("/", pt - 1);
				s = s.substring(0, pt0) + s.substring(pt + 3);
			}
		}
		return s;
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
		if (val != null && val.length() > 0) {
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

	private static String getSourceData(String source, File filesDir, String fileName, String hack, boolean returnString) throws IOException {
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
		if (hack != null) {
			bytes = checkHack("nmredata", new String(bytes)).getBytes();
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