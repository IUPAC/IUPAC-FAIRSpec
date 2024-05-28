package com.integratedgraphics.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.nmrml.parser.Acqu;

import com.integratedgraphics.ifd.vendor.DefaultVendorPlugin;
import com.integratedgraphics.ifd.vendor.jeol.NmrMLJeolAcquStreamReader;
import com.integratedgraphics.ifd.vendor.varian.NmrMLVarianAcquStreamReader;

public class NmrMLConverterTest {

	public final static void main(String[] args) {
		try {

			FileInputStream fis;
			Acqu acq;

			fis = newStream("test/varian/sucrose_1h/procpar");
			NmrMLVarianAcquStreamReader varian = new NmrMLVarianAcquStreamReader(fis);
			acq = varian.read();
			fis.close();
			setParams(varian.getDimension(), acq);

			fis = newStream("test/varian/agilent_2d/procpar");
			varian = new NmrMLVarianAcquStreamReader(fis);
			acq = varian.read();
			fis.close();
			setParams(varian.getDimension(), acq);
			String filename = "test/jeol/1d_1d-13C.jdf";
			byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			NmrMLJeolAcquStreamReader jeol = new NmrMLJeolAcquStreamReader(bytes);
			acq = jeol.read();
			fis.close();
			setParams(jeol.getDimension(), acq);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static FileInputStream newStream(String f) throws FileNotFoundException {
		String inputFolder = new File(f).getAbsolutePath();
		return new FileInputStream(inputFolder);
	}

	protected static void setParams(int dim, Acqu acq) {
		// reportName();
		report("DIM", dim);
		double freq = acq.getTransmiterFreq();
		report("F1", freq);
		String nuc = acq.getObservedNucleus();
		nuc = DefaultVendorPlugin.fixNucleus(nuc);
		report("N1", nuc);
		int nominalFreq = DefaultVendorPlugin.getNominalFrequency(freq, nuc);
		report("SF", nominalFreq);
		String solvent = acq.getSolvent();
		report("SOLVENT", solvent);
		String pp = acq.getPulseProgram();
		report("PP", pp);
		String probe = acq.getProbehead();
		report("PROBE", probe);
	}

	private static void report(String key, Object val) {
		System.out.println(key + " = " + val);

	}

}