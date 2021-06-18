package org.iupac.fairspec.spec;

/**
 * A collection of IFSSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSSpecAnalysisCollection extends IFSStructureSpecCollection  {

	public IFSSpecAnalysisCollection(String name) {
		super(name, ObjectType.SpecAnalysisCollection);
	}

	public void addAnalysis(IFSSpecAnalysis a) {
		super.add(a);
	}

}