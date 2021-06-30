package org.iupac.fairspec.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;

import javajs.util.PT;

/**
 * Just a simple JSON serializer. You can write your own, if you wish.
 * 
 * @author hansonr
 *
 */
public class IFSDefaultJSONSerializer implements IFSSerializerI {

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
			appendKey(key).append(PT.esc(val));
		}
		
		public void appendNoEsc(String key, String val) {
			appendKey(key).append(val);
		}
		
		public StringBuffer appendKey(String key) {
			if (sb.length() > 1)
				sb.append(",\n");
			sb.append(PT.esc(key)).append(":");
			return sb;
		}

	}

	@Override
	public String serialize(IFSSerializableI obj) {
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
		addValue(o, false);
	}

	@Override
	public void addValue(Object val) {
		addValue(val, true);
	}
	
	private void addValue(Object val, boolean addKey) {
		if (val instanceof IFSSerializableI) {
			val = serialize((IFSSerializableI) val);
		} else if (val instanceof List<?>) {
			List<?> list = (List<?>) val;
			String sep = "";
			thisObj.append("[");
			for (int i = 0, n = list.size(); i < n; i++) {
				thisObj.append(sep);
				addValue(list.get(i), false);
				sep = ",\n";
			}
			thisObj.append("]");
			return;
		} else if (val instanceof Map<?, ?>) {
			thisObj.append("{");
			Map<?, ?> map = (Map<?, ?>) val;
			String sep = "";
			for (Entry<?, ?> e : map.entrySet()) {
				thisObj.append(sep);
				thisObj.append(PT.esc(e.getKey().toString()));
				thisObj.append(":");
				addValue(e.getValue(), false);
				sep = ",\n";
			}
			thisObj.append("}");
			return;
		} else if (val instanceof String) {
			val = PT.esc((String) val);
		}
		if (addKey) {
			thisObj.appendNoEsc("value", val.toString());
		} else {
			thisObj.append(val.toString());
		}
	}

	@Override
	public String closeObject() {
		int n = stack.size();
		thisObj = (n > 1 ? stack.get(n - 2) : null);
		return stack.remove(n - 1).close();
	}

}
