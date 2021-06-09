package org.iupac.fairspec.common;

/**
 * A class to correlation one or more structure representations with one or more spectroscopy data represntations.
 * @author hansonr
 *
 */
public class IFSStructureSpec {
	
	
	private IFSStructure structure;
	private IFSSpecData specData;

	public IFSStructureSpec(IFSStructure structure, IFSSpecData specData) {
		this.structure = structure;
		this.specData = specData;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSStructureSpec))
			return false;
		IFSStructureSpec ss = (IFSStructureSpec) o;
		return (ss.structure == structure && ss.specData == specData);
	}

	public IFSStructure getStructure() {
		return structure;
	}

	public IFSSpecData getSpecData() {
		return specData;
	}

}
