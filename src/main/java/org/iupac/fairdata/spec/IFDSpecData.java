package org.iupac.fairdata.spec;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObject;

/**
 * 
 * An abstract class that can refer to multiple spectroscopy data
 * representations of a particular spectroscopic data set. e There is nothing
 * special characterizing this relative to IFDDataObject other than its class
 * name; methods and properties specific to specific types of spectroscopy are
 * provided in subclasses of this class.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDSpecData extends IFDDataObject<IFDSpecDataRepresentation> {

	public IFDSpecData(String name, String type) throws IFDException {
		super(name, type);
	}

	@Override
	public String toString() {
		return (name == null ? super.toString()
				: "[" + classType + " " + index + " " + name + " " + (size() > 0 ? get(0) : null) + "]");
	}

	@Override
	protected void serializeProps(IFDSerializerI serializer) {
		super.serializeProps(serializer);
	}

	abstract protected IFDSpecData newInstance() throws IFDException;

	@Override
	public Object clone() {
		IFDSpecData data;
		try {
			data = newInstance();
			data.add(get(0));
			data.name = name;
			return data;
		} catch (IFDException e) {
		}
		return null;
	}

}
