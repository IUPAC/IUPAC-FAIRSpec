package com.integratedgraphics.extractor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.util.JSJSONParser;

import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorUtils.ObjectParser;

/**
 * Extraction Phase 1.
 * 
 * Set the local source directory.
 * 
 * Read the IFD-extract.json file to:
 * 
 * read additional metadata
 * 
 * set up "object parsers" that read full file names for information from a
 * directory or a compressed file (ZIP, RAR, TAR, TGZ)
 * 
 * process the publication metadata (CrossRef and DataCite)
 * 
 * set what files will be extracted from data sets to stand alone (MOL, CDX,
 * CIF, etc.)
 * 
 * get "rezip" file cues from venders. For example, for Bruker NMR, the regex
 * expression "pdata/[^/]+/procs$" would indicate the presence of a procs file one directory
 * below a directory called "pdata". This will trigger a rezip request in Phase 2.
 * 
 * @author hanso
 *
 */
abstract class IFDExtractorLayer1 extends IFDExtractorLayer0 {

	/**
	 * extract version from IFD-extract.json
	 */
	private String extractVersion;

	String findingaidId;
	
	/**
	 * working memory cache of representations keyed to their localized name
	 * (possibly with an extension for a page within the representation, such as an
	 * MNova file. These are identified by vendors and that can create additional
	 * properties or representations from them in Phase 2b that will need to be
	 * processed in Phase 2c.
	 */
	protected Map<String, ExtractorUtils.CacheRepresentation> vendorCache;

	protected boolean processPhase1(File ifdExtractScriptFile, String localArchive) throws IOException, IFDException {
		// first create objects, a List<String>
		phase1SetLocalSourceDir(localArchive);
		extractScriptFile = ifdExtractScriptFile;
		extractScriptFileDir = extractScriptFile.getParent();
		// Scan data from IFD-extract.json and set up the parsers
		log("!Extracting " + ifdExtractScriptFile.getAbsolutePath());
		extractScript = FAIRSpecUtilities.getFileStringData(ifdExtractScriptFile);
		objectParsers = phase1ParseScript(extractScript);
		if (!processPubURI())
			return false;
		initializePropertyExtraction();
		vendorCache = new LinkedHashMap<String, ExtractorUtils.CacheRepresentation>();
		return true;
	}

