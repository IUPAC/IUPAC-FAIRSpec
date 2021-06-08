package org.iupac.fairspec.common;

/**
 * 
 * A class that can refer to multiple spectroscopy data representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSSpecData extends IFSDataObject<IFSSpecDataRepresentation> {

	public IFSSpecData(String name) {
		super(name);
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.SpecData;
	}

}
