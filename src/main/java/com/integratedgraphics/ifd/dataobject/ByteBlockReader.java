package com.integratedgraphics.ifd.dataobject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

/**
 * A reader that can process blocks that contain an initial byte length followed
 * by a series of bytes. It allows for reading the bytes with a variable byte
 * order, creating smaller ByteBuffers to handle chunks of the byte array.
 * 
 * Extended for MNovaMetadataReader and NmrMLJeolAcquStreamReader.
 * 
 * @author hansonr
 *
 */
public class ByteBlockReader {

	/**
	 * set to true for debugging
	 */
	public static boolean testing = false;

	public static String pointerTest = null; // "version" for instance
	/**
	 * when testing, show integers read
	 */
	public static boolean showInts = false;

	/**
	 * when testing, show the characters associated with integers
	 */
	public static boolean showChars = false;

	/**
	 * when testing, show detail about this block
	 */
	public static int showBlock = -1;

	/**
	 * must be able to use available() -- so not http or https
	 */
	private RewindableInputStream in;

	/**
	 * byte array for ByteBuffer
	 */
	private byte[] buf = new byte[1];

	/**
	 * byte order
	 */
	protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

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

	protected int len;

	
	private int nBlocks;

	/**
	 * The BlockData class holds information about a subarray of bytes within the
	 * file. This array has a location and length. In addition it has a unique
	 * identifier that may be just a simple global serial number, or it could be a
	 * given name, such as "header" or "body" or "page".
	 * 
	 * 
	 * A block may contain subblocks, list as one of:
	 * 
	 * "block1", "block2",...
	 * 
	 * "data1", "data2",...
	 * 
	 * "page1", "page2",... ("body" block only)
	 * 
	 * A block that contains subblocks will have a special subblock named "stack",
	 * which identifies a sequential set of integer lengths found within the file.
	 * 
	 * A "path" field identifies the hierarchy of blocks. So, for example, in a
	 * MNova file,  
	 * 
	 * 
	 * 
	 * 
	 * "block" and "page" type BlockData start with at least one length integer that
	 * also serves as a forward pointer to the first byte after the data. This
	 * integer length does not include its own four bytes.
	 * 
	 * "data" type BlockData may or may not start with an integer length/pointer. 
	 * 
	 * There is no information within the file that I can see that clearly defines the 
	 * start and and of individual data items. So there is some guesswork here. 
	 * 
	 * But a common occurrance is a set of monotonically decreasing pointers that 
	 * ends with a 0 integer, forming a package of fully delineated data items. 
	 * 
	 * Since I have never seen any actual specification for the format, I could 
	 * be completely wrong about all this. All I know is it that it makes some
	 * sense, and for what I need, it works. 
	 * 
	 * @author hanso
	 *
	 */
	public class BlockData {
		public String name;
		private int globalPtr;
		public int localIndex;
		public long blockLoc;
		private long blockLen;
		Stack<BlockData> subblocks;
		BlockData parent;
		int dataBlockCount;
		String id;
		private String path;
		private List<BlockData> pages;
		private boolean lenSet;

		public BlockData(long loc, long len) {
			this(loc, len, null);
		}
		
		public BlockData(long loc, long len, String name) {
			this.name = name;
			this.blockLoc = loc;
			setLength(len);
			globalPtr = nBlocks++;			
		}

		public BlockData findBlock(long pos) {
			if (pages != null) {
				for (int i = 0; i < pages.size(); i++) {
					BlockData b = pages.get(i).findBlock(pos);
					if (b != null)
						return b;
				}
			}
			if (subblocks != null) {
				for (int i = 0; i < subblocks.size(); i++) {
					BlockData b = subblocks.get(i).findBlock(pos);
					if (b != null)
						return b;
				}
			}
			return (pos >= blockLoc && pos < next() ? this : null);
		}

		public void setSubblocks(Stack<BlockData> blocks) {
			subblocks = blocks;
			for (int i = blocks.size(); --i >= 0;) {
				BlockData bd = blocks.get(i);
				bd.setParent(this, i);
			}
		}
		
		private void setParent(BlockData parent, int i) {
			this.parent = parent;
			localIndex = i;
		}

		public void addPage(BlockData bd) {
		   if (pages == null)
			   pages = new ArrayList<BlockData>();
		   pages.add(bd);
		   if (!lenSet)
			   blockLen += bd.blockLen;
		}
		
		public void addSubblock(BlockData bd) {
			if (subblocks == null)
				subblocks = new Stack<BlockData>();
			bd.setParent(this, subblocks.size());
			subblocks.add(bd);		
		}
		
		public void seek() throws IOException {
			seekIn(blockLoc);
		}

		public void skip() throws IOException {
			seekIn(next());
		}

