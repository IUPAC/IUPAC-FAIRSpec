package org.iupac.fairspec.api;

/**
 * The IFSObectI is the public interface for the IFSObject. Note that IFSObject extends
 * ArrayList, so all the methods of ArrayList are also inherited.
 * 
 * See IFSObject for a detailed explanation of IFS objects.
 * 
 * 
 * @author hansonr
 *
 */
public interface IFSObjectI<T> {

	
	public static interface ObjectType {
		// IFS core
		public final static String Unknown = "Unknown";
		public final static String Mixed = "Mixed";
		public final static String Custom = "Custom";
		// IFS struc
		public final static String Structure = "Structure";
		public final static String StructureCollection = "StructureCollection";
		public final static String StructureAnalysis = "StructureAnalysis";
		public final static String StructureAnalysisCollection = "StructureAnalysisCollection";
		// IFS sample
		public final static String Sample = "Sample";
		public final static String SampleCollection = "SampleCollection";
		public final static String SampleAnalysis = "SampleAnalysis";
		public final static String SampleAnalysisCollection = "SampleAnalysisCollection";
	
		// IFSCollection, IFSDataObject, IFSDataObjectCollection, and IFSFindingAid
		// are all abstract and so do not express their own ObjectType


	};

	String getName();

	T getObject(int index);

	int getObjectCount();

	String getObjectType();

}