package org.iupac.fairdata.spec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObjectCollection;

import javajs.util.PT;

/**
 * A collection of IFDSpecData objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSpecDataCollection extends IFDDataObjectCollection<IFDSpecData> {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSpecData.class };
	}

	private Map<String, Constructor<?>> htConstructors = new HashMap<>();

	public IFDSpecDataCollection(String name, IFDSpecData data) throws IFDException {
		this(name);
		addSpecData(data);
	}

	public IFDSpecDataCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SpecDataCollection);
	}
	
	public boolean addSpecData(IFDSpecData data) {
		return super.add(data);
	}
	
	public String getDataType() {
		
		return subtype;
	}
	
//	public boolean remove(Object o) {
//		return super.remove(o);
//		
//	}
//
//	public IFDSpecData remove(int i) {
//		return super.remove(i);
//		
//	}
//

	/**
	 * Use dynamic class loading to create a new IFDSpecData object, caching the
	 * Constructor for speed.
	 */
	@Override
	public IFDSpecData newIFDDataObject(String rootPath, String param, String value, String type) throws IFDException {
		try {
			// spec.xxx --> org.iupac.fairdata.spec.xxx.IFDXXXSpecData
			String className = IFDSpecData.class.getName();
			className = PT.rep(className, "spec.IFD",
					type.toLowerCase() + ".IFD" + type.substring(type.indexOf('.') + 1).toUpperCase());
			Constructor<?> c = htConstructors.get(className);
			if (c == null) {
				htConstructors .put(className, c = Class.forName(className).getDeclaredConstructor());
			}
			IFDSpecData sd = (IFDSpecData) c.newInstance();
			if (sd == null)
				throw new IFDException("Unrecognized IFDSpecData type " + type);
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