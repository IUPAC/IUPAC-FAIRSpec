package com.integratedgraphics.ifs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSVendorPluginI;
import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.core.IFSStructure;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;

import com.integratedgraphics.util.Util;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A class to handle the extraction of objects from a "raw" dataset following 
 * the sequence:
 * 
 * initialize(ifsExtractScriptFile)
 * 
 * setLocalSourceDir(sourceDir)
 * 
 * setCachePattern(pattern)
 * 
 * setRezipCachePattern(pattern)
 * 
 * extractObjects(targetDir);
 * 
 * see ExtractorTest and IFSConst for these values
 * 
 * @author hansonr
 *
 */
public class Extractor implements IFSExtractorI {

	static {
		IFSVendorPluginI.init();
	}
	private static final String version = "0.0.1-alpha_2021_06_14";

	public final static int EXTRACT_MODE_CHECK_ONLY = 1;
	public final static int EXTRACT_MODE_CREATE_CACHE = 2;
	public final static int EXTRACT_MODE_REPACKAGE_ZIP = 4;
	protected static final int LOG_IGNORED = 1;
	protected static final int LOG_OUTPUT = 2;

	private static final String codeSource = "https://github.com/BobHanson/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifs/Extractor.java";

	protected String extractVersion;
	protected List<String> objects;
	protected IFSSpecDataFindingAid findingAid;
	
	@Override
	public IFSFindingAid getFindingAid() {
		return findingAid;
	}
	protected String sourceDir;

	protected static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected static Pattern pStarDotStar;

	protected File targetDir;
	
	protected final static Map<String, IFSVendorPluginI> vendorMap = new LinkedHashMap<>();

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern cachePattern;

	private boolean cachePatternHasParam;
	
	/**
	 * a memory cache of representations
	 */
	protected Map<String, IFSRepresentation> cache;

	protected List<Object[]> paramList;

	protected long extractedByteCount;
	protected IFSRepresentation nextRezip;
	protected String nextRezipName;
	protected IFSVendorPluginI nextRezipVendor;
	protected String rootPath;
	protected Object lastRezipPath;
	List<String> lstManifest = new ArrayList<>(); 
	List<String> lstIgnored = new ArrayList<>();
	private String dataSource;
	private String ifsid;
	private Map<String, IFSObject<?>> localNameToObject;

	private BitSet bsRezipVendors = new BitSet();
	private BitSet bsParamVendors = new BitSet();


	@Override
	public void setCachePattern(String sp) {
		if (sp == null)
			sp = IFSConst.defaultCachePattern;
		String s = "";
		for (int i = 0; i < IFSVendorPluginI.activeVendors.size(); i++) {
		    String cp = IFSVendorPluginI.activeVendors.get(i).vcache;
		    if (cp != null) {
		    	bsParamVendors.set(i);
		    	s += "|" + cp;
		    }
		}
		if (s.length() > 0) {
			s = "(?<param>" + s.substring(1) + ")|" + sp;
			cachePatternHasParam = true;
		} else {
			s = sp;
		}
		cachePattern = Pattern.compile("(?<type>" + s + ")");
		cache = new HashMap<String, IFSRepresentation>();
	}

