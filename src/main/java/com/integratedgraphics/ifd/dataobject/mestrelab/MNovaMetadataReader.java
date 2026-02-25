package com.integratedgraphics.ifd.dataobject.mestrelab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

import com.integratedgraphics.ifd.dataobject.ByteBlockReader;

/**
 * A rough MestReNova file reader that can deliver metadata only (including MOL,
 * CDX, and PNG files) as well as properties.
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
 * A 32-bit reference of 109 points is to an address 109+4 bytes past the
 * reference itself. In other words, the reference value 109 does not include
 * the the four bytes of the reference itself. These references are to the
 * buffer position *after* the data pointed to. So, for example, if the block
 * read looks like this:
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
 * then be used to read data from that particular block of bytes using
 * super.get... methods.
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
 * to be sure which it will be. I had to look at the binary data and decide each
 * time whether the string was UTF-16 ([0x00] a [0x00] c [0x00] q [0x00] u
 * [0x00] s) or not.
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

	public static final String PAGE_TITLE = "Page_Header";

	private final static int magicNumber = 0x4D657374; // M e s t
	/**
	 * big-/little-endian tests; it's totally unclear what would happen if this file
	 * were in litte-endian format.
	 */
	private final static int magicNumberBE = 0xF1E2D3C4;// F1 E2 D3 C4
	private final static int magicNumberLE = 0xC4D3E2F1;// C4 D3 E2 F1

	//private final static byte[] xyzKey = new byte[] { '\0', '.', '\0', 'x', '\0', 'y', '\0', 'z' };

	private final static byte[] molKey = new byte[] { 'M', ' ', ' ', 'E', 'N', 'D' };

	private final static byte[] cdxKey = new byte[] { 'V', 'j', 'C', 'D' };

	private final static byte[] cdxmlKey = new byte[] { '<', 'C', 'D', 'X', 'M', 'L' }; // untested

	private final static byte[] pngKey = new byte[] { (byte) 0x89, 'P', 'N', 'G' };

	private static final int minBlockLengthForStructureData = 50;

	private static final String PAGE_STACK = "block2.stack";
	private static final String PARAM_BLOCK = "block3";
	private static final String TITLE_STACK_DATA = "block2.data2";
	
	private MestrelabDataObjectVendorPlugin plugin;

	public String mnovaVersion;
	public int mnovaVersionNumber;
	private int nPages, nSpectra, nCDX, nMOL, nPNG, nXYZ;
	private ByteOrder byteOrder0;
	private Object outdir;
	ArrayList<TreeMap<String, Object>> reportData;
	private TreeMap<String, Object> pageData;
	private int nPagesTotal;
	private boolean isQuiet;
	private BlockData fileStructure;

	private String fileBlockPageData;

	private String fileBlockVersionData;

	private String fileBlockPagesData;

	private static String thisFileName;

