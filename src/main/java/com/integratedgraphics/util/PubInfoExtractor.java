package com.integratedgraphics.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javajs.util.JSJSONParser;

public class PubInfoExtractor {

	public final static String crossciteURI = "https://data.crosscite.org/application/vnd.datacite.datacite+json/";
	
	public final static String crossrefURI = "https://api.crossref.org/works/";

	public static String getCrossrefUrl(String puburi) {
		if (puburi != null && puburi.startsWith("https://doi.org/")) {
			return crossrefURI + puburi.substring(16);
		}
		return null;
	}
	
	public static String getCrossciteUrl(String puburi) {
		if (puburi != null && puburi.startsWith("https://doi.org/")) {
			return crossciteURI + puburi.substring(16);
		}
		return null;
	}
	
	/**
	 * Retrieve the crossref metadata for the published work and process it to
	 * return a Map summarizing its contents. This map contains a "metadata" item
	 * that provides the original XML if desired.
	 * 
	 * @param puburi
	 * @return null if there is an problem getting this URL
	 * 
	 */
	public static Map<String, Object> getPubInfo(String puburi) throws IOException {
		if (puburi == null)
			return null;
		Map<String, Object> info =  new LinkedHashMap<>(); 
		List<Object> list = new ArrayList<>();
		Map<String, Object> map;

		String url = getCrossciteUrl(puburi);
		System.out.println("CrossRefExtractor: " + url);
		map =  new LinkedHashMap<>();
		Map<String, Object> crossCite = new JSJSONParser().parseMap(Util.getURLContentsAsString(url), false);
		map.put("type", "crosscite");
		map.put("url", url);
		map.put("info", crossCite);
		list.add(map);
		
		url = getCrossrefUrl(puburi);
		System.out.println("CrossRefExtractor: " + url);
		map =  new LinkedHashMap<>();
		map.put("type", "crossref");
		map.put("url", url);
//		System.out.println("CrossRefExtractor: " + url);
//		Map<String, Object> crossRef = new JSJSONParser().parseMap(Util.getURLContentsAsString(url), false);
//		map.put("info", crossRef);		
		list.add(map);
		extractCrossCiteInfo(info, crossCite);
		put(info,"sources", list);
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
	public static void extractCrossRefPubInfo(String url, String pubjson, Map<String, Object> info) {
		System.out.println(pubjson);
		info.clear();
		Map<String, Object> json = new JSJSONParser().parseMap(pubjson, false);
		Map<String, Object> message = getMap(json, "message");
		String title = (String) getValue((Map<String, Object>) getList(message, "titles").get(0), "title", null);
		put(info,"title", title);
		String doi = (String) getObject(message, "published-print", "DOI");
		put(info,"doi", doi);
		put(info,"url", ((Map<String, Object>) getList(message, "link").get(0)).get("URL"));
		List<Object> author = getList(message, "author");
		String s = "";
		if (author != null)
		for (int i = 0; i < author.size(); i++) {
			Map<String, Object> au = (Map<String, Object>) author.get(i);
			String name = getValue(au, "given", "") + " " + getValue(au, "family", "");
			s += ", " + name;
		}
		if (s.length() > 0) {
			put(info,"authors", s.substring(2));
		}
	}

	/**
	 * @param info 
	 * @param crossCite 
	 */
	@SuppressWarnings("unchecked")
	public static void extractCrossCiteInfo(Map<String, Object> info, Map<String, Object> crossCite) {
		put(info,"title",  ((Map<String, Object>)getList(crossCite, "titles").get(0)).get("title"));
		String s = "";		
		List<Object> creators = getList(crossCite, "creators");
		for (int i = 0; i < creators.size(); i++) {
			Map<String, Object> au = (Map<String, Object>) creators.get(i); 
			String name = getValue(au, "givenName", "") + " " + getValue(au, "familyName", "");
			s += ", " + name.trim();
		}
		if (s.length() > 0) {
			put(info,"authors", s.substring(2));
		}
		put(info,"doi", getValue(crossCite, "doi", ""));			
		put(info,"url", getValue(crossCite, "url", ""));
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