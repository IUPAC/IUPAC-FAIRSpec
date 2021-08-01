package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Stack;

import org.iupac.fairspec.util.Util;

import com.integratedgraphics.ifs.vendor.ByteBlockReader;

import javajs.util.Rdr;

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
 * The basic MNova file format is as follows (example from 1.mnova test sample):
 *
 * <code>
 0000000  23-byte sequence "Mestrelab Research S.L.
 0000023  0xF1E2C3D4  byte order test mark

           block 1 -- 40-byte block of pointers:
 0000027  -> to block 2 (158 here)
 0000031  -> to version (106 here)
 ...      
           version
 0000106  20 ".M.e.s.t.R.e.N.o.v.a"  ('.' being <00>, unicode high byte)
 0000124  24 ".1.2...0.3._.2. .1.3.8.4" 

           block 2
 0000158  -> to block 3 (178 here)
 ...
           block 3
 0000178  -> to pages (38628 here) 
 ...
 0038628  -> EOF* (*or a final signature block just before that)
 0038632  -> page header (38668 here)
 0038636  eight ints of what looks like page offsets (0,0,296,209,5,5,291,204)
 0038668  0
 0038672  -> EOF*
 0038676  page count (2 here)
 0038672  -> EOF*         
           page 1
 0038684  40 -> 38728 (40 or 61 bytes, depending upon the version)
 ...
 0038728  -> to next page (408829 here)
 ...
           page 2
 0408829  -> EOF* (in this case)
 ...
 0765763  1
 0765767  17 "MestReNova.Uh7580"   

