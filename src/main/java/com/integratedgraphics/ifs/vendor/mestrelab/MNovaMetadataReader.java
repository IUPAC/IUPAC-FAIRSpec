package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.List;

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
	private int mnovaVersionNumber;

	MNovaMetadataReader(MestrelabIFSVendorPlugin mestrelabIFSVendorPlugin, InputStream in) throws IOException {
		super(in);
		this.plugin = mestrelabIFSVendorPlugin;
	}

	/**
	 * 
	x``` * @param extractor
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
					if (!readBlock1()) {
						i++; // skip items
					}
					break;
				case 2:
					// Items -- This is not necessary to read?
					readItems();
					break;
				case 3:
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

	private boolean readBlock1() throws IOException {
		readBlock();
		// 0x10
		// 0xFA1E5CE
		// 0x1E094C2F
		// 0x84C8143A
		// 0xEC620BE2
		//peekInts(50);
		long pt = readPointer(); // 12687 = 0x318F -> 12869 // 1.mnova 0x962E -> 38628 // 1-v14.mnova 8135
		if (peekInt() == 0) {
			// v.12-
			readInt();
		}
		// readInt(); // 0 // 0x11 in 1-nv14
		skipInTo(pt);
		return false;

	}

	private void readBlock3() throws IOException {
		readInt(); // 12687 = 0x318F -> 12869
		long pt = readPointer();
		if (pt == readPosition())
			return;
		readInt();
		//peekInts(40);
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
		//peekInts(50);
		skipInTo(pt);
	}

	/**
	 * Read the initial block
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean readBlock0() throws IOException {
		readInt(); // to just after version
		readBlock();
		readVersion();
		return true;
	}

	private void readVersion() throws IOException {
		peekInts(20);
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
	 * Read the items.
	 */
	private void readItems() throws IOException {
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
		//peekInts(50);
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
		//peekInts(30);
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
	private final static int[] paramsKey = new int[] { 0xD, 0x5, 0x0 };

	private void readPages() throws IOException {
		if (testing)
			System.out.println("---readPages " + readPosition()); // 38628 - 39077
		peekInts(50);
		readInt(); // to EOF
		readPageInsets();
		readLong(); // long to EOF
		int nPages = readInt();
		readInt();  // also to EOF
		for (int i = 0; i < nPages; i++) {
				readPage(i);
		}
		if (testing)
			System.out.println("---read done ");
	}

	private void readPageInsets() throws IOException {
		skipBlock(); // 32 bytes
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

		readBlock(); // 40 or 61 bytes, depending upon version
		if (plugin != null)
			plugin.newPage();
		nPages++;
		System.out.println("reading page " + (index + 1));
		int len = peekInt(); // to next spectrum or EOF
		long pt = readPointer();
		int skip = findIn(paramsKey, len, false);
		if (skip >= 4) {
			// actually targeting the integer just before 0x0000000D0000000500000000, which
			// holds the number of parameters.
			skipIn(skip - 4);
			readParams();
		}
		skipInTo(pt);
	}

	/**
	 * Key method fore reading all parameters available in the MNova file.
	 * 
	 * @throws IOException
	 */
	private void readParams() throws IOException {

		System.out.println("readParams0 " + readPosition());
		int n = readInt();
		for (int i = 0; i < n; i++) {
			readParam(i);
		}
		System.out.println("readParams1 " + readPosition());
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

		//peekInts(80);
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

	/**
	 * various tests
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void test() throws IOException {
//		if (!testing)
//			return;
//		checkDouble(38969, 4);
//		skipInTo(38965);
//		peekInts(100);
//		findRef(42031);
//		traceRef(39135, true);
		return;
	}

	public static void main(String[] args) {
		String testFile;
		testFile = "test/mnova/3a-C.mnova"; // OK one page
		//testFile = "test/mnova/1.mnova"; // OK two pages
		//testFile = "test/mnova/1-v14.mnova"; // OK two pages
		//testFile = "test/mnova/3a-C-taxol.mnova"; // (v 14) OK, but looking for model
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

