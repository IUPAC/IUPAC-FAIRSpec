package org.iupac.fairspec.common;

import java.util.Hashtable;
import java.util.Map;

/**
 * 
 * A class that can refer to multiple spectroscopy data representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSSpecData extends IFSDataObject<IFSSpecDataRepresentation> {

	@Override
	public ObjectType getObjectType() {
		return ObjectType.SpecData;
	}
	
	protected final Map<String, IFSProperty> htProps = new Hashtable<>();
	private IFSProperty[] properties;
	
	public void setProperties(IFSProperty[] properties) {
		this.properties = properties;
		for (int i = properties.length; i >= 0;)
			htProps.put(properties[i].getName(), properties[i]);				
	}

	public IFSProperty[] getProperties() {
		return properties;
	}
	
	public void setPropertyValue(String name, Object value) throws IFSException {
		try {
			htProps.get(name).setValue(value);
		} catch (Exception e) {
			throw new IFSException("Could not set property " + name + " in " + this.getClass().getName());
		}
		
	}


}
