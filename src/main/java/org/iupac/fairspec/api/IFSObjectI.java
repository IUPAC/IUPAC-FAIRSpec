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

	/** ObjectType may be expanded upon without limitation
	 * 
	 * @author hansonr
	 *
	 */
	enum ObjectType {
		// IFS core
		Unknown, Mixed, Custom,
		// IFS struc
		Structure, StructureCollection, StructureAnalysis, StructureAnalysisCollection,
		// IFS sample
		Sample, SampleCollection, SampleAnalysis, SampleAnalysisCollection,
	
		// IFSCollection, IFSDataObject, IFSDataObjectCollection, and IFSFindingAid
		// are all abstract and so do not express their own ObjectType
		
		// IFS spec core and collections
		SpecDataFindingAid, 
		SpecData, SpecDataCollection,
		SpecAnalysis, SpecAnalysisCollection, 
		NMRSpecData, IRSpecData, MSSpecData, 
		HRMSSpecData, RAMANSpecData, UVVisSpecData,
		// IFS spec combined
		StructureSpec, StructureSpecCollection, 
		StructureSpecAnalysis, StructureSpecAnalysisCollection,
		SampleSpecCollection, SampleSpecAnalysis, SampleSpecAnalysisCollection,
	};

	String getName();

	T getObject(int index);

	int getObjectCount();

	ObjectType getObjectType();

}