</code>
 * 
 * The reader is very fast, since all we are doing is manipulating pointers, for
 * the most part.
 * 
 * Again, the syntactic meaning of the pointers and data elements is a complete
 * guess. All we are interested in here are the parameters. So, how to get them?
 * 
 * 
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
	private int nSpectra;
	private int nMolecules;

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

	private final static boolean newWay = true;
	
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
			if (newWay) {
				readFileAsStack();				
			} else {
				readPointer();
				readBlock();
				readVersion();
				readBlock(); // unknown 16 bytes
				boolean skipItems = true;
				if (skipItems) {
					// necessary for v.14 right now
					readBlock(); // skip to pages
				} else {
					readItems();
				}
				readPages();
				while (readAvailable() > 0)
					skipBlock();			
			}
			System.out.println("MNovaReader ------- nPages=" + nPages 
					+ " nSpectra=" + nSpectra
					+ " nMolecules=" + nMolecules);
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
		setByteOrder(ByteOrder.BIG_ENDIAN);
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

	private final static int[] cdxKey = new int[] { 0x44494600, 0x566A4344 };// le x00464944, 0x44436A56 }; // 

	public static final String CDX_FILE_DATA = ".cdx";
	public static final String MOL_FILE_DATA = ".mol";

	private int readPages() throws IOException {
		if (testing)
			System.out.println("---readPages " + readPosition()); // 38628 - 39077
		readInt(); // to EOF

		readPageInsets();
		readLong(); // long to EOF or next block?
		int nPages = readInt();
		readInt(); // also to EOF
		nSpectra = 0;
		for (int i = 0; i < nPages; i++) {
			readPage(i);
		}
		if (testing)
			System.out.println("---read done for " + nPages + " pages");
		return nPages;
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
		long pt = readToParameters();
		if (pt < 0) {
			pt = -pt;
		} else {
			nSpectra++;
			if (plugin != null)
				plugin.newPage();
			readParams();
			searchForMolecule(index, pt);
		}
		seekIn(pt);
	}

	/**
	 * Read to the parameters for this page. 
	 * 
	 * @return -pt if no parameters, otherwise pt to next page
	 * 
	 * @throws IOException
	 */
	private long readToParameters() throws IOException {
		readBlock(); // 40 or 61 bytes, depending upon version
		int len = peekInt(); // to next spectrum or EOF
		long pt = readPointer(); // to next page
		readPageBlock2(); // 40
		readPageInsets(); // 32
		readFourUnknownInts();
		readInt(); // 0
		readPointer(); // to next page
		readInt(); // 2 -- two what? 3? 4 in taxol
		readPointer(); // to next page
		if (readPosition() == pt)
			return -pt; // nothing on this page
		readInt(); // 0
		readPointer(); // to ? 394266 in 1.mnova
		readInt(); // usually 109; can be 107? Not a pointer 
		if (peekInt() == 0) {
			// no spectrum 
			return -pt;
		}
		readFourUnknownInts();
		readInt(); // 0 in 1.mnova
		readBlock(); // 
		readBlock();
		readInt();
		boolean doSearch = false;
		if (doSearch) {
			// a quick hack -- only works in earlier versions.
			int skip = findIn(paramsKey, len, false);
			if (skip < 4)
				return -1;
			if (skip >= 4) {
				// actually targeting the integer just before 0x0000000D0000000500000000, which
				// holds the number of parameters.
				skipIn(skip - 4);
			}
		} else {
			// skipping our way through to parameters.
			readBlock(); // 189 bytes in 1.mnova; 197 in v14
			readBlock(); // 5957 bytes in 1.mnova; 7107 in v14
			readPointer(); // to next
			readPointer(); // to ? 65042 in 1.mnova; 82869 in v14
			readPointer(); // to ? 60663 in 1.mnova; 78252 in v14
			readBlock(); // 1081 bytes in 1.mnova; 11373 in v14
			readPointer(); // to 50418 in 1.mnova; 68007 in v14
			readInt(); // 0
		}
		return pt;
	}

	/**
	 * Read an unknown block on every page.
	 * 
	 * @throws IOException
	 */
	private void readPageBlock2() throws IOException {
		readBlock();
		// 1.mnova test
//		38732 reading 40 to 38776
//		38736: 0x000036DB = 14043 -> 52783	
//		38740: 0x000036DB = 14043 -> 52787	
//		38744: 0x40279F3E = 1076338494 -> 1076377242	11.811023622047244
//		38748: 0x7CF9F3E8 = 2096755688 -> 2096794440	
//		38752: 0x4072C000 = 1081262080 -> 1081300836	300.0
//		38756: 0x00000000 = 0	
//		38760: 0x00000000 = 0	
//		38764: 0x00000000 = 0	
//		38768: 0x000036DA = 14042 -> 52814	
//		38772: 0x000036DA = 14042 -> 52818	
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
		readByte(); // 255
		readInts(2);
		readInt(); // -1
		int test = readInt();
		if (test > 0) {
			while (peekInt() != -1) {
				readBlock();
			}
		}
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
		}

		@Override
		public String toString() {
			if (value == null)
				return null;
			return value.replace('\n', ' ') 
					+ (units == null ? "" : " " + units)
					+ (source == null ? "" : " FROM " + source);
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
		if (testing)
			System.out.println("readParam " + (index + 1) + " at " + readPosition());
		readBlock(); // D 5 0 1 type
		int count = readInt(); // 1 or 2 (or more?)
		Param param1 = new Param(1);
		Param param2 = (count == 2 ? new Param(2) : null);
		readByte(); // EF, e.g. -- identifier?
		String key = readUTF16String();
		if (plugin != null)
			plugin.addParam(key, null, param1, param2);
		System.out.println(" param " + (index + 1) + " " + key + " = " + param1 + (param2 == null ? "" : "," + param2));
	}

	private void dumpFileInfo() throws IOException {
		showChars = false;
		showInts = false;
		rewindIn();
		seekIn(27);
		long ptr27 = readPointer();
		long ptr31 = readPointer();
		seekIn(ptr31);
		readVersion();
		seekIn(27);
		readBlock();
		readBlock();
		readBlock();
		// pages
		System.out.println("\n===Pages start at " + readPosition());
		readPointer(); // -> EOF
		readBlock(); // 32
		readInt(); // 0
		readPointer(); // -> EOF
		int nPages = readInt();
		System.out.println("...Page count = " + nPages);
		readPointer(); // -> EOF
		for (int i = 0; i < nPages; i++) {
			System.out.println("\n======Page " + (i + 1) + " starts at " + readPosition());
			long ptNext = readToParameters();
			System.out.println(" params at " + readPosition());

			readParams();
			System.out.println("\n======Page " + (i + 1) + " parameters end at " + readPosition());
			searchForMolecule(i, ptNext);
			seekIn(ptNext);
		}
		System.out.println("\n===Pages end at " + readPosition() + " available=" + readAvailable());
	}
	
	private void searchForMolecule(int index, long ptNext) throws IOException {
		int nBlocks = 0;
		long lastPosition = -1;
		boolean haveCDX = false;
		while (readPosition() < ptNext) {
			if (peekInt() > 0) {
				nBlocks++;
				lastPosition = readPosition();
				int len = peekInt();
				System.out.println("extra block " + nBlocks + " len=" + len + " from " + lastPosition + " to "
						+ (lastPosition + len));
				int cdx = (haveCDX ? -1 : findIn(cdxKey, len, false));
				if (cdx >= 0) {
					haveCDX = true;
					checkCDX(lastPosition, cdx);
					continue;
				}
			}
			readBlock();
		}
		System.out.println("\n======Page " + (index + 1) + " additional blocks: " + nBlocks);
		if (lastPosition >= 0)
			checkMolecule(lastPosition);
	}

	private void checkCDX(long lastPosition, int skip) throws IOException {
		testing = showInts = showChars = true;
		seekIn(lastPosition);
		long ptNext = readPointer();
		skipIn(skip - 9);
		ByteOrder bo = byteOrder;
		setByteOrder(ByteOrder.LITTLE_ENDIAN);
		int len = readInt() - 7; // accounts for extra 0 0 
		readInt();
		readByte();
		setByteOrder(bo);
		// VjCD block peekBlockAsString(true);
		byte[] cdxFileData = new byte[len];
		read(cdxFileData, 0, len);
		System.out.println("=====CDX FILE DATA>>>>>");
		nMolecules++;
		System.out.println("[" + cdxFileData.length + " bytes]");
		System.out.println("<<<<<CDX FILE DATA=====");		
		handleModelFileData(CDX_FILE_DATA, cdxFileData, len);
		seekIn(ptNext);
		
	}

	private void checkMolecule(long lastPosition) throws IOException {
 		// notes are for 1-taxol-drop.mnova
//		peekIntsAt(lastPosition - 80, 20 + 0);
//		peekIntsAt(lastPosition, 40);
		//testing = true;
		seekIn(lastPosition);
		long ptNext = readPointer();
		readInt();//
		if (peekInt() == 0)
			return;
		readFourUnknownInts();
		readInt(); // 0
		readBlock(); // -> 501150
		readBlock(); // 178-long Molecule block
		readInt(); // to next
		if (peekInt() == 0)
			return; // 3a-c.mnova
		peekInts(10);
		readBlock();
		peekInts(10);
		readBlock();
		peekInts(10);
		readBlock();
		peekInts(10);
		readSubblock(4);
		peekInts(10);
		int len = peekInt();
		handleModelFileData(MOL_FILE_DATA, readLenString(), len);
		seekIn(ptNext);
	}

	private void handleModelFileData(String type, Object fileData, int len) {
		nMolecules++;
		System.out.println("=====" + type + ">>>>>");
		if (plugin != null)
			plugin.addParam(type, fileData, null, null);
		System.out.println(fileData);
		System.out.println("<<<<<"+ type +"=====");		
		if (testing) {
			File f = new File("test" + zeroFill(nPages, 2) + type);
			try (FileOutputStream fis = new FileOutputStream(f)) {
				if (fileData instanceof String) {
					fileData = ((String) fileData).getBytes();
				}
				fis.write((byte[]) fileData);
				System.out.println("File " + f.getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final static String zeros = "0000";

	private static String zeroFill(int n, int ndig) {
		// n = 100, ndig = 2, s = "0000100" -> "100"
		String s = zeros + n;
		int len;
		return s.substring((len = s.length()) - Math.max(len - 4, ndig));
	}

	private static String testFile;

	public static void main(String[] args) {
		String fname = (args.length == 0 ? testFile : args[0]);
		try {
			String filename = new File(fname).getAbsolutePath();
// this is the 158-MB file
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
		rewindIn();
		
		testing = true;
		showInts = true;
		showChars = true;
		setByteOrder(ByteOrder.LITTLE_ENDIAN);
		setByteOrder(ByteOrder.BIG_ENDIAN);
		// dumpFileInfo();

		showInts = false;
		showChars = false;
		testing = false;
		return;
	}

	/**
	 * The cleanest way to read the file found so far. 
	 * 
	 * 1) read the 27-byte header.
	 * 2) read the pointers onto a stack
	 * 3) read byte[] objects off the stack
	 * 4) process the pages
	 * 
	 * @throws IOException
	 */
	private void readFileAsStack() throws IOException {
		rewindIn();
		seekIn(27);
		Stack<Block> objects = getObjectStack();
		System.out.println(objects.size() + " objects");
		long pt = readPosition();
		// last is version
		Block verObject = objects.pop();
		seekIn(verObject.loc);
		readVersion();
//		testStack(objects);
		// at pages
		seekIn(pt);
		readBlock(); // 16
		readBlock(); // items?
		int nPages = readPages();
		while (readAvailable() > 0) {
			skipBlock();
		}
		System.out.println(nPages + " pages processed, version=" + mnovaVersion);
	}

	private void testStack(Stack<Block> objects) throws IOException {
		Enumeration<Block> e = objects.elements();
		while (e.hasMoreElements()) {
			Block obj = e.nextElement();
			System.out.println("obj " + obj);
			int len = obj.len;
		}
	}

	static {
		 //testFile = "test/mnova/3a-C.mnova"; // OK one page, with ChemDraw drawing
		 //testFile = "test/mnova/1.mnova"; // OK two pages
		 //testFile = "test/mnova/1-deleted.mnova"; // first page param list only, next page blank
		//testFile = "test/mnova/1-v14.mnova"; // OK two pages
		//testFile = "test/mnova/3a-C-taxol.mnova"; // (v 14) OK, but looking for model
		//testFile = "test/mnova/1-caff-taxol.mnova"; // two structures
		//testFile = "test/mnova/1-caff-taxol-rev.mnova"; // two structures
		// testFile = "test/mnova/1-caff-taxol-delete.mnova"; // caffeine deleted in
		
		// ok for structure:
		// testFile = "test/mnova/1-taxol-drop.mnova"; // caffeine deleted in page 1
		// testFile = "test/mnova/1-taxol-drop-move.mnova"; // caffeine deleted in page
		// testFile = "test/mnova/3a-c-morphine.mnova"; // morphine.mol added using file...open
		testFile = "c:\\temp\\iupac\\zip\\22232721\\metadatanmr\\nmr spectra.mnova";
	}

}
