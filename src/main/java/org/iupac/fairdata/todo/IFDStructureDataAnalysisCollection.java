package org.iupac.fairdata.struc;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public abstract class IFDStructureAnalysisCollection extends IFDCollection<IFDStructureAnalysis> {

	public IFDStructureAnalysisCollection(String name, String type) throws IFDException {
		super(name, (type == null ? ObjectType.StructureAnalysisCollection : type));
	}

	public void addAnalysis(IFDStructureAnalysis a) {
		super.add(a);
	}



}