package org.nmrml.converter;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.nmrml.cv.CVLoader;
import org.nmrml.cv.SpectrometerMapper;
import org.nmrml.parser.Acqu;
import org.nmrml.parser.jeol.JeolAcquReader;

public class TestBH {

	public final static void main(String[] args) {
		try {

			Acqu2nmrML nmrmlObj = new Acqu2nmrML();

			Properties prop = new Properties();
			prop.load(nmrMLpipe.class.getClassLoader().getResourceAsStream("resources/config.properties"));
			nmrmlObj.setSchemaLocation(prop.getProperty("schemaLocation"));
			CVLoader cvLoader = new CVLoader(
					nmrMLpipe.class.getClassLoader().getResourceAsStream("resources/onto.ini"));
			nmrmlObj.setCVLoader(cvLoader);

			SpectrometerMapper vendorMapper = new SpectrometerMapper(
					nmrMLpipe.class.getClassLoader().getResourceAsStream("resources/jeol.ini"));

			nmrmlObj.setVendorMapper(vendorMapper);

			String inputFolder = new File("test/jeol/1d_1d-13C.jdf").getAbsolutePath();

			nmrmlObj.setInputFolder(inputFolder);

			JeolAcquReader jeolAcqObj = new JeolAcquReader(inputFolder);
			jeolAcqObj.setVendorMapper(vendorMapper);
			Acqu acq = jeolAcqObj.read();
			nmrmlObj.setAcqu(acq);

	           nmrmlObj.setVendorLabel("JEOL");//Vendor.toUpperCase());
	           nmrmlObj.setIfbinarydata(false);//cmd.hasOption("b"));
	           nmrmlObj.setCompressed(false);//cmd.hasOption("z"));

	           nmrmlObj.Convert2nmrML( "c:/temp/iupac/nmrml/test.nmrml.xml" );


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}