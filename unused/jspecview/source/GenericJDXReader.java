package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import jspecview.common.Coordinate;
import jspecview.exception.JSVException;

public abstract class GenericJDXReader {

  /**
   * Labels for the exporter
   * 
   */
  protected final static String[] VAR_LIST_TABLE = {
      "PEAKTABLE   XYDATA      XYPOINTS",
      " (XY..XY)    (X++(Y..Y)) (XY..XY)    " };

  public static String getVarList(String dataClass) {
    int index = VAR_LIST_TABLE[0].indexOf(dataClass);
    return VAR_LIST_TABLE[1].substring(index + 1, index + 12).trim();
  }

  final static String ERROR_SEPARATOR = "=====================\n";

  protected float nmrMaxY = Float.NaN;

  //  static {
  //    Arrays.sort(TABULAR_DATA_LABELS);  OUCH! - Breaks J2S
  //  }
  protected JDXSource source;
  private JDXSourceStreamTokenizer t;
  protected SB errorLog;
  protected boolean obscure;

  protected boolean done;

  protected String filePath;

  protected boolean isSimulation;

  protected int firstSpec;

  protected boolean isTabularData;

  protected boolean isTabularDataLabel(String label) {
    return (isTabularData = ("##DATATABLE##PEAKTABLE##XYDATA##XYPOINTS#"
        .indexOf(label + "#") >= 0));
  }

  protected double blockID;

  /**
   * For JEOL, skip ##SHIFTREFERENCE
   */
  protected boolean ignoreShiftReference; // BH 2020.09.16

  public GenericJDXReader(String filePath, boolean obscure, int iSpecFirst,
      float nmrNormalization, boolean isSimulation) {
    this.isSimulation = isSimulation;
    if (isSimulation) {
      // TODO: H1 vs. C13 here?
      nmrMaxY = (Float.isNaN(nmrNormalization) ? 10000 : nmrNormalization);
      // filePath = JSVFileManager.getAbbrSimulationFileName(filePath);
    }
    // this.filePath is used for sending information back to Jmol
    // and also for setting the application tree label
    this.filePath = filePath;
    this.obscure = obscure;
    firstSpec = iSpecFirst;
  }

