package org.iupac.fairspec.struc;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.core.IFSRepresentableObject;

/**
 * An IFSAnalysis is a specialized IFSStructureDataAssociation that provides more
 * detailed metadata correlating one or more IFSStructure and one or more IFSDataObject.
 * 
 * Typically, only one structure will be involved, but the class allows for any
 * number of structures (such as in the case of a chemical mixture).
 * 
 * There can be as many spectra as are relevant to an analysis. For example, the
 * analysis can be just one structure and a 1H NMR spectrum. Or it can be a
 * compound along with its associated 1H, 13C, DEPT, HSQC, and HMBC spectra.
 * 
 * Unlike its superclass, IFSAnalysis is expected to describe in detail the
 * correlation between structure and spectra -- using specific structure
 * representations that map atom numbers to spectral signals or sets of signals.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSStructureAnalysis extends IFSRepresentableObject<IFSStructureAnalysisRepresentation> {

	private IFSStructureCollection StructureCollection;
	private IFSDataObjectCollection<?> dataCollection;


	public IFSStructureAnalysis(String name, String type, IFSStructureCollection structureCollection,
			IFSDataObjectCollection<?> dataCollection) throws IFSException {
		super(name, (type == null ? ObjectType.StructureAnalysis : type));
		
	}

	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len,
			String type, String subtype) {
		return new IFSStructureAnalysisRepresentation(ifsReference, object, len, type, subtype);
	}

	public IFSStructureCollection getStructureCollection() {
		return StructureCollection;
	}

	public void setStructureCollection(IFSStructureCollection StructureCollection) {
		this.StructureCollection = StructureCollection;
	}

	public IFSDataObjectCollection<?> getDataCollection() {
		return dataCollection;
	}

	public void setDataCollection(IFSDataObjectCollection<?> dataCollection) {
		this.dataCollection = dataCollection;
	}

	

}
