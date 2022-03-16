package org.iupac.fairdata.api;

/**
 * The IFDObectI is the public interface for the IFDObject. Note that IFDObject extends
 * ArrayList, so all the methods of ArrayList are also inherited.
 * 
 * See IFDObject for a detailed explanation of IFD objects.
 * 
 * 
 * @author hansonr
 *
 */
public interface IFDObjectI<T> {

	
	public static interface ObjectType {
		// IFD core
		public final static String Unknown = "Unknown";
		public final static String Mixed = "Mixed";
		public final static String Custom = "Custom";
		// IFD struc
		public final static String Structure = "Structure";
		public final static String StructureCollection = "StructureCollection";
		public final static String StructureAnalysis = "StructureAnalysis";
		public final static String StructureAnalysisCollection = "StructureAnalysisCollection";
		// IFD sample
		public final static String Sample = "Sample";
		public final static String SampleCollection = "SampleCollection";
		public final static String SampleAnalysis = "SampleAnalysis";
		public final static String SampleAnalysisCollection = "SampleAnalysisCollection";
	
		// IFDCollection, IFDDataObject, IFDDataObjectCollection, and IFDFindingAid
		// are all abstract and so do not express their own ObjectType


	};

	String getName();

	T getObject(int index);

	int getObjectCount();

	String getObjectType();

}