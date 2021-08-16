package org.iupac.fairspec.spec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObjectCollection;

import javajs.util.PT;

/**
 * A collection of IFSSpecData objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSpecDataCollection extends IFSDataObjectCollection<IFSSpecData> {

	private Map<String, Constructor<?>> htConstructors = new HashMap<>();

	public IFSSpecDataCollection(String name) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.SpecDataCollection);
	}
	
	public IFSSpecDataCollection(String name, IFSSpecData data) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.SpecDataCollection);
		addSpecData(data);
	}

	public boolean addSpecData(IFSSpecData data) {
		return super.add(data);
	}
	
	public String getDataType() {
		return subtype;
	}

	/**
	 * Use dynamic class loading to create a new IFSSpecData object, caching the
	 * Constructor for speed.
	 */
	@Override
	public IFSSpecData newIFSDataObject(String rootPath, String param, String value, String type) throws IFSException {
		try {
			// spec.xxx --> org.iupac.fairspec.spec.xxx.IFSXXXSpecData
			String className = IFSSpecData.class.getName();
			className = PT.rep(className, "spec.IFS",
					type.toLowerCase() + ".IFS" + type.substring(type.indexOf('.') + 1).toUpperCase());
			Constructor<?> c = htConstructors.get(className);
			if (c == null) {
				htConstructors .put(className, c = Class.forName(className).getDeclaredConstructor());
			}
			IFSSpecData sd = (IFSSpecData) c.newInstance();
			if (sd == null)
				throw new IFSException("Unrecognized IFSSpecData type " + type);
			sd.setPath(rootPath);
			sd.setPropertyValue(param, value);
			return sd;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
	}

}