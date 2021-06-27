package org.iupac.fairspec.assoc;

import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.sample.IFSSample;
import org.iupac.fairspec.struc.IFSStructure;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * A class to correlation one IFSSample with one or more IFSDataObject.
 * 
 * An abstract object that does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSSampleAssociation extends IFSDataObjectCollection<IFSDataObject<?>> {

	private IFSSample sample;
	private IFSStructureCollection structureCollection;

	public IFSSampleAssociation(String name, String type, IFSSample sample) throws IFSException {
		super(name, type);
		if (sample == null)
			throw new IFSException("IFSSampleDataAssociation must involve a non-null IFSSample");
		this.sample = sample;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSSampleAssociation))
			return false;
		IFSSampleAssociation ss = (IFSSampleAssociation) o;
		return (sample == ss.sample 
				&& (structureCollection == ss.structureCollection
				|| structureCollection != null && structureCollection.equals(ss.structureCollection)));
	}

	@Override
	protected void serializeList(IFSSerializerI serializer) {
		serializer.addObject("sample", sample.getIndex());
		if (structureCollection != null)
			serializer.addObject("struc", getStructureCollection().getIndexList());
		serializer.addObject("data", getIndexList());
	}

	public IFSSample getSample() {
		return sample;
	}

	public void setSample(IFSSample sample) {
		this.sample = sample;
	}

	public IFSStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			try {
				structureCollection = new IFSStructureCollection("sample");
			} catch (IFSException e) {
				// unattainable
			}
		}
		return structureCollection;
	}

	public IFSStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	protected boolean addStructure(IFSStructure struc) {
		return getStructureCollection().add(struc);
	}

}
