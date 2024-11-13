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
	 * 
	 * 
	 * @param extractor will be null if rezipping, otherwise the calling
	 *                  IFDExtractorI
	 * @param originPath   the output path, either for a ZipOutputStream or a
	 *                  FileOutputStream
	 * @param bytes     the decompressed contents of this file, for checking and
	 *                  further processing; may be null in some applications
	 * @param isEmbedded TODO
	 * @return true if accepted (but may be ignored by the extractor)
	 */
	String accept(MetadataReceiverI extractor, String originPath, byte[] bytes, boolean isEmbedded);

	/**
	 * Process a representation (zip file or directory, mol file, etc.), possibly
	 * sending new properties to the Extractor, such as InChI or SMILES for a MOL
	 * file.
	 * 
	 * @param ifdPath the path to this representation; may be null (for example, in
	 *                the case of a zip file), but may be used for distinguishing
	 *                representation types such as mol.2d or sdf.
	 * 
	 * @param bytes   bytes for this representation; may be null for zip files
	 * @return the IFD type key for this digital object (e.g.
	 *         IFDNMRSpecDataRepresentation.IFD_REP_DATAOBJECT_SPEC_NMR_VENDOR_DATASET)
	 */
	String processRepresentation(String ifdPath, byte[] bytes);

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

}
