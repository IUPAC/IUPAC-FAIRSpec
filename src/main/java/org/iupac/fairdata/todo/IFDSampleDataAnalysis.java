package org.iupac.fairdata.sample;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * An IFDAnalysis is a specialized IFDStructureDataAssociation that provides
 * more detailed metadata correlating one or more IFDStructure and one or more
 * IFDDataObject.
 * 
 * Q: Should IFDAnalysis be representable? If so, then it should not connect
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
public abstract class IFDSampleAnalysis extends IFDRepresentableObject<IFDSampleAnalysisRepresentation>{

	private IFDSampleCollection sampleCollection;
	private IFDDataObjectCollection<?> dataCollection;

	public IFDSampleAnalysis(String name, String type, IFDSampleCollection sampleCollection,
			IFDDataObjectCollection<?> dataCollection) throws IFDException {
		super(name, (type == null ? ObjectType.SampleAnalysis : type));
		this.sampleCollection = sampleCollection;
		this.dataCollection = dataCollection;
		if (sampleCollection == null || dataCollection == null)
			throw new IFDException("IFDSampleAnalysis sampleCollection and dataCollection must be non-null.");
	}

	public IFDSampleCollection getSampleCollection() {
		return sampleCollection;
	}

	public void setSampleCollection(IFDSampleCollection sampleCollection) {
		this.sampleCollection = sampleCollection;
	}

	public IFDDataObjectCollection<?> getDataCollection() {
		return dataCollection;
	}

	public void setDataCollection(IFDDataObjectCollection<?> dataCollection) {
		this.dataCollection = dataCollection;
	}
	
	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.addObject("samples", sampleCollection.getIndexList());
		serializer.addObject("data", dataCollection.getIndexList());
	}
	

}
