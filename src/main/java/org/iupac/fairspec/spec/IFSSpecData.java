package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObject;

/**
 * 
 * An abstract class that can refer to multiple spectroscopy data
 * representations of a particular spectroscopic data set.
 * 
 * There is nothing special characterizing this relative to IFSDataObject other
 * than its class name; methods and properties specific to specific types of
 * spectroscopy are provided in subclasses of this class.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSSpecData extends IFSDataObject<IFSSpecDataRepresentation> {

	public IFSSpecData(String name, String type) throws IFSException {
		super(name, type);
	}
	
	@Override
	public String toString() {
		return (name == null ? super.toString() : "[" + classType + " " + index + " " + name  + " " + (size() > 0 ? get(0) : null) + "]");
	}

	
	@Override
	protected void serializeProps(IFSSerializerI serializer) {
		super.serializeProps(serializer);
	}



}
