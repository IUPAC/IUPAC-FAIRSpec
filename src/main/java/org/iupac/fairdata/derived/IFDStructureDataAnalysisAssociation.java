package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysisobject.IFDAnalysisObject;
import org.iupac.fairdata.analysisobject.IFDAnalysisObjectCollection;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;

/**
 * An IFDStructureDataAnalysis is a specialized IFDAssociation. Unlike its
 * superclass, IFDStructureDataAnalysis is expected to describe in detail the
 * correlation between structure and spectra -- using specific structure
 * representations that map atom numbers to spectral signals or sets of signals.
 * 
 * An IFDStructureDataAnalysis should detailed metadata correlating one or more
 * IFDStructure objects (a mixture of diastereoisomers, for example) and one or
 * more IFDDataObject objects (1H, 13C, HMQC spectra). It does so because it
 * also maintains an IFDRepresentableObject (as an IFDAnalysisObject).
 * 
 * 
 * Typically, only one structure will be involved, but the class allows for any
 * number of structures (such as in the case of a chemical mixture).
 * 
 * There can be as many spectra as are relevant to an analysis. For example, the
 * analysis can be just one structure and a 1H NMR spectrum. Or it can be a
 * compound along with its associated 1H, 13C, DEPT, HSQC, and HMBC spectra.
 * 
 * For example, an NMReDATA file would be the IFDAnalysisRepresentation of the
 * IFDAnalysisObject passed to this class's constructor. Since that file also
 * contains the structure, it would also be the IFDStructureRepresentation of
 * the IFDStructure found in the IFDStrutureCollection passed.
 * 
 * An nmrML file that contains structure, spectrum, and analysis could serve as
 * the IFDRepresentation for all three of these objects.
 * 
 * Note that there may need to be a pointer here to a specific representation of
 * a structure. This is because different representations may have different
 * atom numbering. This is possible because each distinctly different
 * IFDRepresentation by definition must have a distinct IFDReference.
 * 
 * Ultimately, this class will be serialized as a simple set of indexes into
 * each of the three primary object collections of the finding aid --
 * structures, dataObjects, and analyses.
 * 
 * 
 * It is the responsibility of the implementer to develop this class further (by
 * subclassing!).
 * 
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDStructureDataAnalysisAssociation extends IFDAssociation implements IFDAnalysisI {

	@SuppressWarnings("unchecked")
	public IFDStructureDataAnalysisAssociation(String type, 
			IFDStructureCollection structureCollection, 
			IFDDataObjectCollection dataCollection,
			IFDAnalysisObjectCollection aoCollection) throws IFDException {
		super(type, new IFDCollection[] { structureCollection, dataCollection, aoCollection});
	}

	final static int ITEM_STRUC = 0;
	final static int ITEM_DATA = 1;
	final static int ITEM_ANALYSIS = 2;
	
	private final static String sdaaPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_STRUCTUREDATA_ASSOCIATION_FLAG);

	@Override
	protected String getIFDPropertyPrefix() {
		return sdaaPrefix;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected IFDStructureDataAnalysisAssociation(IFDCollection[] collection) throws IFDException {
		super(null, collection);		
	}

	public IFDStructureDataAnalysisAssociation() throws IFDException {
		this(new IFDCollection[] { 
				new IFDStructureCollection(), 
				new IFDDataObjectCollection(),
				new IFDAnalysisObjectCollection()});		
	}
	
	public IFDStructureDataAnalysisAssociation(IFDStructure structure, IFDDataObject data, IFDAnalysisObject analysis) throws IFDException {
		this(new IFDStructureCollection(structure), new IFDDataObjectCollection(data), new IFDAnalysisObjectCollection(analysis));
	}

	@SuppressWarnings("unchecked")
	public IFDStructureDataAnalysisAssociation(IFDStructureCollection structureCollection, IFDDataObjectCollection dataCollection, IFDAnalysisObjectCollection analysisCollection) throws IFDException {
		super(null, new IFDCollection[] { structureCollection, dataCollection, analysisCollection });		
	}
	
	public IFDStructureCollection getStructureCollection() {
		// coerce IFDStructureCollection. I do not know why this does not work directly
		return (IFDStructureCollection) (Object) get(ITEM_STRUC);
	}

	public IFDDataObjectCollection getDataObjectCollection() {
		return (IFDDataObjectCollection) (Object) get(ITEM_DATA);
	}

	public IFDAnalysisObjectCollection getAnalysisObjectCollection() {
		return (IFDAnalysisObjectCollection) (Object) get(ITEM_ANALYSIS);
	}

	public IFDStructure getStructure(int i) {
		return (IFDStructure) getStructureCollection().get(i);
	}

	public IFDDataObject getDataObject(int i) {
		return (IFDDataObject) getDataObjectCollection().get(i);
	}

	public IFDAnalysisObject getAnalysisObject(int i) {
		return (IFDAnalysisObject) getAnalysisObjectCollection().get(i);
	}

	public boolean addStructure(IFDStructure struc) {
		return getStructureCollection().add(struc);
	}

	public boolean addDataObject(IFDDataObject data) {
		return getDataObjectCollection().add(data);
	}
	
	public boolean addAnalysisObject(IFDAnalysisObject analysis) {
		return getAnalysisObjectCollection().add(analysis);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDStructureDataAnalysisAssociation))
			return false;
		IFDStructureDataAnalysisAssociation ss = (IFDStructureDataAnalysisAssociation) o;
		return (ss.get(ITEM_STRUC).equals(get(ITEM_STRUC)) 
				&& ss.get(ITEM_DATA).equals(get(ITEM_DATA))
				&& ss.get(ITEM_ANALYSIS).equals(get(ITEM_ANALYSIS))
				);
	}

	public static String getItemName(int i) {
		switch (i) {
		case ITEM_STRUC:
			return "structures";
		case ITEM_DATA:
			return "data";
		case ITEM_ANALYSIS:
			return "analyses";
		default:
			return null;
		}
	}

}
