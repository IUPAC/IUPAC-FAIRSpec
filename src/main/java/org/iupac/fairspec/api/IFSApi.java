package org.iupac.fairspec.api;

public interface IFSApi {

	enum ObjectType {
		Collection, SpecData, Structure, Analysis, SpecDataRepresentation, StructureRepresentation
	};

	enum CollectionType {
		FindingAid, SpecDataCollection, StructureCollection, StructureSpecCollection, AnalysisCollection
	};
	
	ObjectType getObjectType();

	String getName();

}