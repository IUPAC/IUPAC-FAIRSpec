package org.iupac.fairdata.assoc;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.struc.IFDStructure;
import org.iupac.fairdata.struc.IFDStructureCollection;

/**
 * A class to correlation one IFDSample with one or more IFDDataObject.
 * 
 * An abstract object that does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDSampleAssociation extends IFDDataObjectCollection<IFDDataObject<?>> {

	private IFDSample sample;
	private IFDStructureCollection structureCollection;

	public IFDSampleAssociation(String name, String type, IFDSample sample) throws IFDException {
		super(name, type);
		if (sample == null)
			throw new IFDException("IFDSampleDataAssociation must involve a non-null IFDSample");
		this.sample = sample;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDSampleAssociation))
			return false;
		IFDSampleAssociation ss = (IFDSampleAssociation) o;
		return (sample == ss.sample 
				&& (structureCollection == ss.structureCollection
				|| structureCollection != null && structureCollection.equals(ss.structureCollection)));
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.addObject("sample", sample.getIndex());
		if (structureCollection != null)
			serializer.addObject("struc", getStructureCollection().getIndexList());
		serializer.addObject("data", getIndexList());
	}

	public IFDSample getSample() {
		return sample;
	}

	public void setSample(IFDSample sample) {
		this.sample = sample;
	}

	public IFDStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			try {
				structureCollection = new IFDStructureCollection("sample");
			} catch (IFDException e) {
				// unattainable
			}
		}
		return structureCollection;
	}

	public IFDStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	protected boolean addStructure(IFDStructure struc) {
		return getStructureCollection().add(struc);
	}

}
