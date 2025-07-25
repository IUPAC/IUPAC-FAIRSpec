package org.iupac.fairdata.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;

/**
 * Just a simple JSON serializer. You can write your own, if you wish.
 * 
 * @author hansonr
 *
 */
public class IFDDefaultJSONSerializer implements IFDSerializerI {

	public IFDDefaultJSONSerializer(boolean byIDIgnored) {
//		this.byID = byID;
	} 
//	
//	private boolean byID;
	
//	@Override
//	public void setByID(boolean tf) {
//		byID = tf;
//	}

//	@Override
//	public boolean isByID() {
//		return byID;
//	}

	
	private Obj thisObj;
	private List<Obj> stack = new ArrayList<>();
	
	
	protected class Obj {
		
		private StringBuffer sb = new StringBuffer();
		
		Obj() {
			sb.append("{");
		}
		
		String close() {
			sb.append("}");
			return sb.toString();
		}

		public void append(String s) {
			sb.append(s);
		}

		public void append(String key, String val) {
			appendKey(key).append(FAIRSpecUtilities.esc(val));
		}
		
		public void appendNoEsc(String key, String val) {
			appendKey(key).append(val);
		}
		
		public StringBuffer appendKey(String key) {
			if (sb.length() > 1)
				sb.append(",\n");
			sb.append(FAIRSpecUtilities.esc(key)).append(":");
			return sb;
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

	@Override
	public String serialize(IFDSerializableI obj) {
		openObject();
		obj.serialize(this);
		return closeObject();
	}

	@Override
	public void openObject() {
		stack.add(thisObj = new Obj());
	}

	@Override
	public void addAttr(String key, String val) {
		if (val != null)
			thisObj.append(key, val);
	}

	@Override
	public void addAttrInt(String key, long ival) {
		thisObj.appendNoEsc(key, "" + ival);		
	}

	@Override
	public void addAttrBoolean(String key, boolean val) {
		thisObj.appendNoEsc(key, "" + val);		
	}

	@Override
	public void addObject(String key, Object o) {
		thisObj.appendKey(key);
		addValue(o, false, true);
	}

	@Override
	public void addValue(Object val) {
		addValue(val, true, true);
	}
	
	@Override
	public void addCollection(String key, IFDCollection<? extends IFDObject<?>> list, boolean byID) {
		if (!byID) {
			addList(key, list);
			return;
		}
		thisObj.appendKey(key);
		String sep = "";
		thisObj.append("{");
		for (int i = 0, n = list.size(); i < n; i++) {
			thisObj.append(sep);
			IFDObject<?> e = list.get(i);
			key = e.getIDorIndex();
			thisObj.append(FAIRSpecUtilities.esc(key) + ":");
			addValue(e, false, true);
			sep = ",\n";
		}
		thisObj.append("}");
	}

	@Override
	public void addList(String key, List<?> list) {
		thisObj.appendKey(key);
		addValue(list, false, false);		
	}
	
	private void addValue(Object val, boolean addKey, boolean allowSerializable) {
		if (allowSerializable && val instanceof IFDSerializableI) {
			val = serialize((IFDSerializableI) val);
		} else if (val instanceof List<?>) {
			List<?> list = (List<?>) val;
			String sep = "";
			thisObj.append("[");
			for (int i = 0, n = list.size(); i < n; i++) {
				thisObj.append(sep);
				Object v = list.get(i);
				addValue(v, false, true);
				if (sep == "") {
					sep = (v instanceof Number ? "," : ",\n");
				}
			}
			thisObj.append("]");
			return;
		} else if (val instanceof Map<?, ?>) {
			thisObj.append("{");
			Map<?, ?> map = (Map<?, ?>) val;
			String sep = "";
			for (Entry<?, ?> e : map.entrySet()) {
				thisObj.append(sep);
				thisObj.append(FAIRSpecUtilities.esc(e.getKey().toString()));
				thisObj.append(":");
				addValue(e.getValue(), false, true);
				sep = ",\n";
			}
			thisObj.append("}");
			return;
		} else if (val instanceof String) {
			val = FAIRSpecUtilities.esc((String) val);
		} else if (val instanceof byte[]) {
			val = FAIRSpecUtilities.esc(";base64," + ZipUtil.getBase64((byte[]) val));
		}
		if (addKey) {
			thisObj.appendNoEsc("value", val.toString());
		} else {
			thisObj.append(val == null ? null : val.toString());
		}
	}

	@Override
	public String closeObject() {
		int n = stack.size();
		thisObj = (n > 1 ? stack.get(n - 2) : null);
		return stack.remove(n - 1).close();
	}

	@Override
	public String getFileExt() {
		return "json";
	}

	static class Base64 {

		  //                              0         1         2         3         4         5         6
		  //                              0123456789012345678901234567890123456789012345678901234567890123
		  static String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
		  //                              41----------------------5A
		  //                                                        61----------------------7A
		  //                                                                                  30------39    
		  //                                                                                            2B  
		  //                                                                                             2F
		  //                                                 alternative "URL-SAFE"     2D and 5F       -_
		  
		  static int[] decode64 = new int[] {
		    0,0,0,0,     0,0,0,0,     0,0,0,0,     0,0,0,0,      //0x00-0x0F
		    0,0,0,0,     0,0,0,0,     0,0,0,0,     0,0,0,0,      //0x10-0x1F
		    0,0,0,0,     0,0,0,0,     0,0,0,62,    0,62,0,63,    //0x20-0x2F
		    52,53,54,55, 56,57,58,59, 60,61,0,0,   0,0,0,0,      //0x30-0x3F
		    0,0,1,2,     3,4,5,6,     7,8,9,10,    11,12,13,14,  //0x40-0x4F
		    15,16,17,18, 19,20,21,22, 23,24,25,0,  0,0,0,63,     //0x50-0x5F
		    0,26,27,28,  29,30,31,32, 33,34,35,36, 37,38,39,40,  //0x60-0x6F
		    41,42,43,44, 45,46,47,48, 49,50,51,0,  0,0,0,0,      //0x70-0x7F
		  };
		    
		//  public static void write(byte[] bytes, OutputChannel out) {
//		    SB sb = getBase64(bytes);
//		    int len = sb.length();
//		    byte[] b = new byte[1];
//		    for (int i = 0; i < len; i++) {
//		      b[0] = (byte) sb.charAt(i);
//		      out.write(b, 0, 1);
//		    }
		//  }

		  public static byte[] getBytes64(byte[] bytes) {
		    return ZipUtil.getBase64(bytes).getBytes();
		  }  
	}

}