//	private static boolean debugging;

	/**
	 * For testing only, with no extractor plugin.
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	MNovaMetadataReader(byte[] bytes) throws IOException {
		super(bytes);
	}

	MNovaMetadataReader(MestrelabDataObjectVendorPlugin mestrelabIFDVendorPlugin, byte[] bytes) throws IOException {
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
			getFileStructure(true);
			readVersion();
			test();
			readPages();
			System.out.println(nPagesTotal + " pages processed, version=" + mnovaVersion);
			if (strDebug != null)
				System.out.println(strDebug);
			System.out.println("MNovaReader ------- nPages=" + nPages + " nSpectra=" + nSpectra + " nMOL=" + nMOL
					+ " nCDX=" + nCDX + " nPNG=" + nPNG + " nXYZ=" + nXYZ);
			return true;
		} catch (Exception e) {
			logError(e);
			return false;
		} finally {
			try {
				System.out.println("closing pos=" + getPosition() + " avail=" + getAvailable());
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
		long pt0 = getPosition();
		// it is file.body.block1.data6 or file.body.part1.block1.data6
		// but just in case this ever changes, leaving it here as "last block"
		getBlockFromPath(fileBlockVersionData).getSubblock(-1).seek();
		long pt = getPosition();
		readUTF16String(); // MestReNova
		mnovaVersion = readUTF16String(); // 12.0.0-20080
		if (plugin != null)
			plugin.setVersion(mnovaVersion);
		else
			report("version", mnovaVersion, null, null);
		System.out.println("MNova version " + mnovaVersion + " at " + pt);
		try {
			mnovaVersionNumber = Integer.parseInt(mnovaVersion.substring(0, mnovaVersion.indexOf(".")));
		} catch (NumberFormatException nfe) {
			mnovaVersionNumber = Integer.MAX_VALUE;
		}
		seekIn(pt0);
	}

	/**
	 * Seek to the nth page, 1-based.
	 * 
	 * @param n
	 * @return page byte location or -1 (n < 1) or -2 (not enough pages)
	 * @throws IOException
	 */
	long seekPage(int n) throws IOException {
		getBlockFromPath("file.body.page" + n).seek();
		return getPosition();
	}

	private void readHeader() throws IOException {
		seekIn(27); // skip header
	}

	private void readPages() throws IOException {
		seekPage(1);
		nSpectra = 0;
		nPages = 0;
		for (int i = 0; i < nPagesTotal; i++) {
			readPage(++nPages);
		}
		if (testing)
			System.out.println("--- " + nPagesTotal + " pages read");
	}

	/**
	 * for testFID only
	 * 
	 * @param pa
	 * @param n block number; can be -n for from the end -- -1 for "last"; 0 is not allowed
	 * @return
	 * @throws IOException
	 */
	long seekPageBlock(int page, int n) throws IOException {
		testing = true;
		seekPage(page);
		// page is two blocks
		long ptNextBlock = readPointer("page"); // not the next page
		
		System.out.println("page " + page + " ends at " + ptNextBlock + "/" + len);
		if (!readToPageBlockStack(getPosition(), ptNextBlock))
			return -1;
		getDataStack(getPosition(), len, "page block");
		Stack<BlockData> stack = getDataStack(getPosition(), len, "page" + n);
		if (stack == null)
			return -1;
		int ns = stack.size();
		if (n == 0 || n > ns) {
			return -1;
		}
		int i = (n > 0 ? n - 1 : ns + n);
		if (i < 0)
			return -1;
		BlockData bd = stack.get(i);
		bd.seek();
		return bd.loc;
	}

	/**
	 * Just skipping most of this information now, but it gives us the pointer we
	 * need for skipping to the next block.
	 * 
	 * @param index
	 * @return
	 * @throws IOException
	 */
	private void readPage(int page) throws IOException {
		BlockData pageBlock = getBlockFromPath("file.body.page" + page + "." + PAGE_STACK);
		if (pageBlock != null) {
			pageBlock.seek();
			long ptr = getPosition();
			long ptr2ndBlock = readPointer("2ndBlock");
			System.out.println("---reading page " + nPages + " pos=" + pageBlock.loc + " 2nd block pos=" + ptr2ndBlock);
			findPointer(ptr2ndBlock, "test");
			String title = readPageTitle(nPages);
			System.out.println(ptr + " page " + nPages + " title = " + title);
			long pt0 = getPosition();
			if (readToParameters(page)) {
				nSpectra++;
				if (plugin != null)
					plugin.newPage(nPagesTotal > 1 ? nPages : 1);
				report("page", null, null, null);
				if (title != null && title.length() > 0)
					reportParam(PAGE_TITLE, new Param(title), null);
				readParams();
				// this does not allow for structures only.
				searchForExports(getPosition(), ptr2ndBlock, true);
			} else {
				// seekIn(pt0);
				//
				// this allows for structures only --
				// but it does not work
				// searchForExports(getPosition(), index, ptNext, false);
			}
		}
	}

	private boolean readToPageBlockStack(long ptr, long ptr2ndBlock) throws IOException {
		if (!readValidPageHeader(ptr, ptr2ndBlock))
			return false;
		// skipping our way through to parameters -- ad hoc
		// 38876 here we can start page data
		nextBlock(); // to 38964
		nextBlock(); // 172 to 39140
		readPointer(null); // to END		
		nextBlock(); // 189 -> 39337 bytes in 1.mnova; 197 in v14
		//findPointer(0, "page");
		return true;
	}

	/**
	 * Read through the header fields until we reach the data structure stack. 
	 * We will categorize that. 
	 * 
	 * @param ptr
	 * @param ptr2ndBlock 
	 * @return
	 * @throws IOException 
	 */
	private boolean readValidPageHeader(long ptr, long ptr2ndBlock) throws IOException {
		seekIn(ptr); // page start
		//readPageHeader2(); // 40 bytes
		nextBlock();
		// insets are pairs of integers, e.g. (0,0), (296,209), (5,5) (291, 204)
		nextBlock(); // 32 bytes
		readFourUnknownInts();
		readInt(); // 0
		readPointer(null); // to next page
		readInt(); // 1 count of what? (in cyclehex.mnova? 2 sometimes 3? 4 in taxol?
		readPointer(null); // to next page
		if (getPosition() == ptr2ndBlock) {
			System.out.println("nothing on page " + nPages);
			return false; // nothing on this page cyclohex-Si.mnova
		}
		if (getAvailable() == 0) {
			return false;
		}
		readInt(); // 0
		readPointer(null); // to ? 394266 in 1.mnova "NMR TABLE PARAMETERS"
		readInt(); // usually 109; can be 107, 110; id? type? Not a pointer
		if(peekInt() == 0) {
			// no spectrum -- 1-deleted.mnova
			testLog += "no spectrum on page " + nPages + " for " + thisFileName + "\n"; 
			return false;
		}
		readFourUnknownInts();
		readInt(); // 0 in 1.mnova
		return true;
	}

	private String readPageTitle(int page) throws IOException {
		long pt1 = getPosition();
	    BlockData titleData = getStackData(nPages, TITLE_STACK_DATA, -8);
		String title = null;
	    if (titleData != null) {
	    	titleData.seek();
			title = readUTF16String();	    	
	    }
		seekIn(pt1);
		// could be {parm, "Title"}{br}{parm,"Comment"}
		return (title == null || title.startsWith("{") ? "" : title);
	}

	private BlockData getStackData(int page, String path, int n) throws IOException {
		BlockData bd = getBlockFromPath("file.body.page" + page + "." + path);
		if (bd != null) {
			bd.seek();
			Stack<BlockData> s1 = getDataStack(getPosition(), len, null);
			if (n < 0)
				n = s1.size() + n;
			if (n >= 0 && n < s1.size())
				return s1.get(n);
		}
		return null;
	}

	private void seekToPageBlock(int nPages2, String path) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Read to the parameters for this page.
	 * 
	 * @return -pt if no parameters, otherwise pt to next page
	 * 
	 * @throws IOException
	 */
	private boolean readToParameters(int page) throws IOException {
	    BlockData paramBlock = getBlockFromPath("file.body.page" + page + "." + PARAM_BLOCK);
	    if (paramBlock == null)
	    	return false;
	    int n = paramBlock.getSubblockCount();
	    if (n == 0 || n < 3)
	    	return false;
	    BlockData paramData = getBlockFromPath("file.body.page" + page + "." + PARAM_BLOCK + ".data" + (n - 3));
	    if (paramData == null)
	    	return false;
	    paramData.seek();
		readPointer("params1");
		return (readInt() == 0);
	}

