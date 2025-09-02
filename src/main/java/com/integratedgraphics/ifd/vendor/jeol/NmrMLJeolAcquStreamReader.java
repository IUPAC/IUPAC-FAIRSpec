package com.integratedgraphics.ifd.vendor.jeol;

/*
 * derived from NmrMLJeolAcquReader
 * 
 * CC-BY 4.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.nmrml.parser.Acqu;
import org.nmrml.parser.jeol.JeolParameter;

import com.integratedgraphics.ifd.vendor.ByteBlockReader;
import com.integratedgraphics.ifd.vendor.nmrml.NmrMLHeader;

/**
 * Reader for Jeol JDF file
 * 
 * see also https://github.com/cheminfo/jeolconverter/blob/master/src/parseJEOL.js
 *
 * @author Daniel Jacob
 *
 *         Date: 18/09/2017
 *
 *         adapted by Bob Hanson 2021.06.28
 *         
 *         
 * 
 */
public class NmrMLJeolAcquStreamReader extends ByteBlockReader implements NmrMLHeader {

	private static Map<String, Object> jeolIni;

	public int data_Dimension_Number;
	private String creationTime;
	public String title;
	public String comment;
	public double base_Freq;

	public NmrMLJeolAcquStreamReader(byte[] bytes) throws FileNotFoundException, IOException {
		super(bytes);
		if (jeolIni == null) {
			jeolIni = FAIRSpecUtilities.getJSONResource(Acqu.class, "jeol/jeol.ini.json");
		}
	}

