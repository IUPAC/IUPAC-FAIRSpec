package org.iupac.fairdata.contrib.fairspec.schema;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDConst.PROPERTY_TYPE;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.util.JSJSONParser;

import javajs.util.Rdr;

/**
 * A class to contain various generally useful utility methods in association
 * with the extraction of data and metadata, and serialization of FAIRSpec
 * Finding Aids
 * 
 * @author hansonr
 *
 */
public class FAIRSpecSchemaGenerator {

	private static final int IGNORE = -1;
	private static final int VALUE = 0;
	private static final int PROPERTY = 1;
	private static final int REPRESENTATION = 2;
	private static final String TYPE_STRING = "\"type\" : \"string\"";

	private static boolean debugging = true;

	
	Map<String, String> mapProps;
	Map<String, String> mapReps;
	Map<String, String> mapValues;

	private FAIRSpecSchemaGenerator(String schemaTemplateFile, String schemaOutputDir) throws Exception {
		String schemaTemplate;
		if (schemaTemplateFile == null)
			schemaTemplate = FAIRSpecUtilities.getResource(getClass(), "fairspecSchemaTemplate.json");
		else
			schemaTemplate = FAIRSpecUtilities.getFileStringData(new File(schemaTemplateFile));
		if (schemaOutputDir == null)
			throw new RuntimeException("schemaOutputDir must not be null");	
		new File(schemaOutputDir + "/").mkdirs();
		String schema = generateSchema(schemaTemplate);
		FAIRSpecUtilities.putToFile(schema.getBytes(), new File(schemaOutputDir, "fairspec.findingaid.schema.json"));
	}
	
	private String generateSchema(String schemaTemplate) {
		FAIRSpecFindingAid.loadProperties();
		getMaps();
		int pt;
		while ((pt = schemaTemplate.indexOf("_$")) >= 0) {
			int pt1 = schemaTemplate.indexOf("$_", pt + 2);
			String schemaVariable = schemaTemplate.substring(pt + 2, pt1);
			Map<String, String> map;
			int type = getVariableType(schemaVariable);
			switch (type) {
			case PROPERTY:
				map = mapProps;
				break;
			case REPRESENTATION:
				map = mapReps;
				break;
			case VALUE:
				map = mapValues;
				break;
			case IGNORE:
			default:
				throw new RuntimeException("Unknown schemaVariable  " + schemaVariable);
			}	
			String s = fillValue(schemaVariable, type, map);
			System.out.println(s);
			switch (type) {
			case PROPERTY:
			case REPRESENTATION:
				// "_$XXXX$_"
				pt1 = schemaTemplate.indexOf("}", pt1) + 1;
				schemaTemplate = schemaTemplate.substring(0, pt - 1) + s + schemaTemplate.substring(pt1);
				break;
			case VALUE:
				// ..._$XXXX$_...
				schemaTemplate = schemaTemplate.substring(0, pt) + s + schemaTemplate.substring(pt1 + 2);
				break;
			}			
		}
		return schemaTemplate;
	}

	private void getMaps() {
		Properties props = IFDConst.getAllProperties();
		mapProps = new TreeMap<>();
		mapReps = new TreeMap<>();
		mapValues = new TreeMap<>();
		for (Entry<Object, Object> p : props.entrySet()) {
		  String key = p.getKey().toString();
		  String val = p.getValue().toString();
		  switch (getJavaPropertyType(key, val)) {
		  default:
		  case IGNORE:
			  break;
		  case PROPERTY:
			  if (IFDConst.IFD_OBJECT_FIELDS.indexOf(";" + val + ";") < 0)
				  mapProps.put(key, val);
			  break;
		  case REPRESENTATION:
			  mapReps.put(key, val);
			  break;
		  case VALUE:
			  mapValues.put(key, val);
			  break;
		  }
		}
		if (debugging) {
			dumpMap(mapProps, PROPERTY);
			dumpMap(mapReps, REPRESENTATION);
			dumpMap(mapValues, VALUE);
		}
	}

	private void dumpMap(Map<String, String> map, int type) {
		String pre = (type == PROPERTY ? "P " : type == REPRESENTATION ? "R " : "V ");
		for (Entry<String, String> e : map.entrySet()) {
			System.out.println(pre + " " + e.getKey() + " = " + e.getValue());
		}		
	}
	
