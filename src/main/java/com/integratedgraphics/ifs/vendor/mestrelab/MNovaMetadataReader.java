package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.iupac.fairspec.util.Util;

import com.integratedgraphics.ifs.vendor.ByteBlockReader;

/**
 * A rough MestReNova file reader that can deliver metadata only. I have not
 * seen any MestReNova code, so I am guessing here. The file format appears to
 * be based on a stack that can be read from the data stream. Internal
 * references are forward-relative. Some of these may be long references, but I
 * don't think it matters. For the most part, I assume they are integer
 * references. A 32-bit reference 109 points to an address 109+4 from the
 * reference itself. In other words, not inclusive of the the four bits of the
 * reference itself.
 * 
 * A series of references {r1, r2, r3,..., 0}, where r1 > r2 > r3 > ... > 0, can
 * be interpreted as a stack of objects or primitives having size r1 - r2 - 4,
 * r2 - r3 - 4, etc. positioned following the four bytes of the 0 in reverse
 * order -- ...., r3, r2, r1.
 * 
 * Whether these are address pointers to objects or lengths of data structures
 * is not defined in the file itself. The reader must interpret them
 * appropriately. So, for example, if the block read looks like this:
 * 
 * <code> 
31: 0x0000007F = 71 -> 106
35: 0x0000002E = 46 -> 85	
39: 0x0000001E = 30 -> 73	
43: 0x0000000A = 10 -> 57	
47: 0x00000005 = 5 -> 56	
51: 0x00000000 = 0 -> 55	
55: 0x00010000 = 65536
59: 0x00000000 = 0	
63: 0x00080000 = 524288	
67: 0x00000000 = 0	
71: 0x00000000 = 0	
75: 0x00000000 = 0	
79: 0x00040000 = 262144	
83: 0x00000000 = 0	
87: 0x00016E00 = 93696	
91: 0x00000000 = 0	
95: 0x00000800 = 2048	
99: 0x00000000 = 0
103: 0x000000 
</code>
 * 
 * The pointers are {71, 46, 30, 10, 5, 0}. There are five data elements here.
 * First up are two single bytes (5 - 0 - 4 = 1 and 10 - 5 - 4 = 1). These are
 * followed by a 16-byte element (30 - 10 - 4 = 16), a twelve-byte element (46 -
 * 30 - 4 = 12), and a 21-byte element (71 - 46 - 4 = 21).
 * 
 * Read as bytes and ints, we have the sequence:
 * 
 * 0x00, 0x01, [0, 8, 0, 0], [0, 4, 0], and [1, 0x6E, 8, 0, 0]
 * 
 * (Note that we have interpreted the 1 starting at byte 53 as an integer, and
 * the next byte as its own element. We could be completely wrong here. [0, 8]
 * could indicate that the next eight bits are a long, and [0, 4] could indicate
 * an int. Maybe they are all just bytes. That's the fun part!)
 * 
 * Included in the superclass ByteBlockReader are several utility methods that
 * can be used to read the byte array in syntactically defined ways. For
 * example, readBLock() reads a four-byte address and creates a ByteBuffer field
 * comprising the bytes from the current address (after reading that 4-byte
 * reference) to the address pointed to by the reference. This ByteBuffer can
 * then be to read data from that particular block of bytes using super.get...
 * methods.
 * 
 * The reader is very fast, since all we are doing is manipulating pointers, for
 * the most part.
 * 
 * Again, the syntactic meaning of the pointers and data elements is a complete
 * guess.
 * 
 * @author hansonr
 *
 */
class MNovaMetadataReader extends ByteBlockReader {

	private final static int magicNumber = 0x4D657374; // M e s t
	/**
	 * big-/little-endian tests; it's totally unclear what would happen if this file
	 * were in litte-endian format.
	 */
	private final static int magicNumberBE = 0xF1E2D3C4;// F1 E2 D3 C4
	private final static int magicNumberLE = 0xC4D3E2F1;// C4 D3 E2 F1

