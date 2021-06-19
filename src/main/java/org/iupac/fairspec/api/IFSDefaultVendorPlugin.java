package org.iupac.fairspec.api;

import java.util.Map;

import org.iupac.fairspec.common.IFSConst;

/**
 * An abstract class that underlies all the vendor plugins.
 * 
 * @author hansonr
 *
 */
public abstract class IFSDefaultVendorPlugin implements IFSVendorPluginI {

	/*
	 * note that as coded, ONLY ONE vendor can use this. 
	 */
	private static final String rezipPathHeader = "^(?<path#>.+(?:/|\\|)(?<dir#>[^/]+)(?:/|\\|))";

	protected static void register(Class<? extends IFSDefaultVendorPlugin> c) {
		IFSVendorPluginI.registerIFSVendorPlugin(c);
	}

	/**
	 * the extractor calling this plugin, set in startRezipping() or accept()
	 */
	protected IFSExtractorI extractor;

	/**
	 * the regex expression for what entry names are of interest; for example,
	 * "\\.jdf$" meaning "ending in .jdf"
	 */
	protected String paramRegex;

	/**
	 * the regex expression that will trigger rezipping. For example,
	 * "pdata/[^/]+/procs$" would indicate "a procs file one directory below a
	 * directory called pdata."
	 * 
	 */
	protected String rezipRegex;

	/**
	 * whether or not this vendor plugin is involved in rezipping currently
	 */
	protected boolean rezipping;

	/**
	 * whether this plugin is currently enabled
	 */
	protected boolean enabled;

	/**
	 * the index of this vendor in the activeVendors list
	 */
	private int index;

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * Generally true, but can be set to false to disable
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getParamRegex() {
		return paramRegex;
	}

	/**
	 * adds the
	 */
	@Override
	public String getRezipRegex() {
		return (rezipRegex == null ? null : rezipPathHeader.replace("#", "" + index) + rezipRegex);
	}

	/**
	 * Allow for a modification of the directory path to the resource in the
	 * rezipped file. For example, all Bruker datasets must start with a fully
	 * numerical directory name.
	 */
	@Override
	public String getRezipPrefix(String dirName) {
		return null;
	}

	/**
	 * The key method that the extractor uses to offer to the plugin both the name
	 * and the bytes of the zip entry that is being considered for extraction.
	 * 
	 * @param extractor will be null if rezipping, otherwise the calling
	 *                  IFSExtractorI
	 * @param entryName the zip entry name for this file
	 * @param bytes     the decompressed contents of this file
	 * @return true if accepted (but may be ignored by the extractor)
	 */
	@Override
	public boolean accept(IFSExtractorI extractor, String entryName, byte[] bytes) {
		if (extractor != null) {
			this.extractor = extractor;
		}
		return false;
	}

	/**
	 * Indicate whether this entry is one that should be extracted to its own file.
	 * Generally this will be true, but when rezipping, it may be that this is just
	 * a parameter file that can be left in the zipped up dataset rather than ever
	 * delivered by itself (acqus, procs, for example).
	 */
	@Override
	public boolean doExtract(String entryName) {
		return true;
	}

	/**
	 * Rezipping has started; initialize all fields that will be utilized in
	 * endRezip(). Also register the extractor.
	 */
	@Override
	public void startRezip(IFSExtractorI extractor) {
		this.extractor = extractor;
		reportName();
		rezipping = true;
	}

	protected void reportName() {
		addProperty(IFSConst.IFS_SPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}

	/**
	 * Rezipping is complete; finalize any properties that required more than a
	 * single files's information.
	 */
	@Override
	public void endRezip() {
		rezipping = false;
		this.extractor = null;
	}

	/**
	 * When rezipping, include this zip entry or not.
	 */
	@Override
	public boolean doRezipInclude(String entryName) {
		return true;
	}

	/**
	 * Pass back the standardized key/val pair to the IFSExtractorI class.
	 * 
	 * @param key
	 * @param val
	 */
	public void addProperty(String key, Object val) {
		if (val == null)
			return;
		System.out.println(key + " = " + val);
		if (extractor != null)
			extractor.addProperty(key, val);
	}

	/**
	 * Only allow numerical top directories.
	 * 
	 * @param s
	 * @return
	 */
	protected static boolean isUnsignedInteger(String s) {
		// I just don't like to fire exceptions.
		for (int i = s.length(); --i >= 0;)
			if (!Character.isDigit(s.charAt(i)))
				return false;
		return true;
	}

	/**
	 * Get a string value, but strip its first and last characters, return null if
	 * the key is missing or the field is "empty string", for example "" or
	 * &lt;&gt;.
	 * 
	 * @param map
	 * @param key
	 * @param c0
	 * @param c1
	 * @return undelimited value
	 */
	public static String getDelimitedString(Map<String, String> map, String key, char c0, char c1) {
		String val = map.get(key);
		int n;
		return (val == null 
				|| (n = val.length()) < 3 
				|| val.charAt(0) != c0 || val.charAt(n - 1) != c1 ? null
				: val.substring(1, val.length() - 1).trim());
	}

	/**
	 * Get a double value for a number represented as a String
	 * 
	 * @param map
	 * @param key
	 * @return
	 */
	protected static double getDoubleValue(Map<String, String> map, String key) {
		String f = map.get(key);
		try {
			return (f == null ? Double.NaN : Double.valueOf(f).doubleValue());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	
}
