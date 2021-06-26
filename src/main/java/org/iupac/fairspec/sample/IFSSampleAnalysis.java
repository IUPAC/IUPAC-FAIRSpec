package org.iupac.fairspec.sample;

import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.core.IFSRepresentableObject;

/**
 * An IFSAnalysis is a specialized IFSStructureDataAssociation that provides
 * more detailed metadata correlating one or more IFSStructure and one or more
 * IFSDataObject.
 * 
 * Q: Should IFSAnalysis be representable? If so, then it should not connect
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
public class IFSSampleAnalysis extends IFSRepresentableObject<IFSSampleAnalysisRepresentation>{

	private IFSSampleCollection sampleCollection;
	private IFSDataObjectCollection<?> dataCollection;

	public IFSSampleAnalysis(String name, ObjectType type, IFSSampleCollection sampleCollection,
			IFSDataObjectCollection<?> dataCollection) throws IFSException {
		super(name, (type == null ? ObjectType.SampleAnalysis : type));
		this.sampleCollection = sampleCollection;
		this.dataCollection = dataCollection;
		if (sampleCollection == null || dataCollection == null)
			throw new IFSException("IFSSampleAnalysis sampleCollection and dataCollection must be non-null.");
	}

	public IFSSampleCollection getSampleCollection() {
		return sampleCollection;
	}

	public void setSampleCollection(IFSSampleCollection sampleCollection) {
		this.sampleCollection = sampleCollection;
	}

	public IFSDataObjectCollection<?> getDataCollection() {
		return dataCollection;
	}

	public void setDataCollection(IFSDataObjectCollection<?> dataCollection) {
		this.dataCollection = dataCollection;
	}

	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len,
			String type, String subtype) {
		return new IFSSampleAnalysisRepresentation(ifsReference, object, len, type, subtype);
	}

	
	@Override
	protected void serializeList(IFSSerializerI serializer) {
		serializer.addObject("samples", sampleCollection.getIndexList());
		serializer.addObject("data", dataCollection.getIndexList());
	}
	

}
