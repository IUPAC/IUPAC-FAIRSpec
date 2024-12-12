package com.integratedgraphics.ifd.vendor.mestrelab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;
import java.util.TreeMap;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

import com.integratedgraphics.ifd.vendor.ByteBlockReader;

/**
 * A rough MestReNova file reader that can deliver metadata only (including MOL,
 * CDX, and PNG files).
 * 
 * DISCLAIMER: I have not seen any MestReNova code, so I am guessing here. It
 * would be terrific if Mestrelabs would sign on to this project and provide
 * this done correctly.
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
 * A 32-bit reference of 109 points is to an address 109+4 bytes past the reference
 * itself. In other words, the reference value 109 does not include the the four
 * bytes of the reference itself. These references are to the buffer position
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
 * then be used to read data from that particular block of bytes using super.get...
 * methods.
 * 
 * Byte Order
 * 
 * All examples I have seen are for the most part big-endian byte order. There
 * are situations where the format switches to little-endian format,
 * specifically to accommodate embedded EMF+ and CDX formats.
 * https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-emfplus/517262f5-aaf3-4150-b456-9a93c24c3f77.
 * This code navigates those records in association with CDX file storage.
 * 
 * Strings
 * 
 * Strings are stored preceded by their length encoded as a 32-bit integer. They
 * may be straight ASCII character strings or UTF-16. There is no way I know of
 * to be sure which it will be. I had to look at the binary data and
 * decide each time whether the string was UTF-16 ([0x00] a [0x00] c [0x00] q
 * [0x00] u [0x00] s) or not.
 * 
 * CDX, PNG, and MOL exports
 * 
 * CDX and PNG export works by scanning the post-parameter blocks for their
 * respective headers. Both have very simple file record layout, so once the
 * starting point has been found, reading through them is a snap. CDX is a bit
 * of a problem, because there is an EMF+ section that can contain what appears
 * to be a CDX file, but the byte length (from the EMF+ record) did not work
 * out, so I have commented all that out and just skip to the tag now. MOL is
 * more difficult. I just look for "M END" and then navigate in that block to
 * the start and grab it. This navigation is not guaranteed, of course.
 * 
 * 
 * Example
 * 
 * Several examples are in the GitHub folder test/mnova. These vary in versions,
 * including MNova 6, 7, 12, and 14.
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
           pages
           
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
	
	private final static byte[] cdxKey = new byte[] { 'V', 'j', 'C', 'D' };

	private final static byte[] cdxmlKey = new byte[] { '<', 'C', 'D', 'X', 'M', 'L' }; // untested

	private final static byte[] pngKey = new byte[] { (byte) 0x89, 'P', 'N', 'G' };

	private static final int minBlockLengthForStructureData = 50;

	private MestrelabIFDVendorPlugin plugin;

	public String mnovaVersion;
	public int mnovaVersionNumber;
	private int nPages, nSpectra, nCDX, nMOL, nPNG;
	private ByteOrder byteOrder0;
	private Object outdir;
	ArrayList<TreeMap<String, Object>> reportData;
	private TreeMap<String, Object> pageData;
	private int nPagesTotal;

	/**
	 * For testing only, with no extractor plugin.
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	MNovaMetadataReader(byte[] bytes) throws IOException {
		super(bytes);
	}

	MNovaMetadataReader(MestrelabIFDVendorPlugin mestrelabIFDVendorPlugin, byte[] bytes) throws IOException {
		super(bytes);
		this.plugin = mestrelabIFDVendorPlugin;
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
			setByteOrder(ByteOrder.BIG_ENDIAN);
			if (!readMagicNumberAndByteOrder())
				return false;
			test();
			readFileAsStack();
			System.out.println("MNovaReader ------- nPages=" + nPages + " nSpectra=" + nSpectra 
					+ " nMOL=" + nMOL + " nCDX=" + nCDX + " nPNG=" + nPNG);
			return true;
		} catch (Exception e) {
			logError(e);
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
	 * Pretty sure this will always be big-endian.
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean readMagicNumberAndByteOrder() throws IOException {
		if (!checkMagicNumber(magicNumber))
			return false;
		readSimpleString(23); // Mestrelab Research S.L.
		setByteOrder(ByteOrder.BIG_ENDIAN);
		if (!checkMagicNumber(magicNumberBE)) { //
			if (!checkMagicNumber(magicNumberLE)) {
				return false;
			}
			setByteOrder(ByteOrder.LITTLE_ENDIAN);
		}
		byteOrder0 = byteOrder;
		readInt(); // 0xF1E2D3C4
		return true;
	}

	/**
	 * The version is contained in two UTF-16 strings.
	 * 
	 * @throws IOException
	 */
	private void readVersion() throws IOException {
		readUTF16String(); // MestReNova
		mnovaVersion = readUTF16String(); // 12.0.0-20080
		if (plugin != null)
			plugin.setVersion(mnovaVersion);
		else 
			report("version", mnovaVersion, null, null);
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
	 * 1) read the 27-byte header.
	 * 
	 * 2) read the pointers onto a stack
	 * 
	 * 3) read byte[] objects off the stack
	 * 
	 * 4) process the pages
	 * 
	 * @throws IOException
	 */
	private void readFileAsStack() throws IOException {
		rewindIn();
		seekIn(27); // skip header
		Stack<BlockData> objects = getObjectStack();
		//System.out.println(objects.size() + " objects");
		 //testStack(objects);
		long pt = readPosition();
		// last block is version
		BlockData verObject = objects.pop();
		verObject.seek();
		readVersion();
		// at pages
		seekIn(pt);
		nextBlock(); // 16 "block 2"
		nextBlock(); // items? history? "block 3"
		readPages(readPosition());
		// just to see if we have read this cleanly:
		try {
			while (readAvailable() > 0) {
				nextBlock();
			}
		} catch (Exception e) {
			logError(e);
		}
		System.out.println(nPagesTotal + " pages processed, version=" + mnovaVersion);
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

	private void readPages(long pt) throws IOException {
		seekIn(pt);
		if (testing)
			System.out.println("--- readPages " + readPosition()); // 38628 - 39077
		readPointer(); // to EOF or next block
		readPageInsets();
		readInt();
		readPointer(); // to EOF or next block
		nPagesTotal = readInt();
		readPointer(); // also to EOF
		nSpectra = 0;
		for (int i = 0; i < nPagesTotal; i++) {
			readPage(readPosition(), i);
		}
		if (testing)
			System.out.println("--- " + nPagesTotal + " pages read");
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
	private void readPage(long ptr, int index) throws IOException {
		seekIn(ptr);
		nPages++;
		System.out.println("reading page " + (index + 1) + " pos=" + readPosition());
		readPageHeader();
		long ptNext = readPointer(); // to next page
		String header = readPageTextHeader(readPosition(), ptNext);
		if (header != null) {
			System.out.println("page header = " + header);
			if (readToParameters(readPosition())) {
				nSpectra++;
				if (plugin != null)
					plugin.newPage(nPagesTotal > 1 ? nPages : 1);
				report("page", null, null, null);
				if (header != null && header.length() > 0)
					reportParam("Page_Header", new Param(header), null);
				readParams();
				searchForExports(readPosition(), index, ptNext);
			}
		}
		seekIn(ptNext);
	}

	private String readPageTextHeader(long pt0, long pt) throws IOException {
		seekIn(pt0);
		readPageHeader2(); // 40
		readPageInsets(); // 32
		readFourUnknownInts();
		readInt(); // 0
		readPointer(); // to next page
		readInt(); // 1 count of what? (in cyclehex.mnova? 2 sometimes 3? 4 in taxol?
		readPointer(); // to next page
		if (readPosition() == pt)
			return null; // nothing on this page
		readInt(); // 0
		readPointer(); // to ? 394266 in 1.mnova
		readInt(); // usually 109; can be 107, 110; id? type?  Not a pointer
		if (peekInt() == 0) {
			// no spectrum
			return null;
		}
		readFourUnknownInts();
		readInt(); // 0 in 1.mnova
		// skipping our way through to parameters -- ad hoc
		nextBlock(); // to 38964
		nextBlock(); // 172 to 39140 
		readPointer(); // -> 394266 
		nextBlock(); // 189 -> 39337 bytes in 1.mnova; 197 in v14
		// now get the page header
		Stack<BlockData> s = getObjectStack();
		long pt1 = readPosition();
		//testStack(s);
		s.get(s.size() - 1).seek(); // page header
		Stack<BlockData> s1 = getObjectStack();
		//testStack(s1);
		//BlockData d2 = 
		s1.get(s1.size() - 8).seek();
		String header = readUTF16String();
		seekIn(pt1);
		//old: nextBlock(); // 5957 -> 45298 bytes in 1.mnova; 7107 in v14  includes header
		return (header == null || header.startsWith("{") ? "" : header);
	}

	/**
	 * Read to the parameters for this page.
	 * 
	 * @return -pt if no parameters, otherwise pt to next page
	 * 
	 * @throws IOException
	 */
	private boolean readToParameters(long pt0) throws IOException {
		seekIn(pt0);
		Stack<BlockData> stack = getObjectStack();
		//testStack(stack);
		// third from last data block is the parameter block
		BlockData params = stack.get(stack.size() - 3);
		params.seek();
		readPointer();
		return (readInt() == 0);
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
	private void readPageHeader2() throws IOException {
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

		Param(String value) {
			this.value = value;
		}
		
		/**
		 * Read the parameter data.
		 * 
		 * @param index just for debugging -- 1 or 2 for now.
		 * 
		 * @throws IOException
		 */
		Param(long ptr, int index) throws IOException {
			seekIn(ptr);
			long pt1 = readPosition();
			readInt(); // 8, 9, A (or 0,2 for older version)
			int p = peekInt();
			if (p >= 0) {
				units = readUTF16String();
				if (units.equals("acqus")) {
					// version 6.1 may be missing units
					units = null;
					seekIn(pt1);
				}
			} else {
				readInt();
			}
			boolean isNew = (peekInt() == 0);
			if (isNew) {
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

		public HashMap<String, Object> toMap() {
			HashMap<String, Object> map = new HashMap<>();
			if (value != null)
				map.put("value", value);
			if (units != null)
				map.put("units", units);
			if (source != null)
				map.put("source", source);
			return map;
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
		Param param1 = new Param(readPosition(), 1);
		Param param2 = (count == 2 ? new Param(readPosition(), 2) : null);
		readByte(); // EF, e.g. -- identifier?
		String key = readUTF16String();
		reportParam(key, param1, param2);
	}

	private void reportParam(String key, Param param1, Param param2) {
		if (plugin != null)
			plugin.addParam(key, null, param1, param2);
		else
			report(key, null, param1, param2);
		System.out.println("   " + key + " = " + param1 + (param2 == null ? "" : "," + param2));
	}

	private void report(String key, String val, Param param1, Param param2) {
		if (reportData == null)
			reportData = new ArrayList<TreeMap<String, Object>>();
		if (key.equals("page")) {
			reportData.add(pageData = new TreeMap<>());
			if (nPagesTotal > 1)
				pageData.put("#page", Integer.valueOf(nPages));
		} else {
			if (pageData == null) {
				reportData.add(pageData = new TreeMap<>());
				if (nPagesTotal > 1)
					pageData.put("#page", Integer.valueOf(nPages));
			}
			pageData.put(key, param1 == null ? val : param1.toMap());
			if (param2 != null)
				pageData.put(key + "2", param2.toMap());
		}
	}

	private void searchForExports(long pos0, int index, long ptNext) throws IOException {
		int nBlocks = 0;
		// allowing for one of each per page
		boolean haveCDX = false;
		boolean haveMOL = false;
		boolean havePNG = false;
		seekIn(pos0); // for debugging dynamic change of method
		while (readPosition() < ptNext) {
			int len = peekInt();
			if (len > 0) {
				nBlocks++;
				long ptr = readPosition();
				if (testing)
					System.out.println("additional block " + nBlocks + " len=" + len + " from " + ptr + " to " + (ptr + len)
						+ " ptNext=" + ptNext);
				if (len > minBlockLengthForStructureData) {
					int offset;
					offset = (haveCDX ? -1 : findBytes(cdxmlKey, len, false, 112));
					if (offset >= 0) {
						haveCDX = true;
						exportCDXML(ptr, offset, nBlocks);
					}
					offset = (haveCDX ? -1 : findBytes(cdxKey, len, false, 2));
					if (offset >= 0) {
						haveCDX = true;
						exportCDX(ptr, offset, nBlocks);
					}
					offset = (haveMOL ? -1 : findBytes(molKey, len, false, 0));
					if (offset >= 0) {
						haveMOL = true;
						exportMOL(ptr, offset, nBlocks);
					}
					offset = (havePNG ? -1 : findBytes(pngKey, len, false, 0));
					if (offset >= 0) {
						havePNG = true;
						exportPNG(ptr, offset, nBlocks);
					}
				}
			}
			nextBlock();
		}
		System.out.println("\n======Page " + (index + 1) + " additional blocks: " + nBlocks);
	}

	/**
	 * untested
	 * 
	 * @param lastPosition
	 * @param skip
	 * @param nBlock
	 * @throws IOException
	 */
	private void exportCDXML(long lastPosition, int skip, int nBlock) throws IOException {
		long pt0 = lastPosition + skip;
		seekIn(pt0);
		byte[] bytes = readCDXMLdata(pt0);
		int len = (bytes == null ? 0 : bytes.length);
		if (len > 0) {
			nCDX++;
			handleFileData(nBlock, DefaultStructureHelper.CDXML_FILE_DATA, bytes, pt0, len, null, null);
		}
		seekIn(lastPosition);
	}


	/**
	 * Read through to find the end of the CDX file.
	 * 
	 * Read properties and nested objects until the object pointer drops to -1. see
	 * https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/IntroCDX.htm
	 * @param ptr 
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	private byte[] readCDXMLdata(long ptr) throws IOException {
		seekIn(ptr);
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[1000];
		try {
			int n0 = 0, n, pt = -1, ntotal = 0;
			while ((pt = sb.indexOf("</CDXML", n0)) < 0 && (n = read(buf, 0, 1000)) > 0 && ntotal < 100000) {
				sb.append(new String(buf, 0, n));
				n0 = Math.max(0, ntotal - 7);
				ntotal += n;
			}
			if (pt < 0)
				return null;
			sb.setLength(pt);
		} catch (Exception e) {
			logError(e);
			return null;
		}
		seekIn(ptr);
		return ((sb + "</CDXML>").getBytes());
	}

	/**
	 * found the CDX
	 * 
	 * see
	 * https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/IntroCDX.htm
	 * 
	 * @param lastPosition
	 * @param skip
	 * @throws IOException
	 */
	private void exportCDX(long lastPosition, int skip, int nBlock) throws IOException {
		long pt0 = lastPosition + skip;
		// initially I tried navigating the EMF+ records, but it turned out that those
		// held truncated CDX files (so it appears).
		seekIn(pt0);
		byte[] bytes = readCDXdata(pt0);
		int len = (bytes == null ? 0 : bytes.length);
		if (len > 0) {
			nCDX++;
			handleFileData(nBlock, DefaultStructureHelper.CDX_FILE_DATA, bytes, pt0, len, null, null);
		}
		seekIn(lastPosition);
	}

	/**
	 * Read through to find the end of the CDX file.
	 * 
	 * Read properties and nested objects until the object pointer drops to -1. see
	 * https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/IntroCDX.htm
	 * @param ptr 
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	private byte[] readCDXdata(long ptr) throws IOException {
		seekIn(ptr);
		ByteOrder bo = byteOrder0;
		skipIn(22); // header
		try {
			setByteOrder(ByteOrder.LITTLE_ENDIAN);
			int type;
			int nObj = 0;
			while ((type = readShort()) != 0 || nObj > 0) {
				if (type == 0) {
					// end of object
					nObj--;
				} else if ((type & 0x8000) == 0) {
					// property: 2-byte id
					int len = readShort();
					if (len == -1) {
						// large 4-byte length
						len = readInt();
					}
					skipIn(len);
				} else {
					// object: 4-byte id
					readInt();
					nObj++;
				}
			}
		} catch (Exception e) {
			logError(e);
			return null;
		} finally {
			setByteOrder(bo);
		}
		long len = readPosition() - ptr;
		seekIn(ptr);
		return readBytes(len);
	}

// EMT+ attempt -- but this finds the WRONG CDX file! -- one that has a CDIF\0 header and does not seem to be complete
	// readPointer();
//	readInt();
//	readFourUnknownInts();
//	readInt(); // 0
//	nextBlock(); // header block
//	nextBlock(); // PNG image block
//	readPointer();
//	readInts(4);
//	ByteOrder bo = byteOrder;
//	try {
//		setByteOrder(ByteOrder.LITTLE_ENDIAN);
//		BlockData block = readEMF(readPosition(), lastPosition + skip);
//		if (block == null)
//			return;
//		long loc = block.loc + 5+0;
//		seekIn(loc); 
//		int len = block.len - 5; // remove CDIF\0 and last two 0 0 bytes
//		byte[] bytes = new byte[len];
//		read(bytes, 0, len);
//		nStructures++;
//		handleFileData(nBlock, Extractor.CDX_FILE_DATA, bytes, loc, len, null, null);
//	} finally {
//		setByteOrder(bo);
//	}

//	private BlockData readEMF(long ptr, long target) throws IOException {
//		seekIn(ptr);
//		// EMR Header https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-emf/de081cd7-351f-4cc2-830b-d03fb55e89ab
//		// EMF Comment record https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-emf/05940d07-e112-4146-ac05-88fc6a1f70b9
//		if (readInt() != 1) 
//			return null;
//		int headerSize = readInt(); // 108
//		skipIn(headerSize - 8);
//		int type; 
//		do {
//			type = peekInt();
//			BlockData b = readEMTRecord(readPosition());
//			if (b != null) {
//				if (b.loc > target)
//					return null;
//				if (b.loc + b.len > target) { 
//					peekIntsAt(b.loc, 20);
//					return b;
//				}
//			}
//		} while (type != 0x0E);
//		return null;
//	}
//
//	@SuppressWarnings("unused")
//	private BlockData readEMTRecord(long readPosition) throws IOException {
//		seekIn(readPosition);
//		int type = readInt(); // 0x46 0 0 0
//		int size = readInt();
//		BlockData b = null;
//		if (type == 0x46) {// comment
//			int dataSize = readInt();
//			String desc = readSimpleString(4);
//			if (desc.equals("EMF+")) {
//				int type1 = readInt();
//				int size1 = readInt();
//				int dataSize1 = readInt();
//				b = new BlockData(readPosition(), dataSize1);
//			}
//		}
//		seekIn(readPosition + size);
//		return b;
//	}

	/**
	 * Found the PNG file, so export it.
	 * 
	 * @param lastPosition
	 * @param skip
	 * @throws IOException
	 */
	private void exportPNG(long lastPosition, int skip, int nBlock) throws IOException {
		seekIn(lastPosition);
		//int w = 0, h = 0;
		try {
			// this part is basically just for fun. We skip if we have to.
			readPointer();
			readInt();
			readFourUnknownInts();
			readInt(); // 0
			nextBlock(); // -> 501150
			readPointer(); // EOF + 4
			readFourUnknownInts();
			readFourUnknownInts(); // same
			readDouble();
			readDouble();
			readByte();
			readInts(15); // DANGER, WILL ROBINSON!
			readLenStringSafely(); // "OLE Object" or "Image"
			if (peekInt() == -1) {
				readInt();
			} else {
				readUTF16String(); // "OLE Container"
			}
			readDouble();
			readDouble();
			/*w = (int)*/ Math.round(readDouble());
			/*h = (int)*/ Math.round(readDouble());
			readByte(); // 1
			readInt(); // 1
			if (peekInt() != 0x89504E47) {
				// version 6 "Image"
				nextSubblock(2);
				readInt();
				readInt(); // 1
			}
		} catch (Exception e) {
			logError(e);
		}
		if (peekInt() != 0x89504E47) {
			logError(new Exception("Could not navigate to start of PNG image - just skipping to tag"));
			seekIn(lastPosition + skip);
		}
		byte[] bytes = readPNGData(readPosition());
		int len = (bytes == null ? 0 : bytes.length);
		if (len > 0) {
			nPNG++;
			handleFileData(nBlock, DefaultStructureHelper.PNG_FILE_DATA, bytes, readPosition() - len, len, null,
					IFDConst.IFD_REPRESENTATION_FLAG + "png");
		}
		seekIn(lastPosition);
	}

	/**
	 * Read through the PNG file's very simple format.
	 * 
	 * <code>
	 len (4 bytes)  ending with 0
	 tag (4 chars)  ending with IEND
	 data (len bytes)
	 CRC (4 bytes)
	 </code>
	 * 
	 * @param ptr
	 * @return 
	 * @throws IOException
	 */
	private byte[] readPNGData(long ptr) throws IOException {
		seekIn(ptr);
		if (readInt() != 0x89504E47) // 0x89 P N G
			return null;
		readInt(); // 0x0D 0x0A 0x1A 0x0A
		int tag;
		do {
			int len = readInt();
			tag = readInt();
			skipIn(len + 4); // skip CRC
		} while (tag != 0x49454E44); // I E N D
		long len = readPosition() - ptr;
		seekIn(ptr);
		return readBytes(len);
	}

	private void exportMOL(long lastPosition, int skip, int nBlock) throws IOException {
		// targeting the END of the mol file here, so we need to back up to its start.
		// note that this is NOT the mol file dropped. It is created in MNova by
		// OpenBabel.
		seekIn(lastPosition);
		long ptr = lastPosition + skip;
		// testing = showChars = true;
		// peekIntsAt(lastPosition, skip/4 + 4);
		readPointer();
		readInt(); // 107, 109, 110, etc.
		readFourUnknownInts();
		readInt(); // 0
		nextBlock(); // -> 501150
		nextBlock(); // 178-long Molecule block
		readInt(); // to next
		nextBlock();
		nextBlock();
		nextBlock();
		nextSubblock(4);
		if (peekInt() == -1) {
			// page 4 22232721/metadatanmr/nmr spectra.mnova
			readByte();
			readInts(9); // DANGER, WILL ROBINSON!
		}
		int len = readInt();
		if (len > 0 && readPosition() + len < ptr + 10) {
			nMOL++;
			byte[] bytes = readBytes(len);
			handleFileData(nBlock, DefaultStructureHelper.MOL_FILE_DATA, bytes, readPosition(), len, null, null);
		}
		seekIn(lastPosition);
	}

	/**
	 * Handle this CDX or MOL file export, sending it to the plugin if present or
	 * creating the file if that option is chosen and testing.
	 * 
	 * @param nBlock
	 * @param type
	 * @param fileData
	 * @param len
	 * @param fname currently null in all cases (CDX, MOL, PNG)
	 * @param cssInfo
	 */
	private void handleFileData(int nBlock, String type, byte[] fileData, long ptr, int len, String fname,
			String cssInfo) {
		if (plugin != null) {
			if (cssInfo != null)
				plugin.addParam(type + ":css", cssInfo, null, null);
			plugin.addParam(type, fileData, null, null);
		}
		if (fname == null)
			fname = "file_" + zeroFill(nTests, 2) + "_" + zeroFill(nPages, 2) + type;
		String s = "=====Page " + nPages + " block " + nBlock + " byte " + ptr + " " + fname + " [" + len + " bytes] "
				+ (cssInfo != null ? cssInfo : "");
		System.out.println(s);
		if (createStructureFiles) {
			writeToFile(fname, fileData);
		}
		if (plugin == null) {
			report(type, fname, null, null);
		}
	}

	private void writeToFile(String fname, byte[] fileData) {
		if (outdir != null)
			fname = outdir + fname;
		File f = new File(fname);
		try (FileOutputStream fis = new FileOutputStream(f)) {
			fis.write(fileData);
			System.out.println("File " + f.getAbsolutePath());
		} catch (IOException e) {
			logError(e);
		}
	}

	private final static String zeros = "0000";

	private static String zeroFill(int n, int ndig) {
		// n = 100, ndig = 2, s = "0000100" -> "100"
		String s = zeros + n;
		int len;
		return s.substring((len = s.length()) - Math.max(len - 4, ndig));
	}

	/**
	 * No idea!
	 * 
	 * @throws IOException
	 */
	private void readFourUnknownInts() throws IOException {
		if (testing) {
			readInts(4);
		} else {
			skipIn(16);
		}
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
			readPageHeader();
			long ptNext = readPointer(); // to next page
			String header = readPageTextHeader(readPosition(), ptNext);
			if (header != null) {
				System.out.println("page header = " + header);
				if (readToParameters(readPosition())) {
					System.out.println(" params at " + readPosition());
					readParams();
					System.out.println("\n======Page " + (i + 1) + " parameters end at " + readPosition());
					searchForExports(readPosition(), i, ptNext);
				}
			}
			seekIn(ptNext);
		}
		System.out.println("\n===Pages end at " + readPosition() + " available=" + readAvailable());
	}


	private static String testFile;
	private static int defaultTest = 2;
	private static int nTests = -1;

	public static void main(String[] args) {
		int pt = 0;
		String outdir = null;
		if (args.length >= 2 && args[0].equals("-o")) {
			outdir = args[1];
			pt += 2;
		}
		if (testFile == null && args.length == 1 && "--testall".equals(args[0])) {
			testAll(outdir);
		} else if (testFile != null && args.length > pt && "--test".equals(args[pt])) {
			String f = (testFile != null ? testFile : testFiles[defaultTest]);
			runFileTest(f, outdir);
		} else if (args.length != pt) {
			String fname = args[pt];			
			runFileTest(fname, outdir);
		} else {
			System.out.println("usage: MNovaMetadataReader [-o outputdir] --test");
			System.out.println("usage: MNovaMetadataReader [-o outputdir] --testall");
			System.out.println("usage: MNovaMetadataReader [-o outputdir] mnovaFilename");
			System.out.println("a json file will be created");
		}
	}

	private static boolean runFileTest(String fname, String outdir) {
		try {
			File f = new File(fname);
			String filename = f.getAbsolutePath();
// this is the 158-MB file
			byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println(bytes.length + " bytes in " + filename);
			MNovaMetadataReader rdr = new MNovaMetadataReader(bytes);
			if (outdir == null) {
				outdir = new File("t").getAbsoluteFile().getParentFile().getAbsolutePath() + "/";
			} else {
//				outdir = outdir.replace('\\', '/');
				if (!outdir.endsWith("/")) {
					if (outdir.length() == 0) {
						outdir = f.getAbsoluteFile().getParentFile().getAbsolutePath() + "/";
					}
					outdir += "/";
				}
			}
			outdir += f.getName() + ".";
			rdr.outdir = outdir;
			rdr.process();
			System.out.println("MNova file closed for " + filename);
			if (rdr.reportData != null) {
				IFDDefaultJSONSerializer serializer = new IFDDefaultJSONSerializer(false);
				serializer.openObject();
				serializer.addObject("MNova.metadata", rdr.reportData);
				String json = serializer.closeObject();
				rdr.writeToFile("json", json.getBytes());
			}
			return true;
		} catch (IOException e) {
			logError(e);
			return false;
		}
	}

	private static void logError(Exception e) {
		e.printStackTrace();
		return;
	}

	/**
	 * various tests
	 * 
	 * @throws IOException
	 */
	private void test() throws IOException {
		
//		extractInts(686811, 3600>>2, "c:/temp/thead");
		
		
		rewindIn();

		testing = true;
		showInts = false;
		showChars = true;

//		peekIntsAt(2135288-80, 20);
//		findRef(1984530);
//		peekIntsAt(1721399, 10);
//		checkDoubles(1721418,3,0);
//
//		findRef(2135288);
//		// dumpFileInfo();
		
		showInts = false;
		showChars = false;
		testing = false;
		return;
	}

	static void testStack(Stack<BlockData> objects) throws IOException {
		Enumeration<BlockData> e = objects.elements();
		while (e.hasMoreElements()) {
			BlockData obj = e.nextElement();
			System.out.println("obj " + obj);
		}
	}

	static void testAll(String outdir) {

		testFile = testFiles[18];
		createStructureFiles = (testFile != null);

		boolean ok = true;
		for (int i = 0; i < testFiles.length; i++) {
			nTests = i + 1;
			if (runFileTest(testFiles[i], outdir)) {
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
			/* 0 */ "test/mnova/cyclohex.mnova", // no spectrum, just dropped in cyclohexane.xyz structure
			/* 1 */ "test/mnova/3a-C.mnova", // from ACS OK one page, with ChemDraw drawing
			/* 2 */ "test/mnova/1.mnova", // from ACS two pages, no structures
			/* 3 */ "test/mnova/1-deleted.mnova", // first page param list only, next page blank
			/* 4 */ "test/mnova/1-v14.mnova", // saved by MNova v. 14 two pages
			/* 5 */ "test/mnova/3a-C-taxol.mnova", // saved by MNova v. 14; dropped in taxol.mol
			/* 6 */ "test/mnova/1-caff-taxol.mnova", // saved by MNova v. 14; dropped in caffeine.mol and taxol.mol
			/* 7 */ "test/mnova/1-caff-taxol-rev.mnova", // saved by MNova v. 14; dropped in caffeine.mol and taxol.mol in reverse order
			/* 8 */ "test/mnova/1-caff-taxol-delete.mnova", // saved by MNova v. 14; dropped in caffeine.mol and taxol.mol, caffeine deleted in
			/* 9 */ "test/mnova/1-taxol-drop.mnova", // saved by MNova v. 14; dropped in taxol.mol, repositioned, scaled and resized
			/* 10 */ "test/mnova/1-taxol-drop-move.mnova", // saved by MNova v. 14; dropped in taxol.mol and moved
			/* 11 */ "test/mnova/3a-c-morphine.mnova", // saved by MNova v. 14; morphine.mol added using file...open
			// not at GitHub - see 
			/* 12 */ "c:\\temp\\iupac\\zip\\22232721\\metadatanmr\\nmr spectra.mnova", // from ACS; too big for GitHub
			/* 13 */ "test/mnova/3aa-C.mnova", // CDX extraction OK
			/* 14 */ "test/mnova/10.mnova", // PNG extraction OK version 6.1
			/* 15 */ "test/mnova/Substrate_1'h.mnova", // PNG extraction failed 6.1
			/* 16 */ "test/mnova/Substrate_1k.mnova", // Temperature parameter failed 6.1
			/* 17 */ "test/mnova/Products_3a.mnova", // png failed
			/* 18 */ "test/mnova/5-H.mnova", // failed
		};

}
