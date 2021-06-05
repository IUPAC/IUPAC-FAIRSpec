package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSCoreObject;
import org.iupac.fairspec.api.IFSObjectApi.ObjectType;

public class IFSAnalysis extends IFSObject<IFSAnalysisRepresentation> implements IFSCoreObject {

	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.Analysis;
	}

}
