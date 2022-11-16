package org.iupac.fairdata.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDObject;

/**
 * Just a simple JSON serializer. You can write your own, if you wish.
 * 
 * @author hansonr
 *
 */
public class IFDDefaultJSONSerializer implements IFDSerializerI {

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
	public void addObject(String key, Object o) {
		thisObj.appendKey(key);
		addValue(o, false, true);
	}

	@Override
	public void addValue(Object val) {
		addValue(val, true, true);
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
			val = FAIRSpecUtilities.esc(";base64," + Base64.getBase64((byte[]) val));
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
		  private static String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
		  //                              41----------------------5A
		  //                                                        61----------------------7A
		  //                                                                                  30------39    
		  //                                                                                            2B  
		  //                                                                                             2F
		  //                                                 alternative "URL-SAFE"     2D and 5F       -_
		  
		  private static int[] decode64 = new int[] {
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
		    return getBase64(bytes).getBytes();
		  }

		  /**
		   * 
		   * @param bytes
		   * @return BASE64-encoded string, without ";base64,"
		   */
		  public static String getBase64(byte[] bytes) {
		    long nBytes = bytes.length;
		    StringBuffer sout = new StringBuffer();
		    if (nBytes == 0)
		      return sout.toString();
		    for (int i = 0, nPad = 0; i < nBytes && nPad == 0;) {
		      if (i % 75 == 0 && i != 0)
		        sout.append("\r\n");
		      nPad = (i + 2 == nBytes ? 1 : i + 1 == nBytes ? 2 : 0);
		      int outbytes = ((bytes[i++] << 16) & 0xFF0000)
		          | ((nPad == 2 ? 0 : bytes[i++] << 8) & 0x00FF00)
		          | ((nPad >= 1 ? 0 : (int) bytes[i++]) & 0x0000FF);
		      //System.out.println(Integer.toHexString(outbytes));
		      sout.append(base64.charAt((outbytes >> 18) & 0x3F));
		      sout.append(base64.charAt((outbytes >> 12) & 0x3F));
		      sout.append(nPad == 2 ? '=' : base64.charAt((outbytes >> 6) & 0x3F));
		      sout.append(nPad >= 1 ? '=' : base64.charAt(outbytes & 0x3F));
		    }
		    return sout.toString();
		  }

		  //Note: Just a simple decoder here. Nothing fancy at all
		  //      Because of the 0s in decode64, this is not a VERIFIER
		  //      Rather, it may decode even bad Base64-encoded data
		  //
		  // Bob Hanson 4/2007
		  
		  public static byte[] decodeBase64(String strBase64) {
		    int nBytes = 0;
		    int ch;
		    int pt0 = strBase64.indexOf(";base64,") + 1;
		    if (pt0 > 0)
		      pt0 += 7;
		    char[] chars64 = strBase64.toCharArray();
		    int len64 = chars64.length;
		    if (len64 == 0)
		      return new byte[0];
		    for (int i = len64; --i >= pt0;)
		      nBytes += ((ch = chars64[i] & 0x7F) == 'A' || decode64[ch] > 0 ? 3 : 0);
		    nBytes = nBytes >> 2;
		    byte[] bytes = new byte[nBytes];
		    int offset = 18;
		    for (int i = pt0, pt = 0, b = 0; i < len64; i++) {
		      if (decode64[ch = chars64[i] & 0x7F] > 0 || ch == 'A' || ch == '=') {
		        b |= decode64[ch] << offset;
		        //System.out.println(chars64[i] + " " + decode64[ch] + " " + offset + " " + Integer.toHexString(b));
		        offset -= 6;
		        if (offset < 0) {
		          bytes[pt++] = (byte) ((b & 0xFF0000) >> 16);
		          if (pt < nBytes)
		            bytes[pt++] = (byte) ((b & 0xFF00) >> 8);
		          if (pt < nBytes)
		            bytes[pt++] = (byte) (b & 0xFF);
		          offset = 18;
		          b =  0;
		        }
		      }
		    }
		    return bytes;
		  }  
	}


}