//	/**
//	 * Read an unknown block on every page.
//	 * 
//	 * @throws IOException
//	 */
//	private void readPageHeader2() throws IOException {
//		// 1.mnova test -- unclear what this is
////		38732 reading 40 to 38776
////		38736: 0x000036DB = 14043 -> 52783	
////		38740: 0x000036DB = 14043 -> 52787	
////		38744: 0x40279F3E = 1076338494 -> 1076377242	11.811023622047244
////		38748: 0x7CF9F3E8 = 2096755688 -> 2096794440	
////		38752: 0x4072C000 = 1081262080 -> 1081300836	300.0
////		38756: 0x00000000 = 0	
////		38760: 0x00000000 = 0	
////		38764: 0x00000000 = 0	
////		38768: 0x000036DA = 14042 -> 52814	
////		38772: 0x000036DA = 14042 -> 52818	
//	}
//
	/**
	 * Key method fore reading all parameters available in the MNova file.
	 * 
	 * @throws IOException
	 */
	private void readParams() throws IOException {
		boolean t = testing;
		testing = false;
		findPointer(0, "params");
		int n = readInt();
		System.out.println(" " + n + " parameters found at " + getPosition() + " for page " + nPages);
		for (int i = 0; i < n; i++) {
			readParam(i, isQuiet);
		}
		System.out.println(" processed " + n + " parameters now " + getPosition());
		testing = t;
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
			long pt1 = getPosition();
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
	private void readParam(int index, boolean isSilent) throws IOException {
		if (testing && !isSilent)
			System.out.println(" param " + (index + 1) + " at " + getPosition());
		nextBlock(); // D 5 0 1 type
		int count = readInt(); // 1 or 2 (or more?)
		Param param1 = new Param(getPosition(), 1);
		Param param2 = (count == 2 ? new Param(getPosition(), 2) : null);
		readByte(); // EF, e.g. -- identifier?
		String key = readUTF16String();
		if (!isSilent)
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
//			if (debugging)
//				pageData.put("debug",debugMap = new TreeMap<>());
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

	private void searchForExports(long pos0, long ptNext, boolean haveSpec) throws IOException {
		int nBlocks = 0;
		int blockIndex = 0;
		// allowing for one of each per page
		boolean haveCDX = false;
		boolean haveMOL = false;
		boolean havePNG = false;
		boolean haveXYZ = false;
		seekIn(pos0); // for debugging dynamic change of method
		long ptr;
		while ((ptr = getPosition()) < ptNext) {
			blockIndex++;
			int len = (int) (haveSpec ? peekInt() : ptNext - ptr);
			if (len > 0) {
				nBlocks++;
				if (testing) {
					System.out.println("additional block " + nBlocks + " index=" + blockIndex + " len=" + len + " from "
							+ ptr + " to " + (ptr + len) + " ptNext=" + ptNext);
				}
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
//					offset = (haveXYZ ? -1 : findBytes(xyzKey, len, false, 0));
//					if (offset >= 0) {
//						haveXYZ = true;
//						exportXYZ(ptr, offset, nBlocks);
//					}
				}
			}
			if (!haveSpec)
				break;
			nextBlock();
		}
		System.out.println("\n======Page " + nPages + " additional blocks: " + nBlocks);
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
	 * 
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
	 * 
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
		long len = getPosition() - ptr;
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

	private void exportXYZ(long lastPosition, int skip, int nBlock) throws IOException {
		// todo? Do we want this?
	}

	/**
	 * Found the PNG file, so export it.
	 * 
	 * @param lastPosition
	 * @param skip
	 * @throws IOException
	 */
	private void exportPNG(long lastPosition, int skip, int nBlock) throws IOException {
		seekIn(lastPosition);
		// int w = 0, h = 0;
		try {
			// this part is basically just for fun. We skip if we have to.
			readPointer("PNG1");
			readInt();
			readFourUnknownInts();
			readInt(); // 0
			nextBlock(); // -> 501150
			readPointer("PNG2"); // EOF + 4
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
			/* w = (int) */ Math.round(readDouble());
			/* h = (int) */ Math.round(readDouble());
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
		byte[] bytes = readPNGData(getPosition());
		int len = (bytes == null ? 0 : bytes.length);
		if (len > 0) {
			nPNG++;
			handleFileData(nBlock, DefaultStructureHelper.PNG_FILE_DATA, bytes, getPosition() - len, len, null, null);
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
		long len = getPosition() - ptr;
		seekIn(ptr);
		return readBytes(len);
	}

	private byte[] readXYZData(long ptr) throws IOException {
		seekIn(ptr);
		testing = showInts = showChars = true;
		readInt(); // 0x0D 0x0A 0x1A 0x0A
		long len = getPosition() - ptr;
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
		readPointer("MOL");
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
		if (len > 0 && getPosition() + len < ptr + 10) {
			nMOL++;
			byte[] bytes = readBytes(len);
			handleFileData(nBlock, DefaultStructureHelper.MOL_FILE_DATA, bytes, getPosition(), len, null, null);
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
	 * @param fname    currently null in all cases (CDX, MOL, PNG)
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
			fname = "file_" + zeroFill(thisTest, 2) + "_" + zeroFill(nPages, 2) + type;
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

//	/**
//	 * The general idea.
//	 * 
//	 * @throws IOException
//	 */
//	void dumpFileInfo() throws IOException {
//		showChars = false;
//		showInts = false;
//		showBlock = 5;
//		readVersion();
//		int nPages = readToPageCount();
//		readPointer(null); // -> EOF
//		nextBlock();
//		for (int i = 0; i < nPages; i++) {
//			long pt = getPosition();
//			System.out.println("\n======Page " + (i + 1) + " starts at " + pt);
//			long ptNext = readPointer("page");
//			Stack<BlockData> s = readPageBlockStack(ptNext);
//			String header = readPageTitle(getPosition(), s);
//			if (header != null) {
//				System.out.println("page header = " + header);
//				if (readToParameters(getPosition())) {
//					System.out.println(" params at " + getPosition());
//					readParams(i);
//					System.out.println("\n======Page " + (i + 1) + " parameters end at " + getPosition());
//					searchForExports(getPosition(), i, ptNext, true);
//				} else {
//					searchForExports(getPosition(), i, ptNext, true);
//				}
//			}
//			seekIn(ptNext);
//		}
//		System.out.println("\n===Pages end at " + getPosition() + " available=" + getAvailable());
//	}

	/**
	 * 
	 * @param isNew -- ignored -- here just in case we have to go back.
	 * @throws IOException
	 */
	private void getFileStructure(boolean isNew) throws IOException {
		resetIn();

		readHeader();
		long ptBody = getPosition();
		fileStructure = new BlockData(0, len, "file");
		fileStructure.addSubblock(new BlockData(0, ptBody, "header"));
		BlockData body = this.body = new BlockData(ptBody, len - ptBody, "body");
		fileStructure.addSubblock(body);

		fileBlockPagesData = "file.body.part4.block1";
		fileBlockPageData = "file.body.part4.block1.data2";
		fileBlockVersionData = "file.body.part1.block1";
		Stack<BlockData> stack = followPointer(27, len, "part");
		if (stack != null) {
			body.addStack(stack);
			for (BlockData bd : stack) {
				getBlockStructure(bd, 0);
			}
		}
		getFileStructurePages(isNew);
		fileStructure.setPaths(null);
		testFileStructure();
	}

	private Object getFileStructureForJSON() {
		Map<String, Object> map = new TreeMap<>();
		fileStructure.map(map);
		return map;
	}

	private void testFileStructure() throws IOException {
		StringBuffer sb = new StringBuffer();
		fileStructure.getStack(sb, null);
		System.out.println(sb);
	}

	private void getFileStructurePages(boolean isNew) throws IOException {
		fileStructure.setPaths(null);
		BlockData bd33 = getBlockFromPath(fileBlockPageData);
		getBlockStructure(bd33, 12);
		fileStructure.setPaths(null);
		testFileStructure();
		BlockData bd331 = getBlockFromPath(fileBlockPageData + ".block1");
		BlockData bdlast = (bd331 == null ? null : bd331.getSubblock(-1));
		if (bdlast != null) {
			try {
			bdlast.seek();
			System.out.println("page=bdlast is " + bdlast);
			long pos;
			nPages = 0;
			while ((pos = getPosition()) < len) {
				// each page is two blocks.
				nextBlock();
				nPages++;
				long ptr2ndBlock = getPosition();
				long ptNextPage = (getAvailable() < 4 ? len : readPointer(null));
				BlockData page = new BlockData(pos, ptNextPage - pos, "page" + nPages);
				body.addPage(page);
				if (ptNextPage >= len)
					break;
				if (readValidPageHeader(pos + 4, ptr2ndBlock)) {
					getBlockStructure(page, (int) (getPosition() - pos));					
					// back up one pointer
					seekIn(-4);
					nextBlock(); // 172 to 39140
					readPointer(null); // to END
					getBlockStructure(page, (int) (getPosition() - pos));
				}					
				seekIn(ptNextPage);
			}
			} catch (Exception e) {
				System.err.println("error indexing page data for page " + nPages + " pos=" + getPosition());
				logError(e);
			}
		}
		nPagesTotal = nPages;
	}

//	private int readToPageCount() throws IOException {
//		rewindIn();
//		seekIn(27);
//		nextBlock();
//		nextBlock();
//		nextBlock();
//		// pages
//		System.out.println("\n===Pages start at " + getPosition());
//		readPointer("page count3"); // -> EOF
//		nextBlock(); // 32
//		readInt(); // 0
//		readPointer(null); // -> EOF
//		findPointer(0, "npages");
//		int nPages = readInt();
//		System.out.println("...Page count = " + nPages);
//		return nPages;
//	}

	private static String testFile;
	private static int defaultTest = 2;
	private static int testPage = -1;
	private static int thisTest = -1;
	private static int testFile0; // 1-based
	private static int testFile1; // 1-based
	private static String testLog = "";

	private static boolean runFileTest(String fname, String outdir) {
		try {
			// create structures for a specific file given
			createStructureFiles = (testFile != null);
			File f = new File(fname);
			String filename = f.getAbsolutePath();
			byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println("\n\n" + thisTest + " " + bytes.length + " bytes in " + filename);
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

			outdir += f.getName() + "."; // why "." here?
			thisFileName = filename;
			rdr.outdir = outdir;
			if (!rdr.process()) {
				return false;
			}
			System.out.println("MNova file closed for " + filename);
			if (rdr.reportData != null) {
				IFDDefaultJSONSerializer serializer = new IFDDefaultJSONSerializer();
				serializer.openObject();
				serializer.addObject("MNova.metadata", rdr.reportData);
				serializer.addObject("Mnova.fileStructure", rdr.getFileStructureForJSON());
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
		System.err.println("processing " + thisFileName);
		testLog += e + " error processing " + thisFileName + "\n";
		return;
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
//		long pos = getPosition();
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
//		readLenStringSafely(); // Import Item, for example
//		readFourUnknownInts();
//		readUTF16String(); // name?
//		readInts(2);
//		readByte(); // 255
//		readInt(); // -1
//		int test = readInt();
//		if (test > 0) {
//			readInt();
//			readInt();
//			String itemType = readLenStringSafely(); // "Item Type"
//			String subtype = readLenStringSafely(); // "NMR Spectrum"
//			System.out.println("Item " + (index + 1) + ": " + itemType + "/" + subtype);
//		}
//		if (testing)
//			System.out.println("---item end---" + (getPosition() - pos) + " pos=" + getPosition());
//	}
//	/**
//	 * Read an item header.
//	 * 
//	 * @return pointer to next record
//	 * @throws IOException
//	 */
//	private long readItemHeader() throws IOException {
//		readLenStringSafely(); // MestReNova
//		readInt(); // pointer?
//		readInt(); // 0x3000C;
//		readLenStringSafely(); // Windows 10
//		readLenStringSafely(); // DESKTOP-ORV6S3F
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
//		long pos = getPosition();
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
//			System.out.println("---item end---" + (getPosition() - pos) + " pos=" + getPosition());
//	}
//
//	private void readItemData0(int index) throws IOException {
//		readLenStringSafely(); // Import Item, for example
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
	/**
	 * various tests
	 * 
	 * @throws IOException
	 */
	private void test() throws IOException {
		//Stack<BlockData> s = followPointer(27, len, "block");
		//System.out.println(s);
//		findRef(30000, 38528);
//	    seekIn(38628);
//	    peekInts(30);
//		testFileStructure();
//
//		//System.out.println(followPointer(38728, 765767,"test ").toString().replace(',','\n'));
//		getFileStructure(false);
//		
//		
//		
		//findRef(0, 12871);
		testFileStructure();
//		readVersion();

//		System.out.println(followPointer(12969, len,"test ").toString().replace(',','\n'));
		
		System.out.println("OK");
		
//		testFIDs(nPagesTotal);

		if (true)
			return;
		

		testing = false;
		showChars = false;
		showInts = true;

		pointerTest = null;// "version";

		testing = false;
		showChars = true;
		showInts = true;

		isQuiet = false; // true for no list params
		return;
	}

	void testFIDs(int nPages) throws IOException {
		if (testPage > nPages) {
			System.out.println("Too many pages! " + testPage + " > " + nPages + " setting page to " + nPages);
			testPage = nPages;
		}
		if (testPage > 0) {
			testFID(testPage);
		} else {
			for (int i = 0; i < nPages; i++) {
				testFID(i + 1);
			}
		}
	}

	/**
	 * locate the FID for this page
	 * 
	 * @param page
	 * @throws IOException
	 */
	private void testFID(int page) throws IOException {
		System.out.println("Checking FID page " + page + "/" + nPagesTotal);
		long pt = seekPageBlock(page, -1);
		long ptNext = -1;
		long len = -1;
		if (pt >= 0) {
			nextBlock();
			ptNext = peekPointer();
			len = ptNext - getPosition();
			readInt();
			nextBlock();
			pt = readInt();
		}

		switch ((int)pt) {
		case 1:
		case 2:
			readBytes(5);
			break;
		default:
			seekIn(-4);
			String msg = "no spectrum found for " + outdir + " page " + page;
			msg = " version " + mnovaVersion + " " + msg;
			if (testLog != null)
				testLog += msg + "\n";
			System.out.println(msg);
			return;
		}
		readMysteryInts(7);

		// start of FID data
		long ptNext2 = peekPointer();
		if (ptNext2 != ptNext) {
			System.err.println("Something wrong here " + ptNext2 + " not expected " + ptNext);
		}
		readPointer("FID");
		readShort(); // 0x28 Bruker
		len = readInt();
		System.out.println("FID data length is " + len);
		readByte(); // 65794: 0x01
		String msg;
		pt = getPosition();
		if (ptNext == pt + len) {
			msg = "FID page " + page + " at " + pt + " len " + len + " matches data structure size";
		} else {
			msg = "FID " + page + " at " + pt + " len " + len + " does not match data size " + (ptNext - getPosition());
		}
		msg = " version " + mnovaVersion + " " + msg;
		if (testLog != null)
			testLog += msg + "\n";
		System.out.println(msg);
	}

	private void readMysteryInts(int n) throws IOException {
		for (int i = 0; i < n; i++) {
//			System.out.println("next mystery block + 6 ints" + i);
			nextBlock(); 
			readInts(6);
		}
	}

	static void testAll(String outdir) {

		if (testFile0 < 1)
			testFile0 = 1;
		if (testFile1 < testFile1)
			testFile1 = testFile0;
		if (testFile1 > testFiles.length)
			testFile1 = testFiles.length;
		boolean ok = true;
		
		for (int i = testFile0 - 1; i < testFile1 && ok; i++) {
			thisTest = i + 1;
			String msg;
			File f = new File(testFiles[i]);
			if (!f.exists()) {
			  msg = "error - file " + f.getAbsolutePath() + " does not exist";
			  ok = true;// IE continue anyway
			} else if (runFileTest(testFiles[i], outdir)) {
				msg = "Test " + (i + 1) + " on " + testFiles[i] + " OK";
			} else {
				msg = "Test " + (i + 1) + " on " + testFiles[i] + " failed";
				ok = false;
			}
			System.out.println(msg);
			testLog += msg + "\n";
		}
		System.out.println(testLog);
		if (ok && testLog.indexOf("error") < 0)
			System.out.println("All tests successful");

	}

	private static void startup(String[] args) {
		int pt = 0;
		String outdir = null;
		if (args.length >= 2 && args[0].equals("-o")) {
			outdir = args[1];
			pt = 2;
		}
		if (args.length > pt && "--testall".equals(args[pt])) {
			try {
				testFile0 = Integer.parseInt(args[++pt]);
				testFile1 = Integer.parseInt(args[++pt]);
			} catch (Exception e) {
			}
			testLog = "MNovaMetadataReader  " + Arrays.toString(args).replaceAll("[\\[,\\]]", "") + "\n";
			testAll(outdir);
			return;
		}

		String fileName = null;
		if (args.length > pt && "--test".equals(args[pt])) {
			// --test or --test [filename]
			if (args.length > ++pt) {
				String s = args[pt];
				try {
					defaultTest = Integer.parseInt(s);
				} catch (Exception e) {
					testFile = s;
				}
			}
			fileName = (testFile != null ? testFile : testFiles[defaultTest - 1]);
		} else if (args.length > pt) {
			// just the filename
			fileName = args[pt];
		} else {
			System.out.println("usage: MNovaMetadataReader [-o outputdir] --test");
			System.out.println("usage: MNovaMetadataReader [-o outputdir] --testall");
			System.out.println("usage: MNovaMetadataReader [-o outputdir] mnovaFilename");
			System.out.println("a json file will be created");
		}
		if (fileName != null) {
			runFileTest(fileName, outdir);
		}
	}
	
	public static void main(String[] args) {

		testing = false; // verbose option

		// default for --test
		defaultTest = testFiles.length;

		// defaults for testall
		testFile0 = 3;
		testFile1 = 3;//testFiles.length;

		testPage = -1; // just this page if positive

//		debugging = true; // passed on

		startup(args);
	}


	static final String[] testFiles = { //


			/* 1 */ "test/mnova/cyclohex.mnova", // 14.2.1 no spectrum, just dropped in
																				// cyclohexane.xyz structure
			/* 2 */ "test/mnova/3a-C.mnova", // from ACS OK one page, with ChemDraw drawing
			/* 3 */ "test/mnova/1.mnova", // from ACS two pages, no structures
			/* 4 */ "test/mnova/1-deleted.mnova", // first page param list only, next page blank
			/* 5 */ "test/mnova/1-v14.mnova", // saved by MNova v. 14 two pages
			/* 6 */ "test/mnova/3a-C-taxol.mnova", // saved by MNova v. 14; dropped in taxol.mol
			/* 7 */ "test/mnova/1-caff-taxol.mnova", // saved by MNova v. 14; dropped in caffeine.mol and taxol.mol
			/* 8 */ "test/mnova/1-caff-taxol-rev.mnova", // saved by MNova v. 14; dropped in caffeine.mol and taxol.mol
															// in reverse order
			/* 9 */ "test/mnova/1-caff-taxol-delete.mnova", // saved by MNova v. 14; dropped in caffeine.mol and
															// taxol.mol, caffeine deleted in
			/* 10 */ "test/mnova/1-taxol-drop.mnova", // saved by MNova v. 14; dropped in taxol.mol, repositioned,
														// scaled and resized
			/* 11 */ "test/mnova/1-taxol-drop-move.mnova", // saved by MNova v. 14; dropped in taxol.mol and moved
			/* 12 */ "test/mnova/3a-c-morphine.mnova", // saved by MNova v. 14; morphine.mol added using file...open
			// nt at GitHub - see
			/* 13 */ "c:\\temp\\iupac\\acs\\zip\\22232721\\metadatanmr\\nmr spectra.mnova", // from ACS; too big for
																							// GitHub
			/* 14 */ "test/mnova/3aa-C.mnova", // CDX extraction OK
			/* 15 */ "test/mnova/10.mnova", // PNG extraction OK version 6.1
			/* 16 */ "test/mnova/Substrate_1'h.mnova", // PNG extraction failed 6.1
			/* 17 */ "test/mnova/Substrate_1k.mnova", // Temperature parameter failed 6.1
			/* 18 */ "test/mnova/Products_3a.mnova", // png failed
			/* 22 */ "c:/temp/mnova/test/test3.mnova", // bruker sample 1r+1i JDX
			/* 23 */ "c:/temp/mnova/test/test4.mnova", // three spectra, last from Bruker-JDX
			/* 24 */ "c:/temp/mnova/test/test-predict.mnova", // three spectra, last from Bruker-JDX
			/* 25 */ "c:/temp/mnova/test/t.mnova", // Bruker - 5 pages
			/* 26 */ "test/mnova/cyclohex-Si.mnova", // modified
			/* 27 */ "test/mnova/cyclohex.mnova", // 14.2.1 no spectrum, just dropped in cyclohexane.xyz structure
			/* 28 */ "test/mnova/empty6.mnova", // first empty; second with drawing 
			/* 30 */ "test/mnova/draw2.mnova", // first empty; second with drawing 
			/* 31 */ "test/mnova/empty2.mnova", // two empty pages file
			/* 32 */ "test/mnova/empty.mnova", // empty page file
			
	};

}
