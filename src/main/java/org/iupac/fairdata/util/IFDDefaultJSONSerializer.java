package org.iupac.fairdata.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDFindingAid;

import javajs.util.PT;

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
	public String serialize(IFDSerializableI obj) {
		if (obj instanceof IFDAssociation) {
		  obj.serialize(this);
		  return "";
		} else {
			openObject();
			obj.serialize(this);
			return closeObject();			
		}
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
		if (val instanceof IFDSerializableI) {
			val = serialize((IFDSerializableI) val);
		} else if (val instanceof List<?>) {
			List<?> list = (List<?>) val;
			String sep = "";
			thisObj.append("[");
			for (int i = 0, n = list.size(); i < n; i++) {
				thisObj.append(sep);
				Object v = list.get(i);
				addValue(v, false);
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
			if (val != null && val.toString().indexOf('\0') >= 0)
				System.out.println("???");
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

	@Override
	public String createSerialization(IFDFindingAid findingAid, File targetDir, String rootName, List<Object> products) throws IOException {
		// subclasses should be able to use this directly with no changes.
		String s = serialize(findingAid);
		if (targetDir == null)
			return s;
		String aidName = "_IFD_findingaid." + getFileExt();
		if (products != null) {
			findingAid.setPropertyValue(IFDConst.IFD_PROP_COLLECTIONSET_REF, null);
			findingAid.setPropertyValue(IFDConst.IFD_PROP_COLLECTIONSET_LEN, null);
			// byte[] followed by entry name
			products.add(0, s.getBytes());
			products.add(1, aidName);
			String zipName = rootName + "_IFD_collection.zip";
			String path = targetDir + "/" + zipName;
			long len = IFDUtilities.zip(path, targetDir.toString().length() + 1, products);
			findingAid.setPropertyValue(IFDConst.IFD_PROP_COLLECTIONSET_REF, zipName);
			findingAid.setPropertyValue(IFDConst.IFD_PROP_COLLECTIONSET_LEN, len);
			products.remove(1);
			products.remove(0);
			// update external finding aid
			s = serialize(findingAid);
		}
		String faPath = targetDir + "/" + rootName + aidName;
		IFDUtilities.writeBytesToFile(s.getBytes(), new File(faPath));
		return s;
	}

}
