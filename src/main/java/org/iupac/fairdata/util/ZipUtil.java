package org.iupac.fairdata.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

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

}
