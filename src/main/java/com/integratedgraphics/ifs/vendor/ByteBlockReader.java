package com.integratedgraphics.ifs.vendor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A reader that can process blocks that contain an initial byte length followed
 * by a series of bytes.
 * 
 * @author hansonr
 *
 */
public class ByteBlockReader {

	/**
	 * set to true for debugging
	 */
	protected static boolean testing = false;

	/**
	 * when testing, show integers read
	 */
	protected static boolean showInts = false;

	/**
	 * when testing, show the characters associated with integers
	 */
	protected static boolean showChars = false;

	/**
	 * must be able to use available() -- so not http or https
	 */
	private InputStream in;

	/**
	 * byte array for ByteBuffer
	 */
	private byte[] buf = new byte[1];

	/**
	 * byte order
	 */
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	/**
	 * current input stream position
	 */
	private long position = 0;

	/**
	 * ByteBuffer for processing int, double
	 */
	private ByteBuffer buffer;

	/**
	 * mark for input stream
	 */
	private long mark;

	public ByteBlockReader(InputStream in) throws IOException {
		this.in = in;
		try {
			in.reset();
		} catch (Exception e) {
			// ignore
		}
	}

	protected void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		if (buffer != null)
			buffer.order(byteOrder);
	}

	/**
	 * Get the current buffer
	 * 
	 * @return this.buf
	 */
	public byte[] getBuf() {
		return buf;
	}

	/**
	 * Get the current input stream position
	 * 
	 * @return
	 */
	protected long readPosition() {
		return position;
	}

	/**
	 * Read bytes into the ByteBuffer and byte[] array from the input stream.
	 * 
	 * @param len number of bytes to read
	 * @return current buffer
	 * @throws IOException
	 */
	protected byte[] setBuf(int len) throws IOException {
		if (buf.length < len) {
			buf = new byte[len << 1];
			buffer = ByteBuffer.wrap(buf);
			buffer.order(byteOrder);
		}
		buffer.rewind();
		int n = in.read(buf, 0, len);
		if (n != len)
			throw new java.io.EOFException("Tried to read " + len + " bytes but read only " + n);
		position += len;
		return buf;
	}

	/**
	 * Get the number of bytes available in the input stream, which is presumed to
	 * be a file or byte array input stream so as not to block. This method would
	 * not work properly with URL connections.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int readAvailable() throws IOException {
		return in.available();
	}

	/**
	 * Mark the input stream.
	 * 
	 * @param n
	 */
	protected void markIn(int n) {
		mark = position;
		in.mark(n);
	}

	/**
	 * Reset the input stream.
	 * 
	 * @throws IOException
	 */
	protected void resetIn() throws IOException {
		position = mark;
		in.reset();
	}

	/**
	 * Skip a given number of bytes in the input stream.
	 * 
	 * @param n
	 * @throws IOException
	 */
	protected void skipIn(int n) throws IOException {
		position += in.skip(n);
		if (testing)
			System.out.println("skip from " + (position - n) + " by " + n + " to " + position);
	}

	protected void skipInTo(long pt) throws IOException {
		skipIn((int)(pt - readPosition()));
	}

	protected long readPointer() throws IOException {
		int len = readInt(); // to next spectrum
		long pos = readPosition();
		return pos + len;
	}



	/**
	 * Read a 4-byte magic number header.
	 * 
	 * @param magicNumber
	 * @return true if successful.
	 * @throws IOException
	 */
	protected boolean checkMagicNumber(int magicNumber) throws IOException {
		int nAvail = readAvailable();
		if (nAvail < 4)
			return false;
		return (peekInt() == magicNumber);
	}

	/**
	 * Read the next byte value in the input stream.
	 * 
	 * @return a byte value
	 * @throws IOException
	 */
	protected int readByte() throws IOException {
		position++;
		int b = in.read();
		if (testing)
			System.out.println("readByte " + b + " = 0x" + Integer.toHexString(b).toUpperCase());
		return b;
	}

	/**
	 * Read the next two bytes of the input stream as a short value.
	 * 
	 * @return a short value
	 * @throws IOException
	 */
	protected int readShort() throws IOException {
		setBuf(2);
		int i = buffer.getShort();
		if (testing)
			System.out.println("readShort " + i + " = 0x" + Integer.toHexString(i).toUpperCase());
		return i;
	}

	/**
	 * Read the next four bytes of the input stream as a 32-bit integer value.
	 * 
	 * @return a short value
	 * @throws IOException
	 */
	protected int readInt() throws IOException {
		setBuf(4);
		int i = buffer.getInt();
		if (testing && showInts) {
			String s = (showChars ? new String(getBuf(), 0, 4) : "");
			System.out.println("readInt " + (position - 4) + ": " + i + " = 0x" + Integer.toHexString(i).toUpperCase()
					+ " -> " + (position + i) + " " + s);
		}
		return i;
	}

	/**
	 * Read the next eight bytes of the input stream as a 64-bit long value.
	 * 
	 * @return a long value
	 * @throws IOException
	 */
	protected long readLong() throws IOException {
		setBuf(8);
		return buffer.getLong();
	}

	/**
	 * Read the next eight bytes of the input stream as a 64-bit double value.
	 * 
	 * @return a double value
	 * @throws IOException
	 */
	protected double readDouble() throws IOException {
		setBuf(8);
		double d = buffer.getDouble();
		buffer.position(buffer.position() - 8);
		long l = buffer.getLong();
		if (testing)
			System.out.println("ReadDouble 0x" + Long.toHexString(l).toUpperCase() + "\t" + d);
		return d;
	}

	/**
	 * Read the next len bytes from the input stream into a byte array starting at
	 * the offset position of that array.
	 * 
	 * @param b      the target array
	 * @param offset the offset in that array
	 * @param len    the number of bytes to transfer
	 * @return the actual number of bytes transferred
	 * 
	 * @throws IOException
	 */
	public int read(byte[] b, int offset, int len) throws IOException {
		int n = in.read(b, offset, len);
		position += n;
		return n;
	}

	/**
	 * Read a simple String of known length from the input stream.
	 * 
	 * @param len the known length of the string.
	 * @return the string
	 * @throws IOException
	 */
	protected String readSimpleString(int len) throws IOException {
		String s = new String(setBuf(len), 0, len);
		if (testing)
			System.out.println("readString " + s);
		return s;
	}

	/**
	 * Read an ASCII String that has its length indicated as an integer just before
	 * it.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected String readLenString() throws IOException {
		return readSimpleString(readInt());
	}

	/**
	 * Read a UTF-16 String that has its length indicated as an integer just before
	 * it, converting 2-, 3-, and 4-byte sequences properly.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected String readUTF16String() throws IOException {
		long p = readPosition();
		int len = readInt();
		setBuf(len);
		String s = new String(buf, 0, len, "utf16");
		if (testing)
			System.out.println("ReadUTF16 " + p + "(" + len + "): " + s);
		return s;
	}

	protected void readInts(int n) throws IOException {
		for (int i = 0; i < n; i++) {
			if (testing)
				System.out.print(i + " ");
			readInt();
		}
	}

	/**
	 * Check the next integer position; the input stream is marked and reset.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected int peekInt() throws IOException {
		markIn(4);
		int n = readInt();
		resetIn();
		if (testing)
			System.out.println("peekInt " + n);
		return n;
	}

	/**
	 * Read out the nexts n integers, resetting the input stream after doing so.
	 * 
	 * @param n
	 * @throws IOException
	 */
	protected void peekInts(int n) throws IOException {
		boolean l = testing;
		testing = true;
		System.out.println("---peek " + readPosition());
		markIn(n * 4);
		readInts(n);
		resetIn();
		System.out.println("---reset " + readPosition());
		testing = l;
	}

	/**
	 * Find a double value in the input stream within a given distance after the
	 * current point.
	 * 
	 * @param d      double to find
	 * @param length maximum distance to check or -1 for in.available()
	 * @param isAll  true to report all
	 * @return found position or -1
	 * @throws IOException
	 */
	protected int findDouble(double d, int length, boolean isAll) throws IOException {
		long l = Double.doubleToLongBits(d);
		int high = (int) ((l >> 32) & 0xFFFFFFFF);
		int low = (int) (l & 0xFFFFFFFF);
		return findIn(new int[] { high, low }, length, isAll);
	}

	/**
	 * Find an approximate double value in the input stream within a given distance
	 * after the current point.
	 * 
	 * @param d      double to find
	 * @param nBytes the number of bytes of significance, from 2 to 8
	 * @param length maximum distance to check or -1 for in.available()
	 * @param isAll
	 * @return found position or -1
	 * @throws IOException
	 */
	protected int findDoubleApprox(double d, int nBytes, int length, boolean isAll) throws IOException {
		ByteBuffer dbuf = ByteBuffer.allocate(8);
		dbuf.putDouble(d);
		byte[] bytes = dbuf.array();
		nBytes = Math.max(2, Math.min(nBytes, 8));
		if (length < 0)
			length = readAvailable();
		markIn(length);
		setBuf(length);
		resetIn();
		ByteBuffer b = buffer;
		buf = new byte[0];
		int found = -1;
		for (int i = 0, n = length - 8; i < n;) {
			for (int j = 0;;) {
				byte test = b.get();
				if (test != bytes[j]) {
					b.position(++i);
					break;
				}
				if (++j == nBytes) {
					if (isAll) {
						if (found == -1)
							found = i;
						System.out.println(i);
						b.position(++i);
						break;
					}
					return i;
				}
			}
		}
		return found;
	}

	/**
	 * Find the integer key array in the input stream within a given distance after
	 * the current point.
	 * 
	 * @param key
	 * @param length maximum distance to check or -1 for in.available()
	 * @param isAll true to find all
	 * @return found position or -1
	 * @throws IOException
	 */
	protected int findIn(int[] key, int length, boolean isAll) throws IOException {
		if (length < 0)
			length = readAvailable();
		markIn(length);
		setBuf(length);
		resetIn();
		int keylen = key.length;
		ByteBuffer b = buffer;
		buf = new byte[0];
		int found = -1;
		for (int i = 0, n = length - keylen * 4; i < n;) {
			for (int j = 0;;) {
				int test = b.getInt();
				if (test != key[j]) {
					b.position(++i);
					break;
				}
				if (++j == keylen) {
					if (isAll) {
						if (found == -1)
							found = i;
						System.out.println(i);
						b.position(++i);
						break;
					}
					return i;
				}
			}
		}
		return found;
	}

	/**
	 * Get the current ByteBuffer position.
	 * 
	 * @return the position
	 */
	protected int getBufferPosition() {
		return buffer.position();
	}

	/**
	 * Mark the ByteBuffer.
	 */
	protected void markBuffer() {
		buffer.mark();
	}

	/**
	 * Reset the ByteBuffer.
	 */
	protected void resetBuffer() {
		buffer.reset();
	}

	/**
	 * Close the input stream and set this.position to -1.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		position = -1;
		in.close();
	}

	/**
	 * Get the next byte from the ByteBuffer.
	 * 
	 * @return the byte value
	 */
	protected byte getByte() {
		return buffer.get();
	}

	/**
	 * Get the next 32-bit integer from the ByteBuffer.
	 * 
	 * @return the integer value
	 */
	protected int getInt() {
		return buffer.getInt();
	}

	/**
	 * Get the next 16-bit integer from the ByteBuffer.
	 * 
	 * @return the short value
	 */
	protected int getShort() {
		return buffer.getShort();
	}

	/**
	 * Get the next 64-bit double value from the ByteBuffer.
	 * 
	 * @return the double value
	 */
	public double getDouble() {
		return buffer.getDouble();
	}

	/**
	 * Get the next len bytes from the ByteBuffer into a byte array starting at the
	 * offset position of that array.
	 * 
	 * @param b      the target array
	 * @param offset the offset into that array
	 * @param len    the number of bytes to transfer
	 * @return the actual number of bytes transferred
	 */
	protected ByteBuffer get(byte[] b, int offset, int len) {
		return buffer.get(b, offset, len);
	}

	/**
	 * Look from this position to the end of file for integers that point to the
	 * specified location as:
	 * 
	 * [aa] [bb] [cc] [dd] references loc such that
	 * 
	 * address(loc) = address(byte aa) + 4 + 0xaaabbccdd.
	 * 
	 * @param loc
	 * @throws IOException
	 */
	protected void findRef(int loc) throws IOException {
		int n = readAvailable() - 4;
		markIn(n + 4);
		setBuf(n + 4);
		for (int i = 0; i < n;) {
			buffer.mark();
			int val = buffer.getInt();
			buffer.reset();
			if (i >= loc)
				break;
			if (i + 4 + val == loc) {
				if (testing)
					System.out.println(i + "\t0x" + Integer.toHexString(i) + "\t+\t" + val + "\t0x"
							+ Integer.toHexString(val) + "\t=\t" + loc);
			}
			buffer.position(++i);
		}
		resetIn();
	}

	/**
	 * Peek at the next eight bytes to see if they might be a reasonable double
	 * value.
	 * 
	 * @return double
	 * @throws IOException
	 */
	protected double peekDouble() throws IOException {
		int p = buffer.position();
		double d = buffer.getDouble();
		buffer.position(p);
		if (d != 0 && Math.abs(d) >= 1e-10 && Math.abs(d) <= 1e10) {
		} else {
			d = Double.NaN;
		}
		return d;
	}

	/**
	 * Check a block of doubles starting at a given location, allowing for all
	 * possible byte offsets. The input stream is marked and reset.
	 * 
	 * @param loc
	 * @param n
	 * @throws IOException
	 */
	protected void checkDouble(int loc, int n) throws IOException {

		markIn(loc + n * 8 + 8);
		skipIn(loc);
		setBuf(n * 8 + 8);
		for (int i = 0; i < 8;) {
			markBuffer();
			for (int j = 0; j < n; j++) {
				double d = buffer.getDouble();
				if (d != 0 && Math.abs(d) > 1e-10 && Math.abs(d) < 1e10) {
					buffer.position(buffer.position() - 8);
					if (testing)
						System.out.println(i + " " + j + " = " + Long.toHexString(buffer.getLong()) + "\t" + d);
				}
			}
			resetBuffer();
			buffer.position(++i);
		}
		resetIn();
	}

	/**
	 * Read a block of four doubles.
	 * 
	 * @throws IOException
	 */
	protected void readDoubleBox() throws IOException {
		readDouble();
		readDouble();
		readDouble();
		readDouble();
	}

	/**
	 * Skip the number of bytes indicated in the next 32-bit integer of the input
	 * stream.
	 * 
	 * @throws IOException when that integer is less than 0 or greater than the
	 *                     number of bytes available.
	 */
	protected void skipBlock() throws IOException {
		long pos = readPosition();
		long navail = readAvailable();
		int len = readInt();
		if (len < 0 || len > navail)
			throw new IOException(
					"invalid length " + len + " for reading Block where pos=" + pos + " navail=" + navail);
		skipIn(len);
	}

	/**
	 * Read a block into the ByteBuffer for testing, just skipping it if not
	 * testing.
	 * 
	 * @param blockIndex for testing only
	 * @return true if successful; false if EOF
	 * @throws IOException when that integer is less than 0 or greater than the
	 *                     number of bytes available.
	 */
	protected boolean readBlock(int blockIndex) throws IOException {
		long pos = readPosition();
		long navail = readAvailable();
		if (navail == 0)
			return false;
		int len = readInt();
		if (len < 0 || len > navail)
			throw new IOException(
					"invalid length " + len + " for reading Block where pos=" + pos + " navail=" + navail);
		if (testing) {
			System.out.println(pos + " reading " + len + " to " + (pos + len));// " skipping " +
			setBuf(len);
			for (int i = 0, n = Math.min(len >> 2, 64); i < n; i++) {
				double d = (i < n - 1 ? peekDouble() : Double.NaN);
				System.out.println(
						i + ":" + Integer.toHexString(getInt()).toUpperCase() + "\t" + (Double.isNaN(d) ? "" : d));
			}
		} else {
			skipIn(len);
		}
		return true;
	}

	/**
	 * Read a length record, then read the block that is indicated by the next
	 * integer.
	 * 
	 * @param i block index for display only
	 * @return true if successful
	 * @throws IOException
	 */
	protected boolean readSubblock(int i) throws IOException {
		readInt(); // length
		return readBlock(i);
	}

	/**
	 * Just testing to see what an 8-bit double value looks like.
	 * 
	 * First byte 3E-41 indicates positive double between 10E-9 and 10E9
	 * 
	 * First byte BE-C0 indicates negative double between 10E-9 and 10E9
	 * 
	 */
	protected static void doubleTest() {
		for (int i = -20; i <= 20; i++) {
			double d = Math.pow(10, i / 2.);
			showDoubleLong(d);
		}
		for (int i = -20; i <= 20; i++) {
			double d = -Math.pow(10, i / 2.);
			showDoubleLong(d);
		}
		showDoubleLong(Double.NaN);

//		3DDB7CDFD9D7BDBB	1.0E-10
//		3DF5BB233FCB6A90	3.1622776601683795E-10
//		3E112E0BE826D695	1.0E-9
//		3E2B29EC0FBE4534	3.1622776601683795E-9
//		3E45798EE2308C3A	1.0E-8
//		3E60FA3389D6EB40	3.162277660168379E-8
//		3E7AD7F29ABCAF48	1.0E-7
//		3E9538C06C4CA610	3.162277660168379E-7
//		3EB0C6F7A0B5ED8D	1.0E-6
//		3ECA86F0875FCF94	3.162277660168379E-6
//		3EE4F8B588E368F1	1.0E-5
//		3F009456549BE1BD	3.1622776601683795E-5
//		3F1A36E2EB1C432D	1.0E-4
//		3F34B96BE9C2DA2C	3.1622776601683794E-4
//		3F50624DD2F1A9FC	0.001
//		3F69E7C6E43390B7	0.0031622776601683794
//		3F847AE147AE147B	0.01
//		3FA030DC4EA03A72	0.03162277660168379
//		3FB999999999999A	0.1
//		3FD43D136248490F	0.31622776601683794
//		3FF0000000000000	1.0
//		40094C583ADA5B53	3.1622776601683795
//		4024000000000000	10.0
//		403F9F6E4990F227	31.622776601683793
//		4059000000000000	100.0
//		4073C3A4EDFA9759	316.22776601683796
//		408F400000000000	1000.0
//		40A8B48E29793D2F	3162.2776601683795
//		40C3880000000000	10000.0
//		40DEE1B1B3D78C7A	31622.776601683792
//		40F86A0000000000	100000.0
//		41134D0F1066B7CC	316227.7660168379
//		412E848000000000	1000000.0
//		41482052D48065C0	3162277.6601683795
//		416312D000000000	1.0E7
//		417E286789A07F2F	3.162277660168379E7
//		4197D78400000000	1.0E8
//		41B2D940B6044F7E	3.1622776601683795E8
//		41CDCD6500000000	1.0E9
//		41E78F90E385635D	3.1622776601683793E9
//		4202A05F20000000	1.0E10
//		BDDB7CDFD9D7BDBB	-1.0E-10
//		BDF5BB233FCB6A90	-3.1622776601683795E-10
//		BE112E0BE826D695	-1.0E-9
//		BE2B29EC0FBE4534	-3.1622776601683795E-9
//		BE45798EE2308C3A	-1.0E-8
//		BE60FA3389D6EB40	-3.162277660168379E-8
//		BE7AD7F29ABCAF48	-1.0E-7
//		BE9538C06C4CA610	-3.162277660168379E-7
//		BEB0C6F7A0B5ED8D	-1.0E-6
//		BECA86F0875FCF94	-3.162277660168379E-6
//		BEE4F8B588E368F1	-1.0E-5
//		BF009456549BE1BD	-3.1622776601683795E-5
//		BF1A36E2EB1C432D	-1.0E-4
//		BF34B96BE9C2DA2C	-3.1622776601683794E-4
//		BF50624DD2F1A9FC	-0.001
//		BF69E7C6E43390B7	-0.0031622776601683794
//		BF847AE147AE147B	-0.01
//		BFA030DC4EA03A72	-0.03162277660168379
//		BFB999999999999A	-0.1
//		BFD43D136248490F	-0.31622776601683794
//		BFF0000000000000	-1.0
//		C0094C583ADA5B53	-3.1622776601683795
//		C024000000000000	-10.0
//		C03F9F6E4990F227	-31.622776601683793
//		C059000000000000	-100.0
//		C073C3A4EDFA9759	-316.22776601683796
//		C08F400000000000	-1000.0
//		C0A8B48E29793D2F	-3162.2776601683795
//		C0C3880000000000	-10000.0
//		C0DEE1B1B3D78C7A	-31622.776601683792
//		C0F86A0000000000	-100000.0
//		C1134D0F1066B7CC	-316227.7660168379
//		C12E848000000000	-1000000.0
//		C1482052D48065C0	-3162277.6601683795
//		C16312D000000000	-1.0E7
//		C17E286789A07F2F	-3.162277660168379E7
//		C197D78400000000	-1.0E8
//		C1B2D940B6044F7E	-3.1622776601683795E8
//		C1CDCD6500000000	-1.0E9
//		C1E78F90E385635D	-3.1622776601683793E9
//		C202A05F20000000	-1.0E10
//		7FF8000000000000	NaN

	}

	protected static void showDoubleLong(double d) {
		System.out.println(Long.toHexString(Double.doubleToLongBits(d)).toUpperCase() + "\t" + d);
	}

}
