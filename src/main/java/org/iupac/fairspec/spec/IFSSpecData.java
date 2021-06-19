package org.iupac.fairspec.spec;

import org.iupac.fairspec.data.IFSDataObject;

/**
 * 
 * An abstract class that can refer to multiple spectroscopy data
 * representations. There is nothing special characterizing this relative to
 * IFSDataObject other than its class name; methods and properties specific to
 * specific types of spectroscopy are provided in subclasses of this class.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSSpecData extends IFSDataObject<IFSSpecDataRepresentation> {

	public IFSSpecData(String name, ObjectType type) {
		super(name, type);
	}
	
	@Override
	public String toString() {
		return (name == null ? super.toString() : "[" + type + " " + name  + " " + (size() > 0 ? get(0) : null) + "]");
	}



}
