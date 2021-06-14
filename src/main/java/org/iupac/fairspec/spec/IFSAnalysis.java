package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSCoreObjectI;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.object.IFSObject;

@SuppressWarnings("serial")
public class IFSAnalysis extends IFSObject<IFSAnalysisRepresentation> implements IFSCoreObjectI {

	
	public IFSAnalysis(String name) {
		super(name, ObjectType.Analysis);
	}

	@Override
	protected IFSAnalysisRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSAnalysisRepresentation(name, ref, obj, len);

	}

}
