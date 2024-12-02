package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.extract.MetadataReceiverI;

/**
 * Implemented by MetadataExtractor
 * @author hanso
 *
 */
public interface FAIRSpecExtractorI extends MetadataReceiverI {

	/**
	 * Used by MetadataExtractor to inject metadata from a spreadsheet.
	 * 
	 * @param o
	 * @param param
	 */
	void setNewObjectMetadata(IFDObject<?> o, String param);


}