	public Acqu read() throws IOException {

		boolean fprt = true;

		Locale.setDefault(new Locale("en", "US"));
		Acqu acquisition = new Acqu(Acqu.Spectrometer.JEOL);

		String File_Identifier = readSimpleString(8);
		if (fprt)
			System.out.println("Header: File_Identifier = " + File_Identifier);

		int endian = readByte();
		ByteOrder byteOrder = (endian == 1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		acquisition.setByteOrder(byteOrder);
		acquisition.setBiteSyze(8);

		if (fprt)
			System.out.println("Header: Endian = " + endian);

		int Major_version = readByte();
		if (fprt)
			System.out.println("Header: Major_version = " + Major_version);
		skipIn(2);// seek(12);
		data_Dimension_Number = readByte();
		if (fprt)
			System.out.println("Header: Data_Dimension_Number = " + data_Dimension_Number);
		skipIn(1);// seek(14);
		int Data_Type = readByte();
		if (fprt)
			System.out.println("Header: Data_Type = " + Data_Type);

		String Instrument = getTerm("INSTRUMENT", readByte());
		if (fprt)
			System.out.println("Header: Instrument = " + Instrument);
		skipIn(8);// seek(24);
		byte[] Data_Axis_Type = readBytes(8);
		if (fprt)
			System.out.println("Header: Data_Axis_Type = " + Data_Axis_Type[0] + ", ... ");

		byte[] Data_Units = readBytes(16);
		if (fprt)
			System.out.println(String.format("Header: Data_Units = %d, %d, ...", Data_Units[0], Data_Units[1]));

		title = readSimpleString(124);
		if (fprt)
			System.out.println("Header: Title = " + title);

		skipIn(4);// seek(176); // dataAxisRanged
		int Data_Points = readInt();
		if (fprt)
			System.out.println("Header: Data_Points = " + Data_Points);
		skipIn(28);// seek(208);
		int Data_Offset_Start = readInt();
		if (fprt)
			System.out.println("Header: Data_Offset_Start = " + Data_Offset_Start);

		skipIn(188);// seek(400);
		
		creationTime = toDateTime(readInt());
		if (fprt)
			System.out.println("Header: Creation_Time = " + creationTime);
		//String revisionTime = toDateTime(
		readInt()
		//)
		;
		
		// seek(408);
		String Node_Name = readSimpleString(16);
		if (fprt)
			System.out.println("Header: Node_Name = " + Node_Name);
		String Site = readSimpleString(128);
		if (fprt)
			System.out.println("Header: Site = " + Site);
		String Author = readSimpleString(128);

		if (fprt)
			System.out.println("Header: Author = " + Author);
		comment = readSimpleString(128);
		if (fprt)
			System.out.println("Header: Comment = " + comment);
		String Data_Axis_Titles = readSimpleString(256);
		if (fprt)
			System.out.println("Header: Data_Axis_Titles = " + Data_Axis_Titles);

		// seek(1064);
		base_Freq = readDouble();
		if (fprt)
			System.out.println("Header: Base_Freq = " + base_Freq);

		skipIn(56);// seek(1128);
		double Zero_Freq = readDouble();
		if (fprt)
			System.out.println("Header: Zero_Freq = " + Zero_Freq);

		skipIn(76);// seek(1212);
		int Param_Start = readInt();
		if (fprt)
			System.out.println("Header: Param_Start = " + Param_Start);

//        seek(1228);
		skipIn(68);// seek(1284);
		long Data_Start = readInt();
		if (fprt)
			System.out.println("Header: Data_Start = " + Data_Start);
		long Data_Length = readLong();
		if (fprt)
			System.out.println("Header: Data_Length = " + Data_Length);

		if (fprt)
			System.out.println("------");

		skipIn(Param_Start - 1296);//// seek(Param_Start);
		setByteOrder(byteOrder);
		int Parameter_Size = readInt();
		int Low_Index = readInt();
		int High_Index = readInt();
		int Total_Size = readInt();
		if (fprt)
			System.out.println(String.format("Header: Params: Size=%d, Low_Index=%d, High_Index=%d, Total_Size=%d",
					Parameter_Size, Low_Index, High_Index, Total_Size));

		if (fprt)
			System.out.println("------");

		// boolean irr_mode = false;
		int[] factors = null;
		int[] orders = null;

		for (int count = 0; count <= High_Index; count++) {

			JeolParameter param = readParam();
			String param_name = param.name.trim();
			String Unit_label = "";

			boolean flg = false;
			if (param_name.equals("inst_model_number")) {
				acquisition.setInstrumentName(param.valueString);
				flg = true;
			}
			if (param_name.equals("version")) {
				acquisition.setSoftVersion(param.valueString);
				flg = true;
			}
			if (param_name.equals("experiment")) {
				acquisition.setPulseProgram(param.valueString);
				flg = true;
			}
			if (param_name.equals("sample_id")) {
				flg = true;
			}
			if (param_name.equals("probe_id")) {
				flg = true;
			}
			if (param_name.equals("total_scans")) {
				acquisition.setNumberOfScans(BigInteger.valueOf(param.valueInt));
				flg = true;
			}
			if (param_name.equals("acq_delay")) {
				flg = true;
			}
			if (param_name.equals("delay_of_start")) {
				flg = true;
			}
			if (param_name.equals("relaxation_delay")) {
				acquisition.setRelaxationDelay(param.valueDouble);
				flg = true;
			}
			if (param_name.equals("exp_total")) {
				flg = true;
			}
			if (param_name.equals("solvent")) {
				acquisition.setSolvent(param.valueString);
				flg = true;
			}
			if (param_name.equals("temp_set")) {
				Unit_label = getTerm("Unit_labels", param.unit);
				if (Unit_label.equals("dC")) {
					acquisition.setTemperature(param.valueDouble + 273.15);
				} else {
					acquisition.setTemperature(param.valueDouble);
				}
				flg = true;
			}
			if (param_name.equals("spin_set")) {
				acquisition.setSpiningRate((int) (param.valueDouble));
				flg = true;
			}
			if (param_name.equals("irr_mode")) {
				if (param.valueString.toLowerCase().equals("off")) {
					acquisition.setDecoupledNucleus("off");
				}
				flg = true;
			}
			if (param_name.equals("irr_domain")) {
				if (param.valueString.toLowerCase().equals("proton")) {
					acquisition.setDecoupledNucleus("1H");
				}
				if (param.valueString.toLowerCase().equals("Carbon13")) {
					acquisition.setDecoupledNucleus("13C");
				}
				flg = true;
			}
			if (param_name.equals("irr_freq")) {
				flg = true;
			}
			if (param_name.equals("irr_offset")) {
				flg = true;
			}
			if (param_name.equals("x_acq_time")) {
				flg = true;
			}
			if (param_name.equals("x_acq_duration")) {
				flg = true;
			}
			if (param_name.equals("x_probe_map")) {
				acquisition.setProbehead(param.valueString);
				flg = true;
			}
			if (param_name.equals("x_prescans")) {
				acquisition.setNumberOfSteadyStateScans(BigInteger.valueOf(param.valueInt));
				flg = true;
			}
			if (param_name.equals("x_points")) {
				acquisition.setAquiredPoints(param.valueInt);
				flg = true;
			}
			if (param_name.equals("x_domain")) {
				String ObservedNucleus = param.valueString;
				if (param.valueString.toLowerCase().equals("proton")) {
					ObservedNucleus = "1H";
				}
				acquisition.setObservedNucleus(ObservedNucleus);
				acquisition.setDecoupledNucleus("off");
				flg = true;
			}
			if (param_name.equals("x_freq")) {
				Unit_label = getTerm("Unit_labels", param.unit);
				if (Unit_label.equals("Hz")) {
					acquisition.setTransmiterFreq(param.valueDouble / 1000000.0);
				} else {
					acquisition.setTransmiterFreq(param.valueDouble);
				}
				flg = true;
			}
			if (param_name.equals("x_sweep")) {
				acquisition.setSpectralWidthHz(param.valueDouble);
				flg = true;
			}
			if (param_name.equals("x_offset")) {
				acquisition.setFreqOffset(param.valueDouble * base_Freq);
				flg = true;
			}
			if (param_name.equals("x_pulse")) {
				acquisition.setPulseWidth(param.valueDouble);
				flg = true;
			}
			if (param_name.equals("x_resolution")) {
				flg = true;
			}
			if (param_name.equals("factors")) {
				String[] sfactors = param.valueString.trim().replace("  ", " ").split(" ");
				factors = new int[sfactors.length];
				for (int i = 0; i < sfactors.length; i++) {
					try {
						factors[i] = Integer.parseInt(sfactors[i]);
					} catch (NumberFormatException nfe) {
						// Not an integer
					}
				}
				flg = true;
			}
			if (param_name.equals("orders")) {
				String[] sorders = param.valueString.trim().replace("  ", " ").split(" ");
				orders = new int[sorders.length];
				for (int i = 0; i < sorders.length; i++) {
					try {
						orders[i] = Integer.parseInt(sorders[i]);
					} catch (NumberFormatException nfe) {
						// Not an integer
					}
				}
				flg = true;
			}
			if (fprt & flg) {
				System.out.print("Param: %s" + param.name);
				if (param.value_type == 0)
					System.out.print("\t = \t " + param.valueString + "%s ");
				else if (param.value_type == 1)
					System.out.print("\t = \t " + param.valueInt);
				else if (param.value_type == 2)
					System.out.print("\t = \t " + param.valueDouble);
				System.out.println(String.format(" %s%s", getTerm("Unit_prefix", param.unit_prefix),
						getTerm("Unit_labels", param.unit)));
			}
		}

		/* Software */
		acquisition.setSoftware(getTerm("SOFTWARE", "SOFTWARE"));

		/* sweep width in ppm */
		acquisition.setSpectralWidth(acquisition.getSpectralWidthHz() / acquisition.getTransmiterFreq());

		/* Group Delay = 0 */
		acquisition.setDspGroupDelay(0.0);
		if (orders.length > 0 && factors.length > 0) {
			double GroupDelay = 0;
			int nbo = orders[0];
			for (int k = 0; k < nbo; k++) {
				double prodfac = 1;
				for (int i = k; i < nbo; i++)
					prodfac *= factors[i];
				GroupDelay = GroupDelay + 0.5 * ((orders[k + 1] - 1) / prodfac);
			}
			acquisition.setDspGroupDelay(GroupDelay);
		}

		/* Pointer (offset) into the JDF file where data start (in octets) */
		acquisition.setDataOffset(Data_Start);
		/* Data length into the JDF file from data start (in octets) */
		acquisition.setDataLength(Data_Length);

		if (fprt)
			System.out.println("------");

		close();

		return acquisition;
	}

	/**
	 * Convert 4-byte date/time to approximate date and time in ISO-8601 format.
	 * This appears to be within +/-1 sec of what JEOL's Jason program shows. The
	 * relationship is incommensurate; their algorithm must be ever so slightly
	 * different.
	 * 
	 * @param cdt
	 * @return
	 */
	private static String toDateTime(int cdt) {
		int year, month, day;

		//bits: yyyy yyym mmmd dddd tttt tttt tttt tttt
		//.......3...........2............1............
		//......1098 7654 3210 9876 5432 1098 7654 3210
		
		year = 1990 + (cdt >> 25);
		month = ((cdt >> 21) & 0xF);
		day = ((cdt >> 16) & 0x1F);
		 
		// stored number is fraction of day in quanta of 86400/65535 sec.
		// this will be at most 1 sec. off from what JEOL Jason reports.
		
		cdt = cdt & 0xffff;
		

		int seconds = (int)(cdt/65535.0*86399.99);
		
		int hour = seconds / 3600;
		int min = (seconds - hour * 3600) / 60;
		int sec = (seconds - hour * 3600 - min * 60);
 
		String iso8601 = year
				+ (month < 10 ? "-0" : "-") + month   //
				+ (day < 10 ? "-0" : "-") + day       //
				+ "T" + (hour < 10 ? "0" : "") + hour //
				+ (min < 10 ? ":0" : ":") + min       //
				+ (sec < 10 ? ":0" : ":") + sec;

		System.out.println("JEOLDT " + cdt + " " + iso8601);
		return iso8601;
	}
	
	static {
//		System.out.println(toDateTime(0xFFFF));
//		System.out.println(toDateTime(0x6000));
//		System.out.println(toDateTime(0x5000));
//		System.out.println(toDateTime(0x4000));
//		System.out.println(toDateTime(0x3000));
//		System.out.println(toDateTime(0x2000));
//		System.out.println(toDateTime(0x1000));
//		System.out.println(toDateTime(0x0));
//		
//		
	}

	private JeolParameter readParam() throws IOException {
		JeolParameter param = new JeolParameter();
		setBuf(64);
		byte[] b = new byte[16];
		get(b, 0, 4);
		// pos=4
		param.unit_scaler = getShort();
		// pos=6
		int u1 = getByte() & 0xFF;
		// pos=7

		int v1 = (int) Math.floor(u1 / 16);
		int v2 = (v1 > 8) ? (v1 % 8) : (v1 % 8) + 8;
		param.unit_prefix = v2;
		param.unit = getByte();
		// pos=8
		get(b, 0, 8);
		// pos = 16
		markBuffer();
		get(b, 0, b.length);
		// pos 32
		int value_type = getShort();
		resetBuffer();
		// pos 16
		param.value_type = value_type;
		switch (value_type) {
		case 0:
			param.valueString = new String(b).trim();// decoder.decode(buffer).toString().trim();
			break;
		case 1:
			param.valueInt = getInt();
			break;
		case 2:
			param.valueDouble = getDouble();
			break;
		case 3:
			break;
		}
		param.name = new String(getBuf(), 36, 28).toLowerCase().trim();
		return param;
	}

	@SuppressWarnings("unchecked")
	private String getTerm(String section, String key) {
		return (String) ((Map<String, Object>) jeolIni.get(section)).get(key);
	}

	@SuppressWarnings("unchecked")
	private String getTerm(String section, int item) {
		return (String) ((List<Object>) jeolIni.get(section)).get(item);
	}

	@Override
	public int getDimension() {
		return data_Dimension_Number;
	}

	public final static void main(String[] args) {
		String testFile = "test/jeol/1d_1d-13C.jdf";
		String fname = (args.length == 0 ? testFile : args[0]);
		testing = false;
		showInts = false;
		showChars = false;
		try {
			String filename = new File(fname).getAbsolutePath();
			byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			System.out.println(bytes.length + " bytes in " + filename);
			Acqu acq = new NmrMLJeolAcquStreamReader(bytes).read();
			System.out.println(acq);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public String getCreationTime(String creationTime) {
		return creationTime;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getCreationTime() {
		return creationTime;
	}


}