	private MestrelabIFSVendorPlugin plugin;

	private int nPages = 0;
	public String mnovaVersion;
	public int mnovaVersionNumber;

	/**
	 * For testing only, with no extractor plugin.
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	MNovaMetadataReader(byte[] bytes) throws IOException {
		super(bytes);
	}

	MNovaMetadataReader(MestrelabIFSVendorPlugin mestrelabIFSVendorPlugin, byte[] bytes) throws IOException {
		super(bytes);
		this.plugin = mestrelabIFSVendorPlugin;
	}

	public boolean isMNova() throws IOException {
		return checkMagicNumber(magicNumber);
	}

	/**
	 * Process the file.
	 * 
	 * @return
	 */
	public boolean process() {
		try {
			setByteOrder(ByteOrder.BIG_ENDIAN);
			test();
			if (!checkByteOrder())
				return false;
			out: for (int i = 0;; i++) {
				System.out.println("\nblock " + i + " pos " + readPosition() + " avail " + readAvailable());
				switch (i) {
				case 0:
					// The version block should be the last block here (first pointed to), so we
					// skip everything except it.
					readSubblock(1);
					readVersion();
					break;
				case 1:
					readBlock(); // unknown 16 bytes
					boolean skipItems = true;
					if (skipItems) {
						// necessary for v.14 right now
						readBlock(); // skip to pages
					} else {
						readItems();
					}
					break;
				case 2:
					readPages();
					break;
				default:
					if (!readBlock())
						break out;
				}
			}
			System.out.println("MNovaReader ------- nPages=" + nPages);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				System.out.println("closing pos " + readPosition() + " avail " + readAvailable());
				close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Check that the magic number (first four bytes) is correct, and that the next
	 * four bytes (F1 E2 D3 C4 - clever!) are in the proper order.
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean checkByteOrder() throws IOException {
		if (!isMNova())
			return false;
		readSimpleString(23); // Mestrelab Research S.L.
		if (!checkMagicNumber(magicNumberBE)) { //
			if (!checkMagicNumber(magicNumberLE)) {
				return false;
			}
			setByteOrder(ByteOrder.LITTLE_ENDIAN);
		}
		readInt(); // 0xF1E2D3C4
		return true;
	}

	private void readVersion() throws IOException {
		readUTF16String(); // MestReNova
		mnovaVersion = readUTF16String(); // 12.0.0-20080
		if (plugin != null)
			plugin.setVersion(mnovaVersion);
		System.out.println("MNova version " + mnovaVersion);
		try {
			mnovaVersionNumber = Integer.parseInt(mnovaVersion.substring(0, mnovaVersion.indexOf(".")));
		} catch (NumberFormatException nfe) {
			mnovaVersionNumber = Integer.MAX_VALUE;
		}
	}

	/**
	 * Read the items. Bypassed in current code.
	 */
	private void readItems() throws IOException {
		readPointer();
		readInt();// 0
		long pt = readPointer(); // next; EOF in v.14
		int nItems = readInt();
		if (nItems < 0) {
			for (int j = 0; j < -nItems; j++) {
				readItem14(j);
			}
		} else {
			if (testing) {
				for (int j = 0; j < nItems; j++) {
					readItem(j);
				}
			} else {
				seekIn(pt);
			}
		}
		return;
	}

	/**
	 * Read an item:
	 * 
	 * 
	 * type
	 * 
	 * i
	 * 
	 * String "MestReNova"
	 * 
	 * int relative address to next record
	 * 
	 * ... information
	 * 
	 * 
	 * @param index
	 * @throws IOException
	 */
	private void readItem(int index) throws IOException {
		long pos = readPosition();
		int type = readInt(); // 0, 53, 63 -- version dependent?
		int i = readInt();
		if (testing)
			System.out.println("---item--- " + index + " pos=" + pos + " type=" + type + " i=" + i);
		if (type == 0) {
			// older version?
		} else {
			readInt(); // 0
			readItemHeader();
		}
		readLenString(); // Import Item, for example
		readFourUnknownInts();
		readUTF16String(); // name?
		readInts(2);
		readByte(); // 255
		readInt(); // -1
		int test = readInt();
		if (test > 0) {
			readInt();
			readInt();
			String itemType = readLenString(); // "Item Type"
			String subtype = readLenString(); // "NMR Spectrum"
			System.out.println("Item " + (index + 1) + ": " + itemType + "/" + subtype);
		}
		if (testing)
			System.out.println("---item end---" + (readPosition() - pos) + " pos=" + readPosition());
	}

	private void readFourUnknownInts() throws IOException {
		readInts(4);
	}

	private void readItemData14(int index) throws IOException {
		readLenString(); // Import Item, for example
		readInts(4); // these are often the same for non-spectra?
		readUTF16String(); // user name?
		readInts(2);
		readByte(); // 255
		readInt(); // -1
		int test = readInt();
		if (test > 0) {
			int i = 0;
			while (peekInt() != -1) {
				readBlock();
				++i;
			}
			System.out.println(i + " items read");
			readInt(); // -1
			return;
		}
	}

	/**
	 * Read an item header.
	 * 
	 * @return pointer to next record
	 * @throws IOException
	 */
	private long readItemHeader() throws IOException {
		readLenString(); // MestReNova
		readInt(); // pointer?
		readInt(); // 0x3000C;
		readLenString(); // Windows 10
		readLenString(); // DESKTOP-ORV6S3F
		long pt = readPointer();
		return pt;
	}

	/**
	 * key for
	 */
	private final static int[] paramsKey = new int[] { 0xD, 0x5, 0x0 };

	private void readPages() throws IOException {
		if (testing)
			System.out.println("---readPages " + readPosition()); // 38628 - 39077
		readInt(); // to EOF

		readPageInsets();
		readLong(); // long to EOF or next block?
		int nPages = readInt();
		readInt(); // also to EOF
		for (int i = 0; i < nPages; i++) {
			readPage(i);
		}
		if (testing)
			System.out.println("---read done ");
	}

	private void readPageInsets() throws IOException {
		readBlock(); // 32 bytes
		// pairs of integers, e.g. (0,0), (296,209), (5,5) (291, 204)
	}

	/**
	 * Just skipping most of this information now, but it gives us the pointer we
	 * need for skipping to the next block.
	 * 
	 * @param index
	 * @return
	 * @throws IOException
	 */
	private void readPage(int index) throws IOException {
		System.out.println("reading page " + (index + 1));
		nPages++;
		if (plugin != null)
			plugin.newPage();
		readBlock(); // 40 or 61 bytes, depending upon version
		int len = peekInt(); // to next spectrum or EOF
		long pt = readPointer();
		readBlock(); // 40
		readCoordinateBlock(); // 32
		readFourUnknownInts();
		readLongPtr(); // long ptr
		readInt(); // 2 -- two what?   4 in taxol
		readInt(); // pointer to what?
		readInt();
		readInt();
		readBlock(); // 109
		boolean doSearch = false;
		int test = peekInt();
		if (test == -1) {
			// v 14 hack
			seekIn(readPosition() + 15);
			readItemHeader();
			readItemData14(-1);
			readInts(3);
			readUTF16String();
			readUTF16String();
			readInt();
			readFourUnknownInts();
			while (peekInt() == 1) {
				// second page is missing these in 1-v14.mnova
				readInt();
				readFourUnknownInts();
			}
			if (peekInt() == 0)
				readInt();
			peekInts(20);
			readBlock();
			readInt();
		} else {
			if (test == 0)
				readShort();
			readInt();
			readFourUnknownInts();
			readFourUnknownInts();
			readInt(); // ?
			readColorMap();
			readInt(); // 0
			readLenString(); // NMR Spectrum
			readLenString(); // N M R
			readDoubleBox();
			readByte(); // 00
			readLongPtr(); // to ?
		}
		if (doSearch) {
			// a quick hack -- only works in earlier versions. 
			int skip = findIn(paramsKey, len, false);
			if (skip >= 4) {
				// actually targeting the integer just before 0x0000000D0000000500000000, which
				// holds the number of parameters.
				skipIn(skip - 4);
				readParams();
			}
		} else {
			// skipping our way through to parameters.
			readBlock(); // 189 bytes in 1.mnova; 197 in v14
			readBlock(); // 5957 bytes in 1.mnova; 7107 in v14
			readPointer(); // to next
			readPointer(); // to ? 65042 in 1.mnova; 82869 in v14
			readPointer(); // to ? 60663 in 1.mnova; 78252 in v14
			readBlock();   // 1081 bytes in 1.mnova; 11373 in v14
			readPointer(); // to 50418 in 1.mnova; 68007 in v14
			readInt(); // 0
			readParams();
		}
		seekIn(pt);
	}

	/**
	 * early hack
	 * 
	 * @param index
	 * @throws IOException
	 */
	private void readItem14(int index) throws IOException {
		long pos = readPosition();
		int type = readInt(); // 0, 53, 63 --0xC0000000 ? version dependent?
		int i = (type < 0 ? -1 : readInt());
		if (testing)
			System.out.println("---item--- " + index + " pos=" + pos + " type=" + type + " i=" + i);
		long pt = -1;
		if (type == 0) {
			// older version?
		} else {
			readInt(); // 0
			pt = readItemHeader();
		}
		System.out.println("item pt=" + pt);
		readItemData0(index);
		if (testing)
			System.out.println("---item end---" + (readPosition() - pos) + " pos=" + readPosition());
	}

	private void readItemData0(int index) throws IOException {
		readLenString(); // Import Item, for example
		readFourUnknownInts();
		readUTF16String(); // user name?
		readInts(2);
		readByte(); // 255
		readInt(); // -1
		int test = readInt();
		if (test > 0) {
			readInt(); // 0x1C
			readInt(); // 0xC
			if (peekInt() == 0) {
				readInts(3); // ?
				readBlock();
			}
			String itemType = readLenString(); // "Item Type"
			String subtype = readLenString(); // "NMR Spectrum"
			System.out.println("Item " + (index + 1) + ": " + itemType + "/" + subtype);
		}
	}


// not for V.14
//	private void readPageOld(int index) throws IOException {
//		System.out.println("reading page " + (index + 1));
//		nPages++;
//		if (plugin != null)
//			plugin.newPage();
//		readBlock(); // 40 or 61 bytes, depending upon version
//		int len = peekInt();
//		long pt = readPointer();
//		int skip = findIn(paramsKey, len, false);
//		if (skip >= 4) {
//			// actually targeting the integer just before 0x0000000D0000000500000000, which
//			// holds the number of parameters.
//			skipIn(skip - 4);
//			readParams();
//		}
//		seekIn(pt);
//	}

	private void readColorMap() throws IOException {
		readBlock(); // 64 - color map
	}

	private void readCoordinateBlock() throws IOException {
		readBlock();
	}

	/**
	 * Key method fore reading all parameters available in the MNova file.
	 * 
	 * @throws IOException
	 */
	private void readParams() throws IOException {
		int n = readInt();
		for (int i = 0; i < n; i++) {
			readParam(i);
		}
		System.out.println(" processed " + n + " parameters");
		return;

	}

	/**
	 * A class for holding units, source, calculation, and value for an MNova
	 * parameter.
	 * 
	 * @author hansonr
	 *
	 */
	class Param {

		String units;
		String source;
		String calc;
		String value;

		/**
		 * Read the parameter data.
		 * 
		 * @param index just for debugging -- 1 or 2 for now.
		 * 
		 * @throws IOException
		 */
		Param(int index) throws IOException {
			readInt(); // 8, 9, A (or 0,2 for older version)
			int p = peekInt();
			if (p >= 0) {
				units = readUTF16String();
			} else {
				readInt();
			}
			boolean isOld = (peekInt() != 0);
			if (!isOld) {
				readInt(); // 0 (or for older version)
				p = peekInt();
				if (p >= 0) {
					source = readUTF16String();
				} else {
					readInt();
				}
			}
			p = peekInt();
			if (p >= 0) {
				calc = readUTF16String();
			} else {
				readInt();
			}
			readByte(); // FF or 0?
			p = peekInt();
			if (p >= 0) {
				value = readUTF16String();
			} else {
				readInt();
			}
//			if (index == 2) {
//				System.out.println(toString());
//				return;
//			}
		}

		@Override
		public String toString() {
			String s = value + (units == null ? "" : " " + units) + (source == null ? "" : " FROM " + source);
			return s;
		}
	}

	/**
	 * Key method to read a parameter from the MNova file.
	 * 
	 * @param index for debugging only
	 * @return
	 * @throws IOException
	 */
	private void readParam(int index) throws IOException {
		System.out.println("readParam " + index + " " + readPosition());
		readInt(); // D
		readInt(); // 5
		readByte(); // 0
		readInt(); // 1
		int type = readInt();
		int count = readInt(); // 1 or 2 (or more?)
		Param param1 = new Param(1);
		Param param2 = (count == 2 ? new Param(2) : null);
		readByte();
		String key = readUTF16String();
		if (plugin != null)
			plugin.addParam(key, null, param1, param2);
		if (testing)
			System.out.println(" param " + (index + 1) + ": type=" + type + " p1=" + param1 + " p2=" + param2);
	}

	// first try, for reference. Or see the GitHub history.
	//
//		public boolean process0() {
//			
//			int headerLength = 23;
//			setByteOrder(ByteOrder.BIG_ENDIAN);
//			try {
//				if (!checkMagicNumber(magicNumber))
//					return false;
//				readSimpleString(headerLength);
//				int maxBlocks = 20;
//				out: for (int i = 0; i < maxBlocks; i++) {
//					System.out.println("\nblock " + i + " pos " + readPosition() + " avail " + readAvailable());
//					switch (i) {
//					case 0:
//						readInt(); // F1E2D3C4
//						if (!readSubblock())
//							break out;
//						break;
//					case 1:
//						readUTF16String();
//						readUTF16String();
//						break;
//					case 2:
//						readBlock();
//						break;
//					case 3:
//						readSubblock();
//						break;
//					case 4:
//						// Discovered this was not necessary;
//						if (testing)
//							readItems();
//						else
//							skipBlock();
//						break;
//					case 5:
//						readPages();
//						break;
//					default:
//						if (!readBlock())
//							break out;
//					}
//				}
//				System.out.println("MNovaReader ------- nPages=" + nPages);
//				return true;
//			} catch (Exception e) {
//				e.printStackTrace();
//				return false;
//			} finally {
//				try {
//					System.out.println("closing pos " + readPosition() + " avail " + readAvailable());
//					close();
//				} catch (IOException e) {
//				}
//			}
//		}

	private static String testFile;

	public static void main(String[] args) {
		String fname = (args.length == 0 ? testFile : args[0]);
		try {
			testing = true;
			showInts = true;
			//showChars = true;
			String filename = new File(fname).getAbsolutePath();
			byte[] bytes = Util.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println(bytes.length + " bytes in " + filename);
			new MNovaMetadataReader(bytes).process();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * various tests
	 * 
	 * @throws IOException
	 */
	private void test() throws IOException {
		if (!testing)
			return;
		rewindIn();
//		checkDouble(38969, 4);
//		traceRef(461130, true);
//		peekIntsAt(45011, 50);
//		seekIn(45011);
		return;
	}

	static {
		//testFile = "test/mnova/3a-C.mnova"; // OK one page
		//testFile = "test/mnova/1.mnova"; // OK two pages
		//testFile = "test/mnova/1-v14.mnova"; // OK two pages
		testFile = "test/mnova/3a-C-taxol.mnova"; // (v 14) OK, but looking for model
	}

}
