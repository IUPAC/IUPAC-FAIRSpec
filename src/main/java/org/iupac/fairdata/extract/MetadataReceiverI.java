package org.iupac.fairdata.extract;

import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;

import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;


/**
 * Implemented by MetaDataExtractor and DOICrawler. Includes methods for
 * reporting version and source as well as getting an IFDFindingAid, and
 * logging. Also used for adding properties from a vender plugin or a structure
 * representation or its properties from the structure helper.
 * 
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 *
 */
public interface MetadataReceiverI {

	class DeferredProperty {
		/**
		 * a key for the deferredObjectList that flags a structure with a spectrum;
		 * needs attention, as this was created prior to the idea of a compound
		 * association, and it presumes there are no such associations.
		 * 
		 */
		public static final String NEW_PAGE_KEY = "*NEW_PAGE*";
	
		public String originPath;
		public String localizedName;
		public String key;
		public Object value;
		public boolean isInline;
		public String mediaType;
		public String note;
		public byte[] bytes;
		public String oPath;
		public ExtractorResource resource;
		public boolean newPageDone;
		public String sampleName;
	
		public DeferredProperty(String key, Object value) {
			this.key = key;
			this.value = value;
		}
	
		public static DeferredProperty newPage(Object... value) {
			DeferredProperty dp = new DeferredProperty(NEW_PAGE_KEY, value);
			return dp;
		}

		public static DeferredProperty newStructureRep(String key, Object value, boolean isInline,
				String mediaType, String note) {
			DeferredProperty dp = new DeferredProperty(key, value);
			dp.isInline = isInline;
			dp.mediaType = mediaType;
			dp.note = note;
			return dp;
		}
	}

	String getVersion();

	String getCodeSource();

	IFDFindingAid getFindingAid();

	/**
	 * A simple logger that accepts messages and processes them based on 
	 * their initial few characters:
	 * 
	 * "!!" error to System.err and log FileOutputStream
	 * 
	 * "! " warning to System.err and log FileOutputStream
	 * 
	 * "!" but not error or warning to System.out and log FileOutputStream
	 * 
	 * anything else just to FileOutputStream
	 * 
	 * @param string
	 */
	void log(String string);

	/**
	 * 
	 * @param key
	 * @param val
	 * @param isInline
	 * @param mediaType
	 * @param note
	 * @param method
	 */
	//void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInLine, String mediaType, String note, String method);

	void addProperty(String key, Object val);

	void setSpreadSheetMetadata(IFDObject<?> o, String param);

	boolean hasStructureFor(byte[] bytes);

	void addDeferredPropertyOrRepresentation(MetadataReceiverI.DeferredProperty p);

}
