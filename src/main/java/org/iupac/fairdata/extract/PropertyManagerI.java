package org.iupac.fairdata.extract;

/**
 * An interface that allows passing of key information between an extractor and 
 * a structure helper or vender plugin.
 * 
 * Implemented by structure helpers and vender plugins.
 * 
 * @author hansonr
 *
 */
public interface PropertyManagerI {

	/**
	 * The regular expression that this vender plugin or structure helper uses to 
	 * identify digital objects that it wants to inspect. 
	 * 
	 * @return
	 */
	String getParamRegex();

	/**
	 * Start the extraction process on a specified digital item.
	 * 
	 * @param entryName
	 * @return
	 */
	boolean doExtract(String entryName);

	/**
	 * The key method that an extractor calls uses to offer to the
	 * plugin both the name and the bytes of the zip entry that is being considered
	 * for extraction from directories or zip collections. 
	 * 
	 * This process will report back 
	 * 
	 * 
	 * @param extractor will be null if rezipping, otherwise the calling
	 *                  IFDExtractorI
	 * @param originPath   the output path, either for a ZipOutputStream or a
	 *                  FileOutputStream
	 * @param bytes     the decompressed contents of this file, for checking and
	 *                  further processing; may be null in some applications
	 * @return true if accepted (but may be ignored by the extractor)
	 */
	String accept(MetadataReceiverI extractor, String originPath, byte[] bytes);

	/**
	 * Get the IFD type key for this digital object (e.g.
	 *         IFDNMRSpecDataRepresentation.IFD_REP_DATAOBJECT_SPEC_NMR_VENDOR_DATASET)
	 * 
	 * @return the IFD type key
	 */
	String getVendorDataSetKey();

	/**
	 * 
	 * @param extractor
	 * @param key
	 * @param val
	 * @param isInline
	 * @param mediaType
	 */
	static void addPropertyOrRepresentation(MetadataReceiverI extractor, String key, Object val, boolean isInline, String mediaType) {
	}

	/**
	 * reserved for Mestrenova or other complex derived
	 * datasets that cannot be simply analyzed
	 * @return
	 */
	boolean isDerived();

}
