/*
 * Some portions of this file have been modified by Robert Hanson hansonr.at.stolaf.edu 2012-2017
 * for use in SwingJS via transpilation into JavaScript using Java2Script.
 *
 * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.integratedgraphics.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import swingjs.jzlib.CRC32;
import swingjs.jzlib.Inflater;
import swingjs.jzlib.InflaterInputStream;


/**
 * Modified by Bob Hanson for compatibility with jzlib and allowance for
 * improper ZIP construction for zero-length files. Incorporates package-only
 * constants from java.util.zip package.
 * 
 * This continuing saga involves ZIP files with 24-byte data descriptions (!?)
 * and empty data blocks for 0-length Bruker title files (from nmrXiv).
 * Both Windows Explorer and 7-Zip complain about these files.
 * 
 * 
 * @author David Connelly
 */
public class ZipInputStream extends InflaterInputStream {
	
    /*
     * Header signatures
     */
    private final static long LOCSIG = 0x04034b50L;   // "PK\003\004"
    private final static long EXTSIG = 0x08074b50L;   // "PK\007\008"

    /*
     * Header sizes in bytes (including signatures)
     */
    private static final int LOCHDR = 30;       // LOC header size
    private static final int EXTHDR = 16;       // EXT header size

    /*
     * Local file (LOC) header field offsets
     */
    private static final int LOCFLG = 6;        // general purpose bit flag
    private static final int LOCHOW = 8;        // compression method
    //private static final int LOCTIM = 10;       // modification time
    private static final int LOCCRC = 14;       // uncompressed file crc-32 value
    private static final int LOCSIZ = 18;       // compressed size
    private static final int LOCLEN = 22;       // uncompressed size
    private static final int LOCNAM = 26;       // filename length
    private static final int LOCEXT = 28;       // extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    private static final int EXTCRC = 4;        // uncompressed file crc-32 value
    private static final int EXTSIZ = 8;        // compressed size
    private static final int EXTLEN = 12;       // uncompressed size

    /*
     * ZIP64 constants
     */
    private static final int  ZIP64_EXTHDR = 24;           // EXT header size
    private static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID
    private static final long ZIP64_MAGICVAL = 0xFFFFFFFFL;


    /*
     * Zip64 Extra local (EXT) header field offsets
     */
    private static final int  ZIP64_EXTCRC = 4;       // uncompressed file crc-32 value
    private static final int  ZIP64_EXTSIZ = 8;       // compressed size, 8-byte
    private static final int  ZIP64_EXTLEN = 16;      // uncompressed size, 8-byte

    /*
     * Language encoding flag EFS from ZipConstants64
     */
    private static final int EFS = 0x800;       // If this bit is set the filename and
                                        // comment fields for this file must be
                                        // encoded using UTF-8.

  private ZipEntry entry;
  private int flag;
  private CRC32 crc = new CRC32();
  private long remaining;
  private byte[] tmpbuf = new byte[512];

  private static final int STORED = ZipEntry.STORED;
  private static final int DEFLATED = ZipEntry.DEFLATED;

//  private boolean closed = false; ! SwingJS no - InflaterInputStream has this
  // this flag is set to true after EOF has reached for
  // one entry
  private boolean entryEOF = false;

  private String zc;

  /**
   * Check to make sure that this stream has not been closed
   * 
   * @throws IOException
   */
  private void ensureOpen() throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
  }

	final protected static int GET_BYTE_STREAM_FOR_ZIP = -2;
	final protected static int GET_BYTE_STREAM_OR_NULL = -1;

	/**
   * Creates a new ZIP input stream.
   * 
   * SwingJS - probably strips off any BufferedInputStreams and ends up with a
   * raw ByteArrayInputStream.
   * 
   * <p>
   * The UTF-8 {@link java.nio.charset.Charset charset} is used to decode the
   * entry names.
   * 
   * @param in
   *        the actual input stream
   */
  public ZipInputStream(InputStream in) {
	super(new PushbackInputStream(in, 512), newInflater(), 512, true);
     String charset = "UTF-8";
     zc = charset;
  }

  private static Inflater newInflater() {
    return (Inflater) new Inflater().init(0, true);
  }

//  private byte[] byteTest = new byte[] { 0x20 };

	private void initEntry() {
		crc.reset();
		inflater = new Inflater().init(0, true);
		if (entry.getMethod() == STORED) {
			remaining = entry.getSize();
		}
		entryEOF = false;
	}

