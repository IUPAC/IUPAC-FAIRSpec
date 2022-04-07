package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysisobject.IFDAnalysisObjectCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
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
public class IFDStructureDataAnalysis extends IFDAssociation implements IFDAnalysisI {

	@SuppressWarnings("unchecked")
	public IFDStructureDataAnalysis(String type, IFDStructureCollection sampleCollection, IFDDataObjectCollection dataCollection,
			IFDAnalysisObjectCollection aoCollection) throws IFDException {
		super(type, new IFDCollection[] { sampleCollection, dataCollection, aoCollection});
	}

}
