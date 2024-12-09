package org.iupac.fairdata.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer.Base64;

public class ZipUtil {

	public static boolean isGzipS(InputStream is) {
		return isGzipB(getMagic(is, 2));
	}

	public static boolean isGzipB(byte[] bytes) {
		return (bytes != null && bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B);
	}

	  private static byte[] b264;

	  public static boolean isTar(BufferedInputStream bis) {
	      byte[] bytes = getMagic(bis, 264);
	      // check for ustar<00>
	      return (bytes[0] != 0 
	          && (bytes[257] & 0xFF) == 0x75
	          && (bytes[258] & 0xFF) == 0x73
	          && (bytes[259] & 0xFF) == 0x74
	          && (bytes[260] & 0xFF) == 0x61
	          && (bytes[261] & 0xFF) == 0x72
	          );
	  }

	/**
	 * Check for a ZIP input stream - starting with "PK<03><04>"
	 * 
	 * @param is
	 * @return true if a ZIP stream
	 */
	public static boolean isZipS(InputStream is) {
		return isZipB(getMagic(is, 4));
	}

	public static boolean isZipB(byte[] bytes) {
		return (bytes.length >= 4 && bytes[0] == 0x50 // PK<03><04>
				&& bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04);
	}

	public static byte[] getMagic(InputStream is, int n) {
		byte[] abMagic = (n > 264 ? new byte[n] : b264 == null ? (b264 = new byte[264]) : b264);
		try {
			is.mark(n + 1);
			int i = is.read(abMagic, 0, n);
			if (i < n) {
				// ensure
				abMagic[0] = abMagic[257] = 0;
			}
		} catch (IOException e) {
		}
		try {
			is.reset();
		} catch (IOException e) {
		}
		return abMagic;
	}

	
	public static TarArchiveInputStream newTarGZInputStream(InputStream is) throws IOException {
		return new TarArchiveInputStream(new GZIPInputStream(is, 512));
	}

	public static TarArchiveInputStream newTarInputStream(InputStream is) throws IOException {
		return new TarArchiveInputStream(is);
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
	      sout.append(Base64.base64.charAt((outbytes >> 18) & 0x3F));
	      sout.append(Base64.base64.charAt((outbytes >> 12) & 0x3F));
	      sout.append(nPad == 2 ? '=' : Base64.base64.charAt((outbytes >> 6) & 0x3F));
	      sout.append(nPad >= 1 ? '=' : Base64.base64.charAt(outbytes & 0x3F));
	    }
	    return sout.toString();
	  }

	/**
	 * Note: Just a simple decoder here. Nothing fancy at all Because of the 0s in
	 * decode64, this is not a VERIFIER Rather, it may decode even bad
	 * Base64-encoded data
	 * 
	 * Bob Hanson 4/2007
	 * 
	 * @param strBase64
	 * @return
	 */
	
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
	      nBytes += ((ch = chars64[i] & 0x7F) == 'A' || Base64.decode64[ch] > 0 ? 3 : 0);
	    nBytes = nBytes >> 2;
	    byte[] bytes = new byte[nBytes];
	    int offset = 18;
	    for (int i = pt0, pt = 0, b = 0; i < len64; i++) {
	      if (Base64.decode64[ch = chars64[i] & 0x7F] > 0 || ch == 'A' || ch == '=') {
	        b |= Base64.decode64[ch] << offset;
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
