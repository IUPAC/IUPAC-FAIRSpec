package org.iupac.fairdata.core;

import org.iupac.fairdata.analysis.IFDAnalysisObjectCollection;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;

/**
 * An IFDAssociation with an added IFDAnalysisObject. 
 * 
 * See IFDAnalysisObject for details.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDAnalysis extends IFDAssociation implements IFDAnalysisI {
	
	protected IFDAnalysisObjectCollection aoCollection;
	
	public IFDAnalysis(String name, String type, IFDCollection<IFDObject<?>> collection1,
			IFDCollection<IFDObject<?>> collection2, IFDAnalysisObjectCollection aoCollection) throws IFDException {
		super(name, type, collection1, collection2);
		if (aoCollection == null)
			aoCollection = new IFDAnalysisObjectCollection(null);
		types = new Class<?>[] { collection1.getClass(), collection2.getClass(), aoCollection.getClass() };
	}
	
	@Override
	protected void serializeList(IFDSerializerI serializer) {
		super.serializeList(serializer);
		serializer.addObject("obj3", aoCollection.getIndexList());
		serializer.addObject("type3", aoCollection.getObjectType());
	}
	

	
}
