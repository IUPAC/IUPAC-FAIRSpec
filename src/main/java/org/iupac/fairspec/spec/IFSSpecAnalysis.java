package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.object.IFSAnalysis;

@SuppressWarnings("serial")
public class IFSSpecAnalysis extends IFSAnalysis<IFSSpecAnalysisRepresentation> {

	
	public IFSSpecAnalysis(String name) {
		super(name, ObjectType.SpecAnalysis);
	}

	@Override
	protected IFSSpecAnalysisRepresentation newRepresentation(String name, IFSReference ref, Object obj, long len) {
		return new IFSSpecAnalysisRepresentation(name, ref, obj, len);

	}

}
