package org.iupac.fairspec.api;

/**
 * The IFSObectI is the public interface for the IFSObject. Note that IFSObject extends
 * ArrayList, so all the methods of ArrayList are also inherited.
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
		// IFS common
		Unknown, Mixed,
		Structure, StructureCollection, 
		// IFSCOllection, IFSDataObject, IFSDataObjectCollection, and IFSFindingAid
		// are all abstract and so do not express their own ObjectType
		
		// IFS spec collections:
		SpecDataFindingAid, 
		SpecDataCollection, 
		StructureSpecCollection, 
		SpecAnalysisCollection,
		// IFS spec core
		SpecData, SpecAnalysis, 
		NMRSpecData, IRSpecData, MSSpecData, 
		HRMSSpecData, RAMANSpecData, UVVisSpecData,
		// IFS spec combined
		StructureSpec
	};

	String getName();

	T getObject(int index);

	int getObjectCount();

	ObjectType getObjectType();

}