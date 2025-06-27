package com.integratedgraphics.ifd.vendor;

import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.api.VendorPluginI;

/**
 * An abstract class that underlies all the vendor plugins.
 * 
 * @author hansonr
 *
 */
public abstract class DefaultVendorPlugin implements VendorPluginI {

	/*
	 * note that as coded, ONLY ONE vendor can use this. 
	 */
	private static final String rezipPathHeader = "^(?<path#>.+(?:/|\\|)(?<dir#>[^/]+)(?:/|\\|))";

	protected static void register(Class<? extends DefaultVendorPlugin> c) {
		VendorPluginI.registerIFDVendorPlugin(c);
	}

    abstract protected void reportVendor();

	/**
	 * the extractor calling this plugin, set in startRezipping() or accept()
	 */
	protected MetadataReceiverI extractor;

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
	protected boolean processing;

	/**
	 * whether this plugin is currently enabled
	 */
	protected boolean enabled;

	/**
	 * the index of this vendor in the activeVendors list
	 */
	private int index;

	protected DefaultVendorPlugin() {
		paramRegex = "IFD_METADATA$";
	}
	
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

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		if (extractor != null) {
			this.extractor = extractor;
		}
		return processRepresentation(null, null);
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
	 * endDataset(). Also register the extractor.
	 */
	@Override
	public void initializeDataSet(MetadataReceiverI extractor) {
		this.extractor = extractor;
		reportVendor();
		processing = true;
	}

	protected static String getProp(String name) {
		return IFDConst.getProp(name);
	}

	/**
	 * Rezipping is complete; finalize any properties that required more than a
	 * single files's information.
	 */
	@Override
	public void endDataSet() {
		processing = false;
		this.extractor = null;
	}

	/**
	 * When rezipping, include this zip entry or not.
	 */
	@Override
	public boolean doRezipInclude(MetadataReceiverI extractor, String zipfileName, String entryName) {
		return true;
	}

	/**
	 * Pass back the standardized key/val pair to the IFDMetadataReceiverI class.
	 * 
	 * @param key
	 * @param val a String or Double; IFD.Property.NULL to remove value
	 */
	public void addProperty(String key, Object val) {
		if (val == null || extractor == null)
			return;
		if (val instanceof Double) {
			if (Double.isNaN((Double)val))
				return;
		} else if (val instanceof String && val != IFDProperty.NULL) {
			val = ((String) val).trim();
		}
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

	/**
	 * check a file by name if it is something that 
	 * the Extractor should copy out of a directory
	 * before it is zipped up.
	 * 
	 * @return Object[] { ifdtype, (add bytes inline ? Boolean.TRUE : add bytes inline only ? Boolean.FALSE : null (just copy the file) }
	 */
	@Override
	public Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName) {
		return null;
	}

	@Override
	public boolean isDerived() {		return false;
	}

}
