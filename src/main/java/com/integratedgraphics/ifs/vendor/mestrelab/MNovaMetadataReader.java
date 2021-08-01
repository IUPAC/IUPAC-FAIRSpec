package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Stack;

import org.iupac.fairspec.util.Util;

import com.integratedgraphics.ifs.vendor.ByteBlockReader;

/**
 * A rough MestReNova file reader that can deliver metadata only (including CDX
 * and MOL files).
 * 
 * DISCLAIMER: I have not seen any MestReNova code, so I am guessing here.
 * 
 * File Format
 * 
 * The file format appears to be based on a nested set of big-endian block data
 * that can be read from the data stream. Internal references are
 * forward-relative. I assume they are integer references.
 * 
 * So we have:
 * 
 * [ block ] [ block ] ... [ block ]
 * 
 * where [ block ] consists of a pointer stack and a set of data items. The
 * stack is a set of 32-bit integer "forward references" ending in 0.
 * 
 * [ [ pointer-n ] [ pointer-(n-1) ] [ pointer-(n-2) ] ... [ pointer-1 ] 0 ]
 * 
 * Following this are the data items:
 * 
 * [ [ data-1 ] [ data-2 ] [ data-3 ] ... [data-n] ]
 * 
 * Block data can -- and usually do -- contain more data blocks themselves.
 * 
 * A 32-bit reference of 109 points to an address 109+4 bytes past the reference
 * itself. In other words, the reference value 109 does not include the the four
 * bits of the reference itself. These references are to the buffer position
 * *after* the data pointed to. So, for example, if the block read looks like
 * this:
 * 
 * <code> 
31: 0x0000007F = 71 -> 106   Pointer 5 to 21 bytes (106 - 85)
35: 0x0000002E = 46 -> 85	 Pointer 4 to 12 bytes (85 - 73)
39: 0x0000001E = 30 -> 73	 Pointer 3 to 16 bytes (73 - 57)
43: 0x0000000A = 10 -> 57	 Pointer 2 to one byte (57 - 56)
47: 0x00000005 = 5 -> 56	 Pointer 1 to one byte (56 - 55)
51: 0x00000000 = 0 -> 55	 start of data
55: 0x00010000 = 65536       Item 1 [0x00], Item 2 [0x01], and part of Item 3 
59: 0x00000000 = 0	         Item 3
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
106:
</code>
 * 
 * The pointers are {71, 46, 30, 10, 5, 0}. There are five data elements here.
 * First up are two single bytes (5 - 0 - 4 = 1 and 10 - 5 - 4 = 1). These are
 * followed by a 16-byte element (30 - 10 - 4 = 16), a 12-byte element (46 - 30
 * - 4 = 12), and a 21-byte element (71 - 46 - 4 = 21).
 * 
 * Read as bytes and ints, we have the sequence:
 * 
 * 0x00, 0x01, [0, 8, 0, 0], [0, 4, 0], and [1, 0x6E, 8, 0, 0]
 * 
 * (Note that I have interpreted the 32 bits starting at byte 53 as an integer
 * (the number 1, 0x00000001), and the next byte as its own element (0x6E). I
 * easily could be completely wrong here. [0, 8] could indicate that the next
 * eight bits are a long, and [0, 4] could indicate an int. Maybe they are all
 * just bytes. Who knows? That's the fun part!)
 * 
 * Included in the superclass ByteBlockReader are several utility methods that
 * can be used to read the byte array in syntactically defined ways. For
 * example, nextBlock() reads a four-byte address and creates a ByteBuffer field
 * comprising the bytes from the current address (after reading that 4-byte
 * reference) to the address pointed to by the reference. This ByteBuffer can
 * then be to read data from that particular block of bytes using super.get...
 * methods.
 * 
 * Byte Order
 * 
 * All examples I have seen are for the most part big-endian byte order. There
 * are situations where the format switches to little-endian format. But this
 * seems to be minimal, and I only found it the case in association with CDX
 * file storage.
 * 
 * Strings
 * 
 * Strings are stored preceded by their length encoded as a 32-bit integer. They
 * may be straight ASCII character strings or UTF-16. There is no way I know of
 * to be sure which it will be. I had I had to look at the binary data and
 * decide each time whether the string was UTF-16 ([0x00] a [0x00] c [0x00] q
 * [0x00] u [0x00] s) or not.
 * 
 * 
 * Example
 * 
 * Several examples are in the GitHub folder test/mnova. These vary in versions,
 * including 7, 12, and 14.
 * 
 * An example basic MNova file format is as follows (example from 1.mnova test
 * sample in the GitHub test/mnova folder):
 *
 * <code>
 0000000  23-byte sequence "Mestrelab Research S.L.
 0000023  0xF1E2C3D4  byte order test mark
           block 1 -- 40-byte block of pointers:
 0000027  0x0000007F = 127 -> 158 (to part 2)
 0000031  0x00000047 = 71 -> 106 (to version)
 0000035  0x0000002E = 46 -> 85
 0000039  0x0000001E = 30 -> 73 
 0000043  0x0000000A = 10 -> 57 
 0000047  0x00000005 = 5 -> 56 
 0000051  0x00000000 = 0 (end of pointers) 
...
          version
 0000106  20 ".M.e.s.t.R.e.N.o.v.a"  ('.' being <00>, unicode high byte)
 0000124  24 ".1.2...0.3._.2. .1.3.8.4" 

           part 2
 0000158  -> to part 3 (178 here)
 ...
           part 3
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
 0038728  -> to next page (408829 here)
 ...
 0046403  parameters 
 ...
 0050418  additional block 1
 ...		(structure would be here somewhere)
 0408829  additional block 11          
 ...
           page 2
 0408829  -> EOF* (765767 in this case)
 ... etc.
 0765767  17 "MestReNova.Uh7580"   

</code>
 * 
 * Parameters
 * 
 * Again, the syntactic meaning of the pointers and data elements is a complete
 * guess. All we are interested in here are the parameters. These are found in
 * the page block using the readToParameters() method, several blocks down
 * within the page block. (This was determined ad hoc and could easily be
 * flawed.) Generally 31 parameters are encountered: <code>
   Data File Name = ...fid
   Title = Y12180222-0320-HWY-34.2.fid
   Comment = null
   Origin = Bruker BioSpin GmbH FROM acqus
   Owner = root FROM acqus
   Site = null
   Instrument = spect FROM acqus ("Spectrometer" in Version 7? JDX vs Bruker?)
   Author = null
   Solvent = CDCl3 FROM acqus   
   Temperature = 298.5953 FROM acqus
   Pulse Sequence = zgpg30 FROM acqus
   Experiment = 1D
   Probe = 5 mm PABBO BB/19F-1H/D Z-GRD Z116098/0047  FROM acqus
   Number of Scans = 64 FROM acqus
   Receiver Gain = 203 FROM acqus
   Relaxation Delay = 2 FROM acqus
   Pulse Width = 9 FROM acqus
   Presaturation Frequency = 
   Acquisition Time = 1.3631488
   Acquisition Date = 2019-03-21T05:40:00
   Modification Date = 2019-03-21T05:40:16 FROM acqus
   Class = null
   Purity = 100 %
   Spectrum Quality = 0.250679443239435
   Spectrometer Frequency = 100.622829328806
   Spectral Width = 24038.4615384615
   Lowest Frequency = -1958.90196322503
   Nucleus = 13C
   Acquired Size = 32768
   Spectral Size = 65536
   Absolute Reference = null
</code>
 * 
 * Molecules
 * 
 * Molecules are (somewhere) within the set of 10-12 blocks that follow the
 * parameter blocks. CDX and MOL files (as far as I can tell) have to be
 * discovered ungracefully, scanning the block data for key byte sequences, but
 * once the correct block is identified, scanning to the byte or string data
 * appears to be no problem. Once again, though, it is not at all guaranteed
 * that these methods are totally general.
 * 
 * MNova converts structure files to V2000 MOL files using OpenBabel.
 * 
 * 
 * General Performance
 * 
 * The reader is very fast, since all we are doing is manipulating pointers, for
 * the most part. Searching for byte sequences (MOL and CDX) takes a bit of
 * time, but the full file byte stream is read first into a
 * ByteBlockReader.RewindableInputStream that is then used as the basis for a
 * ByteBuffer.
 * 
 * @author hansonr 2021.08.01
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

	private final static byte[] molKey = new byte[] { 'M', ' ', ' ', 'E', 'N', 'D' };

	private final static byte[] cdxKey = new byte[] { /* (CD) IF\0 */ (byte) 0x49, (byte) 0x46, (byte) 0x00,
			/* VjCD */ (byte) 0x56, (byte) 0x6A, (byte) 0x43, (byte) 0x44 };

	public static final String CDX_FILE_DATA = ".cdx";
	public static final String MOL_FILE_DATA = ".mol";
	
	private static final int minBlockLengthForStructureData = 50;

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

	/**
	 * output CDX and MOL files
	 */
	private static boolean createStructureFiles = false;

	/**
	 * Process the file.
	 * 
	 * @return
	 */
	public boolean process() {
		try {
			// pretty sure this will always be big-endian.
			setByteOrder(ByteOrder.BIG_ENDIAN);
			if (!checkByteOrder())
				return false;
			test();
			readFileAsStack();
			System.out.println(
					"MNovaReader ------- nPages=" + nPages + " nSpectra=" + nSpectra + " nMolecules=" + nMolecules);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				System.out.println("closing pos=" + readPosition() + " avail=" + readAvailable());
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
	 * The cleanest way to read the file found so far.
	 * 
	 * 1) read the 27-byte header. 2) read the pointers onto a stack 3) read byte[]
	 * objects off the stack 4) process the pages
	 * 
	 * @throws IOException
	 */
	private void readFileAsStack() throws IOException {
		rewindIn();
		seekIn(27);
		Stack<BlockData> objects = getObjectStack();
		System.out.println(objects.size() + " objects");
		long pt = readPosition();
		// last is version
		BlockData verObject = objects.pop();
		seekIn(verObject.loc);
		readVersion();
//		testStack(objects);
		// at pages
		seekIn(pt);
		nextBlock(); // 16
		nextBlock(); // items?
		int nPages = readPages(readPosition());
		while (readAvailable() > 0) {
			nextBlock();
		}
		System.out.println(nPages + " pages processed, version=" + mnovaVersion);
	}

