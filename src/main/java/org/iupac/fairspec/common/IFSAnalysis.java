package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;

@SuppressWarnings("serial")
public class IFSAnalysis extends IFSObject<IFSAnalysisRepresentation> implements IFSCoreObject {

	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.Analysis;
	}

}
