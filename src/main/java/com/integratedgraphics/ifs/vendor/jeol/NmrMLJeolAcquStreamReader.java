package com.integratedgraphics.ifs.vendor.jeol;

/*
 * derived from NmrMLJeolAcquReader
 * 
 * CC-BY 4.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nmrml.parser.Acqu;
import org.nmrml.parser.jeol.JeolParameter;

import com.integratedgraphics.ifs.util.Util;

/**
 * Reader for Jeol JDF file
 *
 * @author Daniel Jacob
 *
 * Date: 18/09/2017
 *
 * adapted by Bob Hanson 2021.06.28
 * 
 */
public class NmrMLJeolAcquStreamReader {

	private static Map<String, Object> jeolIni;

	private InputStream in;

    byte[] buf = new byte[1];
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private ByteBuffer buffer;
	
	public int data_Dimension_Number;
	public String title;
	public String comment;
	public double base_Freq;
	

	private JeolParameter readParam() throws IOException {
		JeolParameter param = new JeolParameter();
		getBuf(64);
		byte[] b = new byte[16];
		buffer.get(b, 0, 4);
		// pos=4
		param.unit_scaler = buffer.getShort();
		// pos=6
		int u1 = buffer.get() & 0xFF;
		// pos=7

		int v1 = (int) Math.floor(u1 / 16);
		int v2 = (v1 > 8) ? (v1 % 8) : (v1 % 8) + 8;
		param.unit_prefix = v2;
		param.unit = buffer.get();
		// pos=8
		buffer.get(b, 0, 8);
		// pos = 16
		buffer.mark();
		buffer.get(b);
		// pos 32
		int value_type = buffer.getShort();
		buffer.reset();
		// pos 16
		param.value_type = value_type;
		switch (value_type) {
		case 0:
			param.valueString = new String(b).trim();//decoder.decode(buffer).toString().trim();
			break;
		case 1:
			param.valueInt = buffer.getInt();
			break;
		case 2:
			param.valueDouble = buffer.getDouble();
			break;
		case 3:
			break;
		}
		param.name = new String(buf, 36, 28).toLowerCase().trim();
		return param;
	}

    private byte[] getBuf(int len) throws IOException {
    	if (buf.length < len) {
    		buf = new byte[len << 1];
    		buffer = ByteBuffer.wrap(buf);
    		buffer.order(byteOrder);
    	}
    	buffer.rewind();
    	in.read(buf, 0, len);
		return buf;
	}
    
	private String readString(int len) throws IOException {
    	return new String(getBuf(len), 0, len);
    }

	private int readInt() throws IOException {
    	getBuf(4);
    	return buffer.getInt();
		
	}
	
	private long readLong() throws IOException {
    	getBuf(8);
    	return buffer.getLong();
	}
	
	private double readDouble() throws IOException {
    	getBuf(8);
    	return buffer.getDouble();
	}
		
    public NmrMLJeolAcquStreamReader() {
    }
    
   public NmrMLJeolAcquStreamReader(InputStream in) throws FileNotFoundException, IOException {
    	this.in = in;
    	if (jeolIni == null) {
    		jeolIni = Util.getJSONResource(Acqu.class, "jeol.ini.json");
    	}
    }
    
