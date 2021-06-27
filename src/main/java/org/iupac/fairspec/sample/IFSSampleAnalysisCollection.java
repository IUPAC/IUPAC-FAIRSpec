package org.iupac.fairspec.sample;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public abstract class IFSSampleAnalysisCollection extends IFSCollection<IFSSampleAnalysis> {

	public IFSSampleAnalysisCollection(String name, String type) throws IFSException {
		super(name, (type == null ? ObjectType.SampleAnalysisCollection : type));
	}

}