/**
   * Closes the current ZIP entry and positions the stream for reading the next
   * entry.
   * 
   * @exception ZipException
   *            if a ZIP file error has occurred
   * @exception IOException
   *            if an I/O error has occurred
   */
  public void closeEntry() throws IOException {
    ensureOpen();
    while (read(tmpbuf, 0, tmpbuf.length) != -1) {
      // ok
    }
    entryEOF = true;
  }

  /**
   * Returns 0 after EOF has reached for the current entry data, otherwise
   * always return 1.
   * <p>
   * Programs should not count on this method to return the actual number of
   * bytes that could be read without blocking.
   * 
   * @return 1 before EOF and 0 after EOF has reached for current entry.
   * @exception IOException
   *            if an I/O error occurs.
   * 
   */
  @Override
  public int available() throws IOException {
    ensureOpen();
    return (entryEOF ? 0 : 1);
  }

  /**
   * Reads from the current ZIP entry into an array of bytes. If
   * <code>len</code> is not zero, the method blocks until some input is
   * available; otherwise, no bytes are read and <code>0</code> is returned.
   * 
   * @param b
   *        the buffer into which the data is read
   * @param off
   *        the start offset in the destination array <code>b</code>
   * @param len
   *        the maximum number of bytes read
   * @return the actual number of bytes read, or -1 if the end of the entry is
   *         reached
   * @exception NullPointerException
   *            if <code>b</code> is <code>null</code>.
   * @exception IndexOutOfBoundsException
   *            if <code>off</code> is negative, <code>len</code> is negative,
   *            or <code>len</code> is greater than <code>b.length - off</code>
   * @exception ZipException
   *            if a ZIP file error has occurred
   * @exception IOException
   *            if an I/O error has occurred
   */
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    ensureOpen();
    if (off < 0 || len < 0 || off > b.length - len) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    if (entry == null) {
      return -1;
    }
    switch (entry.getMethod()) {
    case DEFLATED:
      len = readInf(b, off, len);
      if (len == -1) {
        readEnd(entry);
        entryEOF = true;
        entry = null;
      } else {
        crc.update(b, off, len);
      }
      return len;
    case STORED:
      if (remaining <= 0) {
        entryEOF = true;
        entry = null;
        return -1;
      }
      if (len > remaining) {
        len = (int) remaining;
      }
      len = in.read(b, off, len); 
      if (len == -1) {
        throw new ZipException("unexpected EOF");
      }
      crc.update(b, off, len);
      remaining -= len;
      if (remaining == 0 && entry.getCrc() != crc.getValue()) {
        throw new ZipException("invalid entry CRC (expected 0x"
            + Long.toHexString(entry.getCrc()) + " but got 0x"
            + Long.toHexString(crc.getValue()) + ")");
      }
      return len;
    default:
      throw new ZipException("invalid compression method");
    }
  }

  /**
   * Skips specified number of bytes in the current ZIP entry.
   * 
   * @param n
   *        the number of bytes to skip
   * @return the actual number of bytes skipped
   * @exception ZipException
   *            if a ZIP file error has occurred
   * @exception IOException
   *            if an I/O error has occurred
   * @exception IllegalArgumentException
   *            if n < 0
   */
  @Override
  public long skip(long n) throws IOException {
      throw new IllegalArgumentException("ZipInputStream.skip not implemented");
	  // unimplemented
//    if (n < 0) {
//      throw new IllegalArgumentException("negative skip length");
//    }
//    ensureOpen();
//    int max = (int) Math.min(n, Integer.MAX_VALUE);
//    int total = 0;
//    while (total < max) {
//      int len = max - total;
//      if (len > tmpbuf.length) {
//        len = tmpbuf.length;
//      }
//      len = read(tmpbuf, 0, len);
//      if (len == -1) {
//        entryEOF = true;
//        break;
//      }
//      total += len;
//    }
//    return total;
  }

  /**
   * Closes this input stream and releases any system resources associated with
   * the stream.
   * 
   * @exception IOException
   *            if an I/O error has occurred
   */
  @Override
  public void close() throws IOException {
    if (!closed) {
      super.close();
      closed = true;
    }
  }

  private byte[] b = new byte[256];

	/*
	 * Reads local file (LOC) header for next entry.
	 */
	private ZipEntry readLOC() throws IOException {
		try {
			readFully(tmpbuf, 0, LOCHDR);
		} catch (EOFException e) {
			return null;
		}
		if (get32(tmpbuf, 0) != LOCSIG) {
			System.arraycopy(tmpbuf, 8, tmpbuf, 0, 22);
			readFully(tmpbuf, 22, 8);
			if (get32(tmpbuf, 0) != LOCSIG) {
				// end of file PK 01 02
				return null;
			}
		}
		
		// get flag first, we need check EFS.
		flag = get16(tmpbuf, LOCFLG);
		// get the entry name and create the ZipEntry first
		int len = get16(tmpbuf, LOCNAM);
		int blen = b.length;
		if (len > blen) {
			do
				blen = blen * 2;
			while (len > blen);
			b = new byte[blen];
		}
		readFully(b, 0, len);
		// Force to use UTF-8 if the EFS bit is ON, even the cs is NOT UTF-8
		String name = (((flag & EFS) != 0) ? toStringUTF8(b, len) : toStringb2(b, len));
		ZipEntry e = createZipEntry(name);
		// now get the remaining fields for the entry
		if ((flag & 1) == 1) {
			throw new ZipException("encrypted ZIP entry not supported");
		}
		e.setMethod(get16(tmpbuf, LOCHOW));
		boolean readSizes = ((flag & 8) != 8 || e.getMethod() != DEFLATED);
		// flag 8 means size is unknown at compression time
			// leave crc and sizes -1
			/* "Data Descriptor" present */
		// still read the 0 if  (e.getMethod() == DEFLATED)
				// DO NOT throw new ZipException
				//System.out.println("ZipInputStream: only DEFLATED entries can have EXT descriptor?? - " + e.getName());
		if (readSizes) {
			e.setCrc(get32(tmpbuf, LOCCRC));
			e.setCompressedSize(get32(tmpbuf, LOCSIZ));
			e.setSize(get32(tmpbuf, LOCLEN));
		}
		len = get16(tmpbuf, LOCEXT);
		if (len > 0) {
			byte[] bb = new byte[len + 4];
			readFully(bb, 0, len);
			//e.setExtra(bb);
			// extra fields are in "HeaderID(2)DataSize(2)Data... format
			if (e.getCompressedSize() == ZIP64_MAGICVAL || e.getSize() == ZIP64_MAGICVAL) {
				int off = 0;
				while (off + 4 < len) {
					int sz = get16(bb, off + 2);
					if (get16(bb, off) == ZIP64_EXTID) {
						off += 4;
						// LOC extra zip64 entry MUST include BOTH original and
						// compressed file size fields
						if (sz < 16 || (off + sz) > len) {
							// Invalid zip64 extra fields, simply skip. Even it's
							// rare, it's possible the entry size happens to be
							// the magic value and it "accidnetly" has some bytes
							// in extra match the id.
							return e;
						}
						e.setSize(get64(bb, off));
						e.setCompressedSize(get64(bb, off + 8));
						break;
					}
					off += (sz + 4);
				}
			} else {
				// check for NFDI 0-length file
				readFully(tmpbuf, 0, 4);
				boolean isEmptyFile = (get32(tmpbuf, 0) == EXTSIG); 
				unread(tmpbuf, 4, 4);
				if (isEmptyFile) {
					e.setSize(0);
					eof = true;
					return e;
				}
			}
		}
		eof = false;
		return e;
	}

  private String toStringUTF8(byte[] b2, int len) {
    try {
      return new String(b2, 0, len, zc);
    } catch (UnsupportedEncodingException e) {
      return toStringb2(b2, len);
    }
  }

  private String toStringb2(byte[] b2, int len) {
    return new String(b2, 0, len);
  }

  /**
   * Creates a new <code>ZipEntry</code> object for the specified entry name.
   * 
   * @param name
   *        the ZIP file entry name
   * @return the ZipEntry just created
   */
  protected ZipEntry createZipEntry(String name) {
    return new ZipEntry(name);
  }

	/*
	 * Reads end of deflated entry as well as EXT descriptor if present.
	 */
	private void readEnd(ZipEntry e) throws IOException {
		int n = inflater.getAvailIn();
		if (n > 0) {
			unread(buf, len, n);
			this.eof = false;
		}
		if ((flag & 8) == 8) {
			/* "Data Descriptor" present */
			if (inflater.getTotalOutL() > ZIP64_MAGICVAL || inflater.getTotalInL() > ZIP64_MAGICVAL) {
				// ZIP64 format
				readFully(tmpbuf, 0, ZIP64_EXTHDR);
				long sig = get32(tmpbuf, 0);
				if (sig != EXTSIG) { // no EXTSIG present
					e.setCrc(sig);
					e.setCompressedSize(get64(tmpbuf, ZIP64_EXTSIZ - ZIP64_EXTCRC));
					e.setSize(get64(tmpbuf, ZIP64_EXTLEN - ZIP64_EXTCRC));
					unread(tmpbuf, ZIP64_EXTHDR - 1, ZIP64_EXTCRC);
				} else {
					e.setCrc(get32(tmpbuf, ZIP64_EXTCRC));
					e.setCompressedSize(get64(tmpbuf, ZIP64_EXTSIZ));
					e.setSize(get64(tmpbuf, ZIP64_EXTLEN));
				}
			} else {
				java.util.zip.ZipInputStream x;
				readFully(tmpbuf, 0, EXTHDR);
				long sig = get32(tmpbuf, 0);
				if (sig != EXTSIG) { // no EXTSIG present
					e.setCrc(sig);
					e.setCompressedSize(get32(tmpbuf, EXTSIZ - EXTCRC));
					e.setSize(get32(tmpbuf, EXTLEN - EXTCRC));
					unread(tmpbuf, EXTHDR - 1, EXTCRC);
				} else { // zip64
					e.setCrc(get32(tmpbuf, EXTCRC));
					e.setCompressedSize(get32(tmpbuf, EXTSIZ));
					e.setSize(get32(tmpbuf, EXTLEN));
					readFully(tmpbuf, 0, 8); // BH			
				}
			}
		}
		if (e.getSize() != inflater.getTotalOutL()) {
			if (e.getSize() == 0) // BH
				e.setSize(inflater.getTotalOutL());
			else
				throw new ZipException(
					"invalid entry size (expected " + e.getSize() + " but got " + inflater.getTotalOutL() + " bytes)");
		}
		if (e.getCompressedSize() != inflater.getTotalInL()) {
			throw new ZipException("invalid entry compressed size (expected " + e.getCompressedSize() + " but got "
					+ inflater.getTotalInL() + " bytes)");
		}
		if (e.getCrc() != crc.getValue()) {
			throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(e.getCrc()) + " but got 0x"
					+ Long.toHexString(crc.getValue()) + ")");
		}
	}

  private void unread(byte[] b, int len, int n) throws IOException {
	((PushbackInputStream) in).unread(b, len - n, n);
  }

