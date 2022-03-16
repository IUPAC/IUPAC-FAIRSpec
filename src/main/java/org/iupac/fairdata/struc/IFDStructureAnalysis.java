package org.iupac.fairdata.struc;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * An IFDAnalysis is a specialized IFDStructureDataAssociation that provides more
 * detailed metadata correlating one or more IFDStructure and one or more IFDDataObject.
 * 
 * Typically, only one structure will be involved, but the class allows for any
 * number of structures (such as in the case of a chemical mixture).
 * 
 * There can be as many spectra as are relevant to an analysis. For example, the
 * analysis can be just one structure and a 1H NMR spectrum. Or it can be a
 * compound along with its associated 1H, 13C, DEPT, HSQC, and HMBC spectra.
 * 
 * Unlike its superclass, IFDAnalysis is expected to describe in detail the
 * correlation between structure and spectra -- using specific structure
 * representations that map atom numbers to spectral signals or sets of signals.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDStructureAnalysis extends IFDRepresentableObject<IFDStructureAnalysisRepresentation> {

	private IFDStructureCollection StructureCollection;
	private IFDDataObjectCollection<?> dataCollection;


	public IFDStructureAnalysis(String name, String type, IFDStructureCollection structureCollection,
			IFDDataObjectCollection<?> dataCollection) throws IFDException {
		super(name, (type == null ? ObjectType.StructureAnalysis : type));
		
	}

	public IFDStructureCollection getStructureCollection() {
		return StructureCollection;
	}

	public void setStructureCollection(IFDStructureCollection StructureCollection) {
		this.StructureCollection = StructureCollection;
	}

	public IFDDataObjectCollection<?> getDataCollection() {
		return dataCollection;
	}

	public void setDataCollection(IFDDataObjectCollection<?> dataCollection) {
		this.dataCollection = dataCollection;
	}

	

}
