package com.integratedgraphics.ifs.vendor.mestrelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import com.integratedgraphics.ifs.vendor.ByteBlockReader;

/**
 * A rough MestReNova file reader. Not particularly useful. But it does check
 * for spectra at least.
 * 
 * @author hansonr
 *
 */
class MNovaReader extends ByteBlockReader {

	private final static int magicNumber = 0x4D657374; // M e s t

//	static {
//		long x = Double.doubleToLongBits(100.622829328806);
//		System.out.println(Long.toHexString(x).toUpperCase());
//		// <40><59><27><DC><6F><8B><8D><88>
//		x = 0;
//	}
	private MestrelabIFSVendorPlugin plugin;

	MNovaReader(MestrelabIFSVendorPlugin mestrelabIFSVendorPlugin, InputStream in) throws IOException {
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
		try {
			// cannot find temperature in code showDoubleLong(298.1983);
			// doubleTest();
			// double:
			findRef(20425);
			// checkDouble(432893, 10);
			if (!readHeader(magicNumber))
				return false;
			readString(headerLength);
			int maxBlocks = 20;
			out: for (int i = 0; i < maxBlocks; i++) {
				System.out.println("\nblock " + i + " pos " + readPosition() + " avail " + readAvailable());
				switch (i) {
				case 0:
					readInt(); // F1E2D3C4
					if (!readSubblock(i))
						break out;
					break;
				case 1:
					readUTF16String();
					readUTF16String();
					break;
				case 2:
					readBlock(2);
					break;
				case 3:
					readSubblock(3);
					break;
				case 4:
					readItems();
					break;
				case 5:
					readSpecs(5);
					break;
				default:
					if (!readBlock(i))
						break out;
				}
			}
			// 178 + 376 = 554
//			markIn(376);
//			skip(372);
//			if (!readNMRParameters()) {
//				return true;
//			}
			System.out.println("MNOVA ------- nSpec=" + nSpec);
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

	private boolean readHeader(int magicNumber) throws IOException {
		int nAvail = readAvailable();
		if (nAvail < 1)
			return false;
		int n = peekInt();
		if (n != magicNumber) {
			return false;
		}
		return true;
	}

	/**
	 * Read items. 
	 */
	private void readItems() throws IOException {
		readInt(); // byte block length
		int nItems = readInt();
		for (int j = 0; j < nItems; j++) {
			if (logging)
				System.out.println("j=" + j);
			readItem();
		}
	}

	private void readItem() throws IOException {
		long n = readPosition();
		if (logging)
			System.out.println("---item--- pos=" + readPosition());
		int type = readInt(); // 0, 53, 63 -- version dependent?
		int i = readInt();
		if (logging)
			System.out.println("----------  " + type + "," + i);
		if (type == 0) {
			// older version?
		} else {
			readInt(); // 0
			readLenString(); // MestReNova
			readInt(); // pointer?
			readInt(); // 0x3000C;
			readLenString(); // Windows 10
			readLenString(); // DESKTOP-ORV6S3F
			readInt();
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
			readLenString(); // "Item Type"
			readLenString(); // "NMR Spectrum"
		}
		if (logging)
			System.out.println("---item end---" + (readPosition() - n) + " pos=" + readPosition());
	}

	/**
	 * key for 
	 */
	private final static int[] paramsKey = new int[] { 0x0D, 0x05, 0x00 };

	private void readSpecs(int i) throws IOException {
		System.out.println("---readSpecInfo " + readPosition());	//  38628 - 39077	
		readInt(); // to EOF
		readBlock(i); // 32 bytes
		readInts(4); // also to EOF
		while (readAvailable() > 0 && peekInt() == 40) {
			System.out.println("---read spec at " + readPosition());
			plugin.newPage();
			long pt = readSpec(i);
			int len = (int) (pt - readPosition());
			int pos = findIn(paramsKey, len);
			if (pos >= 4) {
				skipIn(pos - 4);
				readParams(i);
			}
			skipIn((int)(pt - readPosition()));
		}
		System.out.println("---read done ");
	}

	int nSpec = 0;

	private long readSpec(int i) throws IOException {
		nSpec++;
		System.out.println("reading spectrum " + nSpec);
		readBlock(i); // 40
		int len = readInt(); // to next spectrum
		long pos = readPosition();
		long pt = pos + len;
		readBlock(i); // 40
		readBlock(i); // 32
		readInts(10);
		readInts(12);
		// looking for "1D" or "2D" -- seems to be variable length
		while (peekInt() != 4) {
			readInt();
		}
		String dim = readUTF16String(); // 1D
		String nuc = readUTF16String(); // 13C
		// 2D will report "1H13C, Unknown"
		plugin.addParam("DIM", dim, null, null);
		plugin.addParam("NUC12", nuc, null, null);
		readInts(15);
		readDouble();
		readDouble();
		readByte();
		readInts(15);
		readLenString(); // NMR Spectrum
		readUTF16String(); // N M R
		readDoubleBox();
		return pt;

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
	}

	private void readParams(int ib) throws IOException {

		int n = readInt();

		for (int i = 0; i < n; i++) {
			int b = readParam();
			System.out.println(" param " + i + ": " + b);
		}
		return;

	}

	/**
	 * A class for holding units, source, calculation, and value for an MNova parameter.
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
			String s = value + " " + (units == null ? "" : " " + units) 
					+ (source == null ? "" : " FROM " + source);
			return s;
		}
	}
	
	private int readParam() throws IOException {
//		peekInts(80);
//		logging = true;
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
		plugin.addParam(key, null, param1, param2);
		return type;
	}

}