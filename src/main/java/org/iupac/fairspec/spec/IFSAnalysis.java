package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSCoreObject;
import org.iupac.fairspec.common.IFSObject;
import org.iupac.fairspec.common.IFSReference;

@SuppressWarnings("serial")
public class IFSAnalysis extends IFSObject<IFSAnalysisRepresentation> implements IFSCoreObject {

	
	public IFSAnalysis(String name) {
		super(name, ObjectType.Analysis);
	}

	@Override
	protected IFSAnalysisRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSAnalysisRepresentation(name, ref, obj, len);

	}

}
