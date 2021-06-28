package org.nmrml.converter;

import java.io.File;
import java.io.FileInputStream;

import org.iupac.fairspec.util.IFSDefaultVendorPlugin;
import org.nmrml.cv.SpectrometerMapper;
import org.nmrml.parser.Acqu;

import com.vendor.jeol.NmrMLJeolAcquStreamReader;
import com.vendor.varian.NmrMLVarianAcquStreamReader;

import javajs.util.Rdr;

public class TestBH {

	public final static void main(String[] args) {
		try {

//			Acqu2nmrML nmrmlObj = new Acqu2nmrML();

//			Properties prop = new Properties();
//			prop.load(nmrMLpipe.class.getResourceAsStream("resources/config.properties"));
//			nmrmlObj.setSchemaLocation(prop.getProperty("schemaLocation"));
//			CVLoader cvLoader = new CVLoader(
//					nmrMLpipe.class.getResourceAsStream("resources/onto.ini"));
//			nmrmlObj.setCVLoader(cvLoader);
//
			String inputFolder;
			Acqu acq;

			inputFolder = new File("test/varian/sucrose_1h/procpar").getAbsolutePath();
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(inputFolder);
			acq = varian.read();
			setParams(varian.getDimension(), acq);

			inputFolder = new File("test/varian/agilent_2d/procpar").getAbsolutePath();
			varian = new NmrMLVarianAcquStreamReader(inputFolder);
			acq = varian.read();
			
			setParams(varian.getDimension(), acq);

			inputFolder = new File("test/jeol/1d_1d-13C.jdf").getAbsolutePath();
			byte[] bytes = Rdr.getLimitedStreamBytes(new FileInputStream(inputFolder), -1);
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			SpectrometerMapper vendorMapper = new SpectrometerMapper(
					Acqu2nmrML.class.getResourceAsStream("resources/jeol.ini"));
			jeol.setVendorMapper(vendorMapper);
			acq = jeol.read();

			setParams(jeol.getDimension(), acq);
			
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected static void setParams(int dim, Acqu acq) {
		//reportName();
		report("DIM", dim);
		double freq = acq.getTransmiterFreq();
		report("F1", freq);
 		String nuc = acq.getObservedNucleus();
		nuc = IFSDefaultVendorPlugin.fixNucleus(nuc);
		report("N1", nuc);
		int nominalFreq = IFSDefaultVendorPlugin.getNominalFrequency(freq, nuc);
		report("SF", nominalFreq);
		String solvent = acq.getSolvent();
		report("SOLVENT",solvent);
		String pp = acq.getPulseProgram();
		report("PP", pp);
		String probe = acq.getProbehead();
		report("PROBE", probe);
	}

	private static void report(String key, Object val) {
		System.out.println(key + " = " + val);
		
	}

}