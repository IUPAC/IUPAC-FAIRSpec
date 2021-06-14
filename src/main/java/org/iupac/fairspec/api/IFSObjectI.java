package org.iupac.fairspec.api;

/**
 * This is the public interface for the IFSObject. Note that IFSObject extends
 * ArrayList, so all the methods of ArrayList are also inherited.
 * 
 * IFSObjects come in four flavors: IFSCoreObjects and IFSCollections.
 * 
 * *** IFSCoreObjects ***
 * 
 * These include IFSData, IFSStructure, and IFSAnalysis.
 * 
 * -- IFSData --
 * 
 * a data object, which may hold references to multiple representations of what
 * a scientist would call "their data", such as a full vendor experiment
 * dataset, a PNG image of a spectrum, or a peaklist. Herein we extend this
 * class to IFSSpecData, but we recognize that the system we are developing is
 * not limited to spectroscopic data only.
 * 
 * -- IFSStructure --
 * 
 * a structural object, which may have several representations, such as a MOL
 * file, an InChI or InChIKey, one or more chemical names, one or more SMILES.
 * 
 * -- IFSAnalysis --
 * 
 * an analysis object, which may include some sort of representations that
 * indicate the correlation of specific atoms or groups of atoms with specific
 * signals in a spectrum.
 *
 *
 * *** IFSCollections ***
 *
 * In addition to these three core IFSObject types, metadata relating to
 * collections of objects are found in IFSCollection implementations, of which
 * there are five types:
 * 
 * -- IFSSpecDataCollection, IFSStructureCollection, IFSAnalysisCollection --
 * 
 * These three IFSObjects bring together all or some subset of objects in a
 * collection. For example, "all NMR spectra" or "all structures" or "all 13C
 * NMR" or "all 2D NMR".
 * 
 * -- IFSStrucSpecCollection --
 * 
 * This special collection can be used to correlate specific IFSStructures with
 * specific IFSSpectra. The represent the "connecting links" between spectra and
 * structure and may specify specific representations, for example, a specific
 * MOL file with matching numbers relating to an analysis.
 * 
 * -- IFSFindingAid --
 *
 * In general, an overall collection will contain a "master" collection metadata
 * object, the IFSFindingAid. The IFSFindingAid is a special "collection of
 * collections," providing:
 * 
 * 1) metadata relating to the entire collection. For example, a publication or
 * thesis.
 * 
 * 2) high-level access to lower-level metadata. For example, a list of compound
 * names or key spectroscopy data characteristics.
 * 
 * 3) pointers to subcollections, which may be a pointer to another finding aid
 * or directly to a type of collection.
 * 
 * It is the IFSFindingAid that should be very close to what the user sees. It
 * should reveal information about the collection that allows users to quickly
 * determine whether data in this collection are relevant to their interests.
 * The IFSFindingAid could be static -- deposited with a repository collection
 * -- or dynamically created in response to a query. 
 * 
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
		Unknown, 
		Structure, StructureCollection, 
		// IFSCOllection, IFSDataObject, IFSDataObjectCollection, and IFSFindingAid
		// are all abstract and so do not express their own ObjectType
		
		// IFS spec collections:
		SpecDataFindingAid, 
		SpecDataCollection, 
		StructureSpecCollection, 
		AnalysisCollection,
		// IFS spec core
		SpecData, Analysis, 
		NMRSpecData, IRSpecData, MSSpecData, RAMANSpecData,
		// IFS spec combined
		StructureSpec
	};

	String getName();

	T getObject(int index);

	int getObjectCount();

	ObjectType getObjectType();

}