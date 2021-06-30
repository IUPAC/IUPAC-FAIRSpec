/*
 * CC-BY 4.0
 */

package com.integratedgraphics.ifs.vendor.varian;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nmrml.parser.Acqu;

/**
 * Reader for Varian's propar files
 *
 * @author Daniel Jacob
 *
 * Date: 21/07/2014
 * Time: 15:00
 *
 */
public class NmrMLVarianAcquStreamReader {

    private BufferedReader inputAcqReader;

	private int dimension = 1;
	
	public int getDimension() {
		return dimension;
	}

    // parameter types
    private final static Pattern REGEXP_INTEGER = Pattern.compile("1 (\\d+)"); // Get an intéger
    private final static Pattern REGEXP_DOUBLE = Pattern.compile("1 (\\d+\\.?\\d*)"); // Get a double
    private final static Pattern REGEXP_STRING = Pattern.compile("1 \"(\\S+)\""); // Get a string

    // parameters from procpar
    private final static Pattern REGEXP_SFO1 = Pattern.compile("^sfrq "); //irradiation frequency
    private final static Pattern REGEXP_BF1 = Pattern.compile("^reffrq "); //spectral frequency
    private final static Pattern REGEXP_O1 = Pattern.compile("^tof "); //irradiation frequency offset
    private final static Pattern REGEXP_NUMBEROFSCANS = Pattern.compile("^nt "); //number of scans
    private final static Pattern REGEXP_DUMMYSCANS = Pattern.compile("^ss "); //number of dummy (steady state) scans
    private final static Pattern REGEXP_SPINNINGRATE = Pattern.compile("^spin "); // spinning rate
    private final static Pattern REGEXP_RELAXATIONDELAY = Pattern.compile("^d1 "); // relaxation delay D1
    private final static Pattern REGEXP_TD = Pattern.compile("^np "); //acquired points (real+imaginary)
    private final static Pattern REGEXP_PULSEWIDTH = Pattern.compile("^pw "); // pulseWidth90
    private final static Pattern REGEXP_SW = Pattern.compile("^sw "); //spectral width (Hz)
    private final static Pattern REGEXP_NUC1 = Pattern.compile("^tn "); // observed nucleus
    private final static Pattern REGEXP_NUC2 = Pattern.compile("^dn "); // decoupled nucleus
    private final static Pattern REGEXP_TEMPERATURE = Pattern.compile("^temp "); // temperature in Kelvin
    private final static Pattern REGEXP_SOLVENT = Pattern.compile("^solvent "); // solvent name
    private final static Pattern REGEXP_PROBHD = Pattern.compile("^probe_ "); // probehead
    private final static Pattern REGEXP_PULPROG = Pattern.compile("^seqfil "); //pulse program

    public NmrMLVarianAcquStreamReader(InputStream acqFileInputStream) {
        inputAcqReader = new BufferedReader(new InputStreamReader(acqFileInputStream));
    }

