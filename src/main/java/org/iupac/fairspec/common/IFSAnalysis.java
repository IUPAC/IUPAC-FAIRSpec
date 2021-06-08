package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;

@SuppressWarnings("serial")
public class IFSAnalysis extends IFSObject<IFSAnalysisRepresentation> implements IFSCoreObject {

	
	public IFSAnalysis(String name) {
		super(name);
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.Analysis;
	}

	@Override
	protected IFSAnalysisRepresentation newRepresentation(String name, IFSReference ref, Object obj) {
		return new IFSAnalysisRepresentation(name, ref, obj);

	}


}