//	/**
//	 * Read the items. Bypassed in current code.
//	 */
//	private void readItems() throws IOException {
//		readPointer();
//		readInt();// 0
//		long pt = readPointer(); // next; EOF in v.14
//		int nItems = readInt();
//		if (nItems < 0) {
//			for (int j = 0; j < -nItems; j++) {
//				readItem14(j);
//			}
//		} else {
//			if (testing) {
//				for (int j = 0; j < nItems; j++) {
//					readItem(j);
//				}
//			} else {
//				seekIn(pt);
//			}
//		}
//		return;
//	}
//
//	/**
//	 * Read an item:
//	 * 
//	 * 
//	 * type
//	 * 
//	 * i
//	 * 
//	 * String "MestReNova"
//	 * 
//	 * int relative address to next record
//	 * 
//	 * ... information
//	 * 
//	 * 
//	 * @param index
//	 * @throws IOException
//	 */
//	private void readItem(int index) throws IOException {
//		long pos = readPosition();
//		int type = readInt(); // 0, 53, 63 -- version dependent?
//		int i = readInt();
//		if (testing)
//			System.out.println("---item--- " + index + " pos=" + pos + " type=" + type + " i=" + i);
//		if (type == 0) {
//			// older version?
//		} else {
//			readInt(); // 0
//			readItemHeader();
//		}
//		readLenString(); // Import Item, for example
//		readFourUnknownInts();
//		readUTF16String(); // name?
//		readInts(2);
//		readByte(); // 255
//		readInt(); // -1
//		int test = readInt();
//		if (test > 0) {
//			readInt();
//			readInt();
//			String itemType = readLenString(); // "Item Type"
//			String subtype = readLenString(); // "NMR Spectrum"
//			System.out.println("Item " + (index + 1) + ": " + itemType + "/" + subtype);
//		}
//		if (testing)
//			System.out.println("---item end---" + (readPosition() - pos) + " pos=" + readPosition());
//	}
//	/**
//	 * Read an item header.
//	 * 
//	 * @return pointer to next record
//	 * @throws IOException
//	 */
//	private long readItemHeader() throws IOException {
//		readLenString(); // MestReNova
//		readInt(); // pointer?
//		readInt(); // 0x3000C;
//		readLenString(); // Windows 10
//		readLenString(); // DESKTOP-ORV6S3F
//		long pt = readPointer();
//		return pt;
//	}
//
//
//	/**
//	 * early hack
//	 * 
//	 * @param index
//	 * @throws IOException
//	 */
//	private void readItem14(int index) throws IOException {
//		long pos = readPosition();
//		int type = readInt(); // 0, 53, 63 --0xC0000000 ? version dependent?
//		int i = (type < 0 ? -1 : readInt());
//		if (testing)
//			System.out.println("---item--- " + index + " pos=" + pos + " type=" + type + " i=" + i);
//		long pt = -1;
//		if (type == 0) {
//			// older version?
//		} else {
//			readInt(); // 0
//			pt = readItemHeader();
//		}
//		System.out.println("item pt=" + pt);
//		readItemData0(index);
//		if (testing)
//			System.out.println("---item end---" + (readPosition() - pos) + " pos=" + readPosition());
//	}
//
//	private void readItemData0(int index) throws IOException {
//		readLenString(); // Import Item, for example
//		readFourUnknownInts();
//		readUTF16String(); // user name?
//		readByte(); // 255
//		readInts(2);
//		readInt(); // -1
//		int test = readInt();
//		if (test > 0) {
//			while (peekInt() != -1) {
//				nextBlock();
//			}
//		}
//	}

	private void readFourUnknownInts() throws IOException {
		readInts(4);
	}

	private int readPages(long pt) throws IOException {
		seekIn(pt);
		if (testing)
			System.out.println("--- readPages " + readPosition()); // 38628 - 39077
		readPointer(); // to EOF or next block
		readPageInsets();
		readInt();
		readPointer(); // to EOF or next block
		int nPages = readInt();
		readPointer(); // also to EOF
		nSpectra = 0;
		for (int i = 0; i < nPages; i++) {
			readPage(i);
		}
		if (testing)
			System.out.println("--- " + nPages + " pages read");
		return nPages;
	}

	private void readPageInsets() throws IOException {
		nextBlock(); // 32 bytes
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
		nPages++;
		System.out.println("reading page " + (index + 1) + " pos=" + readPosition());
		long pt = readToParameters(readPosition());
		if (pt < 0) {
			// parameters where not found
			pt = -pt;
		} else {
			nSpectra++;
			if (plugin != null)
				plugin.newPage();
			readParams();
			searchForStructures(readPosition(), index, pt);
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
	private long readToParameters(long pt0) throws IOException {
		seekIn(pt0);
		readPageHeader();
		long pt = readPointer(); // to next page
		readPageBlock2(); // 40
		readPageInsets(); // 32
		readFourUnknownInts();
		readInt(); // 0
		readPointer(); // to next page
		readInt(); // 1 (in cyclehex.mnova? 2 -- two what? 3? 4 in taxol
		readPointer(); // to next page
		if (readPosition() == pt)
			return -pt; // nothing on this page
		readInt(); // 0
		readPointer(); // to ? 394266 in 1.mnova
		readInt(); // usually 109; can be 107, 110; id? type?  Not a pointer
		if (peekInt() == 0) {
			// no spectrum
			return -pt;
		}
		readFourUnknownInts();
		readInt(); // 0 in 1.mnova
		// skipping our way through to parameters -- ad hoc
		nextBlock(); // to 38964
		nextBlock(); // 172 to 39140 
		readPointer(); // -> 394266 
		nextBlock(); // 189 -> 39337 bytes in 1.mnova; 197 in v14
		nextBlock(); // 5957 -> 45298 bytes in 1.mnova; 7107 in v14
		Stack<BlockData> stack = getObjectStack();
		System.out.println(
				stack.elementAt(1).getData().length
				+ " " + stack.elementAt(1).getData()[0]);
		BlockData params = stack.get(stack.size() - 3);
		params.seek();
		readPointer();
		if (readInt() != 0)
			return -pt; // no parameters    // 0
		System.out.println(readPosition());
		return pt; // 46403
	}

	/**
	 * Read a 40 or 61 byte header, depending upon version
	 * @throws IOException
	 */
	private void readPageHeader() throws IOException {
		nextBlock();
	}

	/**
	 * Read an unknown block on every page.
	 * 
	 * @throws IOException
	 */
	private void readPageBlock2() throws IOException {
		nextBlock();
		// 1.mnova test -- unclear what this is
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
	 * Key method fore reading all parameters available in the MNova file.
	 * 
	 * @throws IOException
	 */
	private void readParams() throws IOException {
		System.out.println("parameters found at " + readPosition());
		int n = readInt(); // cound of parameters
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
			return value.replace('\n', ' ') + (units == null ? "" : " " + units)
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
		System.out.println(" param " + (index + 1) + " at " + readPosition());
		nextBlock(); // D 5 0 1 type
		int count = readInt(); // 1 or 2 (or more?)
		Param param1 = new Param(1);
		Param param2 = (count == 2 ? new Param(2) : null);
		readByte(); // EF, e.g. -- identifier?
		String key = readUTF16String();
		if (plugin != null)
			plugin.addParam(key, null, param1, param2);
		System.out.println("   " + key + " = " + param1 + (param2 == null ? "" : "," + param2));
	}

	/**
	 * The general idea.
	 * 
	 * @throws IOException
	 */
	void dumpFileInfo() throws IOException {
		showChars = false;
		showInts = false;
		rewindIn();
		seekIn(27);
		readPointer();
		long ptr31 = readPointer();
		seekIn(ptr31);
		readVersion();
		seekIn(27);
		nextBlock();
		nextBlock();
		nextBlock();
		// pages
		System.out.println("\n===Pages start at " + readPosition());
		readPointer(); // -> EOF
		nextBlock(); // 32
		readInt(); // 0
		readPointer(); // -> EOF
		int nPages = readInt();
		System.out.println("...Page count = " + nPages);
		readPointer(); // -> EOF
		for (int i = 0; i < nPages; i++) {
			long pt = readPosition();
			System.out.println("\n======Page " + (i + 1) + " starts at " + pt);
			long ptNext = readToParameters(pt);
			System.out.println(" params at " + readPosition());
			readParams();
			System.out.println("\n======Page " + (i + 1) + " parameters end at " + readPosition());
			searchForStructures(readPosition(), i, ptNext);
			seekIn(ptNext);
		}
		System.out.println("\n===Pages end at " + readPosition() + " available=" + readAvailable());
	}

	private void searchForStructures(long pos0, int index, long ptNext) throws IOException {
		int nBlocks = 0;
		boolean haveCDX = false;
		boolean haveMOL = false;
		seekIn(pos0); // for debugging dynamic change of method
		while (readPosition() < ptNext) {
			int len = peekInt();
			if (len > 0) {
				nBlocks++;
				long ptr = readPosition();
				if (testing)
					System.out.println("additional block " + nBlocks + " len=" + len + " from " + ptr + " to "
							+ (ptr + len) + " ptNext=" + ptNext);
				if (len > minBlockLengthForStructureData) {

					int offset = (haveMOL ? -1 : findBytes(molKey, len, false));
					if (offset >= 0) {
						haveMOL = true;
						exportMOL(ptr, offset);
					}
					if (readAvailable() == 0)
						return;
					offset = (haveCDX ? -1 : findBytes(cdxKey, len, false));
					if (offset >= 0) {
						haveCDX = true;
						exportCDX(ptr, offset);
						continue;
					}
				}
			}
			nextBlock();
		}
		if (testing)
			System.out.println("\n======Page " + (index + 1) + " additional blocks: " + nBlocks);
	}

	/**
	 * found the C
	 * 
	 * @param lastPosition
	 * @param skip
	 * @throws IOException
	 */
	private void exportCDX(long lastPosition, int skip) throws IOException {
		seekIn(lastPosition);
		long ptNext = readPointer();
		skipIn(skip - 11);
		ByteOrder bo = byteOrder;
		setByteOrder(ByteOrder.LITTLE_ENDIAN);
		int len = readInt() - 7; // accounts for extra 0 0
		readInt();
		readByte();
		setByteOrder(bo);
		// VjCD block peekBlockAsString(true);
		byte[] cdxFileData = new byte[len];
		read(cdxFileData, 0, len);
		nMolecules++;
		handleFileData(CDX_FILE_DATA, cdxFileData, len, null);
		seekIn(ptNext);

	}

	private void exportMOL(long lastPosition, int skip) throws IOException {
		seekIn(lastPosition);
		long ptNext = readPointer();
		readInt();//
		if (peekInt() == 0)
			return;
		readFourUnknownInts();
		readInt(); // 0
		nextBlock(); // -> 501150
		nextBlock(); // 178-long Molecule block
		readInt(); // to next
		if (peekInt() == 0)
			return; // 3a-c.mnova
		nextBlock();
		nextBlock();
		nextBlock();
		nextSubblock(4);
		if (peekInt() == -1) {
			// page 4 22232721/metadatanmr/nmr spectra.mnova
			readByte();
			readInts(9); // hack
		}
		int len = peekInt();
		nMolecules++;
		handleFileData(MOL_FILE_DATA, readLenStringSafely(), len, null);
		seekIn(ptNext);
	}

	/**
	 * Handle this CDX or MOL file export, sending it to the plugin if present or
	 * creating the file if that option is chosen and testing.
	 * 
	 * @param type
	 * @param fileData
	 * @param len
	 * @param fname
	 */
	private void handleFileData(String type, Object fileData, int len, String fname) {
		boolean isString = (type == MOL_FILE_DATA);
		System.out.println("=====Page Molecule " + nPages + " " + type + ">>>>>");
		if (plugin != null)
			plugin.addParam(type, fileData, null, null);
		System.out.println(isString ? fileData : "[" + len + " bytes]");
		System.out.println("<<<<<" + type + "=====");
		if (testing && createStructureFiles) {
			if (fname == null)
				fname = "test_" + nTests + "_" + zeroFill(nPages, 2) + type;
			File f = new File(fname);
			try (FileOutputStream fis = new FileOutputStream(f)) {
				if (isString) {
					fileData = ((String) fileData).getBytes();
				}
				fis.write((byte[]) fileData);
				System.out.println("File " + f.getAbsolutePath());
			} catch (IOException e) {
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
	private static int defaultTest = 2;
	private static int nTests;

	public static void main(String[] args) {
		if (args.length == 0 && testFile == null) {
			testAll();
		} else {
			String fname = (args.length == 0 ? testFiles[defaultTest] : args[0]);
			runFileTest(fname);
		}
	}

	private static boolean runFileTest(String fname) {
		try {
			String filename = new File(fname).getAbsolutePath();
// this is the 158-MB file
			byte[] bytes = Util.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println(bytes.length + " bytes in " + filename);
			new MNovaMetadataReader(bytes).process();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
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
		createStructureFiles = true;

//		peekIntsAt(2135288-80, 20);
//		findRef(1984530);
//		peekIntsAt(1721399, 10);
//		checkDoubles(1721418,3,0);
//
//		seekIn(1721085);
//		nextBlock();
//		readDouble();
//		readDouble();
//		readByte(); // 2
//		readInt();  // 0
//		readByte(); // 1
//		readInt(); // 21
//		readByte(); // 0
//		readInts(9); 
//		readDouble();
//		readDouble();
//		readInts(19);
//		readDouble();
//		readByte();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		nextBlock();
//		peekInts(30);
//		
//		seekIn(1721442-180);
//		peekInts(50);
//		nextBlock();
//		readInts(6);
//		peekInts(30);
//		nextBlock();
//		readInts(6);
//		nextBlock();
//		peekInts(30);
//		
//		
////		checkDoubles(1985716, 50, -1);
//
//		peekIntsAt(1985562, 400);
//		peekIntsAt(1985718, 600);
//		
////		extractInts(1985566, 16000, "testI2.xls");
//		checkDoubles(1722461, 50, 0);
//		checkDoubles(1984529, 50, 0);
//		extractDoubles(1722461, (1984529-1722461)/8, "testD1.xls");
//
//		checkDoubles(2002393,200, 0);
//		checkDoubles(1982393,200, 0);
//		checkDoubles(1942393,200, 0);
//		checkDoubles(1882393,200, 0);
//		checkDoubles(1842393,200, 0);
//		checkDoubles(1802393,200, 0);
//		checkDoubles(1782393,200, 0);
//		checkDoubles(1762393,200, 0);
//		checkDoubles(1742393,200, 0);
//		checkDoubles(1722393,200, 0);
//		checkDoubles(1984429, 50, 0);
//
//			
//		peekIntsAt(2111538-1380,300);
//		peekIntsAt(2093742-80,24);
//		peekIntsAt(215276-40,20);
//		findRef(2135276);
//		findRef(2135280);
//		findRef(2135284);
//		findRef(2135288);
//		// dumpFileInfo();
//
		// showInts = false;
		showChars = false;
		testing = false;
		return;
	}

//	private void testStack(Stack<Block> objects) throws IOException {
//		Enumeration<Block> e = objects.elements();
//		while (e.hasMoreElements()) {
//			Block obj = e.nextElement();
//			System.out.println("obj " + obj);
//			int len = obj.len;
//		}
//	}

	static void testAll() {

		boolean ok = true;
		for (int i = 0; i < testFiles.length; i++) {
			nTests = i + 1;
			if (runFileTest(testFiles[i])) {
				System.out.println("Test " + i + " on " + testFiles[i] + " OK");
			} else {
				System.err.println("Test " + i + " on " + testFiles[i] + " failed");
				ok = false;
				break;
			}
		}
		if (ok)
			System.out.println("All tests successful");
		
	}
	
	static final String[] testFiles = {
			// no structure?
			/* 0 */ "test/mnova/cyclohex.mnova", // no spectrum, just a cyclohexane .xyz structure
			// ok for structure:
			/* 1 */ "test/mnova/3a-C.mnova", // OK one page, with ChemDraw drawing
			/* 2 */ "test/mnova/1.mnova", // OK two pages, no structures
			/* 3 */ "test/mnova/1-deleted.mnova", // first page param list only, next page blank
			/* 4 */ "test/mnova/1-v14.mnova", // OK two pages
			/* 5 */ "test/mnova/3a-C-taxol.mnova", // (v 14) OK, but looking for model
			/* 6 */ "test/mnova/1-caff-taxol.mnova", // two structures
			/* 7 */ "test/mnova/1-caff-taxol-rev.mnova", // two structures
			/* 8 */ "test/mnova/1-caff-taxol-delete.mnova", // caffeine deleted in
			/* 9 */ "test/mnova/1-taxol-drop.mnova", // caffeine deleted in page 1, taxol on page 2
			/* 10 */ "test/mnova/1-taxol-drop-move.mnova", // caffeine deleted in page
			/* 11 */ "test/mnova/3a-c-morphine.mnova", // morphine.mol added using file...open
			/* 12 */ "c:\\temp\\iupac\\zip\\22232721\\metadatanmr\\nmr spectra.mnova", };

	static {
//		testFile = testFiles[12];
	}

}
