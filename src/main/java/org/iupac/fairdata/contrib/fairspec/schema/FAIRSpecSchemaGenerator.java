package org.iupac.fairdata.contrib.fairspec.schema;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

/**
 * The FAIRSpeSchemaGenerator class runs independently of the Extractor and 
 * creates a fairspec.findingaid.schema.json from a template file.
 * 
 * see src/main/resources/org/iupac/fairdata/contrib/fairspec/schema/fairspecSchemaTemplate.json
 * 
 * @author hansonr
 *
 */
public class FAIRSpecSchemaGenerator {

	private static final int COMMENT = -1;
	private static final int IGNORE = 0;
	private static final int VALUE = 1;
	private static final int PROPERTY = 2;
	private static final int REPRESENTATION = 3;
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
		File f = new File(schemaOutputDir, "fairspec.findingaid.schema.json");
		FAIRSpecUtilities.putToFile(schema.getBytes(), f);
		
		System.out.println("done " + f.getAbsolutePath());

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
			case COMMENT:
				map = null;
				break;
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
			switch (type) {
			case COMMENT:
				// "_$comment$_" : "string",
				// remove ENTIRE LINE
				pt = schemaTemplate.lastIndexOf('\n', pt);
				pt1 = schemaTemplate.indexOf("\n", pt1);
				break;
			case PROPERTY:
			case REPRESENTATION:
				// "_$IFD.property...$_" : {"type":"string"}
				// "_$IFD.representation...$_" : {"type":"string"}
				pt1 = schemaTemplate.indexOf("}", pt1) + 1;
				pt--; // "
				break;
			case VALUE:
				// ..._$XXXX$_...
				pt1 += 2;
				break;
			}			
			schemaTemplate = schemaTemplate.substring(0, pt) + s + schemaTemplate.substring(pt1);
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
		return (var.equals("comment") ? COMMENT : var.indexOf(".property.") >= 0 ? PROPERTY 
				: var.indexOf(".representation.") >= 0 ? REPRESENTATION : VALUE);
	}

	private String fillValue(String schemaVariable, int type, Map<String, String> map) {
		String s = "";
		if (map == null)
			return s;
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
		if (type == VALUE) {
		   throw new RuntimeException("value was not found :" + schemaVariable);
		}
		return (s.length() == 0 ? "" : "\n" + s.substring(0, s.length() - 2) + "\n");
	}

	private String addProperty(String var, String val, String subtype) {
		int ptKey = val.indexOf(var);
		int ptSchema = val.indexOf("#|");
		String key = trim(val);
		// property.dataobject.fairspec
		// IFD.property.dataobject.fairspec.nmr.expt_dimension
		// pick up also the "." after the find
		key = key.substring(ptKey + var.length() + 1);
		if (subtype != null && key.startsWith(subtype))
			key = key.substring(subtype.length() + 1);
		String schema = (ptSchema > 0 ? val.substring(ptSchema + 2).trim() : TYPE_STRING);
		return "\"" + key + "\":{" + schema + "}";
	}

	private static String trim(String val) {
		int pt = val.indexOf("#");
		return (pt < 0 ? val : val.substring(0, pt)).trim();
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
		
		int t = (Integer.MAX_VALUE);
		System.out.println(Instant.ofEpochSecond(t));
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
