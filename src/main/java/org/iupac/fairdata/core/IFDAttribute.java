package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * A class associating a name with either a single value or a list of values.
 * 
 * When adding and an attribute with the given name exists:
 *   
 *    (1) if the value is null, the attribute is removed
 *    
 *    (2) if the value matches a value already present, no addition is made
 *    
 *    (3) otherwise, a new value is added to this attribute
 *    
 * @author hansonr
 *
 */
public class IFDAttribute implements IFDSerializableI, Comparable<IFDAttribute> {

	private final String name;
	private Object value;
	private List<Object> values;

	public IFDAttribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}


	public String getName() {
		return name;
	}

	/**
	 * Return a single value or a list of values.
	 * 
	 * @return
	 */
	public Object getValue() {
		return values == null ? value : 
			values;
	}

	/**
	 * Return a single value or a list of values.
	 * 
	 * @return
	 */
	public List<Object> getValues() {
		return values;
	}

	@Override
	public String getSerializedType() {
		return "IFDAttribute";
	}



	
	public static void add(List<IFDAttribute> params, String name, Object value) {
		if (value == null || name == null)
			return;
		IFDAttribute p = null;
		for (int i = params.size(); --i >= 0;) {
			p = params.get(i);
			if (p.name.equals(name))  {
				if (p.values != null ? p.values.contains(value)
						: p.value.equals(value))
					continue;
				if (p.values == null) {
					p.values = new ArrayList<Object>();
					p.values.add(p.value);
					p.value = null;
				}
				p.values.add(value);
				return;
			}
		}
		params.add(new IFDAttribute(name, value));
	}

	public static void remove(List<IFDAttribute> params, String name) {
		if (name == null)
			return;
		for (int i = params.size(); --i >= 0;) {
			if (params.get(i).name.equals(name)) {
				params.remove(i);
				return;
			}
		}
	}
	
	/**
	 * Remove this attribute or a value of this attribute.
	 * 
	 * @param params
	 * @param name
	 * @param value
	 * @return
	 */
	public static boolean remove(List<IFDAttribute> params, String name, Object value) {
		if (name == null)
			return false;
		for (int i = params.size(); --i >= 0;) {
			if (params.get(i).name.equals(name)) {
				IFDAttribute p = params.get(i);
				if (value == null || value.equals(p.value)) {
					params.remove(i);
					return true;
				}
				if (p.values != null && p.values.contains(value)) {
						p.values.remove(value);
						switch (p.values.size()) {
						case 1:
							p.value = p.values.get(0);
							p.values = null;
							break;
						case 0:
							params.remove(i);
							break;
						}
						return true;
				}
				break;
			}
		}
		return false;
	}


	@Override
	public int compareTo(IFDAttribute o) {
		return (name.compareTo(o.getName()));
	}

//	public static class ParamComparator implements Comparator<IFDAttribute> {
//		@Override
//		public int compare(IFDAttribute p1, IFDAttribute p2) {
//			return p1.name.compareTo(p2.name);
//		}
//	}
//
//	private static ParamComparator sorter;
//	
//	public static void sort(List<IFDAttribute> params) {
//		if (sorter == null)
//			sorter = new ParamComparator();
//		params.sort(sorter);
//	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		// NOTE: THIS METHOD IS NOT USED; IFDAttribute 
		// does not use "name" as an attribute.
		// instead, it creates a TreeMap as for IFDProperty
//		if ((value == null || value == "") && values == null)
//			return;
//		serializer.addAttr("name", name);
//		serializer.addValue(values == null ? value : values);
	}
	
	@Override
	public String toString() {
		return (getValue() == null ? "" : "[IFDParam " + name + "=" + value + "]");
	}
}
