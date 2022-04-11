package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * A class to correlate one or more IFDSample with one or more IFDStructure.
 * Only two array items are required -- one IFDSampleCollection and one
 * IFDStructureCollection.
 * 
 * Each of these collections allows for one or more item, resulting in a
 * one-to-one, many-to-one, one-to-many, or many-many associations.
 * 
 * This class is designed to be used only when there is no data directly linking
 * a sample with a chemical structure. This might be the case, for example, when
 * a commercial sample is obtained and there is no need to verify its chemical
 * nature via spectroscopy.
 * 
 * This object is not representable itself.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleStructureAssociation extends IFDAssociation {
	
	private final static int ITEM_SAMPLE = 0;
	private final static int ITEM_STRUCTURE = 1;
	
	@SuppressWarnings("unchecked")
	public IFDSampleStructureAssociation(String type, IFDSample sample, IFDStructure data) throws IFDException {
		super(type, new IFDCollection[] { new IFDSampleCollection(null, sample), new IFDStructureCollection(null, data) });
	}


	@SuppressWarnings("unchecked")
	public IFDSampleStructureAssociation(String type, IFDSampleCollection sampleCollection, IFDStructureCollection dataCollection) throws IFDException {
		super(type, new IFDCollection[] { sampleCollection, dataCollection });
	}
	
	public IFDSampleCollection getSampleCollection() {
		// for whatever reason, we have to coerce this 
		return (IFDSampleCollection) (Object) get(ITEM_SAMPLE);
	}

	public IFDStructureCollection getStructureCollection() {
		// for whatever reason, we have to coerce this 
		return (IFDStructureCollection) (Object) get(ITEM_STRUCTURE);
	}

	protected boolean addSample(IFDSample s) {
		return getSampleCollection().add(s);
	}

	protected boolean addStructure(IFDStructure data) {
		return getStructureCollection().add(data);
	}

	public IFDSample getFirstSample() {
		return (IFDSample) getSampleCollection().get(0);
	}

	public IFDStructure getFirstStructure() {
		return (IFDStructure) getStructureCollection().get(0);
	}

	public IFDSample getSample(int i) {
		return (IFDSample) getSampleCollection().get(i);
	}

	public IFDStructure getStructure(int i) {
		return (IFDStructure) getStructureCollection().get(i);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDSampleStructureAssociation))
			return false;
		IFDSampleStructureAssociation ss = (IFDSampleStructureAssociation) o;
		return (ss.get(ITEM_SAMPLE).equals(get(ITEM_SAMPLE)) && ss.get(ITEM_STRUCTURE).equals(get(ITEM_STRUCTURE)));
	}


}