	private static int getJavaPropertyType(String key, String val) {
		boolean isFlag = (val.startsWith(".") || val.endsWith("."));
		if (isFlag || key.indexOf("_EXTRACTOR_") >= 0)
			return IGNORE;
		if (key.indexOf("_PROPERTY_") >= 0)
			return PROPERTY;
		if (key.indexOf("_REP_") >= 0)
			return REPRESENTATION;
		return VALUE;
	}

	private static int getVariableType(String var) {
		return (var.indexOf(".property.") >= 0 ? PROPERTY 
				: var.indexOf(".representation.") >= 0 ? REPRESENTATION : VALUE);
	}

	private String fillValue(String schemaVariable, int type, Map<String, String> map) {
		String s = "";
		int ptSubtype = schemaVariable.indexOf('/');
		String subtype = (ptSubtype < 0 ? null : schemaVariable.substring(ptSubtype + 1));
		if (ptSubtype > 0) {
			schemaVariable = schemaVariable.substring(0, ptSubtype);
		}
		for (Entry<String, String> e : map.entrySet()) {
			String pval = e.getValue();
			String pkey = (type == VALUE ? e.getKey() : pval);
			if (pkey.indexOf(schemaVariable) >= 0) {
				switch (type) {
				case PROPERTY:
					s += addProperty(schemaVariable, pval, subtype) + ",\n";
					break;
				case REPRESENTATION:
					s += addRep(schemaVariable, pval, subtype) + ",\n";
					break;
				case VALUE:
					return e.getValue();
				}
			}
		}
		return (s.length() == 0 ? "" : "\n" + s.substring(0, s.length() - 2) + "\n");
	}

	private String addProperty(String var, String val, String subtype) {
		int ptKey = val.indexOf(var);
		int ptSchema = val.indexOf("|");
		String key = (ptSchema < 0 ? val : val.substring(0, ptSchema));
		// property.dataobject.fairspec
		// IFD.property.dataobject.fairspec.nmr.expt_dimension
		// pick up also the "." after the find
		key = key.substring(ptKey + var.length() + 1);
		if (subtype != null && key.startsWith(subtype))
			key = key.substring(subtype.length() + 1);
		String schema = (ptSchema > 0 ? val.substring(ptSchema + 1) : TYPE_STRING);
		return "\"" + key + "\":{" + schema + "}";
	}

	private String addRep(String var, String val, String subtype) {
		// val: IFD.representation.dataobject.fairspec.ir.spectrum_peaklist
		var = var.substring(4); // remove rep.
		int ptSchema = val.indexOf("|");
		String key = val.substring(0, ptSchema);
		int ptKey = key.indexOf(var);
		// IFD.property.dataobject.fairspec
		key = key.substring(ptKey + var.length() + 1);
		if (subtype != null && key.startsWith(subtype))
			key = key.substring(subtype.length() + 1);
		String schema = (ptSchema > 0 ? val.substring(ptSchema + 1) : TYPE_STRING);		
		return "\"" + key + "\":{" + schema + "}";
	}
	
	public final static void main(String[] args) {
		// this is the way to convert back and forth from long to date:
		//System.out.println(Instant.ofEpochSecond(1717948362));
		//System.out.println(Instant.ofEpochSecond(OffsetDateTime.parse("2024-06-09T23:52:42+08:00").toInstant().toEpochMilli()/1000));
		//System.out.println(Instant.parse("2024-06-09T23:52:42Z").toEpochMilli()/1000);
		String schemaTemplateFile = null;
		String schemaOutputDir = null;
		try {
			switch (args.length) {
			case 2:
				schemaTemplateFile = args[1];
				break;
			case 1:
				schemaOutputDir = args[0];
				break;
			case 0:
				break;
		    default:
		    	throw new RuntimeException("too many arguments");
			}
			if (schemaTemplateFile == null && schemaOutputDir == null) {
				if (!debugging) {
					System.out.println("syntax: FAIRSpecSchemaGenerator.jar <outputDir> (schemaTemplateFile)");
					return;
				}
			}
			if (schemaOutputDir == null)
				schemaOutputDir = "c:/temp/schema/";
			new FAIRSpecSchemaGenerator(schemaTemplateFile, schemaOutputDir);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("failed");
		}
	}
}
