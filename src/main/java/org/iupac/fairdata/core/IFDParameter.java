package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * A class associating a name with either a single value or a list of values.
 * 
 * When adding and a parameter with the given name exists:
 *   
 *    (1) if the value is null, the parameter is removed
 *    
 *    (2) if the value matches a value already present, no addition is made
 *    
 *    (3) otherwise, a new value is added to this parameter
 *    
 * @author hansonr
 *
 */
public class IFDParameter implements IFDSerializableI, Comparable<IFDParameter> {

	private final String name;
	private Object value;
	private List<Object> values;

	public IFDParameter(String name, Object value) {
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
		return values == null ? value : values;
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
		return "IFDParameter";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (value == null)
			return;
		serializer.addAttr("name", name);
		serializer.addValue(values == null ? value : values);
	}
	
	@Override
	public String toString() {
		return (value == null ? "" : "[IFDParam " + name + "=" + value + "]");
	}


	public static void remove(List<IFDParameter> params, String name) {
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
	 * Remove this parameter or a value of this parameter.
	 * 
	 * @param params
	 * @param name
	 * @param value
	 * @return
	 */
	public static boolean remove(List<IFDParameter> params, String name, Object value) {
		if (name == null)
			return false;
		for (int i = params.size(); --i >= 0;) {
			if (params.get(i).name.equals(name)) {
				IFDParameter p = params.get(i);
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
	
	public static void add(List<IFDParameter> params, String name, Object value) {
		if (value == null || name == null)
			return;
		IFDParameter p = null;
		for (int i = params.size(); --i >= 0;) {
			p = params.get(i);
			if (p.name.equals(name))  {
				if (p.values != null ? p.values.contains(value)
						: p.value.equals(value))
					continue;
				if (p.values == null)
					p.values = new ArrayList<Object>();
				p.values.add(value);
				p.value = null;
				return;
			}
		}
		params.add(new IFDParameter(name, value));
	}


	@Override
	public int compareTo(IFDParameter o) {
		return (name.compareTo(o.getName()));
	}

	public static class ParamComparator implements Comparator<IFDParameter> {
		@Override
		public int compare(IFDParameter p1, IFDParameter p2) {
			return p1.name.compareTo(p2.name);
		}
	}

	private static ParamComparator sorter;
	
	public static void sort(List<IFDParameter> params) {
		if (sorter == null)
			sorter = new ParamComparator();
		params.sort(sorter);
	}

}
