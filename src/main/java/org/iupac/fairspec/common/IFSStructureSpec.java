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

	public IFSStructure getStructure() {
		return structure;
	}

	public void setStructure(IFSStructure structure) {
		this.structure = structure;
	}

	public IFSSpecData getSpecData() {
		return specData;
	}

	public void setSpecData(IFSSpecData specData) {
		this.specData = specData;
	}

}
