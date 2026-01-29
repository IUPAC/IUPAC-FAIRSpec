package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * A class associating a name with either a single value or a list of values
 * where the item is not a standard IFDProperty. 
 * 
 * All values must be String Number, or NumberString. 
 * 
 * (NumberString preserves the precision given in an input string, such as from 
 * a JDX file or a Bruker DX-style parameter file. 
 * 
 * 
 * When adding and an attribute with the given name exists:
 *   
 *    (1) if the value is null, the attribute is removed
 *    
 *    (2) if the value matches a value already present, no addition is made
 *    
 *    (3) otherwise, a new value is added to this attribute
 *    
 *    (4) a RuntimeException is thrown if the type of value (String or Number)
 *        being added does not match the type of value already in the attributee. 
 *    
 * Serialization of multiple values will be as an array of values. 
 * Each value in the array must be of the same type -- String or Number
 *    
 * @author hansonr
 *
 */
public class Attribute implements IFDSerializableI, Comparable<Attribute> {

	public interface NumberString {
	  // a tag only	
	}

	public static class DoubleString implements NumberString {
	
		private final String s;
		private final double d;
		
		public DoubleString(String val) {
			this.s = val;
			this.d = Double.parseDouble(val);
		}
	
		@Override
		public String toString() {
			return s;
		}
	
		public double value() {
			return d;
		}
		
		@Override
		public boolean equals(Object o) {
			return (o != null && s.equals(o.toString()));
		}
		
		@Override
		public int hashCode() {
			return s.hashCode();
		}
	}


	public static class FloatString implements NumberString {
	
		private final String s;
		private final float f;
		
		public FloatString(String val) {
			this.s = val;
			this.f = Float.parseFloat(val);
		}
	
		@Override
		public String toString() {
			return s;
		}
	
		public float value() {
			return f;
		}
		
		@Override
		public boolean equals(Object o) {
			return (o != null && s.equals(o.toString()));
		}
		
		@Override
		public int hashCode() {
			return s.hashCode();
		}
	
	}


	private final String name;
	private Object value;
	private List<Object> values;
    private boolean isString;

	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
		isString = true;
	}

	public Attribute(String name, Number value) {
		this.name = name;
		this.value = value;
	}


	public Attribute(String name, NumberString value) {
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


	 public static void add(List<Attribute> attributes, String name, Object value) {
		if (value == null || name == null)
			return;
		boolean isString = value instanceof String;
		boolean isNumber = !isString && value instanceof Number;
		if (!isString && !isNumber && !(value instanceof NumberString))
			throw new RuntimeException("Attributes must be either String or Number adding " + value + " type " + value.getClass().getName());
		Attribute p = null;
		for (int i = attributes.size(); --i >= 0;) {
			p = attributes.get(i);
			if (p.name.equals(name))  {
				if (p.isString != isString) {
					throw new RuntimeException("Atribute values must not be of mixed type.");
				}
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
		attributes.add(isString 
				? new Attribute(name, (String) value) 
						: isNumber ? new Attribute(name, (Number) value)
								: new Attribute(name, (NumberString) value));
	}

	public static void remove(List<Attribute> params, String name) {
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
	public static boolean remove(List<Attribute> params, String name, Object value) {
		if (name == null)
			return false;
		for (int i = params.size(); --i >= 0;) {
			if (params.get(i).name.equals(name)) {
				Attribute p = params.get(i);
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
	public int compareTo(Attribute o) {
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


	public static void mergeAll(List<Attribute> attFrom, List<Attribute> attrTo) {		
		for (int i = 0, n = attFrom.size(); i < n; i++) {
			Attribute a = attFrom.get(i);
			Attribute.add(attrTo, a.getName(), a.getValue());
		}
		
	}
}
