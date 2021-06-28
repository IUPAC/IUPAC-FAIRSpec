package org.iupac.fairspec.util;

import java.io.File;
import java.io.FileInputStream;

import org.nmrml.converter.Acqu2nmrML;
import org.nmrml.cv.SpectrometerMapper;
import org.nmrml.parser.Acqu;

import com.vendor.jeol.NmrMLJeolAcquStreamReader;

import javajs.util.Rdr;

public class IFSNMRConverter {

	
	
	public final static void main(String[] args) {
		try {

//			Acqu2nmrML nmrmlObj = new Acqu2nmrML();
//
//			Properties prop = new Properties();
//			prop.load(Acqu2nmrML.class.getResourceAsStream("resources/config.properties"));
//			nmrmlObj.setSchemaLocation(prop.getProperty("schemaLocation"));
//			CVLoader cvLoader = new CVLoader(
//					Acqu2nmrML.class.getResourceAsStream("resources/onto.ini"));
//			nmrmlObj.setCVLoader(cvLoader);

			SpectrometerMapper vendorMapper = new SpectrometerMapper(
					Acqu2nmrML.class.getResourceAsStream("resources/jeol.ini"));

//			nmrmlObj.setVendorMapper(vendorMapper);

			String f = new File("test/jeol/1d_1d-13C.jdf").getAbsolutePath();
			byte[] bytes = Rdr.getLimitedStreamBytes(new FileInputStream(f), -1);
//			nmrmlObj.setInputFolder(inputFolder);
			NmrMLJeolAcquStreamReader jeolAcqObj = new NmrMLJeolAcquStreamReader(bytes);
			jeolAcqObj.setVendorMapper(vendorMapper);
			Acqu acq = jeolAcqObj.read();
			System.out.println(acq);
//			nmrmlObj.setAcqu(acq);
//
//	           nmrmlObj.setVendorLabel("JEOL");//Vendor.toUpperCase());
//	           nmrmlObj.setIfbinarydata(false);//cmd.hasOption("b"));
//	           nmrmlObj.setCompressed(false);//cmd.hasOption("z"));
//
//	           nmrmlObj.Convert2nmrML( "c:/temp/iupac/nmrml/test.nmrml.xml" );
//
//
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}