  /**
   * The starting point for reading all data.
   * 
   * @param reader
   *        a BufferedReader or a JSVZipFileSequentialReader
   * @param isZipFile 
   * 
   * @return source
   * @throws JSVException
   */
  protected JDXSource getJDXSource(Object reader, boolean isZipFile)
      throws JSVException {
    source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
    t = new JDXSourceStreamTokenizer((BufferedReader) reader);
    errorLog = new SB();

    String label = null;
    String value = null;
    boolean isOK = false;
    while (!done && "##TITLE".equals(t.peakLabel())) {
      isOK = true;
      if (label != null && !isZipFile)
        errorLog.append(
            "Warning - file is a concatenation without LINK record -- does not conform to IUPAC standards!\n");
      GenericJDXDataObject spectrum = newSpectrum();
      Lst<String[]> dataLDRTable = new Lst<String[]>();
      while (!done && (label = t.getLabel()) != null
          && (value = getValue(label)) != null) {
        if (isTabularData) {
          setTabularDataType(spectrum, label);
          if (!processTabularData(spectrum, dataLDRTable))
            throw new JSVException("Unable to read JDX file");
          addSpectrum(spectrum, false);
          if (isSimulation && spectrum.getXUnits().equals("PPM"))
            spectrum.setHZtoPPM(true);
          spectrum = null;
          continue;
        }
        if (label.equals("##DATATYPE") && value.toUpperCase().equals("LINK")) {
          getBlockSpectra(dataLDRTable);
          spectrum = null;
          continue;
        }
        if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
          getNTupleSpectra(dataLDRTable, spectrum, label);
          spectrum = null;
          continue;
        }
        if (label.equals("##JCAMPDX")) {
          setVenderSpecificValues(t.rawLine);
        }
        if (spectrum == null)
          spectrum = newSpectrum();
        if (readDataLabel(spectrum, label, value, errorLog, obscure))
          continue;
        addHeader(dataLDRTable, t.rawLabel, value);
        if (checkCustomTags(spectrum, label, value))
          continue;
      }
    }
    if (!isOK)
      throw new JSVException("##TITLE record not found");
    source.setErrorLog(errorLog.toString());
    return source;
  }

  /**
   * 
   * @param spectrum
   * @param label
   * @param value
   * @param errorLog
   * @param obscure
   * @return true to skip saving this key in the spectrum headerTable
   */
  protected boolean readDataLabel(GenericJDXDataObject spectrum, String label,
                                  String value, SB errorLog, boolean obscure) {
    if (readHeaderLabel(spectrum, label, value, errorLog, obscure))
      return true;

    // NOTE: returning TRUE for these means they are 
    // not included in the header map -- is that what we want?

    label += " ";
    if (("##MINX ##MINY ##MAXX ##MAXY ##FIRSTY ##DELTAX ##DATACLASS ")
        .indexOf(label) >= 0)
      return true;

    // NMR variations: need observedFreq, offset, dataPointNum, and shiftRefType 
    switch (("##FIRSTX  " + "##LASTX   " + "##NPOINTS " + "##XFACTOR "
        + "##YFACTOR " + "##XUNITS  " + "##YUNITS  " + "##XLABEL  "
        + "##YLABEL  " + "##NUMDIM  " + "##OFFSET  ").indexOf(label)) {
    case 0:
      spectrum.fileFirstX = Double.parseDouble(value);
      return true;
    case 10:
      spectrum.fileLastX = Double.parseDouble(value);
      return true;
    case 20:
      spectrum.nPointsFile = Integer.parseInt(value);
      return true;
    case 30:
      spectrum.xFactor = Double.parseDouble(value);
      return true;
    case 40:
      spectrum.yFactor = Double.parseDouble(value);
      return true;
    case 50:
      spectrum.setXUnits(value);
      return true;
    case 60:
      spectrum.setYUnits(value);
      return true;
    case 70:
      spectrum.setXLabel(value);
      return false; // store in hashtable
    case 80:
      spectrum.setYLabel(value);
      return false; // store in hashtable
    case 90:
      spectrum.numDim = Integer.parseInt(value);
      return true;
    case 100:
      if (spectrum.shiftRefType != 0) {
        if (spectrum.offset == GenericJDXDataObject.ERROR)
          spectrum.offset = Double.parseDouble(value);
        // bruker doesn't need dataPointNum
        spectrum.dataPointNum = 1;
        // bruker type
        spectrum.shiftRefType = 1;
      }
      return false;
    default:
      //    if (label.equals("##PATHLENGTH")) {
      //      jdxObject.pathlength = value;
      //      return true;
      //    }

      if (label.length() < 17)
        return false;
      if (label.equals("##.OBSERVEFREQUENCY ")) {
        spectrum.observedFreq = Double.parseDouble(value);
        return true;
      }
      if (label.equals("##.OBSERVENUCLEUS ")) {
        spectrum.setObservedNucleus(value);
        return true;
      }
      if ((label.equals("##$REFERENCEPOINT "))
          && (spectrum.shiftRefType != 0)) {
        spectrum.offset = Double.parseDouble(value);
        // varian doesn't need dataPointNum
        spectrum.dataPointNum = 1;
        // varian type
        spectrum.shiftRefType = 2;
        return false; // save in file  
      }
      if (label.equals("##.SHIFTREFERENCE ")) {
        //TODO: don't save in file??
        if (ignoreShiftReference
            || !(spectrum.dataType.toUpperCase().contains("SPECTRUM")))
          return true;
        value = PT.replaceAllCharacters(value, ")(", "");
        StringTokenizer srt = new StringTokenizer(value, ",");
        if (srt.countTokens() != 4)
          return true;
        try {
          srt.nextToken();
          srt.nextToken();
          spectrum.dataPointNum = Integer.parseInt(srt.nextToken().trim());
          spectrum.offset = Double.parseDouble(srt.nextToken().trim());
        } catch (Exception e) {
          return true;
        }
        if (spectrum.dataPointNum <= 0)
          spectrum.dataPointNum = 1;
        spectrum.shiftRefType = 0;
        return true;
      }
    }
    return false;
  }

  protected static boolean readHeaderLabel(JDXHeader jdxHeader, String label,
                                           String value, SB errorLog,
                                           boolean obscure) {
    switch (("##TITLE###" + "##JCAMPDX#" + "##ORIGIN##" + "##OWNER###"
        + "##DATATYPE" + "##LONGDATE" + "##DATE####" + "##TIME####")
            .indexOf(label + "#")) {
    case 0:
      jdxHeader.setTitle(
          obscure || value == null || value.equals("") ? "Unknown" : value);
      return true;
    case 10:
      jdxHeader.jcampdx = value;
      float version = PT.parseFloat(value);
      if (version >= 6.0 || Float.isNaN(version)) {
        if (errorLog != null)
          errorLog
              .append("Warning: JCAMP-DX version may not be fully supported: "
                  + value + "\n");
      }
      return true;
    case 20:
      jdxHeader.origin = (value != null && !value.equals("") ? value
          : "Unknown");
      return true;
    case 30:
      jdxHeader.owner = (value != null && !value.equals("") ? value
          : "Unknown");
      return true;
    case 40:
      jdxHeader.dataType = value;
      return true;
    case 50:
      jdxHeader.longDate = value;
      return true;
    case 60:
      jdxHeader.date = value;
      return true;
    case 70:
      jdxHeader.time = value;
      return true;
    }
    return false;
  }

  /**
   * Set any vendor-specific values. For example, JEOL implementation must
   * ignore ##SHIFTREFERENCE
   * 
   * @param rawLine
   */
  protected void setVenderSpecificValues(String rawLine) {
    if (rawLine.indexOf("JEOL") >= 0) {
      System.out.println("Skipping ##SHIFTREFERENCE for JEOL " + rawLine);
      ignoreShiftReference = true;
    }
  }

  protected String getValue(String label) {
    String value = (isTabularDataLabel(label) ? "" : t.getValue());
    return ("##END".equals(label) ? null : value);
  }

  protected void setTabularDataType(GenericJDXDataObject spectrum, String label) {
    if (label.equals("##PEAKASSIGNMENTS"))
      spectrum.setDataClass("PEAKASSIGNMENTS");
    else if (label.equals("##PEAKTABLE"))
      spectrum.setDataClass("PEAKTABLE");
    else if (label.equals("##XYDATA"))
      spectrum.setDataClass("XYDATA");
    else if (label.equals("##XYPOINTS"))
      spectrum.setDataClass("XYPOINTS");
    //    try {
    //      t.readLineTrimmed();
    //    } catch (IOException e) {
    //      e.printStackTrace();
    //    }
  }

  /**
   * reads BLOCK data
   * 
   * @param sourceLDRTable
   * @return source
   * @throws JSVException
   */
  protected JDXSource getBlockSpectra(Lst<String[]> sourceLDRTable)
      throws JSVException {

    Logger.debug("--JDX block start--");
    String label = "";
    String value = null;
    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    boolean forceSub = false;
    while ((label = t.getLabel()) != null && !label.equals("##TITLE")) {
      value = getValue(label);
      if (isNew && !readHeaderLabel(source, label, value, errorLog, obscure))
        addHeader(sourceLDRTable, t.rawLabel, value);
      if (label.equals("##BLOCKS")) {
        int nBlocks = PT.parseInt(value);
        if (nBlocks > 100 && firstSpec <= 0)
          forceSub = true;
      }
    }
    value = getValue(label);
    // If ##TITLE not found throw Exception
    if (!"##TITLE".equals(label))
      throw new JSVException("Unable to read block source");
    if (isNew)
      source.setHeaderTable(sourceLDRTable);
    source.type = JDXSource.TYPE_BLOCK;
    source.isCompoundSource = true;
    Lst<String[]> dataLDRTable;
    GenericJDXDataObject spectrum = newSpectrum();
    dataLDRTable = new Lst<String[]>();
    readDataLabel(spectrum, label, value, errorLog, obscure);
    try {
      String tmp;
      while ((tmp = t.getLabel()) != null) {
        if ((value = getValue(tmp)) == null && "##END".equals(label)) {
          Logger.debug("##END= " + t.getValue());
          break;
        }
        label = tmp;
        if (isTabularData) {
          setTabularDataType(spectrum, label);
          if (!processTabularData(spectrum, dataLDRTable))
            throw new JSVException("Unable to read Block Source");
          continue;
        }
        if (label.equals("##DATATYPE") && value.toUpperCase().equals("LINK")) {
          // embedded LINK
          getBlockSpectra(dataLDRTable);
          spectrum = null;
          label = null;
        } else if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
          getNTupleSpectra(dataLDRTable, spectrum, label);
          spectrum = null;
          label = "";
        }
        if (done)
          break;
        if (spectrum == null) {
          spectrum = newSpectrum();
          dataLDRTable = new Lst<String[]>();
          if (label == "")
            continue;
          if (label == null) {
            label = "##END";
            continue;
          }
        }
        if (value == null) {
          // ##END -- Process Block

          if (spectrum.getXYCoords().length > 0
              && !addSpectrum(spectrum, forceSub))
            return source;
          spectrum = newSpectrum();
          dataLDRTable = new Lst<String[]>();
          continue;
        }
        if (readDataLabel(spectrum, label, value, errorLog, obscure))
          continue;

        addHeader(dataLDRTable, t.rawLabel, value);
        if (checkCustomTags(spectrum, label, value))
          continue;
      } // End Source File
    } catch (Exception e) {
      throw new JSVException(e.getMessage());
    }
    addErrorLogSeparator();
    source.setErrorLog(errorLog.toString());
    Logger.debug("--JDX block end--");
    return source;
  }

  /**
   * reads NTUPLE data
   * 
   * @param sourceLDRTable
   * @param spectrum0
   * @param label
   * 
   * @throws JSVException
   * @return source
   */
  @SuppressWarnings("null")
  protected JDXSource getNTupleSpectra(Lst<String[]> sourceLDRTable,
                                       GenericJDXDataObject spectrum0, String label)
      throws JSVException {
    double[] minMaxY = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };
    blockID = Math.random();
    boolean isOK = true;//(spectrum0.is1D() || firstSpec > 0);
    if (firstSpec > 0)
      spectrum0.numDim = 1; // don't display in 2D if only loading some spectra

    boolean isVARNAME = label.equals("##VARNAME");
    if (!isVARNAME) {
      label = "";
    }
    Map<String, Lst<String>> nTupleTable = new Hashtable<String, Lst<String>>();
    String[] plotSymbols = new String[2];

    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    if (isNew) {
      source.type = JDXSource.TYPE_NTUPLE;
      source.isCompoundSource = true;
      source.setHeaderTable(sourceLDRTable);
    }

    // Read NTuple Table
    while (!(label = (isVARNAME ? label : t.getLabel())).equals("##PAGE")) {
      isVARNAME = false;
      StringTokenizer st = new StringTokenizer(t.getValue(), ",");
      Lst<String> attrList = new Lst<String>();
      while (st.hasMoreTokens())
        attrList.addLast(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    } //Finished With Page Data
    Lst<String> symbols = nTupleTable.get("##SYMBOL");
    if (!label.equals("##PAGE"))
      throw new JSVException("Error Reading NTuple Source");
    String page = t.getValue();
    /*
     * 7.3.1 ##PAGE= (STRING).
    This LDR indicates the start of a PAGE which contains tabular data. It may have no
    argument, or it may be omitted when the data consists of one PAGE. When the Data Table
    represents a property like a spectrum or a particular fraction, or at a particular time, or at a
    specific location in two or three dimensional space, the appropriate PAGE VARIABLE
    values will be given as arguments of the ##PAGE= LDR, as in the following examples:
    ##PAGE= N=l $$ Spectrum of first fraction of GCIR run
    ##PAGE= T=10:21 $$ Spectrum of product stream at time: 10:21
    ##PAGE= X=5.2, Y=7.23 $$ Spectrum of known containing 5.2 % X and 7.23% Y
     */

    GenericJDXDataObject spectrum = null;
    boolean isFirst = true;
    while (!done) {
      if ((label = t.getLabel()).equals("##ENDNTUPLES")) {
        t.getValue();
        break;
      }

      if (label.equals("##PAGE")) {
        page = t.getValue();
        continue;
      }

      // Create and add Spectra
      if (spectrum == null) {
        spectrum = newSpectrum();
        spectrum0.copyTo(spectrum);
        spectrum.setTitle(spectrum0.getTitle());
        if (!spectrum.is1D()) {
          int pt = page.indexOf('=');
          if (pt >= 0)
            try {
              spectrum
                  .setY2D(Double.parseDouble(page.substring(pt + 1).trim()));
              String y2dUnits = page.substring(0, pt).trim();
              int i = symbols.indexOf(y2dUnits);
              if (i >= 0)
                spectrum.setY2DUnits(nTupleTable.get("##UNITS").get(i));
            } catch (Exception e) {
              //we tried.            
            }
        }
      }

      Lst<String[]> dataLDRTable = new Lst<String[]>();
      spectrum.setHeaderTable(dataLDRTable);

      while (!label.equals("##DATATABLE")) {
        addHeader(dataLDRTable, t.rawLabel, t.getValue());
        label = t.getLabel();
      }

      boolean continuous = true;
      String line = t.flushLine();
      if (line.trim().indexOf("PEAKS") > 0)
        continuous = false;

      // parse variable list
      int index1 = line.indexOf('(');
      int index2 = line.lastIndexOf(')');
      if (index1 == -1 || index2 == -1)
        throw new JSVException("Variable List not Found");
      String varList = line.substring(index1, index2 + 1);

      int countSyms = 0;
      for (int i = 0; i < symbols.size(); i++) {
        String sym = symbols.get(i).trim();
        if (varList.indexOf(sym) != -1) {
          plotSymbols[countSyms++] = sym;
        }
        if (countSyms == 2)
          break;
      }

      setTabularDataType(spectrum,
          "##" + (continuous ? "XYDATA" : "PEAKTABLE"));

      if (!readNTUPLECoords(spectrum, nTupleTable, plotSymbols, minMaxY))
        throw new JSVException("Unable to read Ntuple Source");
      if (!spectrum.nucleusX.equals("?"))
        spectrum0.nucleusX = spectrum.nucleusX;
      spectrum0.nucleusY = spectrum.nucleusY;
      spectrum0.freq2dX = spectrum.freq2dX;
      spectrum0.freq2dY = spectrum.freq2dY;
      spectrum0.y2DUnits = spectrum.y2DUnits;
      for (int i = 0; i < sourceLDRTable.size(); i++) {
        String[] entry = sourceLDRTable.get(i);
        String key = JDXSourceStreamTokenizer.cleanLabel(entry[0]);
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          dataLDRTable.addLast(entry);
      }
      if (isOK)
        addSpectrum(spectrum, !isFirst);
      isFirst = false;
      spectrum = null;
    }
    addErrorLogSeparator();
    source.setErrorLog(errorLog.toString());
    Logger.info("NTUPLE MIN/MAX Y = " + minMaxY[0] + " " + minMaxY[1]);
    return source;
  }

  protected boolean processTabularData(GenericJDXDataObject spec, Lst<String[]> table)
      throws JSVException {
    spec.setHeaderTable(table);

    if (spec.dataClass.equals("XYDATA")) {
      spec.checkRequiredTokens();
      decompressData(spec, null, false);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE")
        || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      // check if there is an x and y factor
      try {
        t.readLineTrimmed();
      } catch (IOException e) {
        // ignore
      }
      Coordinate[] xyCoords;

      if (spec.xFactor != GenericJDXDataObject.ERROR
          && spec.yFactor != GenericJDXDataObject.ERROR)
        xyCoords = Coordinate.parseDSV(t.getValue(), spec.xFactor,
            spec.yFactor);
      else
        xyCoords = Coordinate.parseDSV(t.getValue(), 1, 1);
      spec.setXYCoords(xyCoords);
      double fileDeltaX = Coordinate.deltaX(
          xyCoords[xyCoords.length - 1].getXVal(), xyCoords[0].getXVal(),
          xyCoords.length);
      spec.setIncreasing(fileDeltaX > 0);
      return true;
    }
    return false;
  }

  protected boolean readNTUPLECoords(GenericJDXDataObject spec,
                                     Map<String, Lst<String>> nTupleTable,
                                     String[] plotSymbols, double[] minMaxY) {
    Lst<String> list;
    if (spec.dataClass.equals("XYDATA")) {
      // Get Label Values

      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##VARNAME");
      spec.varName = list.get(index2).toUpperCase();

      list = nTupleTable.get("##FACTOR");
      spec.xFactor = Double.parseDouble(list.get(index1));
      spec.yFactor = Double.parseDouble(list.get(index2));

      list = nTupleTable.get("##LAST");
      spec.fileLastX = Double.parseDouble(list.get(index1));

      list = nTupleTable.get("##FIRST");
      spec.fileFirstX = Double.parseDouble(list.get(index1));
      //firstY = Double.parseDouble((String)list.get(index2));

      list = nTupleTable.get("##VARDIM");
      spec.nPointsFile = Integer.parseInt(list.get(index1));

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));

      if (spec.nucleusX == null
          && (list = nTupleTable.get("##.NUCLEUS")) != null) {
        spec.setNucleusAndFreq(list.get(0), false);
        spec.setNucleusAndFreq(list.get(index1), true);
      } else {
        if (spec.nucleusX == null)
          spec.nucleusX = "?";
      }

      decompressData(spec, minMaxY, true);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE")
        || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));
      spec.setXYCoords(
          Coordinate.parseDSV(t.getValue(), spec.xFactor, spec.yFactor));
      return true;
    }
    return false;
  }

  protected void decompressData(GenericJDXDataObject spec, double[] minMaxY, boolean isNTUPLE) {

    int errPt = errorLog.length();
    double fileDeltaX = Coordinate.deltaX(spec.fileLastX, spec.fileFirstX,
        spec.nPointsFile);
    spec.setIncreasing(fileDeltaX > 0);
    spec.setContinuous(true);
    JDXDecompressor decompressor = new JDXDecompressor(t, spec.fileFirstX,
        spec.xFactor, spec.yFactor, fileDeltaX, spec.nPointsFile, isNTUPLE);

    double[] firstLastX = new double[2];
    long t = System.currentTimeMillis();
    Coordinate[] xyCoords = decompressor.decompressData(errorLog, firstLastX);
    if (Logger.debugging)
      Logger.debug("decompression time = " + (System.currentTimeMillis() - t) + " ms");
    spec.setXYCoords(xyCoords);
    double d = decompressor.getMinY();
    if (minMaxY != null) {
      if (d < minMaxY[0])
        minMaxY[0] = d;
      d = decompressor.getMaxY();
      if (d > minMaxY[1])
        minMaxY[1] = d;
    }
    double freq = (Double.isNaN(spec.freq2dX) ? spec.observedFreq
        : spec.freq2dX);
    // apply offset
    boolean isHz = freq != GenericJDXDataObject.ERROR && spec.getXUnits().toUpperCase().equals("HZ");
    if (spec.offset != GenericJDXDataObject.ERROR && freq != GenericJDXDataObject.ERROR
        && spec.dataType.toUpperCase().contains("SPECTRUM")
        && spec.jcampdx.indexOf("JEOL") < 0 // BH 2020.09.16 J Muzyka Centre College
        ) {
      Coordinate
          .applyShiftReference(xyCoords, spec.dataPointNum, spec.fileFirstX,
              spec.fileLastX, spec.offset, isHz ? freq : 1, spec.shiftRefType);
    }

    if (isHz) {
      Coordinate.applyScale(xyCoords, (1.0 / freq), 1);
      spec.setXUnits("PPM");
      spec.setHZtoPPM(true);
    }
    if (errorLog.length() != errPt) {
      errorLog.append(spec.getTitle()).append("\n");
      errorLog.append("firstX: " + spec.fileFirstX + " Found " + firstLastX[0]
          + "\n");
      errorLog.append("lastX from Header " + spec.fileLastX + " Found "
          + firstLastX[1] + "\n");
      errorLog.append("deltaX from Header " + fileDeltaX + "\n");
      errorLog.append("Number of points in Header " + spec.nPointsFile
          + " Found " + xyCoords.length + "\n");
    } else {
      //errorLog.append("No Errors decompressing data\n");
    }

    if (Logger.debugging) {
      System.err.println(errorLog.toString());
    }

  }

  public static void addHeader(Lst<String[]> table, String label,
                               String value) {
    String[] entry;
    for (int i = 0; i < table.size(); i++)
      if ((entry = table.get(i))[0].equals(label)) {
        entry[1] = value;
        return;
      }
    table.addLast(new String[] { label, value,
        JDXSourceStreamTokenizer.cleanLabel(label) });
  }

  protected void addErrorLogSeparator() {
    if (errorLog.length() > 0
        && errorLog.lastIndexOf(ERROR_SEPARATOR) != errorLog.length()
            - ERROR_SEPARATOR.length())
      errorLog.append(ERROR_SEPARATOR);
  }

  abstract protected boolean checkCustomTags(GenericJDXDataObject spectrum, String label,
                                             String value)
      throws JSVException;

  abstract protected boolean addSpectrum(GenericJDXDataObject spectrum, boolean forceSub);

  protected GenericJDXDataObject newSpectrum() {
    return new GenericJDXDataObject();
  }


}