	/**
	 * Make all variable substitutions in IFD-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object}
	 *         lines of the file
	 * @throws IFDException
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	@SuppressWarnings("unchecked")
	private List<ObjectParser> phase1GetObjectParsers(List<Object> jsonArray) throws IFDException, MalformedURLException, IOException {

		// input:

		// {"FAIRSpec.extract.version":"0.2.0-alpha","keys":[
		// {"example":"compound directories containing unidentified bruker files and
		// hrms zip file containing .pdf"},
		// {"journal":"acs.orglett"},{"hash":"0c00571"},
		// {"figshareid":"21975525"},
		//
		// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
		// {"IFD.property.collectionset.source.publication.uri":"https://doi.org/10.1021/{IFDid}"},
		// {"IFD.property.collectionset.source.data.license.uri":"https://creativecommons.org/licenses/by-nc/4.0"},
		// {"IFD.property.collectionset.source.data.license.name":"cc-by-nc-4.0"},
		//
		// {"data0":"{IFD.property.collectionset.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{IFDid}/suppl_file/ol{hash}_si_002.zip}"},
		// {"data":"{IFD.property.collectionset.source.data.uri::https://ndownloader.figshare.com/files/{figshareid}}"},
		//
		// {"path":"{data}|FID for Publication/{id=IFD.property.sample.label::*}.zip|"},
		// {"FAIRSpec.extractor.object":"{path}{IFD.representation.dataobject.fairspec.nmr.vendor.dataset::{IFD.property.label::<id>/{xpt=::*}}.zip|{xpt}/*/}"},
		// {"FAIRSpec.extractor.object":"{path}<id>/{IFD.representation.structure.mol.2d::<id>.mol}"},
		// {"FAIRSpec.extractor.object":"{path}{IFD.representation.dataobject.fairspec.hrms.document::{IFD.property.label::<id>/HRMS.zip|**/*}.pdf}"}
		// ]}

		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		List<ObjectParser> parsers = new ArrayList<>();
		List<Object> ignored = new ArrayList<>();
		List<Object> rejected = new ArrayList<>();
		List<Object> accepted = new ArrayList<>();
		ExtractorResource source = null;
		boolean isDefaultStructurePath = false;
		List<Object> replacements = null;

		for (int i = 0; i < jsonArray.size(); i++) {
			Object o = jsonArray.get(i);

			// all aspects here are case-sensitive

			// simple strings are ignored, except for "EXIT"

			if (o instanceof String) {
				if (o.equals(FAIRSpecExtractorHelper.EXIT))
					break;
				// ignore all other strings
				continue;
			}

			Map<String, Object> directives = (Map<String, Object>) o;

			for (Entry<String, Object> e : directives.entrySet()) {

				// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
				// ..-----------------key---------------...------val-------.

				String key = e.getKey();
				if (key.startsWith("#"))
					continue;
				o = e.getValue();

				// non-String values

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_METADATA)) {
					if (o instanceof Map) {
						phase1ProcessMetadataElement(o);
					} else if (o instanceof List) {
						for (Object m : (List<Object>) o) {
							phase1ProcessMetadataElement(m);
						}
					} else {
						logWarn("extractor template METADATA element is not a map or array",
								"Extractor.getObjectParsers");
					}
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_REPLACEMENTS)) {
					replacements = (List<Object>) o;
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_ACCEPT)) {
					if (o instanceof String) {
						accepted.add(o);
					} else {
						accepted.addAll((List<Object>) o);
					}
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_REJECT)) {
					if (o instanceof String) {
						rejected.add(o);
					} else {
						rejected.addAll((List<Object>) o);
					}
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_IGNORE)) {
					if (o instanceof String) {
						ignored.add(o);
					} else {
						ignored.addAll((List<Object>) o);
					}
					continue;
				}
				// String values

				String val = o.toString();
				if (val.indexOf("{") >= 0) {
					String s = FAIRSpecUtilities.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						if (debugging)
							log(val + "\n" + s + "\n");
						e.setValue(s);
					}
					val = s;
				}
				log("!" + key + " = " + val);
				String keyDef = null;
				int pt = key.indexOf("=");
				if (pt > 0) {
					keyDef = key.substring(0, pt);
					key = key.substring(pt + 1);
				}
				// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
				// ..keydef=-----------------key--------

				if (key.equals(FAIRSPEC_EXTRACTOR_REFERENCES)) {
					val = FAIRSpecUtilities.getFileStringData(new File(toAbsolutePath(val)));
					log("!processing " + val);
					Map<String, Map<String, Object>> htCompoundFileReferences = new HashMap<>();
					List<Object> jsonMap = (List<Object>) new JSJSONParser().parse(val, false);
					for (int j = jsonMap.size(); --j >= 0;) {
						Map<String, Object> jm = (Map<String, Object>) jsonMap.get(j);
						String file = (String) jm.get("file");
						String cmpd = (String) jm.get("cmpd");
						htCompoundFileReferences.put(cmpd + (file == null ? "" : "|" + file), jm);
					}
					htURLReferences = htCompoundFileReferences;
				}
				

				 if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_LOCAL_SOURCE_FILE)) {
					localSourceFile = (val.length() == 0 ? null : val);
					if (localSourceDir != null)
						localSourceFile = localSourceDir + "/" + localSourceFile;
					continue;
				}
				 
				if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI)) {
					// allow for a local version (debugging mostly)
					boolean isRemote = val.startsWith("http");
					boolean isRelative = val.startsWith("./"); // as in "./structures"
					if (isRelative)
						localSourceFile = null;
					String local = localizeURL(val);
					boolean isFoundLocal = (local.startsWith("file:/") && new File(local.substring(6)).exists()); 
					if (isFoundLocal) {
						localSourceFile = localizeURL(val);
					} else {
						source = null;
						isDefaultStructurePath = isDefaultStructurePath(val);
						String msg = "local source directory does not exist (ignored): " + val;
						if (isDefaultStructurePath)
							logNote(msg, "phase1CheckSource");
						else
							logWarn(msg, "phase1CheckSource");
						if (isDefaultStructurePath)
							continue;
					}
					source = htResources.get(val);
					if (source == null) {
						source = new ExtractorResource(htResources.size() + 1, localSourceFile == null || isRelative ? val : null);
						htResources.put(val, source);
					}
					source.setLocalSourceFileName(localSourceFile == null ? null : localizeURL(null));
					if (!isRemote)
						continue;
					// go ahead and add this data source to the metadata
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OBJECT)) {
					if (source == null) {
						if (isDefaultStructurePath)
							continue;
						throw new IFDException(
								IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI + " was not set before " + val);
					}
					ObjectParser parser = newObjectParser(source, val);
					parser.setReplacements(replacements);
					parsers.add(parser);
					continue;
				}
				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_RELATED_METADATA)) {
					ifdMetadataFileName = val;
				}
				if (key.startsWith(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OPTION_FLAG)
						|| key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OPTIONS)) {
					setExtractorOption(key, val);
					continue;
				}
				if (key.startsWith(IFDConst.IFD_PROPERTY_FLAG)) {
					if (key.equals(IFDConst.IFD_PROPERTY_FINDINGAID_ID) 
							|| key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_ID)) {
						faHelper.getFindingAid().setID(ifdid = val);
					}
					if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_BYID)) {
						setExtractorOption(key, val);
						continue;
					}
					faHelper.getFindingAid().setPropertyValue(key, val);
					if (keyDef == null)
						continue;
				}

				// custom definition
				keys.add(0, "{" + (keyDef == null ? key : keyDef) + "}");
				values.add(0, val);
			}
		}

		// ensure the faHelper has a non-null id
		faHelper.getFindingAid().setID(ifdid);
		String s = "";
		if (rejected.size() > 0) {
			for (int i = 0; i < rejected.size(); i++) {
				s += "(" + rejected.get(i) + ")|";
			}
		}
		lstRejected.setAcceptPattern(FAIRSpecUtilities.rep(s, ".", "\\.") + FAIRSpecExtractorHelper.junkFilePattern);
		if (accepted.size() > 0) {
			s = "";
			for (int i = 0; i < accepted.size(); i++) {
				s += "|(" + accepted.get(i) + ")";
			}
			acceptRegex = FAIRSpecUtilities.rep(s.substring(1), ".", "\\.");
			lstAccepted.setAcceptPattern(acceptRegex);
		} else {
			acceptRegex = null;
		}
		if (ignored.size() > 0) {
			s = "";
			for (int i = 0; i < ignored.size(); i++) {
				s += "|(" + ignored.get(i) + ")";
			}
			ignoreRegex = FAIRSpecUtilities.rep(s.substring(1), ".", "\\.");
		} else {
			ignoreRegex = null;
		}
		return parsers;
	}
	
	/**
	 * Parse the script form an IFD-extract.js JSON file starting with the creation
	 * of a Map by JSJSONParser.
	 * 
	 * @param script
	 * @return parsed list of objects from an IFD-extract.js JSON
	 * @throws IOException
	 * @throws IFDException
	 */
	@SuppressWarnings("unchecked")
	private List<ObjectParser> phase1ParseScript(String script) throws IOException, IFDException {
		if (faHelper != null)
			throw new IFDException("Only one finding aid per instance of Extractor is allowed (for now).");

		faHelper = helper = newExtractionHelper();

		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACT_VERSION);
		if (logging())
			log(extractVersion);
		List<Object> scripts = (List<Object>) jsonMap.get("keys");
		if (config != null) {
			List<Object> defaultScripts = (List<Object>) config.get("defaultScripts");
			if (defaultScripts != null) {
				scripts.addAll(defaultScripts);
			}
		}
		List<ObjectParser> objectParsers = phase1GetObjectParsers(scripts);
		if (logging())
			log(objectParsers.size() + " extractor regex strings");

		log("!license: "
				+ faHelper.getFindingAid().getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME)
				+ " at "
				+ faHelper.getFindingAid().getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI));

		return objectParsers;
	}

	@SuppressWarnings("unchecked")
	private void phase1ProcessMetadataElement(Object m) throws IFDException {
//		 {"FAIRSpec.extractor.metadata":[
//         	{"FOR":"IFD.property.fairspec.compound.id",
//         		"METADATA_FILE":"./Manifest.xlsx",
//         		"METADATA_KEY":"TM compound number"
//         	}
//          ]},
		Map<String, Object> map = (Map<String, Object>) m;
		String key = (String) map.get("FOR");
		if (key == null) {
			throw new IFDException("extractor template METADATA element does not contain 'FOR' key in " + m);
		}
		if (htSpreadsheetMetadata == null)
			htSpreadsheetMetadata = new HashMap<String, Map<String, Object>>();
		htSpreadsheetMetadata.put(key, map);
		if (key.startsWith("IFD.")) {
			String fileName = (String) map.get(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_METADATA_FILE);
			if (fileName == null) {
				logWarn("METADATA extraction map did not contain the key " + FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_METADATA_FILE, "phase1ProcessMetadataElement");
			} else {
				fileName = toAbsolutePath(fileName);
				String err = FAIRSpecUtilities.loadFileMetadata(key, map, fileName);
				if (err != null)
					logWarn(err + " loading " + fileName, "loadFileMetadata");
			}
		}
	}

	private void phase1SetLocalSourceDir(String sourceDir) {
		if ("-".equals(sourceDir))
			sourceDir = null;
		if (sourceDir != null && sourceDir.indexOf("://") < 0)
			sourceDir = "file:///" + sourceDir.replace('\\', '/');
		localSourceDir = sourceDir;
	}

	/**
	 * Get a new ObjectParser for this data. Note that this method may be overridden
	 * if desired.
	 * 
	 * @param source
	 * @param sObj
	 * @return
	 * @throws IFDException
	 */
	private ObjectParser newObjectParser(ExtractorResource source, String sObj) throws IFDException {
		return new ObjectParser((IFDExtractor) this, source, sObj);
	}

	protected String toAbsolutePath(String fname) {
		if (fname.startsWith("./"))
			fname = extractScriptFileDir.replace('\\', '/') + fname.substring(1);
		return fname;
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 * @throws IFDException
	 */
	protected String localizeURL(String sUrl) throws IFDException {
		if (sUrl == null) {
//			if (new File(localSourceFile).isAbsolute())
			sUrl = localSourceFile;
//			else
//				sUrl = localSourceDir + "/" + localSourceFile;
		} else {
			boolean isRelative = sUrl != null && (sUrl.startsWith("./") || sUrl.indexOf("/./") >= 0);
			if (!isRelative && localSourceDir != null) {
				if (FAIRSpecUtilities.isZip(localSourceDir)) {
					sUrl = localSourceDir;
				} else if (localSourceDir.endsWith("/*")) {
					sUrl = localSourceDir.substring(0, localSourceDir.length() - 1);
				} else {
					int pt = sUrl.lastIndexOf("/");
					if (pt >= 0) {
						sUrl = localSourceDir + sUrl.substring(pt);
						if (!FAIRSpecUtilities.isZip(sUrl) && !sUrl.endsWith("/"))
							sUrl += ".zip";
					}
				}

			}
		}
		sUrl = toAbsolutePath(sUrl);

		if (sUrl.indexOf("//") < 0 && !sUrl.startsWith("file:/"))
			sUrl = "file:/" + sUrl;
		return sUrl;
	}


	private static boolean isDefaultStructurePath(String val) {
		return (DEFAULT_STRUCTURE_DIR_URI.equals(val) || DEFAULT_STRUCTURE_ZIP_URI.equals(val));
	}
	

}
