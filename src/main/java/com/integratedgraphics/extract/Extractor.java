package com.integratedgraphics.extract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.iupac.fairspec.api.IFSObjectAPI;
import org.iupac.fairspec.api.IFSObjectAPI.ObjectType;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSFindingAid;

import com.integratedgraphics.util.Util;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Rdr;

public class Extractor {


	public Extractor() {
		clearZipCache();
	}
	
	private String extractVersion;
	private List<String> objects;
	private IFSFindingAid findingAid;
	private String sourceDir;

	public List<String> getObjectsForFile(File ifsExtractScript) throws IOException {
		System.out.println("Extracting " + ifsExtractScript.getAbsolutePath());
		return getObjectsForStream(ifsExtractScript.toURI().toURL().openStream());
	}

	public List<String> getObjectsForStream(InputStream is) throws IOException {
		byte[] bytes = Util.getLimitedStreamBytes(is, -1, null, true);
		String script = new String(bytes);
		return objects = parseScript(script);
	}

	/**
	 * 
	 * @param script
	 * @return parsed list of objects from an IFS-extract.js JSON
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private List<String> parseScript(String script) throws IOException {
		Map<String, Object> jp = (Map<String, Object>) new JSJSONParser().parse(script, false);
		System.out.println(jp);
		extractVersion = (String) jp.get("IFS-extract-version");
		System.out.println(extractVersion);
		List<Map<String, Object>> pathway = (List<Map<String, Object>>) jp.get("pathway");
		List<String> objects = getObjects(pathway);
		System.out.println(objects.size() + " objects found");
		return objects;
	}

	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 * @return list of IFSObject definitions
	 */ 
	private List<String> getObjects(List<Map<String, Object>> pathway) {
		
		//input:
		
		//		 {"IFS-extract-version":"0.1.0-alpha","pathway":[
		//         {"hash":"0c00571"},
		//         {"pubid":"acs.orglett.{hash}"},
		//         {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
		//         {"data":"{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
		//
		//         {"path":"{data}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}"},
		//         {"objects":"{path}/{IFS.structure.representation.mol.2d::{id}.mol}"},
		//         {"objects":"{path}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"},
		//         {"objects":"{path}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"},
		//        ]}
		
		//output:
		
		// [
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"
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
						System.out.println(val+"\n"+s+"\n");
						e.setValue(s);
					}
					val = s;
				}
				if (key.equals("objects")) {
					objects.add("{IFS.finding.aid.object::" + val + "}");
				} else {
					keys.addLast("{" + key + "}");
					values.addLast(val);
				}
			}
		}
		return objects;
		
	}

	public Set<String> extractObjects(File targetDir) throws IFSException, IOException {

		
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"

// [parse first node]
		
		// {IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

// [start a new finding aid and download this resource]
		
		// |
		
// [get file list]
				
		
		//(\Q^FID for Publication/\E)(\Q{id=IFS.structure.param.compound.id::*}\E)(\Q.zip\E)
	
// [pass to StructureIterator]		
// [find matches and add structures to finding aid structure collection]
		
		// |

// [get file list]
		// [for each structure...]
		
		// \Q{id}/\E(\Q{IFS.structure.representation.mol.2d::{id}.mol}/E)"

		// add this representation to this structure 

		
		
		
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"

		// [parse first node]
				
		// {IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

		// [start a new finding aid and download this resource]
				
				// |
				
		// [get file list]
			
				//FID for Publication/{id=IFS.structure.param.compound.id::*}.zip     
		
		// [pass to StructureIterator]
		// [find matches and add structures to finding aid structure collection	if necessary	
				
				// |                 

		// [get file list]
				
				// {id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}

		// [pass to SpecDataIterator]
		// [find matches and add NMR spec data to finding aid spec data collection; also add struc+spec to finding aid StrucSpecCollection]	

		// bruker files identified with / just before vendor dataset closing }
		// standard ^....$
		// * or / at end just removes $
		// *. becomes [^.]
		// .  becomes [.]
		// /**/*.pdf  becomes /(?:[^/]+/)*).+\Q.pdf\E

//		String s = "test/ok/here/1c.pdf";    // test/**/*.pdf
//		Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");
		
		System.out.println("=====");
		
		if (sourceDir != null)
			System.out.println("extractObjects from " + sourceDir);
		System.out.println("extractObjects to " + targetDir.getAbsolutePath());
		
		int[] pt = new int[1];
		
		for (int i = 0; i < objects.size(); i++) {
			String sObj = objects.get(i);
			System.out.println("found object " + sObj);
			pt[0]= 0;
			String sAid = getValue(sObj, "IFS.finding.aid.object", pt);
			if (sAid == null)
				throw new IFSException("no IFS.finding.aid.object:" + sObj);
			pt[0] = 0;
			String sUrl = getValue(sAid, "IFS.finding.aid.source.data.uri", pt);
			if (sUrl == null)
				throw new IFSException("no IFS.finding.aid.source.data.uri:" + sAid);
			String sData = sAid.substring(pt[0]);
			sUrl = localizeURL(sUrl);	
			if (findingAid == null)
				findingAid = new IFSFindingAid(sObj);
			System.out.println("opening " + sUrl);
			Map<String, ZipEntry> map = getZipContents(sUrl);
			if (findingAid.zipContents == null)
				findingAid.zipContents = new LinkedHashMap<String, ZipEntry>();
			findingAid.zipContents.putAll(map);
			processObject(sData, map.keySet());
		}
		return findingAid.zipContents.keySet();
	}

	private void processObject(String sData, Set<String> set) {
//		ParseObject po = new ParseObject(sData);
//		for (String fname: set) {
//			po.check(fname);
//				
//			}
//		}
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 */
	private String localizeURL(String sUrl) {
		if (sourceDir != null) {
		  int pt = sUrl.lastIndexOf("/");
		  sUrl = sourceDir + sUrl.substring(pt);
		}
		return sUrl;
	}

	private String getValue(String sObj, String key, int[] pt) throws IFSException {
		key = "{" + key + "::";
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

	/**
	 * parse the object string for a known type
	 * 
	 * @param sObj
	 * @param pt
	 * @return ObjectType
	 */
	private ObjectType parseType(String sObj, int[] pt) {
		int p = sObj.indexOf("{", pt[0]);
		if (p != pt[0])
			return null;
		ObjectType type = null;
		int p0 = p + 1;
		for (int i = p0, len = sObj.length(); i < len; i++) {
			switch (sObj.charAt(i)) {
				case '{':
				return null;
				case '=':
					p0 = i + 1;
					break;
				case ':':
				    return IFSObjectAPI.getObjectTypeForName(sObj.substring(p0, i));
			}
		}
		return null;
	}
	
	
	
	private static Map<String, Map<String, ZipEntry>> htZipContents = new HashMap<>();
	
	static boolean debugging;

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
//	private static ZipEntry findZipEntry(String zipFile, String fileName, boolean isContains) {
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

	/**
	 * Gets the contents of a zip file.
	 * 
	 * @param zipPath the path to the zip file
	 * @return a set of file names in alphabetical order
	 * @throws IOException 
	 */
	public static Map<String, ZipEntry> getZipContents(String zipPath) throws IOException {
		URL url = new URL(zipPath);// getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them
									// already
		Map<String, ZipEntry> fileNames = htZipContents.get(url.toString());
		if (fileNames != null)
			return fileNames;
		// Scan URL zip stream for files.
		fileNames = new LinkedHashMap<String, ZipEntry>();
		htZipContents.put(url.toString(), fileNames);
		return readZipContentsIteratively(url.openStream(), fileNames, "");
	}

	private static Map<String, ZipEntry> readZipContentsIteratively(InputStream is, Map<String, ZipEntry> fileNames, String baseName) throws IOException {
		if (baseName.length() > 0)
			System.out.println("opening " + baseName);
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		int n = 0;
		while ((zipEntry = zis.getNextEntry()) != null) {
			if (!zipEntry.isDirectory() && zipEntry.getSize() == 0)
				continue;
			n++;
			String fname = baseName + zipEntry.getName();
			if (debugging)
				System.out.println(fname);
			fileNames.put(fname, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			if (fname.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, fname + "|");
			}
		}
		if (baseName.length() == 0)
			zis.close();
		return fileNames;
	}

	public void setSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}


	
	class ParseObject {

		private String sData;
		
		private Pattern p;
		private Matcher m;

		public ParseObject(String sData) {
			this.sData = sData;
		}
		
	}
	

}
