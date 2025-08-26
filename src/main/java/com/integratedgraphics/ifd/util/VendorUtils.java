package com.integratedgraphics.ifd.util;

public class VendorUtils {

	public static class DoubleString {
	
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

	public static class FloatString {
	
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

}