	private IFSVendorPluginI getVendorForParams(Matcher m) {
		for (int i = bsParamVendors.nextSetBit(0); i >= 0; i = bsParamVendors.nextSetBit(i + 1)) {
			String ret = m.group("param" + i);
			if (ret != null && ret.length() > 0) {
				return IFSVendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * Set the file match zip cache pattern.
	 * 
	 * @param procs
	 * @param toExclude
	 */	
	@Override
	public void setRezipCachePattern(String procs, String toExclude) {
		String s = "";
		
		for (int i = 0; i < IFSVendorPluginI.activeVendors.size(); i++) {
		    String cp = IFSVendorPluginI.activeVendors.get(i).vrezip;
		    if (cp != null) {
		    	bsRezipVendors.set(i);
		    	s = s + "|" + cp;
		    }
		}
		s += (procs == null ? "" : "|" + procs);
		if (s.length() > 0)
			s = s.substring(1);
		rezipCachePattern = Pattern.compile(s);
	//	rezipCacheExcludePattern = (toExclude == null ? null : Pattern.compile(toExclude));
		rezipCache = new ArrayList<>();
	}
	
	private IFSVendorPluginI getVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return IFSVendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}




	/**
	 * patterns to ignore completely.
	 */
	Pattern ignorePattern = Pattern.compile("(MACOSX)|(desktop\\.ini)|(\\.DS_Store)");
	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern rezipCachePattern;
	protected List<IFSRepresentation> rezipCache;
	protected long ignoredByteCount;
	protected int manifestCount;
	protected int ignoredCount;
	private Map<String, IFSStructure> htManifestNameToStructure = new LinkedHashMap<>();
	private Map<String, IFSSpecData> htManifestNameToSpecData = new LinkedHashMap<>();
	//private String zipDirName;
	//private File extractScriptFile;
	private String extractScript;

	private String dataLicenseURI;

	private String dataLicenseName;

	private String license;

	private String puburi;

	private Map<String, Object> pubInfo = new LinkedHashMap<>();

	private String localName;

	public Extractor() {
		clearZipCache();
	}
	
	///////// PHASE 1: Reading the IFS-extract.json file ////////

	@Override
	public void initialize(File ifsExtractScriptFile) throws IOException {
		getObjectsForFile(ifsExtractScriptFile);
		if (puburi != null) {
			getPubInfo(puburi);
		}
	}

	private final static String crossciteURI = "https://data.crosscite.org/application/vnd.datacite.datacite+xml/";
	
	private void getPubInfo(String puburi) {
		if (puburi != null && puburi.startsWith("https://doi.org/")) {
			String url = crossciteURI + puburi.substring(16);
			try {
				InputStream is = new URL(url).openStream();
				byte[] bytes = Util.getLimitedStreamBytes(is, -1, null, true, true);
				is.close();
				extractPubInfo(new String(bytes));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	protected void extractPubInfo(String pubxml) {
		log(pubxml);
		pubInfo.clear();
		int[] pt = new int[1];
		List<String> authors = new ArrayList<>();				
		String au;
		String s = "";
		while ((au = extractXML("creatorName", pubxml, pt)) != null) {
			authors.add(au);
			s += ", " + au;
		}
		if (s.length() > 0)
			pubInfo.put("authors", s.substring(2));
		s = extractXML("title>", pubxml, null);
		if (s != null)
			pubInfo.put("title", s);
		s = extractXML("description descriptionType=\"SeriesInformation\"", pubxml, null);
		if (s != null)
			pubInfo.put("desc", s);
		pubInfo.put("uri",  puburi);
	}

	private String extractXML(String key, String xml, int[] pt) {
		if (pt == null)
			pt = new int[1];
		int p = xml.indexOf("<" + key, pt[0]);
		if (p < 0 || (p = xml.indexOf(">", p) + 1) <= 0)
			return null;
		int p2 = key.indexOf(" ");
		p2= xml.indexOf("</" + (p2 < 0 ? key : key.substring(0, p2)), p);
		if (p2 < 0)
			return null;
		pt[0] = p2 + key.length() + 2;
		return xml.substring(p, p2);
	}
	



	public List<String> getObjectsForFile(File ifsExtractScript) throws IOException {
		//extractScriptFile = ifsExtractScript;
		log("Extracting " + ifsExtractScript.getAbsolutePath());
		return getObjectsForStream(ifsExtractScript.toURI().toURL().openStream());
	}

	public List<String> getObjectsForStream(InputStream is) throws IOException {
		byte[] bytes = Util.getLimitedStreamBytes(is, -1, null, true, true);
		extractScript = new String(bytes);
		return objects = parseScript(extractScript);
	}

	/**
	 * 
	 * @param script
	 * @return parsed list of objects from an IFS-extract.js JSON
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected List<String> parseScript(String script) throws IOException {
		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get("IFS-extract-version");
		ifsid = (String) jsonMap.get("ifsid");
		log(extractVersion);
		List<Map<String, Object>> pathway = (List<Map<String, Object>>) jsonMap.get("pathway");
		List<String> objects = getObjects(pathway);
		log(objects.size() + " digital objects found");
		return objects;
	}

	
	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 * @return list of IFSObject definitions
	 */
	protected List<String> getObjects(List<Map<String, Object>> pathway) {

		// input:

		// {"IFS-extract-version":"0.1.0-alpha","pathway":[
		// {"hash":"0c00571"},
		// {"pubid":"acs.orglett.{hash}"},
		// {"src":"IFS.findingaid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
		// {"data":"{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
		//
		// {"path":"{data}|FID for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}"},
		// {"objects":"{path}/{IFS.structure.representation.mol.2d::{id}.mol}"},
		// {"objects":"{path}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}-NMR.zip}"},
		// {"objects":"{path}/HRMS.zip|{IFS.spec.hrms.representation.pdf::**/*.pdf}"},
		// ]}

		// output:

		// [
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}-NMR.zip}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|{IFS.spec.hrms.representation.pdf::**/*.pdf}"
		// ]

		Lst<String> keys = new Lst<>();
		Lst<String> values = new Lst<>();
		List<String> objects = new ArrayList<>();
		for (int i = 0; i < pathway.size(); i++) {
			Map<String, Object> def = pathway.get(i);
			for (Entry<String, Object> e : def.entrySet()) {
				String key = e.getKey();
				String val = (String) e.getValue();
				if (val.indexOf("{") >= 0) {
					String s = PT.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						if (debugging)
							log(val + "\n" + s + "\n");
						e.setValue(s);
					}
					val = s;
				}
				switch (key) {
				case "puburi":
					try {
						puburi = getValue(val, "IFS.findingaid.source.publication.uri", null);
					} catch (IFSException e1) {
					}
					log(puburi);
					break;
				case "license":
					license = val;
					break;
				case "objects":
					objects.add("{IFS.findingaid.object::" + val + "}");
					break;
				default:
					keys.addLast("{" + key + "}");
					values.addLast(val);
				}
			}
		}
		return objects;

	}

	///////// PHASE 2: Parsing the ZIP file and extrating objects from it ////////

	/**
	 * Find and extract all objects of interest from a ZIP file.
	 * 
	 */
	@Override
	public IFSSpecDataFindingAid extractObjects(File targetDir) throws IFSException, IOException {
		if (cache == null)
			setCachePattern(null);
		if (rezipCache == null)
			setRezipCachePattern(null, null);
		this.targetDir = targetDir;
		targetDir.mkdir();

		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"

// [parse first node]

		// {IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

// [start a new finding aid and download this resource]

		// |

// [get file list]

		// (\Q^FID for
		// Publication/\E)(\Q{id=IFS.structure.param.compound.id::*}\E)(\Q.zip\E)

// [pass to StructureIterator]		
// [find matches and add structures to finding aid structure collection]

		// |

// [get file list]
		// [for each structure...]

		// \Q{id}/\E(\Q{IFS.structure.representation.mol.2d::{id}.mol}/E)"

		// add this representation to this structure

		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}-NMR.zip}"

		// [parse first node]

		// {IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

		// [start a new finding aid and download this resource]

		// |

		// [get file list]

		// FID for Publication/{id=IFS.structure.param.compound.id::*}.zip

		// [pass to StructureIterator]
		// [find matches and add structures to finding aid structure collection if
		// necessary

		// |

		// [get file list]

		// {id}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}-NMR.zip}

		// [pass to SpecDataIterator]
		// [find matches and add NMR spec data to finding aid spec data collection; also
		// add struc+spec to finding aid StrucSpecCollection]

		// bruker directories identified with / just before vendor dataset closing }
		// standard ^....$
		// * at end just removes $
		// *. becomes [^.]
		// . becomes [.]
		// **/*.pdf becomes (?:[^/]+/)*).+\Q.pdf\E

//		String s = "test/ok/here/1c.pdf";    // test/**/*.pdf
//		Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
//		Matcher m = p.matcher(s);
//		log(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");

		log("=====");

		if (sourceDir != null)
			log("extractObjects from " + sourceDir);
		log("extractObjects to " + targetDir.getAbsolutePath());

		String lastRootPath = null;
		String lastURL = null;
		findingAid = null;

		// Note that some files have multiple objects.
		// These may come from multiple sources, or they may be from the same source.
		paramList = new ArrayList<>();

		if (license != null) {
			dataLicenseURI = getValue(license, "IFS.findingaid.license.uri", null);
			dataLicenseName = getValue(license, "IFS.findingaid.license.name", null);
			log("! IFS.findingaid.license: " + dataLicenseName + " " + dataLicenseURI);
		}
		for (int i = 0; i < objects.size(); i++) {

			String sObj = objects.get(i);
			log("found object " + sObj);
			
			// Read the Object data and create a ParseObject for it.
			// Note that these objects are just the abstract model that will
			// be matched using regex Pattern/Matcher to the file entries.

			// next should never be a problem, as we do this wrapping ourselves
			String sAid = getValue(sObj, "IFS.findingaid.object", null);
			if (sAid == null)
				throw new IFSException("no IFS.findingaid.object:" + sObj);

			int[] pt = new int[1];

			String unlocalizedURL = getValue(sAid, "IFS.findingaid.source.data.uri", pt);

			lastURL = setFindingAidAndGlobals(sAid, unlocalizedURL, lastURL);

			String sData = sAid.substring(pt[0] + 1); // skip first "|"

			// localize the URL if we are using a local copy of a remote resource.

			String sURL = localizeURL(unlocalizedURL);
			if (debugging)
				log("opening " + sURL);

			lastRootPath = initializeCollection(sURL, lastRootPath);

			// At this point we now have all spectra ready to be associated with structures.

			parseZipFileNamesForObjects(sURL, new ObjectParser(sData));

			rezipFilesAndExtractParameters(sURL);

			processParamList();

			log("found " + localNameToObject.size() + " IFS objects");

		}
		findingAid.setPubInfo(pubInfo);
		setCollectionManifests(false);
		findingAid.finalizeExtraction();
		return findingAid;
	}

	/**
	 * This method is called by IFSVendorPluginI classes. 
	 */
	@Override
	public void addParam(String param, Object val) {
		paramList.add(new Object[] { localName, param, val });
	}
	
	private void processParamList() {
		for (Object[] a: paramList) {
			String localName = (String) a[0];
			String param = (String) a[1];
			Object value = a[2];
			IFSSpecData spec = htManifestNameToSpecData.get(localName);
			if (spec == null) {
				System.out.println("! manifest not found for " + localName);
			} else {
				spec.setPropertyValue(param, value);
			}
		}
		paramList.clear();
		
		for (Entry<String, IFSRepresentation> e : cache.entrySet()) {
			String localName = e.getKey();
			IFSStructure s = htManifestNameToStructure.get(localName);
			if (s != null) {
				IFSRepresentation r = e.getValue();
				IFSRepresentation r1 = s.getRepresentation(r.getRef().getPath(), localName, true);
				r1.setLength(r.getLength());
				r1.setType(r.getType());
			}
		}
	}

	private String setFindingAidAndGlobals(String sAid, String unlocalizedURL, String lastURL) throws IFSException {
		// must have a source
		if (unlocalizedURL == null) {
			throw new IFSException("no IFS.findingaid.source.data.uri:" + sAid);
		}
		
		if (findingAid == null) {
			findingAid = new IFSSpecDataFindingAid(ifsid, unlocalizedURL);
			if (dataLicenseURI != null) {
				findingAid.setPropertyValue("IFS.fairspec.data.license.uri", dataLicenseURI);
			}
			if (dataLicenseName != null) {
				findingAid.setPropertyValue("IFS.fairspec.data.license.name", dataLicenseName);
			}
//			if (puburi != null) {
//				findingAid.setPropertyValue("IFS.findingaid.source.publication.uri", puburi);
//			}
			localNameToObject = new HashMap<>();
		} else if (!unlocalizedURL.equals(lastURL)){
			findingAid.addUrl(unlocalizedURL);
			lastURL = unlocalizedURL;
		}
		return lastURL;
	}

	private String initializeCollection(String sURL, String lastRootPath) throws IOException {
		
		String zipPath = sURL.substring(sURL.lastIndexOf(":") + 1);
		String rootPath = new File(zipPath).getName();
		
		// remove ".zip" if present in the overall name
		
		if (rootPath.endsWith(".zip"))
			rootPath = rootPath.substring(0, rootPath.indexOf(".zip"));

		new File(targetDir + "/" + rootPath).mkdir();
					
		if (lastRootPath != null && !rootPath.equals(lastRootPath)) {
			// close last collection logs
			setCollectionManifests(false);
		}
		if (!rootPath.equals(lastRootPath)) {
			// open a new log
			this.rootPath = lastRootPath = rootPath;				
			setCollectionManifests(true);
		}
		
		return lastRootPath;
	}

	private void parseZipFileNamesForObjects(String zipPath, ObjectParser parser) throws IOException, IFSException {
		URL url = new URL(zipPath);// getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them
		Map<String, ZipEntry> zipFiles = htZipContents.get(url.toString());
		if (zipFiles == null) {
			// Scan URL zip stream for files.
			zipFiles = readZipContentsIteratively(url.openStream(), new LinkedHashMap<String, ZipEntry>(), "", false);
			htZipContents.put(url.toString(), zipFiles);
		}
		for (String zipName : zipFiles.keySet()) {
			IFSObject<?> obj = addIFSObjectsForName(parser, zipName);
			if (obj != null) {
				localNameToObject.put(getLocalName(zipName), obj);
			}
		}

	}

	private void rezipFilesAndExtractParameters(String sURL) throws MalformedURLException, IOException {
		if (rezipCache != null && rezipCache.size() > 0) {
			lastRezipPath = null;
			// this will drain the rezipCache
			getNextRezipName();
			readZipContentsIteratively(new URL(sURL).openStream(), null, "", true);
		}		
	}

	protected void setCollectionManifests(boolean isOpen) throws IOException {
		if (!isOpen) {
			writeBytesToFile(extractScript.getBytes(), getFileTarget("_IFS_extract.json"));
			
			outputListJSON(lstManifest, getFileTarget("_IFS_manifest.json"), "manifest");
			if (lstIgnored.size() > 0)
				outputListJSON(lstIgnored, getFileTarget("_IFS_ignored.json"), "ignored");
		}
		lstManifest.clear();
		lstIgnored.clear();
	}

	protected static void writeBytesToFile(byte[] bytes, File fileTarget) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileTarget);
		fos.write(bytes);
		fos.close();
	}


	@SuppressWarnings("deprecation")
	protected void outputListJSON(List<String> lst, File fileTarget, String type) throws IOException {
		log("! saved " + fileTarget + " (" + lst.size() + " items)");
		// Date d = new Date();
		// all of a sudden, on 2021.06.13 at 1 PM
		// file:/C:/Program%20Files/Java/jdk1.8.0_251/jre/lib/sunrsasign.jar cannot be
		// found when
		// converting d.toString() due to a check in Date.toString for daylight savings time!
		
		StringBuffer sb = new StringBuffer();
		sb.append("{" + "\"IFS.fairspec.version\":\"" + IFSConst.IFS_FAIRSpec_version + "\",\n");
		if (dataLicenseURI != null) {
			sb.append("\"IFS.fairspec.data.license.uri\":\"" + dataLicenseURI + "\",\n");
		}
		if (dataLicenseName != null) {
			sb.append("\"IFS.fairspec.data.license.name\":\"" + dataLicenseName + "\",\n");
		}
		sb.append("\"IFS.extractor.version\":\"" + version + "\",\n") 
		  .append("\"IFS.extractor.code\":\"" + codeSource + "\",\n") 
		  .append("\"IFS.extractor.list.type\":\"" + type + "\",\n")
		  .append("\"IFS.extractor.scirpt\":\"_IFS_extract.json\",\n") 
		  .append("\"IFS.extractor.source\":\"" + dataSource + "\",\n")
		  .append("\"IFS.extractor.creation.date\":\"" + findingAid.getDate().toGMTString() + "\",\n")
		  .append("\"IFS.extractor.count\":" + lst.size() + ",\n")
		  .append("\"IFS.extractor.list\":\n" + "[\n");
		String sep = "";
		if (type.equals("manifest")) {
			// list the zip files first
			for (String name : lst) {
				if (type.equals("manifest") && name.endsWith(".zip")) {
					sb.append((sep + "\"" + name + "\"\n"));
//					sb.append(sep).append(getManifestEntry(name));
					sep = ",";
				}
			}
			for (String name : lst) {
				if (type.equals("manifest") && !name.endsWith(".zip")) {
					sb.append((sep + "\"" + name + "\"\n"));
//					sb.append(sep).append(getManifestEntry(name));
					sep = ",";
				}
			}
		} else {
			for (String name : lst) {
				sb.append((sep + "\"" + name + "\"\n"));
				sep = ",";
			}
		}
		sb.append("]}\n");
		writeBytesToFile(sb.toString().getBytes(), fileTarget);
	}

//	private Object getManifestEntry(String name) {
//		IFSRepresentation rep = cache.get(name);
//		IFSSpecData sd = htManifestNameToSpecData.get(name);
//		String params = "";//(sd == null ? "" : ", " + sd.getParamJSON());
//		long len = rep.getLength();
//		return "{\"name\":\"" + name + "\","
//				+ "\"len\":" + len + ","
//				+ "\"type\":\"" + rep.getType() + "\","
//				+ "\"origin\":\"" + rep.getRef().getValue() + "\""
//				+ params
//				+ "}\n";
//	}

	/**
	 * Use the regex ObjectParser to match a file name with a pattern defined in the IFS-extract.json description.
	 * This will result in the formation of one or more IFSObjects -- an IFSAanalysis, IFSStructureSpecCollection, IFSSpecDataObject, or IFSStructure, for instance.
	 * But that will probably change. 
	 * 
	 * The parser specifically looks for Matcher groups, regex (?<xxxx>...), that 
	 * have been created by the ObjectParser from an object line such as: 
	 * 
	 *  {IFS.spec.nmr.representation.vendor.dataset::{IFS.structure.param.compound.id::*-*}-{IFS.spec.nmr.param.expt::*}.jdf}
     *
     * 
	 * 
	 * @param parser
	 * @param zipName
	 * @return one of IFSStructureSpec, IFSSpecData, IFSStructure, in that order, depending upon availability
	 * 
	 * @throws IFSException
	 */
	private IFSObject<?> addIFSObjectsForName(ObjectParser parser, String zipName) throws IFSException {
		Matcher m = parser.p.matcher(zipName);
		if (!m.find())
			return null;
		findingAid.beginAddObject(zipName);
		if (debugging)
			log("found " + zipName);
		
		// If an IFSSpecData object is added, then it will also be added to htManifestNameToSpecData

		for (String key : parser.keys.keySet()) {
			String param = parser.keys.get(key);
			String value = m.group(key);
			final String localName = getLocalName(zipName);
			IFSObject<?> obj = findingAid.addObject(rootPath, param, value, localName);
			if (obj instanceof IFSSpecData) {
				htManifestNameToSpecData.put(localName, (IFSSpecData) obj);
			} else if (obj instanceof IFSStructure && param.indexOf("structure.representation.") >= 0) {
				htManifestNameToStructure.put(localName, (IFSStructure) obj);
			}				
			if (debugging)
				log("found " + param + " " + value);
			;
		}
		return findingAid.endAddObject();
	}

	protected void log(String msg) {
		if (debugging || msg.startsWith("!"))
			System.out.println(msg);
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 */
	protected String localizeURL(String sUrl) {
		dataSource = sUrl;
		if (sourceDir != null) {
			int pt = sUrl.lastIndexOf("/");
			sUrl = sourceDir + sUrl.substring(pt);
			if (!sUrl.endsWith(".zip"))
				sUrl += ".zip";
		}
		return sUrl;
	}

	protected static String getValue(String sObj, String key, int[] pt) throws IFSException {
		key = "{" + key + "::";
		if (pt == null) 
			pt = new int[1];
		int p = sObj.indexOf(key, pt[0]);
		if (p < 0)
			return null;
		int q = -1;
		int nBrace = 1;
		p += key.length();
		int len = sObj.length();
		for (int i = p; i < len && nBrace > 0; i++) {
			switch (sObj.charAt(i)) {
			case '{':
				q = i;
				nBrace++;
				break;
			case '}':
				if (--nBrace < 0) {
					throw new IFSException("unopened '}' in " + sObj + " at char " + i);
				}
				q = i;
				break;
			}
		}
		if (nBrace > 0) {
			throw new IFSException("unclosed '{' in " + sObj + " at char " + q);
		}
		pt[0] = q;
		return sObj.substring(p, pt[0]++);
	}

	protected static Map<String, Map<String, ZipEntry>> htZipContents = new LinkedHashMap<>();

	protected static boolean debugging;

	/**
	 * zip contents caching can save time in complex loading.
	 * 
	 */
	public static void clearZipCache() {
		htZipContents.clear();
	}

//	/**
//	 * Retrieve the ZipEntry for a file in a given zip file.
//	 * 
//	 * Note that in JavaScript only, the ZipEntry returned contains a reference to
//	 * the underlying ByteArrayInputStream ultimately backing this zip file. This
//	 * allows us to retrieve the file data directly from the ZipEntry using
//	 * jsutil.getZipBytes(zipEntry).
//	 * 
//	 * Names starting with "." are taken as case-insensitive extensions
//	 * 
//	 * 
//	 * @param zipFile
//	 * @param fileName   zip entry name or lower-case extension starting with "."
//	 * @param isContains fileName is a fragment of the entry name, not the other way
//	 *                   around
//	 * @return the ZipEntry for this file, possibly cached.
//	 */
//	protected static ZipEntry findZipEntry(String zipFile, String fileName, boolean isContains) {
//		Map<String, ZipEntry> contents = getZipContents(zipFile);
//		if (contents == null)
//			return null;
//		if (!isContains) {
//			return contents.get(fileName);
//		}
//		boolean isLCExt = fileName.startsWith(".");
//		for (Entry<String, ZipEntry> entry : contents.entrySet()) {
//			String key = entry.getKey();
//			if ((isLCExt ? key.toLowerCase() : key).indexOf(fileName) >= 0)
//				return entry.getValue();
//		}
//		return null;
//	}

	protected Map<String, ZipEntry> readZipContentsIteratively(InputStream is, Map<String, ZipEntry> fileNames,
			String baseName, boolean doRezip) throws IOException {
		if (debugging && baseName.length() > 0)
			log("opening " + baseName);
		boolean isTopLevel = (baseName.length() == 0);
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		ZipEntry rezipEntry = null;
		while ((zipEntry = (rezipEntry == null ? zis.getNextEntry() : rezipEntry)) != null) {
			rezipEntry = null;
			if (zipEntry.isDirectory())
				//zipDirName = baseName + zipEntry.getName();
			if (!zipEntry.isDirectory() && zipEntry.getSize() == 0)
				continue;
			String zipName = baseName + zipEntry.getName();
			if (ignorePattern.matcher(zipName).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				logCollectionFile(zipName, LOG_IGNORED, zipEntry.getSize());
				continue;
			}
			if (debugging)
				log(zipName);
			if (fileNames != null) {
				fileNames.put(zipName, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			}
			if (zipName.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, zipName + "|", doRezip);
			} else if (doRezip) {
				if (zipName.equals(nextRezipName)) {
					rezipEntry = rezip(nextRezipVendor, zis, zipName, zipEntry);
					getNextRezipName();
				} else {
					final String localName = getLocalName(zipName);
					if (!zipEntry.isDirectory() && !lstIgnored.contains(zipName) && !lstManifest.contains(localName)) {
						logCollectionFile(zipName, LOG_IGNORED, zipEntry.getSize());
						log("! ignoring " + rootPath + "|" + zipName);
					}
				}
			} else {
				checkToCache(zipName, zis, zipEntry);
			}
		}
		if (isTopLevel)
			zis.close();
		return fileNames;
	}

	protected void logCollectionFile(String fileName, int mode, long len) {
		switch (mode) {
		case LOG_IGNORED:
			ignoredByteCount += Math.max(len, 0);
			ignoredCount++;
			lstIgnored.add(fileName);
			break;
		case LOG_OUTPUT:
			extractedByteCount += len;
			manifestCount++;
			lstManifest.add(fileName);
			break;
		}
	}

	/**
	 * When a ZipEntry is a directory and has been identified as a SpecData object,
	 * we need to catalog and rezip that file.
	 * 
	 * Create a new zip file that reconfigures the file directory to contain what we
	 * want it to.
	 * 
	 * @param zis
	 * @param zipDirName
	 * @param entry
	 * @return next (unassociated) zip entry
	 * @throws IOException
	 */
	protected ZipEntry rezip(IFSVendorPluginI vendor, ZipInputStream zis, String zipDirName, ZipEntry entry)
			throws IOException {
		log("rezipping " + zipDirName + " for " + entry + " " + new File(entry.getName()).getName());
		File outFile = getFileTarget(zipDirName + ".zip");
		final String localName = getLocalName(zipDirName);
		FileOutputStream fos = new FileOutputStream(outFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		String dirName = entry.getName();
		String parent = new File(entry.getName()).getParent();
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		String newDir = vendor.getRezipPrefix(dirName.substring(lenOffset));
		if (newDir == null) {
			newDir = "";
		} else {
			newDir = newDir + "/";
			lenOffset = dirName.length();
		}
		String entryName;
		Matcher m = null;
		vendor.startRezip(this);
		long len = 0;
		while ((entry = zis.getNextEntry()) != null) {
			if (entry.isDirectory() || (entryName = entry.getName()).startsWith("__MACOS"))
				continue;
			if (!entryName.startsWith(dirName))
				break;
			if (ignorePattern.matcher(entryName).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				continue;
			}
			
			
			String param = null;
			IFSVendorPluginI v = null;
		   // include in zip?
			boolean doInclude = (vendor == null || vendor.doRezipInclude(entryName));
			// cache this one? -- could be a different vendor -- JDX inside Bruker directory, for example
			boolean doCache = (cachePattern != null && (m = cachePattern.matcher(entryName)).find()
					&& (param = getParamName(m)) != null 
					&& ((v = getVendorForParams(m)) == null || v.doExtract(entryName)));
						
					
			
			
			boolean doCheck = (doCache || v != null);

			len = entry.getSize();
			if (len != 0) {
				OutputStream os;
				if (doCheck) {
					os = new ByteArrayOutputStream();
				} else if (doInclude) {
					os = zos;
				} else {
					continue;
				}
				String outName = newDir + entryName.substring(lenOffset);
				if (doInclude)
					zos.putNextEntry(new ZipEntry(outName));
				Util.getLimitedStreamBytes(zis, len, os, false, false);
				if (doCheck) {
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					if (doInclude)
						zos.write(bytes);
					this.localName = localName;
					if (v == null || v == vendor) {
						vendor.accept(null, outName, bytes);
					} else {
						v.accept(this, outName, bytes);
					}
				}
				if (doInclude)
					zos.closeEntry();
			}
		}
		vendor.endRezip();
		zos.close();
		fos.close();
		len = outFile.length();
		IFSRepresentation r = findingAid.getSpecDataRepresentation(zipDirName);
		r.setLength(len);
		cacheFile(localName, zipDirName, len, "application/zip");
		logCollectionFile(localName, LOG_OUTPUT, len);
		extractedByteCount += len;
		return entry;
	}

	protected void getNextRezipName() {
		if (rezipCache.size() == 0) {
			nextRezipName = null;
			nextRezip = null;
		} else {
			nextRezipName = (String) (nextRezip = rezipCache.remove(0)).getRef().getValue();
			nextRezipVendor = (IFSVendorPluginI) nextRezip.getData();
		}
	}

	protected void checkToCache(String zipName, InputStream zis, ZipEntry zipEntry)
			throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m;
		
		// check for files that should be pulled out - these might be JDX files, for example.
		// "param" appears if a vendor has flagged these files for parameter extraction. 
		
		if (cachePattern != null && (m = cachePattern.matcher(zipName)).find()) {
			String type = m.group("type");
			String param = getParamName(m);
			IFSVendorPluginI v = getVendorForParams(m);
			boolean doExtract = (param == null || v == null || v.doExtract(zipName));
			boolean doCheck = (v != null && doExtract);
			
//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely
		
			
			File f = (doExtract ? getFileTarget(zipName) : null);
			OutputStream os = (!doCheck && !doExtract ? null : 
				doCheck ? new ByteArrayOutputStream() : new FileOutputStream(f));
			if (os != null)		
				Util.getLimitedStreamBytes(zis, len, os, false, true);
			String localName = getLocalName(zipName);
			if (doExtract) {				
				len = f.length();
				cacheFile(localName, zipName, len, type);
				logCollectionFile(localName, LOG_OUTPUT, len);
				if (doCheck) {
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					// set this.localName for parameters
					// preserve this.localName, as we might be in a rezip. 
					// as, for example, a JDX file within a Bruker dataset
					writeBytesToFile(bytes, f);
					len = bytes.length;
					String oldLocal = this.localName;
					this.localName = localName;
					// indicating "this" here notifies the vendor plugin that 
					// this is a one-shot file, not a collection.
					v.accept(this, zipName, bytes);
					this.localName = oldLocal;
				}
				extractedByteCount += len;
			} 
		}
		
		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point is 
		// finding a procs file within a Bruker data set. Or, in principle, an acqus file and 
		// just an FID but no pdata/ directory. But for now we want to see that processed data.
		
		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(zipName)).find()) {
			
			// e.g. exptno/./pdata/procs
			
			zipName = m.group("path");
			if (zipName.equals(lastRezipPath)) {
				log("duplicate path " + zipName);
			} else {
				lastRezipPath = zipName;
				IFSVendorPluginI v = getVendorForRezip(m);
				IFSRepresentation ref = new IFSRepresentation("rezip", new IFSReference(zipName, getLocalName(zipName), "./" + rootPath), v, len);				
				rezipCache.add(ref);
				log("rezip added " + zipName);
			}
		}

	}

