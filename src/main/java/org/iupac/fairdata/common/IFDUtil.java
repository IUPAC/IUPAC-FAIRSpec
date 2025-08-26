package org.iupac.fairdata.common;

public class IFDUtil {

	/**
	 * Create a sort key that keeps numbers in order
	 * no matter where they are in an identifier.
	 * 
	 * @param id
	 * @return
	 */
	public static String getNumericalSortKey(String id) {
		int[] ret = new int[2];
		id = id.toUpperCase();
		int val = getBestNumber(id, ret);
		if (val == 0)
			return id + "__________";
		String sval = "" + val;
		sval = ("0000000000" + sval).substring(sval.length());
		return id.substring(0, ret[0]) + sval + id.substring(ret[1]);
	}
	
//	static {
//		System.out.println(getNumericalSortKey("testing"));
//		System.out.println(getNumericalSortKey("Compound 33"));
//		System.out.println(getNumericalSortKey("COmpound 33a"));
//		System.out.println(getNumericalSortKey("15b"));
//		System.out.println(getNumericalSortKey("15c"));
//
//// -->		
////		TESTING__________
////		COMPOUND 0000000033
////		COMPOUND 0000000033A
////		0000000015B
////		0000000015C
//	}

	private static int getBestNumber(String id, int[] ret) {
		int pt1 = -1, n = id.length();
		int val = 0;
		for (int i = 0; i < n; i++) {
			int c = (int) id.charAt(i);
			if (c >= 48 && c <= 57) {
				if (pt1 < 0)
					pt1 = i;
				val = val * 10 + (c - 48);
			} else {
				if (pt1 >= 0) {
					n = i;
					break;
				}
			}
		}
		ret[0] = pt1;
		ret[1] = n;
		return val;
	}

	/**
	 * parse very simple positive integers; may have continuance after but not before
	 * 
	 * @param substring
	 * @return
	 */
	public static int parsePositiveInt(String s) {
		int n = Math.min(s.length(), 9);
		int i = -1;
		int val = 0;
		while (++i < n) {
			int c = (int) s.charAt(i);
			if (c < 48 || c > 57) {
					break;
			}
			val = val * 10 + (c - 48);
		}
		return (i == 0 ? Integer.MIN_VALUE : val);
	}

	/**
	 * parse very simple positive integers followed by [a-z]*; 
	 * may have continuance after but not before
	 * 
	 * TODO: could check for ' 
	 * 
	 * @param substring
	 * @return
	 */
	public static String parsePositiveIntABC(String s) {
		int n = Math.min(s.length(), 9);
		int i = -1;
		int val = 0;
		boolean haveInt = false;
		String id = "";
		while (++i < n) {
			char c = s.charAt(i);
			if (!haveInt) {
				if (c >= '0' && c <= '9') {
					val = val * 10 + (c - 48);
					continue;
				}
				if (val == 0)
					return "";
				id = "" + val;
				haveInt = true;
			}
			if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')) {
				break;
			}
			id += c;			
		}
		return id;
	}

	/**
	 * just way simpler code than Character.isDigit(ch);
	 * 
	 * @param ch
	 * @return
	 */
	public static boolean isDigit(char ch) {
		int c = ch;
		return (48 <= c && c <= 57);
	}


	public static String getShortFileName(String fileName) {
		int pt = fileName.lastIndexOf("/");
		return (pt >= 0 ? fileName.substring(pt + 1) : fileName);
	}

}