    public Acqu read() throws Exception {
        Matcher matcher;
        Locale.setDefault(new Locale("en", "US"));
        Acqu acquisition = new Acqu(Acqu.Spectrometer.VARIAN);
        String line = inputAcqReader.readLine();
        while (inputAcqReader.ready() && (line != null)) {
        	if (line.length() < 20 || Character.isDigit(line.charAt(0))) {
                line = inputAcqReader.readLine();
                continue; // BH 
        	}
        	if (line.startsWith("acqdim")) {
                line = inputAcqReader.readLine();
                try {
                dimension = Integer.parseInt(line.substring(2,3));
                } catch (Exception e) {
                	System.out.println("???");
                	// ignore
                }
        	} else
            //spectral frequency
            if (REGEXP_BF1.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setSpectralFrequency(Double.parseDouble(matcher.group(1)));
                }
            } else 
            //irradiation_frequency
            if (REGEXP_SFO1.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setTransmiterFreq(Double.parseDouble(matcher.group(1)));
                }
            } else
            //irradiation_frequency offset
            if (REGEXP_O1.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setFreqOffset(Double.parseDouble(matcher.group(1)));
                }
            } else
            /* sweep width in Hertz*/
            if (REGEXP_SW.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setSpectralWidthHz(Double.parseDouble(matcher.group(1)));
                }
            } else
            /* number of data points */
            if (REGEXP_TD.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_INTEGER.matcher(line).find()) {
                    matcher = REGEXP_INTEGER.matcher(line);
                    matcher.find();
                    acquisition.setAquiredPoints(Integer.parseInt(matcher.group(1)));
                }
            } else 
            // number of scans
            if (REGEXP_NUMBEROFSCANS.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_INTEGER.matcher(line).find()) {
                    matcher = REGEXP_INTEGER.matcher(line);
                    matcher.find();
                    acquisition.setNumberOfScans(BigInteger.valueOf(Long.parseLong(matcher.group(1))));
                }
            } else 
            // number of dummy (steady state) scans
            if (REGEXP_DUMMYSCANS.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_INTEGER.matcher(line).find()) {
                    matcher = REGEXP_INTEGER.matcher(line);
                    matcher.find();
                    acquisition.setNumberOfSteadyStateScans(BigInteger.valueOf(Long.parseLong(matcher.group(1))));
                }
            } else
            // spinning rate
            if (REGEXP_SPINNINGRATE.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_INTEGER.matcher(line).find()) {
                    matcher = REGEXP_INTEGER.matcher(line);
                    matcher.find();
                    acquisition.setSpiningRate(Integer.parseInt(matcher.group(1)));
                }
            } else
            // relaxation delay D1
            if (REGEXP_RELAXATIONDELAY.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setRelaxationDelay(Double.parseDouble(matcher.group(1)));
                }
            } else
            // pulseWidth
            if (REGEXP_PULSEWIDTH.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setPulseWidth(Double.parseDouble(matcher.group(1)));
                }
            } else
            // temperature in Kelvin
            if (REGEXP_TEMPERATURE.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_DOUBLE.matcher(line).find()) {
                    matcher = REGEXP_DOUBLE.matcher(line);
                    matcher.find();
                    acquisition.setTemperature(Double.parseDouble(matcher.group(1)) + 274.15 );
                }
            } else
            // solvent name
            if (REGEXP_SOLVENT.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_STRING.matcher(line).find()) {
                    matcher = REGEXP_STRING.matcher(line);
                    matcher.find();
                    acquisition.setSolvent(matcher.group(1));
                }
            } else
            // observed nucleus
            if (REGEXP_NUC1.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_STRING.matcher(line).find()) {
                    matcher = REGEXP_STRING.matcher(line);
                    matcher.find();
                    acquisition.setObservedNucleus(matcher.group(1));
                }
            } else
            // decoupled nucleus
            if (REGEXP_NUC2.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_STRING.matcher(line).find()) {
                    matcher = REGEXP_STRING.matcher(line);
                    matcher.find();
                    acquisition.setDecoupledNucleus(matcher.group(1));
                    if ( acquisition.getDecoupledNucleus().equals("") ) {
                         acquisition.setDecoupledNucleus("off");
                    }
                }
            } else
            // probehead
            if (REGEXP_PROBHD.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_STRING.matcher(line).find()) {
                    matcher = REGEXP_STRING.matcher(line);
                    matcher.find();
                    acquisition.setProbehead(matcher.group(1));
                }
            } else
            // pulse program
            if (REGEXP_PULPROG.matcher(line).find()) {
                line = inputAcqReader.readLine();
                if (REGEXP_STRING.matcher(line).find()) {
                    matcher = REGEXP_STRING.matcher(line);
                    matcher.find();
                    acquisition.setPulseProgram(matcher.group(1));
                }
            }


            line = inputAcqReader.readLine();
        }

        /* sweep width in ppm*/
        acquisition.setSpectralWidth( acquisition.getSpectralWidthHz()/acquisition.getTransmiterFreq());

        /* Group Delay = 0 */
        acquisition.setDspGroupDelay( 0.0 );

        inputAcqReader.close();
        return acquisition;
    }

}
