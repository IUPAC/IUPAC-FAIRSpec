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
		id = id.toUpperCase();
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
		if (val == 0)
			return id + "__________";
		String sval = "" + val;
		sval = ("0000000000" + sval).substring(sval.length());
		return id.substring(0, pt1) + sval + id.substring(n);
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
	 * just way simpler code than Character.isDigit(ch);
	 * 
	 * @param ch
	 * @return
	 */
	public static boolean isDigit(char ch) {
		int c = ch;
		return (48 <= c && c <= 57);
	}
	
}