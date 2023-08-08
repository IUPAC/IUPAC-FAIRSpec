package com.integratedgraphics.ifd.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

import org.iupac.fairdata.util.JSJSONParser;

public class PubInfoExtractor {

	public final static String CROSSREF = "Crossref";
	public final static String DATACITE = "DataCite";
	public final static String crossciteURL = "https://data.crosscite.org/application/vnd.datacite.datacite+json/";
		//"https://data.crosscite.org/application/vnd.datacite.datacite+json/";
	
	public final static String crossrefURL = "https://api.crossref.org/works/";
	

	/**
	 * Return Crossref metadata URL
	 * @param puburi
	 * @return
	 */
	public static String getCrossrefMetadataUrl(String puburi) {
		if (puburi != null && puburi.startsWith("https://doi.org/")) {
			return crossrefURL + puburi.substring(16);
		}
		return null;
	}
	
	public static String getCrossciteMetadataUrl(String puburi) {
		if (puburi != null && puburi.startsWith("https://doi.org/")) {
			return crossciteURL + puburi.substring(16);
		}
		return null;
	}
	
	/**
	 * Retrieve the crossRef metadata for the published work and process it to
	 * return a Map summarizing its contents. This map contains a "metadata" item
	 * that provides the original XML if desired. 
	 * 
	 * @param uri
	 * @param addPublicationMetadata adds full Crossref or DataCite metadata to finding aid
	 * @param agency "crossref" or "datacite"
	 * @return null if there is an problem getting this URL
	 * 
	 */
	public static Map<String, Object> getPubInfo(String uri, boolean addPublicationMetadata, String agency) throws IOException {
		if (uri == null)
			return null;
		String url = null;
		switch (agency) {
		case CROSSREF:
			url = getCrossrefMetadataUrl(uri);
			break;
		case DATACITE:
			url = getCrossciteMetadataUrl(uri);
			break;
		default:			
			System.out.println("PubInfoExtractor: unknown type " + agency + " should be one of 'datacite' or 'crossref'");
			return null;
		}
		System.out.println("PubInfoExtractor: " + url);
		Map<String, Object> metadata = new JSJSONParser().parseMap(FAIRSpecUtilities.getURLContentsAsString(url), false);
		System.out.println("PubInfoExtractor: " + (metadata == null ? "no metadata" : "extracted " + metadata.entrySet().size() + " metadata items"));
		Map<String, Object> info = new LinkedHashMap<>();
		try {
			if (metadata != null) {
				switch (agency) {
				case CROSSREF:
					extractCrossRefInfo(info, metadata);
					break;
				case DATACITE:
					extractCrossCiteInfo(info, metadata);
					break;
				}
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("registrationAgency", agency);
				map.put("metadataUrl", url);
				if (addPublicationMetadata)
					map.put("metadata", metadata);
				put(info, "metadataSource", map);
			}
		} catch (Throwable t) {
			System.err.println(t);
			info = null;
		}
		return info;
	}

	/**
	 * Considered doing this but could not figure out the journal citation
	 * 
	 * @param url
	 * @param pubjson
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static void extractCrossRefInfo(Map<String, Object> info, Map<String, Object> json) {
		info.clear();
		Map<String, Object> message = getMap(json, "message");
		String title = (String) getValue(message, "title", null);
		if (title != null && title.startsWith("[") && title.endsWith("]")) {
			title = title.substring(1, title.length() - 1);
		}
		put(info,"title", title);
		String doi = (String) getObject(message, "DOI");
		List<Object> author = getList(message, "author");
		String s = "";
		if (author != null)
		for (int i = 0; i < author.size(); i++) {
			Map<String, Object> au = (Map<String, Object>) author.get(i);
			String name = getValue(au, "given", "") + " " + getValue(au, "family", "");
			s += ", " + name;
			String orcid = getValue(au, "ORCID", null);
			if (orcid != null)
				s += " (" + orcid.replace("http:", "https:") + ")";
		}
		if (s.length() > 0) {
			put(info,"authors", s.substring(2));
		}
		put(info,"doi", doi);
		put(info,"doiLink", "https://doi.org/" + doi);
		put(info,"url", ((Map<String, Object>) getList(message, "link").get(0)).get("URL"));
	}

	/**
	 * @param info 
	 * @param crossCite 
	 */
	@SuppressWarnings("unchecked")
	public static void extractCrossCiteInfo(Map<String, Object> info, Map<String, Object> crossCite) {
		put(info,"dataTitle",  ((Map<String, Object>)getList(crossCite, "titles").get(0)).get("title"));
		String s = "";		
		List<Object> creators = getList(crossCite, "creators");
		for (int i = 0; i < creators.size(); i++) {
			Map<String, Object> au = (Map<String, Object>) creators.get(i); 
			String name = getValue(au, "givenName", "") + " " + getValue(au, "familyName", "");
			s += ", " + name.trim();
		}
		if (s.length() > 0) {
			put(info,"dataCreators", s.substring(2));
		}
		String doi = getValue(crossCite, "doi", "");
		put(info,"dataDoi", doi);			
		put(info,"dataDoiLink", "https://doi.org/" + doi);
		put(info,"dataUrl", getValue(crossCite, "url", ""));
	}


	private static void put(Map<String, Object> info, String key, Object value) {
		if (value != null)
			info.put(key, value);
	}

	private static String getValue(Map<String, Object> au, String key, String defValue) {
		Object o = au.get(key);
		return (o == null ? defValue : o.toString());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getMap(Map<String, Object> json, String... keys) {
		return (Map<String, Object>) getObject(json, keys);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> getList(Map<String, Object> json, String... keys) {
		return (List<Object>) getObject(json, keys);
	}

	@SuppressWarnings("unchecked")
	private static Object getObject(Map<String, Object> json, String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			json = (Map<String, Object>) json.get(keys[i]);
		}
		return json.get(keys[keys.length - 1]);
	}

	public static String extractOuterXML(String key, String xml) {
		if (xml == null)
			return null;
		int p = xml.indexOf("<" + key);
		int p2 = key.indexOf(" ");
		p2 = xml.indexOf("</" + (p2 < 0 ? key : key.substring(0, p2)) 
				+ (key.endsWith(">") ? "" : ">"), p);
		if (p2 < 0)
			return null;
		p2 = xml.indexOf(">", p2) + 1;
		return xml.substring(p, p2);
	}

	/**
	 * Super-simple XML parser. 
	 * 
	 * @param key
	 * @param xml
	 * @param pt
	 * @return
	 */
	public static String extractXML(String key, String xml, int[] pt) {
		if (xml == null)
			return null;
		if (pt == null)
			pt = new int[1];
		int p = xml.indexOf("<" + key, pt[0]);
		if (p < 0 || (p = xml.indexOf(">", p) + 1) <= 0)
			return null;
		int p2 = key.indexOf(" ");
		p2 = xml.indexOf("</" + (p2 < 0 ? key : key.substring(0, p2)) 
				+ (key.endsWith(">") ? "" : ">"), p);
		if (p2 < 0)
			return null;
		pt[0] = p2 + key.length() + 2;
		return xml.substring(p, p2);
	}

}