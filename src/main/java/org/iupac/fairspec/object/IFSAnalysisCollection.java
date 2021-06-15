package org.iupac.fairspec.object;

@SuppressWarnings({ "serial" })
public abstract class IFSAnalysisCollection extends IFSDataObjectCollection<IFSAnalysis<?>> {

	public IFSAnalysisCollection(String name, ObjectType type) {
		super(name, type);
	}

}