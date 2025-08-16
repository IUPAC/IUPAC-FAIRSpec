package org.nmrml.parser;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * This could be expanded as necessary.
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 *
 */
public class NMRmlFAIRSpecUtil {

	public static double[][] getRealImaginaryFromBase64Complex128(String sdata) {
		byte[] bytes = Base64.getDecoder().decode(sdata);
		ByteBuffer b = ByteBuffer.wrap(bytes);
		if ((bytes.length % 16) != 0) {
			throw new RuntimeException("NMRmlFAIRSpecUtil byte length not multiple of 16 " + bytes.length);
		}
		try {
			int n = bytes.length / 16;
			double[][] values = new double[2][n];
			for (int i = 0, dpt = 0; i < n; i++) {
				values[0][dpt] = b.getDouble();
				values[1][dpt] = b.getDouble();
				dpt++;
			}
			return values;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