	private String getParamName(Matcher m) {
		try {
			if (cachePatternHasParam)
				return m.group("param");
		} catch (Exception e) {
		}
		return null;
	}

	private void cacheFile(String localName, String zipName, long len, String type) {
		type = IFSSpecDataFindingAid.MediaTypeFromName(localName);		
		cache.put(localName, new IFSRepresentation(type, new IFSReference(zipName, localName, "./" + rootPath), null, len));
	}

	protected File getFileTarget(String fname) {
		return new File(targetDir + "/" + rootPath + "/" + getLocalName(fname));
	}

	protected static String getLocalName(String fname) {
		boolean isDir = fname.endsWith("/");
//		if (isDir)
//			System.out.println(fname);
		return fname.replace('|', '_').replace('/', '_').replace(' ', '_') + (isDir ? ".zip" : "");
	}

	@Override
	public void setLocalSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}

	/**
	 * A class for parsing the object string and using regex to match filenames.
	 * 
	 * @author hansonr
	 *
	 */

	static class ObjectParser {

		protected String sData;

		protected Pattern p;

		protected List<String> regexList;

		protected Map<String, String> keys;

		public ObjectParser(String sData) throws IFSException {
			this.sData = sData;
			init();
		}

		/**
		 * Prepare pattern and match.
		 * 
		 * @throws IFSException
		 * 
		 * 
		 */
		protected void init() throws IFSException {
			// Using Java and JavaScript named capture groups for in-line defining.
			//
			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q
			//
			// **/ becomes \\E(?:[^/]+/)*)\\Q
			//
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			//
			// * becomes \\E.+\\Q
			//
			// {id=IFS.spec.nmr.param.expt::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			//
			// {IFS.spec.nmr.param.expt::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			//
			// <id> becomes \\k<id>
			//
			// generally ... becomes ^\\Q...\\E$
			//
			// \\Q\\E in result is removed
			//
			// so:
			//
			// {IFS.spec.nmr.param.expt::*} becomes \\E(?<IFS0nmr0param0expt>.+)\\Q
			//
			// {IFS.spec.nmr.representation.vendor.dataset::{IFS.structure.param.compound.id::*-*}-{IFS.spec.nmr.param.expt::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFS0nmr0representation0vendor0dataset>(?<IFS0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFS0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFS.structure.param.compound.id::*}.zip|{IFS.spec.nmr.representation.vendor.dataset::{id}_{IFS.spec.nmr.param.expt::*}/}
			//
			// becomes:
			//
			// ^(?<id>*)\\Q.zip|\\E(?<IFS0nmr0representation0vendor0dataset>\\k<id>\\Q_\\E(<IFS0nmr0param0expt>*)\\Q/\\E)$

			// so....

			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q

			String s = fixRegex(null);

			// **/ becomes \\E(?:[^/]+/)*\\Q

			s = PT.rep(s, "**/", "\\E(?:[^/]+/)\2\\Q");

			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				if (pStarDotStar == null)
					pStarDotStar = Pattern.compile("\\*(.)\\*");
				while ((m = pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					s = PT.rep(s, "*" + schar + "*", "\\E([^" + schar + "]+(?:" + schar + "[^" + schar + "]+)\2)\\Q");
				}
			}
			// * becomes \\E.+\\Q

			s = PT.rep(s, "*", "\\E[^|/]+\\Q");

			// {id=IFS.spec.nmr.param.expt::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFS.spec.nmr.param.expt::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>

			s = compileIFSDefs(s, true, true);

			// restore '*'
			s = s.replace('\2', '*');

			// restore regex
			// wrap with quotes and constraints ^\\Q...\\E$

			s = "^\\Q" + fixRegex(s) + "\\E$";

			// \\Q\\E in result is removed

			s = PT.rep(s, "\\Q\\E", "");

			if (debugging)
				System.out.println("pattern: " + s);
			p = Pattern.compile(s);
//			m = p.matcher("FID for Publication/S6.zip|S6/HRMS.zip|HRMS/67563_hazh180_maxis_pos.pdf");
//			log(m.find());
		}

		/**
		 * Find and regex-ify all {id=IFS.param::value} or {IFS.param::value}.
		 * 
		 * @param s growing regex string
		 * @return regex string with all {...} fixed
		 * @throws IFSException
		 */
		protected String compileIFSDefs(String s, boolean isFull, boolean replaceK) throws IFSException {
			while (s.indexOf("::") >= 0) {
				Matcher m = objectDefPattern.matcher(s);
				if (!m.find())
					return s;
				String param = m.group(1);
				String val = m.group(2);
				String pv = "{" + param + "::" + val + "}";
				if (val.indexOf("::") >= 0)
					val = compileIFSDefs(val, false, replaceK);
				int pt = param.indexOf("=");
				if (pt == 0)
					throw new IFSException("bad {def=key::val} expression: " + param + "::" + val);
				if (keys == null)
					keys = new LinkedHashMap<String, String>();
				String key;
				if (pt > 0) {
					key = param.substring(0, pt);
					param = param.substring(pt + 1);
				} else {
					key = param.replace('.', '0');
				}
				keys.put(key, param);
				String bk = "{" + key + "}";
				if (s.indexOf(bk) >= 0) {
					s = PT.rep(s,  bk, "<" + key + ">");					
				}
				// escape < and > here
				s = PT.rep(s, pv, (replaceK ? "\\E(?\3" + key + "\4\\Q" : "\\E(?<" + key + ">\\Q") + val + "\\E)\\Q");
			}
			if (isFull && (s.indexOf("<") >= 0 || s.indexOf("\3") >= 0)) {
				// now fix k< references and revert \3 \4
				s = PT.rep(s, "<", "\\E\\k<");
				s = PT.rep(s, ">", ">\\Q").replace('\3', '<').replace('\4', '>');
			}
			return s;
		}

		protected String fixRegex(String s) throws IFSException {
			if (sData.indexOf("{regex::") < 0)
				return (s == null ? sData : s);
			if (s == null) {
				// init
				s = sData;
				regexList = new ArrayList<>();
				int[] pt = new int[1];
				int i = 0;
				while ((pt[0] = s.indexOf("{regex::")) >= 0) {
					// save regex and replace by \0n\1
					int p0 = pt[0];
					String rx = getValue(s, "regex", pt);
					regexList.add("\\E" + rx + "\\Q");
					s = s.substring(0, p0) + "\0" + (i++) + "\1" + s.substring(pt[0]);
				}
			} else {
				// restore regex
				int p;
				while ((p = s.indexOf('\0')) >= 0) {
					int p2 = s.indexOf('\1');
					int i = Integer.parseInt(s.substring(p + 1, p2));
					s = s.substring(0, p) + regexList.get(i) + s.substring(p2 + 1);
				}
			}
			return s;
		}

	}

}
