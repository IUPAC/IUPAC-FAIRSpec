package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;

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
 * It could have a property that expresses this relationship.
 * 
 * If there are no spectra, then the FAIRSpecCompoundAssociation becomes simply
 * an association of structures, much the same way a PubChem "compound" such as
 * 21508 (2,3-dibromobutane)
 * https://pubchem.ncbi.nlm.nih.gov/compound/2,3-dibromoButane is ambiguous,
 * describing any one of four stereoisomers, or some mixture of them.
 * 
 * However, in such a case, when the FAIRSpecCompoundAssociation is associated
 * with experimental spectra, this ambiguity can be removed through the addition
 * of properties or parameters that express the relationship of the related
 * structures.
 * 
 * In addition, a single IFDCompoundAssociation can express a mixture of constitutionally
 * unrelated chemical compounds that are associated with a set of spectra.
 * "Compounds 3a and 3b". In this case, the structures would have different
 * properties or representations that clearly identify them as distinctly
 * different chemical compounds.
 * 
 * It might be possible to have add a property to FAIRSpecCompoundAssociation that would be
 * a mInChi to allow a more fine-grained description of the assocated chemical
 * compounds.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCompoundAssociation extends IFDStructureDataAssociation {

	private final static String cmpdPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.getProp("FAIRSPEC_COMPOUND_FLAG"));

	@Override
	protected String getPropertyPrefix() {
		return cmpdPrefix;
	}

	protected FAIRSpecCompoundAssociation() throws IFDException {
		super();
	}
}