		public byte[] getData() throws IOException {
			long pt = getPosition();
			seekIn(blockLoc);
			byte[] bytes = readBytes(blockLen);
			seekIn(pt);
			return bytes;
		}
		
		/**
		 * Get a subblock. 0-based
		 * 
		 * @param i -n for "from the end", with -1 being the last one 
		 * @return
		 */
		public BlockData getSubblock(int i) {
			if (subblocks == null)
			  return null;
			if (i < 0)
				i = subblocks.size() + i;
			return (i < 0 || i >= subblocks.size() ? null : subblocks.get(i));
		}
		
		public void addStack(Stack<BlockData> s) {
			for (BlockData bd : s) {
				if (bd.blockLen > 0) 
					addSubblock(bd);
			}
		}

		public void setPaths(String pathName) throws IOException {
			if (pathName == null && path != null)
				clearPaths();
			path = (pathName == null ? "" : pathName + ".") + getTag();
			htPathToBlock .put(path, this);
			if (subblocks != null)
				for (int i = 0; i < subblocks.size(); i++)
					subblocks.get(i).setPaths(path);
			if (pages != null)
				for (int i = 0; i < pages.size(); i++)
					pages.get(i).setPaths(path);
		}

		public void clearPaths() throws IOException {
			htPathToBlock.remove(path);
			path = null;
			if (subblocks != null)
				for (int i = 0; i < subblocks.size(); i++)
					subblocks.get(i).clearPaths();
		}

		public void getStack(StringBuffer sb, String pathName) throws IOException {
			long pt = getPosition();
			sb.append(path).append(getInfo()).append('\n');
			seek();
			if (subblocks != null) {
				for (int i = 0; i < subblocks.size(); i++) {
					subblocks.get(i).getStack(sb, path);
				}
			}
			if (pages != null) {
				for (int i = 0; i < pages.size(); i++) {
					pages.get(i).getStack(sb, path);
				}
			}
			seekIn(pt);
		}
		
		protected String getTag() {
			return (path != null ? path : getNameOrID());
		}

		protected String getNameOrID() {
			return (name != null ? name : "data" + (localIndex >= 0 ? localIndex : globalPtr));
		}

		@Override
		public String toString() {
			return "[Block " + getTag() + getInfo()	+ "]";
		}

		private String getInfo() {
			return " loc=" + blockLoc + " len=" + blockLen + " to " + next();
		}

		public void map(Map<String, Object> map) {
			map.put("path",  path);
			map.put("location", Integer.valueOf((int)blockLoc));
			map.put("length",  Integer.valueOf((int) blockLen));
			if (subblocks != null) {
				Map<String, Object> subMap = new TreeMap<>();
				map.put("subblocks", subMap);
				for (int i = 0; i < subblocks.size(); i++) {
					Map<String, Object> m = new TreeMap<>();
					subMap.put(subblocks.get(i).getNameOrID(), m);
					subblocks.get(i).map(m);
				}
			}
			if (pages != null) {
				Map<String, Object> pageMap = new TreeMap<>();
				map.put("pages", pageMap);
				for (int i = 0; i < pages.size(); i++) {
					Map<String, Object> m = new TreeMap<>();
					pageMap.put(pages.get(i).getNameOrID(), m);
					pages.get(i).map(m);
				}
			}
		}

		public int getSubblockCount() {
			return (subblocks == null ? 0 : subblocks.size());
		}

		public int getDataBlockCount() {
			int n = getSubblockCount();
			return (n == 0 ? 0 : "stack".equals(subblocks.get(0).name) ? n - 1 : n);
		}

		public long next() {
			return blockLoc + blockLen;
		}

		public Stack<BlockData> getSubblocks() {
			return subblocks;
		}

		public void setLength(long l) {
			blockLen = l;
			lenSet = (l != 0);
		}

		public long getLength() {
			return blockLen;
		}
}
	
	public ByteBlockReader(InputStream in) throws IOException {
		this(FAIRSpecUtilities.getLimitedStreamBytes(in, -1, null, true, true));
	}