/*
   * Reads bytes, blocking until all bytes are read.
   */
  private void readFully(byte[] b, int off, int len) throws IOException {
    while (len > 0) {
      int n = in.read(b, off, len);
      if (n == -1) {
        throw new EOFException();
      }
      off += n;
      len -= n;
    }
  }

  /*
   * Fetches unsigned 16-bit value from byte array at specified offset.
   * The bytes are assumed to be in Intel (little-endian) byte order.
   */
  private static final int get16(byte b[], int off) {
    return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
  }

  /*
   * Fetches unsigned 32-bit value from byte array at specified offset.
   * The bytes are assumed to be in Intel (little-endian) byte order.
   */
  private static final long get32(byte b[], int off) {
    return (get16(b, off) | ((long) get16(b, off + 2) << 16)) & 0xffffffffL;
  }

  /*
   * Fetches signed 64-bit value from byte array at specified offset.
   * The bytes are assumed to be in Intel (little-endian) byte order.
   */
  private static final long get64(byte b[], int off) {
    return get32(b, off) | (get32(b, off + 4) << 32);
  }
  
	/**
	 * SwingJS addition
	 * 
	 * Reads the next ZIP file entry and positions the stream at the beginning of
	 * the entry data.
	 * 
	 * @return the next ZIP file entry, or null if there are no more entries
	 * @exception ZipException if a ZIP file error has occurred
	 * @exception IOException  if an I/O error has occurred
	 */
	public ZipEntry getNextEntry() throws IOException {
		ensureOpen();
		if (entry != null) {
			closeEntry();
		}
		if ((entry = readLOC()) == null) {
			return null;
		}
		initEntry();
		return entry;
	}

}
