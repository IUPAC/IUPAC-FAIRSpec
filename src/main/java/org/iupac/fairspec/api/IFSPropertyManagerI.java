package org.iupac.fairspec.api;

public interface IFSPropertyManagerI {

	String getParamRegex();

	boolean doExtract(String entryName);

	/**
	 * The key method that an extractor uses to offer to the plugin both the name
	 * and the bytes of the zip entry that is being considered for extraction.
	 * 
	 * @param extractor will be null if rezipping, otherwise the calling
	 *                  IFSExtractorI
	 * @param ifsPath the output path, either for a ZipOutputStream or a FileOutputStream
	 * @param bytes     the decompressed contents of this file, for checking and further processing; may be null in some applications
	 * @return true if accepted (but may be ignored by the extractor)
	 */
	String accept(IFSExtractorI extractor, String ifsPath, byte[] bytes);

	/**
	 * Process a representation (zip file or directory, mol file, etc.), possibly
	 * sending new properties to the Extractor, such as InChI or SMILES for a MOL
	 * file.
	 * 
	 * @param ifsPath the path to this representation; may be null (for example, in
	 *                the case of a zip file), but may be used for distinguishing
	 *                representation types such as mol.2d or sdf.
	 * 
	 * @param bytes   bytes for this representation; may be null for zip files
	 * @return the IFS type key for this digital object (e.g.
	 *         IFSNMRSpecDataRepresentation.IFS_REP_SPEC_NMR_VENDOR_DATASET)
	 */
	String processRepresentation(String ifsPath, byte[] bytes);

	static void addProperty(IFSExtractorI extractor, String key, Object val) {
		if (val == null)
			return;
		System.out.println(key + " = " + val);
		if (extractor != null)
			extractor.addProperty(key, val);
	}


}
