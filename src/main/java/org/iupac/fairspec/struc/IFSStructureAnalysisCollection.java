package org.iupac.fairspec.struc;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSStructureAnalysisCollection extends IFSCollection<IFSStructureAnalysis> {

	public IFSStructureAnalysisCollection(String name, String type) throws IFSException {
		super(name, (type == null ? ObjectType.StructureAnalysisCollection : type));
	}

	public void addAnalysis(IFSStructureAnalysis a) {
		super.add(a);
	}



}