	public Acqu read() throws IOException {

		boolean fprt = true;

		Locale.setDefault(new Locale("en", "US"));
		Acqu acquisition = new Acqu(Acqu.Spectrometer.JEOL);

		String File_Identifier = readString(8);
		if (fprt)
			System.err.println("Header: File_Identifier = " + File_Identifier);

		int endian = in.read();
		ByteOrder byteOrder = (endian == 1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		acquisition.setByteOrder(byteOrder);
		acquisition.setBiteSyze(8);

		if (fprt)
			System.err.println("Header: Endian = " +endian);

		int Major_version = in.read();
		if (fprt)
			System.err.println("Header: Major_version = " + Major_version);
		in.skip(2);//seek(12);
		data_Dimension_Number = in.read();
		if (fprt)
			System.err.println("Header: Data_Dimension_Number = " +data_Dimension_Number);
		in.skip(1);//seek(14);
		int Data_Type = in.read();
		if (fprt)
			System.err.println("Header: Data_Type = " +Data_Type);

		String Instrument = getTerm("INSTRUMENT", in.read());
		if (fprt)
			System.err.println("Header: Instrument = " + Instrument);
		in.skip(8);//seek(24);
		byte[] Data_Axis_Type = new byte[8];
		in.read(Data_Axis_Type);
		if (fprt)
			System.err.println("Header: Data_Axis_Type = " + Data_Axis_Type[0] + ", ... ");

		byte[] Data_Units = new byte[16];
		in.read(Data_Units);
		if (fprt)
			System.err.println(String.format("Header: Data_Units = %d, %d, ...", Data_Units[0], Data_Units[1]));

		title = readString(124);
		if (fprt)
			System.err.println("Header: Title = " + title);

		in.skip(4);//seek(176);
		int Data_Points = readInt();
		if (fprt)
			System.err.println("Header: Data_Points = " +Data_Points);

		in.skip(28);//seek(208);
		int Data_Offset_Start = readInt();
		if (fprt)
			System.err.println("Header: Data_Offset_Start = " +Data_Offset_Start);

		in.skip(196);//seek(408);
		String Node_Name = readString(16);
		if (fprt)
			System.err.println("Header: Node_Name = " + Node_Name);
		String Site = readString(128);
		if (fprt)
			System.err.println("Header: Site = " + Site);
		String Author = readString(128);

		if (fprt)
			System.err.println("Header: Author = " + Author);
		comment = readString(128);
		if (fprt)
			System.err.println("Header: Comment = " + comment);
		String Data_Axis_Titles = readString(256);
		if (fprt)
			System.err.println("Header: Data_Axis_Titles = " + Data_Axis_Titles);

		//seek(1064);
		base_Freq = readDouble();
		if (fprt)
			System.err.println("Header: Base_Freq = " + base_Freq);

		in.skip(56);//seek(1128);
		double Zero_Freq = readDouble();
		if (fprt)
			System.err.println("Header: Zero_Freq = " + Zero_Freq);

		in.skip(76);//seek(1212);
		int Param_Start = readInt();
		if (fprt)
			System.err.println("Header: Param_Start = " +Param_Start);

//        seek(1228);
		in.skip(68);//seek(1284);
		long Data_Start = readInt();
		if (fprt)
			System.err.println("Header: Data_Start = " +Data_Start);
		long Data_Length = readLong();
		if (fprt)
			System.err.println("Header: Data_Length = " +Data_Length);

		if (fprt)
			System.err.println("------");

		in.skip(Param_Start - 1296);////seek(Param_Start);

		buffer.order(this.byteOrder = byteOrder);
		int Parameter_Size = readInt();
		int Low_Index = readInt();
		int High_Index = readInt();
		int Total_Size = readInt();
		if (fprt)
			System.err.println(String.format("Header: Params: Size=%d, Low_Index=%d, High_Index=%d, Total_Size=%d",
					Parameter_Size, Low_Index, High_Index, Total_Size));

		if (fprt)
			System.err.println("------");

		//boolean irr_mode = false;
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
				System.err.print("Param: %s" + param.name);
				if (param.value_type == 0)
					System.err.print("\t = \t " + param.valueString + "%s ");
				else if (param.value_type == 1)
					System.err.print("\t = \t " +param.valueInt);
				else if (param.value_type == 2)
					System.err.print("\t = \t " + param.valueDouble);
				System.err.println(String.format(" %s%s",
						getTerm("Unit_prefix", param.unit_prefix),
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
			System.err.println("------");

		in.close();

		return acquisition;
	}

	@SuppressWarnings("unchecked")
	private String getTerm(String section, String key) {
		return (String) ((Map<String, Object>)jeolIni.get(section)).get(key);
	}

	@SuppressWarnings("unchecked")
	private String getTerm(String section, int item) {
		return (String) ((List<Object>)jeolIni.get(section)).get(item);
	}

	public int getDimension() {
		return data_Dimension_Number;
	}
	
	public final static void main(String[] args) {
		try {
			String f = new File("test/jeol/1d_1d-13C.jdf").getAbsolutePath();
			FileInputStream fis = new FileInputStream(f);
			NmrMLJeolAcquStreamReader jeolAcqObj = new NmrMLJeolAcquStreamReader(fis);
			Acqu acq = jeolAcqObj.read();
			fis.close();
			System.out.println(acq);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
