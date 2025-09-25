package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.analysisobject.IFDAnalysisObjectCollection;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.derived.IFDStructureDataAnalysisAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * A class that identifies this as an IFDStructureDataAssociation specifically
 * of type type FAIRSpecCompoundAssociation.
 * 
 * The FAIRSpecCompoundAssociation is a purely metadata object that has no
 * direct representations as such (debate?), but has the ability to associate
 * multiple structures (isomers of "Compound 3a", for example) with multiple
 * spectra.
 * 
 * It uses the term "compound" in its common use in the literature as a ("one or
 * more isomerically related compounds").
 * 
 * It could have a property that expresses this relationship, such as a mInChI,
 * but this is not established in this version.
 * 
 * If there are no spectra, then the FAIRSpecCompoundAssociation becomes simply
 * an association of isomerically related structures, much the same way a
 * PubChem "compound" such as 21508 (2,3-dibromobutane)
 * https://pubchem.ncbi.nlm.nih.gov/compound/2,3-dibromoButane is ambiguous,
 * describing any one of four stereoisomers, or some mixture of them.
 * 
 * Ambiguity can be removed through the addition of properties or attributes
 * that express the relationship and relative amounts of the related structures.
 * 
 * 
 * In the case where the FAIRSpecCompoundAssociation includes one or more
 * associated spectra, a single IFDCompoundAssociation can express a mixture of
 * constitutionally unrelated chemical compounds that are associated with a set
 * of spectra, "Compounds 3 and 4" where 3 was the reactant and 4 was the
 * product in a chemical reaction, and both are present in an NMR sample. In
 * this case, the structures would have different properties or representations
 * that unambiguously identify them as distinctly different chemical compounds.
 * 
 * It might be possible to have add a property to FAIRSpecCompoundAssociation
 * that would be a mInChi or its equivalent in order to allow a more
 * fine-grained description of the associated chemical compounds.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecAnalysisAssociation extends IFDStructureDataAnalysisAssociation {

	private final static String analysisPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.getProp("FAIRSPEC_ANALYSIS_FLAG"));

	@Override
	protected String getIFDPropertyPrefix() {
		return analysisPrefix;
	}

	protected FAIRSpecAnalysisAssociation() throws IFDException {
		super();
	}
}
