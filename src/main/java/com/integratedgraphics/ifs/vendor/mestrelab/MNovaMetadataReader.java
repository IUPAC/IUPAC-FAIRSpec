package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.iupac.fairspec.util.Util;

import com.integratedgraphics.ifs.vendor.ByteBlockReader;

/**
 * A rough MestReNova file reader that can deliver metadata only.
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
	private String mnovaVersion;
	private long ptrToVersion;
	private int mnovaVersionXX;

	MNovaMetadataReader(MestrelabIFSVendorPlugin mestrelabIFSVendorPlugin, InputStream in) throws IOException {
		super(in);
		this.plugin = mestrelabIFSVendorPlugin;
	}

	/**
	 * 
	 * @param extractor
	 * @return
	 */
	public boolean process() {
		int headerLength = 23;
		setByteOrder(ByteOrder.BIG_ENDIAN);
		// testing = true;
		try {
			test();
			if (!checkMagicNumber(magicNumber))
				return false;
			readSimpleString(headerLength);
			if (!checkMagicNumber(magicNumberBE)) { //
				if (!checkMagicNumber(magicNumberLE)) {
					return false;
				}
				setByteOrder(ByteOrder.LITTLE_ENDIAN);
			}
			int maxBlocks = 20;
			out: for (int i = 0; i < maxBlocks; i++) {
				System.out.println("\nblock " + i + " pos " + readPosition() + " avail " + readAvailable());
				switch (i) {
				case 0:
					readInt(); // 0xF1E2D3C4
					readBlock0(); // includes version
					break;
				case 1:
					readBlock1();
					break;
				case 2:
					// Items -- This is not necessary to read?
					readItems();
					break;
				case 3:
					readPages(3);
					break;
				default:
					if (!readBlock(i))
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

	private void readBlock1() throws IOException  {
		readBlock(1);
		// 0x10
		// 0xFA1E5CE
		// 0x1E094C2F
		// 0x84C8143A
		// 0xEC620BE2
		peekInts(50);
		long pt = readPointer(); // 12687 = 0x318F -> 12869  // 1.mnova 0x962E -> 38628 // 1-v14.mnova 8135
		if (peekInt() == 0) {
			readInt();
		} else {
			skipInTo(pt);
//			readInt(); // 0 // 11 in 1-nv14
		}
		return;
	}

	private void readBlock3() throws IOException {
		readInt(); // 12687 = 0x318F -> 12869
		long pt = readPointer();
		if (pt == readPosition())
			return;
		readInt();
		peekInts(40);
		readLenString(); // Create Attached Document Items
		readInts(4);
		readUTF16String(); // hansonr
		readByte();
		readInts(11);
		readUTF16String(); // Title
		readInt(); // 0C
		readUTF16String(); // .
		readUTF16String(); // Name
		readInt();
		readLenString(); // Molecule
		readUTF16String(); // Molecule
		readInt(); // 0C
		readLenString(); // {9410359c-fb30-43d5-92ca-155219bc9c92}
		readUTF16String(); // ID
		readInt(); // 0C
		readLenString(); // {7dfbc937-b91d-40bd-85b4-bb0e54c5dbb0}
		readInt();
		readLenString(); // Item
		readInts(2);

		readInts(7);
		readLenString(); // ad.stolaf
		readShort(); // -1
		readShort(); // C7C0
		readByte();
		readShort();
		readItemHeader();
		readLenString();
		readInts(4); // Create Molecule from Template (Molecule)
		readUTF16String(); // hansonr
		readByte();
		readInts(11);
		readUTF16String(); // Title
		readInt(); // 0C
		readUTF16String(); // .
		readUTF16String(); // Name
		readInt();
		readLenString(); // Molecule
		readUTF16String(); // Molecule
		readInt(); // 0C
		readLenString(); // {9410359c-fb30-43d5-92ca-155219bc9c92}
		readUTF16String(); // ID
		readInt(); // 0C
		readLenString(); // {7dfbc937-b91d-40bd-85b4-bb0e54c5dbb0}
		readInt();
		readLenString(); // Item
		peekInts(50);
		skipInTo(pt);
	}

	/**
	 * Read the initial block
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean readBlock0() throws IOException {
		int len0 = readInt(); // to just after version
		ptrToVersion = readPointer();
		long pos = readPosition(); // 35
		readInt();// -> 3393
		readInt();// -> 3381
		readInt();// -> 3365
		readInt();// -> 3364
		readInt();// -> 3363
		int flag = readShort();
		if (flag == 1) {
			readInt();
			readInts(7); // 8 0 0 0 4 0 1
			readByte(); // 110 0x6E
			readInt(); // 0
			readInt(); // 8
			readInt(); // 0
			readInt(); // 0
			// was "block1"
			readVersion();
			return true;
		}
		skipInTo(ptrToVersion);
		readVersion();
//		readShort(); // 0xD6
//		readInt();// 0xC6
//		readInt();// 0
//		readInt();// 0
//		readInt();// 66
//		readUTF16String();// MS Shell Dlg 2
//		readInts(4);
//		readByte(); // 0x10
//		readUTF16String(); // {index}
//		readInts(6);
//		readInt(); // 36 --> 206
//		readUTF16String();// MS Shell Dlg 2
//		readInts(4);
//		readByte(); // 0x10
//		readUTF16String(); // {name, 0}
//		readByte();// 1
//		peekInts(100);
//		readInts(9);
		return true;
	}

	private void readVersion() throws IOException {
		readUTF16String(); // MestReNova
		mnovaVersion = readUTF16String(); // 12.0.0-20080
		if (plugin != null)
			plugin.setVersion(mnovaVersion);
		System.out.println("MNova version " + mnovaVersion);
		try {
			mnovaVersionXX = Integer.parseInt(mnovaVersion.substring(0, mnovaVersion.indexOf(".")));			
		} catch (NumberFormatException nfe) {
			mnovaVersionXX = Integer.MAX_VALUE;
		}
	}

	private void readParams0() throws IOException {
		readInt(); // 30 @4781 -taxol
		readInt(); // D
		readInt(); // 5
		readByte(); // 0
		readInt(); // 1
		int type = readInt(); // 0
		int count = readInt(); // 0
		readInt(); // 5
		readShort();
		readByte();
		readByte(); // 1
		readInt();
		readByte(); // 0
		readInt(); // -1
		readByte();
		readInt();// 35 0x23 -> 4863
		readInts(4);
		readInt(); // 0x3A
		readInt(); // 2
		readLenString(); // timestamp
		readByte(); // 7
		readInts(3);
		readLenString(); // origin
		readInt(); // 0A
		readUTF16String(); // Mnova
		int len1 = readInt(); // -> 5072
		readLenString(); // C 0 0 0 0

		readBlock(0);
//		readInt(); // 0x7c
//		readInt(); // 1
//		readInt(); // 25
//		readInt(); // 17
//		readInt(); // 5
//		readInt(); // 0
//		readInt(); // 0
//		readInt(); // 0
//		readInt(); // 0
//		readByte(); // 0
//		readInt(); // 0
//		peekInts(60);
//		readInt();
		long pt = readPointer();
//		readInt(); // 110 -> 5310
//		readInt(); // ->5759		
//		readInt(); // ->5755
//
//		readInt(); // ->5735
//		readInt(); // ->5727
//		readInt(); // ->5723
//		readInt(); // ->5719
//		readInt(); // ->5715
//		
//		readInt(); // ->5711
//		readInt(); // 8
//		readInt(); // 0
//		readInt(); // 0
//		readInt(); // 0

		skipInTo(pt);
		return;
//		return;
	}

	/**
	 * Read the items.
	 */
	private void readItems() throws IOException {
		long pt = readPointer(); // next
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
				skipInTo(pt);
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
	 * lenString "MestReNova"
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
		long pt = -1;
		if (type == 0) {
			// older version?
		} else {
			pt = readItemHeader();
		}
		readLenString(); // Import Item, for example
		readInts(4); // these are often the same for non-spectra?
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

	private void readItem14(int index) throws IOException {
		peekInts(50);
		long pos = readPosition();
		int type = readInt(); // 0, 53, 63 --0xC0000000 ? version dependent?
		int i = (type < 0 ? -1 : readInt());
		if (testing)
			System.out.println("---item--- " + index + " pos=" + pos + " type=" + type + " i=" + i);
		long pt = -1;
		if (type == 0) {
			// older version?
		} else {
			pt = readItemHeader();
		}
		System.out.println("item pt="+pt);
		readLenString(); // Import Item, for example
		readInts(4); // these are often the same for non-spectra?
		readUTF16String(); // user name?
		readInts(2);
		readByte(); // 255
		readInt(); // -1
		int test = readInt();
		peekInts(30);
		if (test > 0) {
			readInt(); // 0x1C
			readInt(); // 0xC
			if (peekInt() == 0) {
				readInts(3); // ?
				readBlock(-1);
			}
			String itemType = readLenString(); // "Item Type"
			String subtype = readLenString(); // "NMR Spectrum"
			System.out.println("Item " + (index + 1) + ": " + itemType + "/" + subtype);
		}
		if (testing)
			System.out.println("---item end---" + (readPosition() - pos) + " pos=" + readPosition());
	}

	/**
	 * Read an item header.
	 * 
	 * @return pointer to next record
	 * @throws IOException
	 */
	private long readItemHeader() throws IOException {
		readInt(); // 0
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
	private final static int[] paramsKey = new int[] { 0x0D, 0x05, 0x00 };

	private void readPages(int i) throws IOException {
		if (testing)
			System.out.println("---readSpecInfo " + readPosition()); // 38628 - 39077
		readInt(); // to EOF
		peekInts(50);
		readBlock(i);
//		skipBlock(); // 32 bytes
		readInts(4); // also to EOF
		// TODO: Properly check for end of pages
		while (readAvailable() > 0 && peekInt() == 40) {
			if (testing)
				System.out.println("---read spec at " + readPosition());
			readPage(i);
		}
		showInts = false;
		if (testing)
			System.out.println("---read done ");
	}

	/**
	 * Just skipping most of this information now, but it gives us the pointer we
	 * need for skipping to the next block.
	 * 
	 * @param i
	 * @return
	 * @throws IOException
	 */
	private void readPage(int i) throws IOException {
		peekInts(60);
		readBlock(i);
//		skipBlock(); // 40
		if (readAvailable() == 0)
			return;
		if (plugin != null)
			plugin.newPage();
		nPages++;
		System.out.println("reading page " + nPages);
		int len = readInt(); // to next spectrum
		long pos = readPosition();
		int skip = findIn(paramsKey, len, false);
		if (skip >= 4) {
			// actually targeting the integer just before 0x0000000D0000000500000000, which
			// holds the number of parameters.
			skipIn(skip - 4);
			readParams();
		}
		skipInTo(pos + len);

//		readBlock(i); // 40
//		readBlock(i); // 32
//		readInts(10);
//		readInts(12);
//		// looking for "1D" or "2D" -- seems to be variable length
//		while (peekInt() != 4) {
//			readInt();
//		}
//		String dim = readUTF16String(); // 1D
//		String nuc = readUTF16String(); // 13C
//		// 2D will report "1H13C, Unknown"
//		//plugin.addParam("DIM", dim, null, null);
//		//plugin.addParam("NUC12", nuc, null, null);
//		readInts(15);
//		readDouble();
//		readDouble();
//		readByte();
//		readInts(15);
//		readLenString(); // NMR Spectrum
//		readUTF16String(); // N M R
//		readDoubleBox();

//		// all this next varies with spectrum - test 5 only
//		readByte();
//		readInts(46);
//		readByte();
//		readInts(30);
//		readByte();
//		readShort();
//		readInts(2);
//		readUTF16String(); // SimSun
//		readInts(8);
//		readUTF16String(); // MNova Default
//		readInt();
//		readInts(2); // 2E wrap
//		readUTF16String(); // Red-Blue (Gradient)
//		readBlock(i); // 464 block of gradient colors
//		readByte();
//		readShort();
//		readInts(2);
//		//
////		skip(479);
//		readUTF16String(); // Helvetica
//		readInts(44); // more?
//		readInts(44); // more?
//		readInts(44); // more?
//		readInts(2);
//		readInts(4);
//		readUTF16String(); // Arial
//		readInts(39); // more?
//		readUTF16String(); // f1 (ppm)
//		readByte();
//		readShort();
//		readInts(24);
//		readUTF16String(); // Intensity
//		readByte();
//		readShort();
//		readInts(52);
//		readUTF16String(); // Helvetica
//		readInts(66);
//		readUTF16String(); // Standard
//		readBlock(i); // 464 block of gradient colors
//		readInts(3);
//		readUTF16String(); // Arial
//		readInts(83);
//		readUTF16String(); // Arial
//		readByte();
//		readShort();
//		readInts(25); // more?
//		readUTF16String(); // Arial
//		readByte();
//		readShort();
//		readInts(78);
//		readUTF16String(); // SimSun
//		readInts(41);
//		readUTF16String(); // Arial
//		readInts(15);
//		readUTF16String(); // SimSun
//		readByte();
//		readInts(21);
//		readUTF16String(); // MNovaDecault
//		readByte();
//		readShort();
//		readInts(14);
//		readUTF16String(); // SimSun
//		readByte();
//		readShort();
//		readInts(29);
//		readUTF16String(); // SimSun
//		readInts(14);
//		readUTF16String(); // SimSun
//		readByte();
//		readShort();
//		readInts(14);
//		readUTF16String(); // SimSun
//		readByte();
//		readShort();
//		readInts(20);
//		readUTF16String(); // SimSun
//		readShort();
//		readInts(18);
//		readUTF16String(); // Standard
//		readBlock(i); // 464 block of gradient colors
//		readInts(3);
//		readInts(10);
//		readUTF16String(); // SimSun
//		readInts(6);
//		readInts(43);
//		readUTF16String(); // SimSun
//		readByte();
//		readInts(26);
//		readUTF16String(); // SimSun
//		readByte();
//		readShort();
//		readInts(14);
//		readUTF16String(); // SimSun
//		readShort();
//		readInts(69);
//		readUTF16String(); // SimSun
//		readByte();
//		readShort();
//		readInts(10);
//		readUTF16String(); // $ d ....
//		readUTF16String(); // Intensity
//		readByte();
//		readShort();
//		readInts(45);
//		readShort();
//		readInts(45);
//		readByte();
//		readShort();
//		readInts(202);
//		readInt(); // 00 00 0FED @ 20425
//		readInt(); // 0
//		return pt;
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
			String s = value + " " + (units == null ? "" : " " + units) + (source == null ? "" : " FROM " + source);
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
		peekInts(80);
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
			System.out.println(" param " + (index + 1) + ": " + type + " " + param1 + " " + param2);
	}

	/**
	 * various tests
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void test() throws IOException {
		if (!testing)
			return;
		// Q: In 3a-C.mnova (test #5), is the temperature saved in binary format?
		// System.out.println(findDoubleApprox(298.5953, 6, -1));
		// A: no
		// How about the spec freq for carbon?
//		int pt = findDouble(100.622829328806,-1);
		// yes, 38402
//		checkDouble(pt-32, 10);
//		0 3 = c09e9b9b9c3f66d8	-1958.901963225033
//		0 4 = 405927dc6f8b8d88	100.622829328806
//		0 5 = 40d7799d89d89d7d	24038.46153846149
//		0 6 = 4050fef8c0000001	67.9839324951172
//		0 7 = c0030064df5ebfa8	-2.375192399100303
//		0 8 = 3fc94f5fcc4607da	0.19773480867666998
//		0 9 = c01803273e4a7afe	-6.0030793889006855

		// Q: In 1.mnova (test #0), where is the spectrometer frequency?
//		int pt = findDouble(400.13280091,-1, true);
//		checkDouble(pt-32, 10);
//		checkDouble(66243-32, 10);
//		0 3 = c08926dfb45f1ed0	-804.8592307502058
//		0 4 = 4079021ff3d8d544	400.13280091   ##$SFO1
//		0 5 = 40bc2b89d89d89e4	7211.538461538472 ##$SW_h
//		0 6 = 4050ff18c0000001	67.9858856201172 ##$GRPDLY
//		0 7 = c01147a67363ea9f	-4.319970896695536
//		0 8 = 3fc6eed117459591	0.17916310917204872
//		0 9 = c08a3d343e377689	-839.650509293847
//		4 6 = c0000001c01147a6	-2.000003338363018
//		4 9 = 3e37768900000000	5.462911900622203E-9

		
//		 findRef(106);
//		 findRef(8030); // 1-v14.mnova to version
	}

	public static void main(String[] args) {
		String testFile;
		testFile = "test/mnova/3a-C.mnova";
		//testFile = "test/mnova/1.mnova";
		//testFile = "test/mnova/3a-C-taxol.mnova"; // (v 14)
		//testFile = "test/mnova/1-v14.mnova";
		String fname = (args.length == 0 ? testFile : args[0]);
		try {
			testing = true;
			showInts = true;
			showChars = true;
			String filename = new File(fname).getAbsolutePath();
			byte[] bytes = Util.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println(bytes.length + " bytes in " + filename);
			MNovaMetadataReader reader = new MNovaMetadataReader(null, new ByteArrayInputStream(bytes));
			reader.process();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

// references to EOF 
//834277 bytes in C:\Users\hansonr\git\IUPAC-FAIRSpec\test\mnova\1-v14.mnova
//8135	0x1fc7	+	826138	0xc9b1a	=	834277
//8179	0x1ff3	+	826094	0xc9aee	=	834277
//8187	0x1ffb	+	826086	0xc9ae6	=	834277
//456366	0x6f6ae	+	377907	0x5c433	=	834277
//456470	0x6f716	+	377803	0x5c3cb	=	834277
//456478	0x6f71e	+	377795	0x5c3c3	=	834277
//818002	0xc7b52	+	16271	0x3f8f	=	834277
//818566	0xc7d86	+	15707	0x3d5b	=	834277
//818586	0xc7d9a	+	15687	0x3d47	=	834277