	public ByteBlockReader(byte[] bytes) {
		this.len = bytes.length;
		this.in = new RewindableInputStream(bytes);
	}

	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		if (buffer != null)
			buffer.order(byteOrder);
	}

	/**
	 * Get the current byte array underlying the ByteBuffer and InputStream.
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
	public long getPosition() {
		return position;
	}

	/**
	 * Read bytes into the ByteBuffer and byte[] array from the input stream. the
	 * "read" methods in this class handle this automatically, but if you prefer,
	 * you can load the buffer this way yourself and then use the "get" methods
	 * (getByte, getShort, getInt, getLong, getDouble).
	 * 
	 * @param len number of bytes to read
	 * @return current buffer
	 * @throws IOException
	 */
	public byte[] setBuf(int len) throws IOException {
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
	public int getAvailable() throws IOException {
		return in.available();
	}

	/**
	 * Mark the input stream. A future resetIn() will return to this position. This
	 * method is private. To do this publicly, just use long ptr = readPosition()
	 * and then, later, seekIn(ptr).
	 * 
	 * @param n
	 */
	private void markIn(int n) {
		mark = position;
		in.mark(n);
	}

	/**
	 * Reset the input stream.
	 * 
	 * @throws IOException
	 */
	public void resetIn() throws IOException {
		position = mark;
		in.reset();
	}

	/**
	 * Skip a given number of bytes in the input stream.
	 * 
	 * @param n
	 * @throws IOException
	 */
	public void skipIn(int n) throws IOException {
		if (n > 0) {
			if (position < 0)
				position = 0;
			position += in.skip(n);
			if (testing)
				System.out.println("skip from " + (position - n) + " by " + n + " to " + position);
		}
	}

	/**
	 * Set the position of the input stream to the given location.
	 * 
	 * 
	 * @param loc if less than 0, will be relative to the current position
	 * @throws IOException when attempting to read a negative location or past the
	 *                     end of the input stream.
	 */
	public void seekIn(long loc) throws IOException {
		if (loc < 0)
			loc = getPosition() + loc;
		if (loc < 0)
			loc = 0;
		if (getPosition() > loc)
			rewindIn();
		if (loc > getPosition())
			skipIn((int) (loc - getPosition()));
	}

	/**
	 * Read a 32-bit integer as a byte block data pointer, adding its value to the
	 * address of the byte that follows it.
	 * @return
	 * @throws IOException
	 */
	public long readPointer() throws IOException {
		long navail = getAvailable();
		int len = peekInt();
		if (len < 0 || len > navail)
			throw new IOException("invalid length " + len + " for nextBlock where pos=" + getPosition() + " navail=" + navail);
		return getPosition() + readInt() + 4;
	}

	/**
	 * check a pointer for its file structure block
	 * @param pos less than or equal to 0 is relative to the current position
	 * @param why
	 */
	protected void findPointer(BlockData bd, long pos, String why) {
		if (pos <= 0)
			pos = getPosition() + pos;
		BlockData block = (pos == len ? null : bd.findBlock(pos));
		String msg = "!! " + why + " pointer " + pos 
				+ (block != null ? " is in block " + block.path + " + " + (pos - block.blockLoc)
						: pos ==  len ? " is EOF" : " block could not be found");
		if (block != null && "stack".equals(block.name)) {
		  int pt = (int)(pos - block.blockLoc) / 4; 
		  msg += " index " + pt;
		}
		if (strDebug != null && strDebug.indexOf(msg) < 0)
			strDebug.append(msg).append('\n');
		System.out.println(msg);
	}

	public long readLongPtr() throws IOException {
		long len = readLong();
		long pos = getPosition();
		return pos + len;
	}

	/**
	 * Read a 4-byte magic number header.
	 * 
	 * @param magicNumber
	 * @return true if successful.
	 * @throws IOException
	 */
	public boolean checkMagicNumber(int magicNumber) throws IOException {
		int nAvail = getAvailable();
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
	public int readByte() throws IOException {
		position++;
		int b = in.read();
		if (testing && showInts)
			dump("Byte", b, 1, Integer.toHexString(b).toUpperCase(), "");
		return b;
	}

	/**
	 * Read the next two bytes of the input stream as a short value.
	 * 
	 * @return a short value
	 * @throws IOException
	 */
	public int readShort() throws IOException {
		setBuf(2);
		int i = buffer.getShort();
		if (testing && showInts)
			dump("Short", i, 2, Integer.toHexString(i).toUpperCase(), "");
		return i;
	}

	/**
	 * Read the next four bytes of the input stream as a 32-bit integer value.
	 * 
	 * @return a short value
	 * @throws IOException
	 */
	public int readInt() throws IOException {
		setBuf(4);
		int i = buffer.getInt();
		if (testing && showInts) {
			dump("Int", i, 4, toHex(i), (showChars ? showString(getBuf(), 0, 4) : ""));
		}
		return i;
	}

	private String showString(byte[] buf, int pt, int len) {
		String st = new String(buf, pt, len);
		String s = "";
		for (int i = 0; i < st.length(); i++) {
			int c = st.codePointAt(i);
			if (c >= 32 && c <= 125)
				s += st.substring(i, i + 1);
			else
				s += " <" + Integer.toHexString(c) + "> ";
		}
		return s;
	}

	/**
	 * Read the next eight bytes of the input stream as a 64-bit long value.
	 * 
	 * @return a long value
	 * @throws IOException
	 */
	public long readLong() throws IOException {
		setBuf(8);
		long l = buffer.getLong();
		if (testing && showInts)
			dump("Long", l, 8, toHex(l), "");
		return l;
	}

	protected StringBuffer sbOut;

	protected StringBuffer strDebug;

	protected Map<String, BlockData> htPathToBlock = new HashMap<>();
	
	protected BlockData getBlockFromPath(String path) {
		return htPathToBlock.get(path);
	}

	private void dump(String type, long val, int len, String hex, String s) {
		String msg = "read" + type + " " + (position - len) + ": " + hex + " = " + val
				+ (type == "Int" ? " -> " + (position + val) : "") + " " + s;
		if (sbOut == null)
			System.out.println(msg);
		else 
			sbOut.append(msg).append('\n');
	}

	private String toHex(int i) {
		String s = "00000000" + Integer.toHexString(i).toUpperCase();
		return "0x" + s.substring(s.length() - 8);
	}

	private String toHex(long i) {
		String s = "0000000000000000" + Long.toHexString(i).toUpperCase();
		return "0x" + s.substring(s.length() - 16);
	}

	/**
	 * Read the next eight bytes of the input stream as a 64-bit double value.
	 * 
	 * @return a double value
	 * @throws IOException
	 */
	public double readDouble() throws IOException {
		setBuf(8);
		double d = buffer.getDouble();
		buffer.position(buffer.position() - 8);
		long l = buffer.getLong();
		if (testing)
			System.out.println("ReadDouble 0x" + Long.toHexString(l).toUpperCase() + "\t" + d);
		return d;
	}

	public float readFloat() throws IOException {
		setBuf(4);
		float f = buffer.getFloat();
		buffer.position(buffer.position() - 4);
		int l = buffer.getInt();
		if (testing)
			System.out.println("ReadFloat 0x" + Integer.toHexString(l).toUpperCase() + "\t" + f);
		return f;
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

	public byte[] readBytes(long len) throws IOException {
		int l = (int) len;
		byte[] bytes = new byte[l];
		read(bytes, 0, l);
		return bytes;
	}

	public byte[] readLen4() throws IOException {
		int len = readInt();
		return readBytes(len * 4);
	}

	/**
	 * Read a simple String of known length from the input stream.
	 * 
	 * @param len the known length of the string.
	 * @return the string
	 * @throws IOException
	 */
	public String readSimpleString(int len) throws IOException {
		String s = new String(setBuf(len), 0, len);
		if (testing)
			System.out.println("readString >" + s + "<");
		return s.trim();
	}

	/**
	 * Read an ASCII String that has its length indicated as an integer just before
	 * it. However, if that string starts with 0x00xx00xx, we will assume you meant
	 * to read a UTF-16 string and switch to that.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readLenStringSafely() throws IOException {
		int len = readInt();
		long b = peekInt();
		return ((b & 0xFF00FF00) == 0 ? readUTF16String(len) : readSimpleString(len));
	}

	/**
	 * Read a UTF-16 String that has its length indicated as an integer just before
	 * it, converting 2-, 3-, and 4-byte sequences properly.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readUTF16String() throws IOException {
		return readUTF16String(readInt());
	}

	/**
	 * Read a UTF-16 String of given length, converting 2-, 3-, and 4-byte sequences
	 * properly. Regular ASCII text sent to this method will return what looks like
	 * Mandarin.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readUTF16String(int len) throws IOException {
		long p = getPosition();
		setBuf(len);
		String s = new String(buf, 0, len, "utf16");
		if (testing)
			System.out.println("ReadUTF16 " + p + "(" + len + ") >" + s + "<");
		return s;
	}

	/**
	 * Read through a set of 4-byte integers.
	 * 
	 * @param n
	 * @throws IOException
	 */
	public void readInts(int n) throws IOException {
		for (int i = 0; i < n; i++) {
			readInt();
		}
	}

	/**
	 * Just look at a byte; don't change the input stream position.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int peekByte() throws IOException {
		markIn(1);
		int n = readByte();
		resetIn();
		if (testing)
			System.out.println("peekByte " + n);
		return n;
	}

	/**
	 * Check the next integer position; the input stream is marked and reset.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int peekInt() throws IOException {
		markIn(4);
		int n = readInt();
		resetIn();
		if (testing && showInts)
			System.out.println("peekInt " + n);
		return n;
	}
	
	public long peekPointer() throws IOException {
		int len = peekInt();
		long pos = getPosition() + 4; // have not read the point yet
		return pos + len;
	}



	/**
	 * Find the integer key array in the input stream within a given distance after
	 * the current point.
	 * 
	 * @param key
	 * @param length maximum distance to check or -1 for in.available()
	 * @param isAll  true to find all
	 * @return found position or -1
	 * @throws IOException
	 */
	public int findInts(int[] key, int length, boolean isAll) throws IOException {
		if (length < 0)
			length = getAvailable();
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
						b.position(++i);
						System.out.println("byteBlock found pos=" + i);
						break;
					}
					return i;
				}
			}
		}
		return found;
	}

	public int findBytes(byte[] key, int length, boolean isAll, int nPre0) throws IOException {
		if (length < 0)
			length = getAvailable();
		markIn(length);
		setBuf(length);
		resetIn();
		int keylen = key.length;
		ByteBuffer b = buffer;
		buf = new byte[0];
		int found = -1;
		byte b0 = key[0];
		for (int i = 0, n = length - keylen; i < n;) {
			for (int j = 0, ptb0 = 0;;) {
				byte test = b.get();
				if (test == b0 && ptb0 == 0)
					ptb0 = j;
				if (test != key[j]) {
					b.position(i = i + (ptb0 == 0 ? j + 1 : ptb0));
					break;
				}
				if (++j == keylen) {
					if (nPre0 > 0) {
						boolean isOK = (i >= nPre0);
						if (isOK) {
							for (int k = 1; k <= nPre0; k++) {
								if (b.get(i - k) != 0) {
									isOK = false;
									break;
								}
							}
						}
						if (!isOK) {
							b.position(++i);
							break;
						}
					}
					if (isAll) {
						if (found == -1)
							found = i;
						System.out.println("byteBlock found pos=" + i);
						b.position(i = i + (ptb0 == 0 ? j + 1 : ptb0));
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
	public int getBufferPosition() {
		return buffer.position();
	}

	/**
	 * Mark the ByteBuffer.
	 */
	public void markBuffer() {
		buffer.mark();
	}

	/**
	 * Reset the ByteBuffer.
	 */
	public void resetBuffer() {
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
	public byte getByte() {
		return buffer.get();
	}

	/**
	 * Get the next 32-bit integer from the ByteBuffer.
	 * 
	 * @return the integer value
	 */
	public int getInt() {
		return buffer.getInt();
	}

	/**
	 * Get the next 64-bit long integer from the ByteBuffer.
	 * 
	 * @return the long value
	 */
	public long getLong() {
		return buffer.getLong();
	}

	/**
	 * Get the next 16-bit integer from the ByteBuffer.
	 * 
	 * @return the short value
	 */
	public int getShort() {
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
	public ByteBuffer get(byte[] b, int offset, int len) {
		return buffer.get(b, offset, len);
	}

	//////// debugging tools ///////

	/**
	 * Read out the next n integers, resetting the input stream after doing so.
	 * 
	 * @param n negative number show prevous -n ints
	 * @throws IOException
	 */
	public void peekInts(int n) throws IOException {
		if (n < 0) {
			seekIn(4 * n);
			peekIntsSb(null, -n);
			readInts(-n);
			return;
		}
		peekIntsSb(null, n);
	}

	public void peekIntsSb(StringBuffer sb, int n) throws IOException {

		if (getAvailable() < 4 * n)
			n = getAvailable()/4;
		String msg = "PeekInts " + n + " pos=" + getPosition();// + " navail=" + getAvailable();
		if (sb == null)
			System.out.println(msg);
		else 
			sb.append(msg).append('\n');
		boolean bt = testing;
		boolean bi = showInts;
		boolean bc = showChars;
		this.sbOut = sb;
		testing = showInts = true;
		//showChars = false;
		markIn(n * 4);
		readInts(n);
		resetIn();
		testing = bt;
		showInts = bi;
		showChars = bc;
		this.sbOut = null;
	}

	/**
	 * Peek at a set of integers for debugging.
	 * 
	 * @param pos
	 * @param n
	 * @throws IOException
	 */
	public void peekIntsAt(long pos, int n) throws IOException {
		boolean l = testing;
		boolean l1 = showInts;
		testing = showInts = true;
		long pos0 = getPosition();
		setPosition(pos);
		readInts(n);
		setPosition(pos0);
		testing = l;
		showInts = l1;
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
	public int findDouble(double d, int length, boolean isAll) throws IOException {
		long l = Double.doubleToLongBits(d);
		int high = (int) ((l >> 32) & 0xFFFFFFFF);
		int low = (int) (l & 0xFFFFFFFF);
		return findInts(new int[] { high, low }, length, isAll);
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
	public int findDoubleApprox(double d, int nBytes, int length, boolean isAll) throws IOException {
		ByteBuffer dbuf = ByteBuffer.allocate(8);
		dbuf.putDouble(d);
		byte[] bytes = dbuf.array();
		nBytes = Math.max(2, Math.min(nBytes, 8));
		if (length < 0)
			length = getAvailable();
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
	 * Look from position pos to loc integers that point to the
	 * specified location as:
	 * 
	 * [aa] [bb] [cc] [dd] references loc such that
	 * 
	 * address(loc) = address(byte aa) + 4 + 0xaaabbccdd.
	 * 
	 * @param pos starting position
	 * @param loc target position
	 * @throws IOException
	 */
	public void findRef(long pos, long loc) throws IOException {
		System.out.println("finding all pointers to " + loc + " after " + pos);
		String s = "";
		long n = loc - pos;
		seekIn(pos);
		if (n < 0)
			return;
		int nfound = 0;
		for (long i = pos; i < loc; i++) {
			seekIn(i);
			if (getAvailable() < 4)
				break;
			int val = readInt();
			if (i + 4 + val == loc) {
					String msg = i + "\t0x" + toHex(i) + "\t+\t" + val + " (0x" + toHex(val) + ")\t=\t" + loc;
					s += msg + "\n";
					System.out.println(msg);
					followPointer(i, len, "ref=" + loc);
					nfound++;
			}			
		}
		System.out.println("done " + nfound + "\n" + s);
		if (nfound == 0) {
			seekIn(loc);
			peekInts(-5);
			peekInts(1);
		} else {
		}
	}

	public long findRefRev(long pos, long loc) throws IOException {
		System.out.println("finding all pointers to " + loc + " after " + pos);
		String s = "";
		long n = loc - pos;
		seekIn(loc);
		if (n >= 4) {
			for (long i = loc; --i >= pos;) {
				seekIn(i);
				if (getAvailable() < 4)
					break;
				int val = readInt();
				if (i + 4 + val == loc) {
					return i;
				}
			}
		}
		return -1;
	}

	public List<Object> traceRef(int loc, boolean isTop) throws IOException {
		List<Object> nextLevel = new ArrayList<>();
		nextLevel.add(loc);
		int n = getAvailable() - 4;
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
					System.out.println(i + "\t0x" + toHex(i) + "\t+\t" + val + "\t0x" + toHex(val) + "\t=\t" + loc);
				nextLevel.add(i);
			}
			buffer.position(++i);
		}
		resetIn();
		for (int i = 1; i < nextLevel.size(); i++) {
			loc = ((Integer) nextLevel.get(i)).intValue();
			List<Object> tree = traceRef(loc, false);
			if (tree.size() > 1)
				nextLevel.set(i, tree);
		}
		if (isTop) {
			System.out.println(loc);
			dumpList(nextLevel, "");
		}
		return nextLevel;
	}

	@SuppressWarnings("unchecked")
	private void dumpList(List<Object> nextLevel, String indent) {
		indent += nextLevel.get(0) + " ";
		for (int i = 1; i < nextLevel.size(); i++) {
			Object o = nextLevel.get(i);
			if (o instanceof List) {
				dumpList((List<Object>) o, indent);
			} else {
				System.out.println(indent + o);
			}

		}
	}

	/**
	 * Check a block of doubles starting at a given location, allowing for all
	 * possible byte offsets. The input stream is marked and reset.
	 * 
	 * @param loc
	 * @param n
	 * @throws IOException
	 */
	public void checkDoubles(int loc, int n, int offset) throws IOException {

		markIn(loc + n * 8 + 8);
		seekIn(loc);
		setBuf(n * 8 + 8);
		for (int i = (offset == -1 ? 0 : offset), i1 = (offset == -1 ? 8 : offset + 1); i < i1;) {
			markBuffer();
			for (int j = 0; j < n; j++) {
				double d = buffer.getDouble();
				if (d != 0 && Math.abs(d) > 1e-10 && Math.abs(d) < 1e10) {
					buffer.position(buffer.position() - 8);
					if (testing)
						System.out.println(
								i + " " + (loc + i + j * 8) + " : " + Long.toHexString(buffer.getLong()) + "\t" + d);
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
	public void readDoubleBox() throws IOException {
		readDouble();
		readDouble();
		readDouble();
		readDouble();
	}

	/**
	 * Skip the number of bytes indicated in the next 32-bit integer of the input
	 * stream, adding four bytes for the reference itself.
	 * 
	 * 
	 * @throws IOException when that integer is less than 0 or greater than the
	 *                     number of bytes available.
	 */
	public void nextBlock() throws IOException {
		long pos = readPointer();
		seekIn(pos);
	}
	
	public void skipBlocks(int n) throws IOException {
		for (int i = 0; i < n; i++) {
			nextBlock();		
		}
	}

//	 * When testing, read into the ByteBuffer from the current location up to the block indicated by the 
//	 * data pointer at the current location. When not testing, the same as skipToBlock().
//	 * 

	/**
	 * Skip a number of pointers and read the block that is indicated by the next
	 * pointer.
	 * 
	 * @param n the depth of the subblock to read
	 * @return true if successful
	 * @throws IOException
	 */
	public void nextSubblock(int n) throws IOException {
		readInts(n);
		nextBlock();
	}

	/**
	 * Just testing to see what an 8-bit double value looks like.
	 * 
	 * First byte 3E-41 indicates positive double between 10E-9 and 10E9
	 * 
	 * First byte BE-C0 indicates negative double between 10E-9 and 10E9
	 * 
	 */
	public static void doubleTest() {
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

	public static void showDoubleLong(double d) {
		System.out.println(Long.toHexString(Double.doubleToLongBits(d)).toUpperCase() + "\t" + d);
	}

	public static class RewindableInputStream extends ByteArrayInputStream {

		public RewindableInputStream(byte[] buf) {
			super(buf);
		}

		public void setPosition(long pt) {
			pos = mark = (int) pt;
		}

	}

	public void rewindIn() throws IOException {
		setPosition(0);
	}

	/**
	 * Sets the position ("seeks") on the rewindableInputStream.
	 * 
	 * @param pos
	 * @throws IOException
	 */
	private void setPosition(long pos) throws IOException {
		in.setPosition(position = pos);
	}

	/**
	 * For debugging, extract printable [A-z] characters from a block. This is just
	 * a quick way to get a sense of what is in a block when decoding.
	 * 
	 * @param doblock Set true to cut to 60-wide string
	 * @return cleaned ASCII string.
	 * @throws IOException
	 */
	public String peekBlockAsString(boolean doblock) throws IOException {
		long ptr = getPosition();
		int len = peekInt();
		StringBuffer sb = new StringBuffer();
		boolean t = testing;
		testing = false;
		for (int i = 0, p = 0; i < len; i++) {
			int b = readByte();
			if (b >= 60 && b <= 122) {
				sb.append((char) b);
				if (doblock && ++p % 80 == 0)
					sb.append('\n');
			}
		}
		seekIn(ptr);
		testing = t;
		String s = sb.toString();
		if (doblock)
			System.out.println(s);
		return s;
	}

	public Stack<BlockData> followPointer(long pos, long target, String prefix) throws IOException {
		//System.out.println("trying to follow " + pos + " to " + target + " for " + prefix);
		long p0 = pos;
		Stack<BlockData> stack = new Stack<>();
		long loc = -1;
		try {
			seekIn(pos);
			loc = peekPointer();
			int n = 0;
			while (pos < loc && loc <= target) {
				long len = loc - pos;
				System.out.println("followPointer found " + pos + "+" + (len-4) + "->" + loc  + " for " + prefix);
				BlockData bd = new BlockData(pos, len, prefix + ++n);
				stack.add(bd);
				if (loc == target) {
					break;
				}
				pos = loc;
				seekIn(pos);
				loc = -1; // in case of exception
				loc = peekPointer();
			}
			if (loc == target) {
				seekIn(p0);
				System.out.println("!!! OK " + p0);
			}
		} catch (Exception e) {
			if (pos < target) {
				seekIn(pos);
				peekInts(3);
			}

		}
		seekIn(p0);
		return (loc == target ? stack : null);
	}
	
	/**
	 * Read the pointer stack and then return a stack of Block objects indicating
	 * the location and length of the data for that object.
	 * 
	 * A pointer stack involves a series of monotonically decreasing numbers ending
	 * with 0.
	 * 
	 * Each pointer points to its forward-relative location. The difference between
	 * two sequential pointers gives the length.
	 * 
	 * @return the pointer to the next block
	 * @throws IOException
	 */
	public Stack<BlockData> getDataStack(long pos0, long pos1, String why) throws IOException {
		seekIn(pos0);
		Stack<Long> ptrStack = new Stack<>();
		Stack<BlockData> objStack = new Stack<>();
		try {//
			int len0 = peekInt();
			int len1;
			while ((len1 = peekInt()) != 0) {
				long pt = getPosition() + 4 + len1;
				if (len0 == 16 && (len1 < 0 || pt > this.len)) {
					if (pos0 == len - 21) {
						// legitimate EOF
						return null;
					}
//                      this pointer is to an actual data block, not a pointer stack					
//						readInt 12871: 0x00000010 = 16 -> 12891 
//						readInt 12875: 0x78937008 = 2022928392 -> 2022941271 
//						readInt 12879: 0x0123459C = 19088796 -> 19101679 
//						readInt 12883: 0x86D52F1A = -2032849126 -> -2032836239 
//						readInt 12887: 0x9CB0AD05 = -1666142971 -> -1666130080 
							
					//int[] a = new int[] {readInt(), readInt(), readInt(), readInt()};
					//System.out.println("!!!Found 16 x x x x at " + pos0 + "-- skipping " + Arrays.toString(a));
					// or
					skipIn(16);
					return getDataStack(getPosition(), pos1, why);
				}
				ptrStack.push(readPointer());
			}
		} catch (Exception e) {

			// System.out.println("EOF found" + e);
			// EOF found
			return null;
		}
		readInt(); // 0
		long ptNext = getPosition(); // start of data
		while (ptrStack.size() > 0) {
			long ptr = ptrStack.pop().longValue();
			long len = ptr - ptNext;
			if (ptr > pos1) {
				// setting negative len indicates abandon this
				len = -len;
			}
			BlockData bd = new BlockData(ptNext, len);
			objStack.push(bd);
			ptNext = ptr;
		}
		seekIn(ptNext);
		return objStack;
	}

	public void testStack(Stack<BlockData> stack, int max) throws IOException {
		if (stack == null)
			return;
		Enumeration<BlockData> e = stack.elements();
		int i = 0;
		while (e.hasMoreElements()) {
			BlockData obj = e.nextElement();
			System.out.println("obj " + ++i + " " + obj);
			long len = obj.getLength();
			if (len < max)
				peekIntsAt(obj.blockLoc, (int) (len/4));
		}
	}

	public void showStack(Stack<?> stack) {
		Enumeration<?> e = stack.elements();
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			System.out.print(o + " ");
		}
		System.out.println();
	}

	protected void getBlockStructure(BlockData bdParent, int byteShift, int i0) throws IOException {
		int n = i0;
		strDebug = new StringBuffer();
		bdParent.seek();
		if (byteShift > 0) {
			bdParent.addSubblock(new BlockData(getPosition(), byteShift, "block" + ++n));
		}
		skipIn(Math.abs(byteShift));
		long ptNext = -1;
		long ptEnd = bdParent.next();
		long pt0;
		long pt1 = ptEnd + 4;
		while (getAvailable() > 0 && (pt0 = getPosition()) < ptEnd) {
			Stack<BlockData> s = getDataStack(pt0, pt1, null);
			if (s == null || s.size() == 0) {
				if (ptNext > pt0)
					seekIn(ptNext);
				break;
			}
			ptNext = addParentBlockDataStack(bdParent, s, pt0, ++n);
			seekIn(ptNext);
		}		
		return;
		
	}


	private long addParentBlockDataStack(BlockData bdParent, Stack<BlockData> s, long pt0, int n) {
		BlockData block = new BlockData(pt0, 0, "block" + n);
		long ptData = s.get(0).blockLoc;
		long ptNext = getPosition();
		if (ptData > pt0)
			block.addSubblock(new BlockData(pt0, ptData - pt0, "stack"));
		block.addStack(s);
		if (ptNext == pt0) {
			BlockData bdlast = s.get(s.size() - 1);
			ptNext = bdlast.next(); 							
		}
		block.setLength(ptNext - pt0);
		bdParent.addSubblock(block);
		return ptNext;
	}

	/**
	 * Dump the contents as a list of 64-bit floating-point numbers.
	 *  
	 * @param loc
	 * @param len
	 * @param fname
	 * @throws IOException
	 */
	public void extractDoubles(long loc, int len, String fname) throws IOException {
		boolean t = testing;
		testing = false;
		seekIn(loc);
		long ptr = getPosition();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++)
			sb.append(readDouble()).append('\n');
		File f = new File(fname);
		try (FileOutputStream fis = new FileOutputStream(f)) {
			byte[] bytes = sb.toString().getBytes();
			fis.write(bytes);
			System.out.println(bytes.length + " bytes written to " + f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		seekIn(ptr);
		testing = t;
	}

	/**
	 * Dump the contents as a list of integers.  
	 * 
	 * @param loc
	 * @param len
	 * @param fname
	 * @throws IOException
	 */
	public void extractInts(long loc, int len, String fname) throws IOException {
		boolean t = testing;
		testing = false;
		seekIn(loc);
		long ptr = getPosition();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++)
			sb.append(Integer.toHexString(readInt())).append('\n');
		File f = new File(fname);
		try (FileOutputStream fis = new FileOutputStream(f)) {
			byte[] bytes = sb.toString().getBytes();
			fis.write(bytes);
			System.out.println(bytes.length + " bytes written to " + f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		seekIn(ptr);
		testing = t;
	}
	public String bytesToHex(int n) throws IOException {
		if (n == 0)
			return "";
		long pt = getPosition();
		byte[] bytes = readBytes(n);
		seekIn(pt);
		String s = "";
		for (int i = 0; i < n; i++) {
			String sb = Integer.toHexString(bytes[i] + 0xFF00).toUpperCase();
			s += "  " + sb.substring(sb.length() - 2);
		}
		return s;
	}

}
