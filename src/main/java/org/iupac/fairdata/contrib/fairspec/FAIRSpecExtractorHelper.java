package org.iupac.fairdata.contrib.fairspec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDCollectionSet;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.core.IFDResource;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.derived.IFDSampleDataAssociationCollection;
import org.iupac.fairdata.derived.IFDSampleStructureAssociation;
import org.iupac.fairdata.derived.IFDSampleStructureAssociationCollection;
import org.iupac.fairdata.derived.IFDStructureDataAnalysisCollection;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.extract.ExtractorI;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

/**
 * 
 * This class is tailored to the task of creating an IUPAC FAIRData Collection
 * and its associated IUPAC FAIRData Finding Aid from zipped data aggregations,
 * particularly from supporting information packages.
 * 
 * It is not a fully automated system -- the starting point is an "IUPAC
 * FAIRSpec Data and Metadata Extraction Template" (see the extract/ folder)
 * 
 * This class is best instantiated by third-party extractors. (see
 * com.integratedgraphics.ifd.Extractor and ExtractorTest)
 * 
 * 
 * It is currently under development and should NOT be considered to be a an
 * IUPAC standard.
 * 
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class FAIRSpecExtractorHelper implements FAIRSpecExtractorHelperI {

	static {
		FAIRSpecFindingAid.loadProperties();
	}

	// these values are in fairspec.properties
	public static final String FAIRSPEC_EXTRACTOR_FLAG = IFDConst.getProp("FAIRSPEC_EXTRACTOR_FLAG");
	public static final String FAIRSPEC_EXTRACTOR_OBJECT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OBJECT");
	public static final String FAIRSPEC_EXTRACTOR_ASSIGN = IFDConst.getProp("FAIRSPEC_EXTRACTOR_ASSIGN");
	public static final String FAIRSPEC_EXTRACTOR_REJECT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REJECT");
	public static final String FAIRSPEC_EXTRACTOR_IGNORE = IFDConst.getProp("FAIRSPEC_EXTRACTOR_IGNORE");
	public static final String FAIRSPEC_EXTRACTOR_OPTION_FLAG = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OPTION_FLAG");
	public static final String FAIRSPEC_EXTRACTOR_OPTIONS = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OPTIONS");

	public interface ClassTypes {

		// right now these are (unfortunately) saved as literals, because they are
		// used in switch cases.

		public final static String Sample = "org.iupac.fairdata.sample.IFDSample";
		public final static String SampleCollection = "org.iupac.fairdata.sample.IFDSampleCollection";

		public final static String Structure = "org.iupac.fairdata.structure.IFDStructure";
		public final static String StructureCollection = "org.iupac.fairdata.structure.IFDStructureCollection";

		public final static String DataObject = "org.iupac.fairdata.dataobject.IFDDataObject";
		public final static String DataObjectCollection = "org.iupac.fairdata.dataobject.IFDDataObjectCollection";

		public final static String SampleDataAssociation = "org.iupac.fairdata.derived.IFDSampleDataAssociation";
		public final static String SampleDataAssociationCollection = "org.iupac.fairdata.derived.IFDSampleDataAssociationCollection";

//		public final static String StructureDataAssociation = "org.iupac.fairdata.derived.IFDStructureDataAssociation";
//		public final static String StructureDataAssociationCollection = "org.iupac.fairdata.derived.IFDStructureDataAssociationCollection";
//
		public final static String SampleDataAnalysis = "org.iupac.fairdata.derived.IFDSampleDataAnalysis";
		public final static String SampleDataAnalysisCollection = "org.iupac.fairdata.derived.IFDSampleDataAnalysisCollection";

		public final static String StructureDataAnalysis = "org.iupac.fairdata.derived.IFDStructureDataAnalysis";
		public final static String StructureDataAnalysisCollection = "org.iupac.fairdata.derived.IFDStructureDataAnalysisCollection";

		public final static String Compound = "org.iupac.fairdata.contrib.fairspec.FAIRSpecCompound";
		public final static String CompoundCollection = "org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundCollection";
		
	}

	/**
	 * A static class that holds a list allows minimal access to an ArrayList and
	 * can report if it will accept or not accept a file based on its name. It can be serialized as a JSON object.
	 * 
	 * @author hansonr
	 *
	 */
	public static class FileList {
		private String rootPath;
		private final String name;
		private final List<String> files = new ArrayList<>();
		private Pattern acceptPattern;
		private long byteCount;
	
		public FileList(String rootPath, String name) {
			this.name = name;
			this.rootPath = rootPath;
		}
	
		public int size() {
			return files.size();
		}
	
		public String serialize(StringBuffer sb) {
			String[] list = files.toArray(new String[files.size()]);
			Arrays.sort(list);
			boolean returnString = (sb == null);
			if (returnString)
				sb = new StringBuffer();
			String sep = "";
			for (int i = 0; i < list.length; i++) {
				String fname = list[i];
				sb.append((sep + "\"" + rootPath + "/" + fname + "\""));
				sep = ",\n";
			}
			sb.append("\n");
			return (returnString ? sb.toString() : null);
		}
	
		public boolean contains(String fileName) {
			return files.contains(fileName);
		}
	
		public void add(String fileName, long len) {
			files.add(fileName);
			byteCount += len;
		}
	
		public void remove(String localizedName, long len) {
			files.remove(localizedName);
			byteCount -= len;
		}
		
		public long getByteCount() {
			return byteCount;
		}
	
		public boolean accept(String fileName) {
			return (acceptPattern != null && acceptPattern.matcher(fileName).find());
		}
	
		public void setAcceptPattern(String pattern) {
			acceptPattern = Pattern.compile(pattern);
		}
	
		public String getName() {
			return name;
		}
	
		public static int getListCount(List<FileList> lists, String name) {
			int n = 0;
			for (int i = lists.size(); --i >= 0;) {
				FileList list = lists.get(i);
				if (list.getName().equals(name))
					n += list.size();
			}
			return n;
		}
	
		public static long getByteCount(List<FileList> lists, String name) {
		    long n = 0;
			for (int i = lists.size(); --i >= 0;) {
				FileList list = lists.get(i);
				if (list.getName().equals(name))
					n += list.getByteCount();
			}
			return n;
		}
	
		@Override
		public String toString() {
			return serialize(null);
		}
	
	}

	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REJECT_PATTERN");

	/**
	 * the files we want extracted -- just PDF and PNG here; all others are taken
	 * care of by individual IFDVendorPluginI classes
	 */
	public static final String defaultCachePattern = "" + "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
	;

	public final static int SAMPLE_COLLECTION = 0;
	public final static int STRUCTURE_COLLECTION = 1;
	public final static int DATA_COLLECTION = 2;

	public final static int SAMPLE_STRUCTURE_COLLECTION = 0;
	public final static int SAMPLE_DATA_COLLECTION = 1;
	public final static int STRUCTURE_DATA_COLLECTION = 2;
	public final static int SAMPLE_DATA_ANALYSIS_COLLECTION = 3;
	public final static int STRUCTURE_DATA_ANALYSIS_COLLECTION = 4;

	public static final String FAIRSPEC_EXTRACT_VERSION = IFDConst.getProp("FAIRSPEC_EXTRACT_VERSION");
	public static final String DATAOBJECT_FAIRSPEC_FLAG = IFDConst.getProp("DATAOBJECT_FAIRSPEC_FLAG");

	public static final String IFD_PROPERTY_SAMPLE_LABEL = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_SAMPLE_FLAG, IFDConst.IFD_LABEL_FLAG);
	public static final String IFD_PROPERTY_SAMPLE_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_SAMPLE_FLAG, IFDConst.IFD_ID_FLAG);

	private static final String IFD_PROPERTY_STRUCTURE_LABEL = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_STRUCTURE_FLAG, IFDConst.IFD_LABEL_FLAG);
	public static final String IFD_PROPERTY_STRUCTURE_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_STRUCTURE_FLAG, IFDConst.IFD_ID_FLAG);
	public static final String IFD_PROPERTY_DATAOBJECT_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_DATAOBJECT_FLAG, IFDConst.IFD_ID_FLAG);

	protected final FAIRSpecFindingAid findingAid;

	protected IFDStructureCollection structureCollection;
	protected IFDDataObjectCollection dataObjectCollection;
	protected FAIRSpecCompoundCollection compoundCollection;
	protected IFDStructureDataAnalysisCollection structureDataAnalysisCollection;
	protected IFDSampleCollection sampleCollection;
	protected IFDSampleDataAssociationCollection sampleDataCollection;
	protected IFDSampleStructureAssociationCollection sampleStructureCollection;

	/**
	 * Set associations to be listed by ID, not by index.
	 */
	protected boolean byId;

	@Override
	public void setById(boolean tf) {
		byId = tf;
	}

	/**
	 * current state of extraction
	 * 
	 */
	protected String currentOriginPath;
	protected IFDStructure currentStructure;
	protected IFDDataObject currentDataObject;
	protected IFDAssociation currentAssociation;
	protected IFDSample currentSample;
	protected IFDResource currentResource;
	protected List<Object[]> currentDataProps;

	/**
	 * temporary holding arrays separating objects from associations so that they
	 * can be added later to the finding aid in a desired order.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] objects = new IFDCollection[3];
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] collections = new IFDCollection[5];
	private ExtractorI extractor;

	public FAIRSpecExtractorHelper(ExtractorI extractor, String creator) throws IFDException {
		if (extractor == null)
			throw new IFDException("FAIRSpecExtractorHelper: extractor cannot be null");
		this.extractor = extractor;
		findingAid = new FAIRSpecFindingAid(null, null, creator);
	}

	@Override
	public FAIRSpecFindingAid getFindingAid() {
		return findingAid;
	}

	/**
	 * start processing the next "line" (zip file entry path)
	 * 
	 * @param originPath
	 */
	@Override
	public void beginAddingObjects(String originPath) {
		if (isAddingObjects())
			endAddingObjects();
		currentOriginPath = originPath;
	}

	public boolean isAddingObjects() {
		return (currentOriginPath != null);
	}

	@Override
	public IFDStructure getCurrentStructure() {
		return currentStructure;
	}

	public void setCurrentStructure(IFDStructure struc) {
		currentStructure = struc;
	}

	public IFDDataObject getCurrentSpecData() {
		return currentDataObject;
	}

	public void setCurrentSpecData(IFDDataObject spec) {
		currentDataObject = spec;
	}

	/**
	 * This list will grow.
	 * 
	 * @param key
	 * @return
	 * @throws IFDException
	 */
	public static String getObjectTypeForPropertyOrRepresentationKey(String key, boolean allowError) throws IFDException {
		if (IFDConst.isProperty(key))
			key = FAIRSpecUtilities.rep(key, IFDConst.IFD_PROPERTY_FLAG, "\0");
		else if (IFDConst.isRepresentation(key))
			key = FAIRSpecUtilities.rep(key, IFDConst.IFD_REPRESENTATION_FLAG, "\0");
		else if (IFDConst.isObject(key))
			key = FAIRSpecUtilities.rep(key, IFDConst.IFD_OBJECT_FLAG, "\0");
		else if (allowError)
			return "Unknown";
		else
			throw new IFDException("bad IFD identifier: " + key);
		if (key.startsWith("\0structure."))
			return ClassTypes.Structure;
		if (key.startsWith("\0sample."))
			return ClassTypes.Sample;
		if (key.startsWith("\0fairspec.compound."))
			return ClassTypes.Compound;
		if (key.startsWith("\0association.sampledata"))
			return ClassTypes.SampleDataAssociation;
		if (key.startsWith("\0analysis.structuredata"))
			return ClassTypes.StructureDataAnalysis;
		if (key.startsWith("\0analysis.sampledata"))
			return ClassTypes.SampleDataAnalysis;
		if (key.startsWith("\0dataobject.fairspec.")) {
			// adds next part, e.g "nmr"
			return key.replace('\0', '.').substring(0, key.indexOf(".", 21));
		}
		if (key.startsWith("\0dataobject."))
			return ClassTypes.DataObject;
		return "Unknown";
	}

	/**
	 * Add the object to the appropriate collection.
	 * 
	 * @param rootPath
	 * @param param
	 * @param value
	 * @param localizedName
	 * @return
	 * @throws IFDException
	 */
	@Override
	public IFDObject<?> addObject(String rootPath, String param, String value, String localizedName, long len)
			throws IFDException {

		if (!isAddingObjects())
			throw new IFDException("addObject " + param + " " + value + " called with no current object file name");

		String type = getObjectTypeForPropertyOrRepresentationKey(param, false);
		if (type.startsWith(DATAOBJECT_FAIRSPEC_FLAG)) {
			if (currentDataObject == null && currentDataProps != null) {
				for (Object[] s : currentDataProps) {
					if (IFDConst.isID((String) s[0])) {
						currentDataObject = (IFDDataObject) getSpecCollection().getObjectByID((String) s[1]);
						break;
					}
				}
			}
			boolean isNew = (currentDataObject == null);
			if (currentDataObject == null) {
				currentDataObject = (IFDDataObject) checkAddNewObject(getSpecCollection(), type, rootPath, param, value,
						localizedName, currentOriginPath, len, true);
			} else {
				checkAddRepOrSetParam(currentDataObject, param, value, localizedName, len);
			}
			if (currentDataProps != null) {
				addProperties((IFDObject<?>) currentDataObject, currentDataProps);
			}
			if (isNew)
				extractor.setNewObjectMetadata(currentDataObject, IFD_PROPERTY_DATAOBJECT_ID);
			if (currentAssociation != null && currentAssociation instanceof FAIRSpecCompoundAssociation) {
				((IFDStructureDataAssociation) currentAssociation).getDataObjectCollection().add(currentDataObject);
			}
			return currentDataObject;
		}
		switch (type) {
		case ClassTypes.Sample:
			currentSample = (IFDSample) checkAddNewObject(getSampleCollection(), type, rootPath, param, value,
					localizedName, null, len, true);
			if (currentAssociation != null && currentAssociation instanceof IFDSampleDataAssociation) {
				((IFDSampleDataAssociation) currentAssociation).getSampleCollection().add(currentSample);
			}
			return currentSample;
		case ClassTypes.Structure:
			if (currentStructure == null) {
				currentStructure = (IFDStructure) checkAddNewObject(getStructureCollection(), type, rootPath, param,
						value, localizedName, null, len, true);
			} else {
				checkAddRepOrSetParam(currentStructure, param, value, localizedName, len);
			}
//			else
//				currentStructure.setPropertyValue(param, value);
			if (currentAssociation != null && currentAssociation instanceof IFDStructureDataAssociation) {
				((IFDStructureDataAssociation) currentAssociation).getStructureCollection().add(currentStructure);
			}
			return currentStructure;
		case ClassTypes.DataObject:
			if (currentDataObject == null) {
				if (IFDConst.isID(param) && this.byId) {
					currentDataObject = (IFDDataObject) checkAddNewObject(getSpecCollection(), type, rootPath, param,
							value, localizedName, currentOriginPath, len, false);
					if (currentDataObject != null)
						return currentDataObject;
				}
				if (currentDataProps == null) {
					currentDataProps = new ArrayList<>();
				}
				currentDataProps.add(new String[] { param, value });
//				System.err.println(
//						"FAIRSpecFindingAidHelper.addObject data object property found before data object initialized "
//								+ param + " " + value + " for " + currentOriginPath);
			} else {
				checkAddRepOrSetParam(currentDataObject, param, value, localizedName, len);
			}
			if (currentAssociation != null) {
				if (currentAssociation instanceof FAIRSpecCompoundAssociation)
					((FAIRSpecCompoundAssociation) currentAssociation).getDataObjectCollection().add(currentDataObject);
				else if (currentAssociation instanceof IFDSampleDataAssociation)
					((IFDSampleDataAssociation) currentAssociation).getDataObjectCollection().add(currentDataObject);
			}
			return currentDataObject;
		case ClassTypes.Compound:
			currentAssociation = getCompoundCollection().getObjectByID(value);
			if (currentAssociation == null)
				currentAssociation = new FAIRSpecCompoundAssociation();
			currentAssociation.setPropertyValue(param, value);
			if (IFDConst.isID(param))
				extractor.setNewObjectMetadata(currentAssociation, param);
			getCompoundCollection().add(currentAssociation);
//				System.out
//						.println("addObject currentAssociation=" + param + "..." + value + "..." + currentAssociation);
			return currentAssociation;
		case ClassTypes.SampleDataAssociation:
			currentAssociation = getSampleDataCollection().getObjectByID(value);
			if (currentAssociation == null) {
				getSampleDataCollection().add(currentAssociation = new FAIRSpecCompoundAssociation());
				currentAssociation.setPropertyValue(param, value);
			}
			return null;
		case ClassTypes.SampleDataAssociationCollection:
		case ClassTypes.CompoundCollection:
		case ClassTypes.StructureDataAnalysisCollection:
		case ClassTypes.StructureDataAnalysis:
			System.out.println("FAIRSpecExtractionHelper.addObject " + type + " not implemented");
			// TODO
			return null;
		case ClassTypes.StructureCollection:
		case ClassTypes.SampleCollection:
			// should not be generic
		default:
			System.err.println("FAIRSpecFindingAidHelper.addObject raw collection -- could not add " + param + " "
					+ value + " for " + currentOriginPath);
			break;
		}
		return null;
	}

	public static void addProperties(IFDObject<?> o, List<Object[]> props) {
		for (Object[] s : props) {
			Object val = s[1];
			if (val != null)
				o.setPropertyValue((String) s[0], val);
		}
	}

	private IFDRepresentableObject<? extends IFDRepresentation> checkAddNewObject(IFDCollection<?> c, String type,
			String rootPath, String param, String value, String localName, String originPath, long len, boolean forceNew) throws IFDException {
		String key;
		boolean isID = IFDConst.isID(param);
		if (isID) {
			key = value;
		} else {
			switch (type) {
			case ClassTypes.SampleDataAssociation:
			case ClassTypes.Compound:
				currentAssociation.setPropertyValue(param, value);
				return null;
			case ClassTypes.Sample:
				key = rootPath + "::" + (originPath == null ? localName : value);
				break;
			case ClassTypes.Structure:
				key = rootPath + "::" + (originPath == null ? localName : value);
				break;
			case ClassTypes.DataObject:
			default:
				key = rootPath + "::" + originPath;
				break;
			}
		}
		@SuppressWarnings("unchecked")
		IFDRepresentableObject<? extends IFDRepresentation> o = (IFDRepresentableObject<? extends IFDRepresentation>) (isID
				? c.getObjectByID(key)
				: c.getPath(key));
		boolean isNew = (o == null);
		if (isNew) {
			if (!forceNew)
				return null;
			switch (type) {
			case ClassTypes.Sample:
				o = new IFDSample();
				o.setPath(rootPath);
				o = ((IFDSampleCollection) c).addWithPath(key, (IFDSample) o);
				break;
			case ClassTypes.Structure:
				o = new IFDStructure();
				o.setPath(rootPath);
				o = ((IFDStructureCollection) c).addWithPath(key, (IFDStructure) o);
				break;
			case ClassTypes.DataObject:
			default:
				o = FAIRSpecDataObject.createFAIRSpecObject(type);
				o.setPath(rootPath);
				o = ((IFDDataObjectCollection) c).addWithPath(key, (IFDDataObject) o);
				break;
			}
			if (o == null)
				throw new IFDException("FAIRSpecExtractorHelper.addNewObject object not found for path=" + rootPath
						+ " and originPath=" + currentOriginPath);
		}
		o.setResource(currentResource);
		checkAddRepOrSetParam(o, param, value, localName, len);
		if (isNew && isID)
			extractor.setNewObjectMetadata(o, param);
		return o;
	}

	private void checkAddRepOrSetParam(IFDRepresentableObject<? extends IFDRepresentation> o, String param,
			String value, String localName, long len) {
		if (IFDConst.isRepresentation(param)) {
			o.findOrAddRepresentation(currentOriginPath, localName, null, param,
					FAIRSpecUtilities.mediaTypeFromFileName(localName)).setLength(len);
		} else {
			o.setPropertyValue(param, value);
		}
	}

	@Override
	public IFDObject<?> endAddingObjects() {
		if (!isAddingObjects())
			return null;
		try {
			if (currentAssociation instanceof IFDStructureDataAssociation) {
				if (currentDataObject != null)
					((IFDStructureDataAssociation) currentAssociation).addDataObject(currentDataObject);
				if (currentStructure != null)
					((IFDStructureDataAssociation) currentAssociation).addStructure(currentStructure);
				return currentAssociation;
			}
			if (currentAssociation instanceof IFDSampleDataAssociation) {
				if (currentDataObject != null)
					((IFDSampleDataAssociation) currentAssociation).addDataObject(currentDataObject);
				if (currentStructure != null)
					((IFDSampleDataAssociation) currentAssociation).addSample(currentSample);
				return currentAssociation;
			}
			if (currentStructure != null && currentDataObject != null) {
				return getCompoundCollection().addAssociation(currentStructure, currentDataObject);
			}
			if (currentSample != null && currentDataObject != null)
				return getSampleDataCollection().addAssociation(currentSample, currentDataObject);
			return (currentStructure != null ? currentStructure
					: currentSample != null ? currentSample : currentDataObject);
		} catch (IFDException e) {
			// not possible
			return null;
		} finally {
			currentOriginPath = null;
			currentStructure = null;
			currentSample = null;
			currentDataObject = null;
			currentAssociation = null;
			currentDataProps = null;
		}
	}

	/**
	 * Remove structures for which there are no data associations. This, of course,
	 * is a completely optional step.
	 * 
	 * @return
	 */
	@Override
	public int removeStructuresWithNoAssociations() {
		List<IFDAssociation> lstRemove = new ArrayList<>();
		IFDStructureDataAssociationCollection strucData = getCompoundCollection();
		int n = 0;
		for (IFDAssociation assoc : strucData) {
			List<IFDDataObject> empty = new ArrayList<>();
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> dataCollection = assoc.getObject(1);
			for (IFDRepresentableObject<? extends IFDRepresentation> d : dataCollection) {
				if (d.size() == 0)
					empty.add((IFDDataObject) d);
			}
			n += empty.size();
			dataCollection.removeAll(empty);
			if (dataCollection.size() == 0) {
				lstRemove.add(assoc);
				IFDStructure st = (IFDStructure) assoc.getFirstObj1();
				extractor.log("! FAIRSpecExtractionHelper.removeStructuresWithNoAssociation removing structure "
						+ st.getLabel());
				getStructureCollection().remove(st);
				n++;
			}
		}
		n += lstRemove.size();
		strucData.removeAll(lstRemove);
		return n;
	}

	/**
	 * This method will return the FIRST structure associated with a spectrum and
	 * optionally remove it if found
	 * 
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	@Override
	public IFDStructure getFirstStructureForSpec(IFDDataObject spec, boolean andRemove) {
		return (IFDStructure) getCompoundCollection().getFirstObj1ForObj2(spec, andRemove);
	}

	/**
	 * This method will return the FIRST sample associated with a spectrum and
	 * optionally remove it if found
	 * 
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	@Override
	public IFDSample getFirstSampleForSpec(IFDDataObject spec, boolean andRemove) {
		return (IFDSample) getSampleDataCollection().getFirstObj1ForObj2(spec, andRemove);
	}

	@Override
	public FAIRSpecCompoundAssociation createCompound(IFDStructure struc, IFDDataObject spec)
			throws IFDException {
		return (FAIRSpecCompoundAssociation) getCompoundCollection().addAssociation(struc, spec);
	}

	@Override
	public IFDSampleDataAssociation associateSampleSpec(IFDSample sample, IFDDataObject spec) throws IFDException {
		return (IFDSampleDataAssociation) getSampleDataCollection().addAssociation(sample, spec);
	}

	@Override
	public IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String originPath,
			String localName, String name) throws IFDException {
		if (getSpecCollection().indexOf(spec) < 0)
			getSpecCollection().add(spec);
		IFDStructure struc = (IFDStructure) checkAddNewObject(getStructureCollection(), ClassTypes.Structure, rootPath,
				IFD_PROPERTY_STRUCTURE_LABEL, name, localName, null, 0, true);
		struc.findOrAddRepresentation(originPath, localName, null, ifdRepType,
				FAIRSpecUtilities.mediaTypeFromFileName(localName));
		getStructureCollection().add(struc);
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) getCompoundCollection()
				.getAssociationForSingleObj2(spec);
		if (ss == null) {
			ss = getCompoundCollection().addAssociation(struc, spec);
		} else {
			ss.getStructureCollection().add(struc);
		}
		return struc;
	}

	@Override
	public FAIRSpecCompoundAssociation findCompound(IFDStructure struc, IFDDataObject spec) {
		return (FAIRSpecCompoundAssociation) getCompoundCollection().findAssociation(struc, spec);
	}

	public IFDSampleDataAssociation getSampleAssociation(IFDSample struc, IFDDataObject spec) {
		return (IFDSampleDataAssociation) getSampleDataCollection().findAssociation(struc, spec);
	}

	@Override
	public IFDRepresentation getSpecDataRepresentation(String zipName) {
		return (dataObjectCollection == null ? null : dataObjectCollection.getRepresentation(zipName));
	}

	public IFDSampleCollection getSampleCollection() {
		if (sampleCollection == null) {
			objects[SAMPLE_COLLECTION] = sampleCollection = new IFDSampleCollection();
			sampleCollection.setID("samples");
		}
		return sampleCollection;
	}

	public IFDStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			objects[STRUCTURE_COLLECTION] = structureCollection = new IFDStructureCollection();
			structureCollection.setID("structures");
		}
		return structureCollection;
	}

	@Override
	public IFDDataObjectCollection getSpecCollection() {
		if (dataObjectCollection == null) {
			objects[DATA_COLLECTION] = dataObjectCollection = new IFDDataObjectCollection();
			dataObjectCollection.setID("spectra");
		}
		return dataObjectCollection;
	}

	public IFDSampleStructureAssociationCollection getSampleStructureCollection() {
		if (sampleStructureCollection == null) {
			collections[SAMPLE_STRUCTURE_COLLECTION] = sampleStructureCollection = new IFDSampleStructureAssociationCollection(
					byId);
			sampleStructureCollection.setID("sample-structure associations");
		}
		return sampleStructureCollection;
	}

	public IFDSampleDataAssociationCollection getSampleDataCollection() {
		if (sampleDataCollection == null) {
			collections[SAMPLE_DATA_COLLECTION] = sampleDataCollection = new IFDSampleDataAssociationCollection(
					byId);
			sampleDataCollection.setID("sample-spectra associations");
		}
		return sampleDataCollection;
	}

	@Override
	public FAIRSpecCompoundCollection getCompoundCollection() {
		if (compoundCollection == null) {
			collections[STRUCTURE_DATA_COLLECTION] = compoundCollection = new FAIRSpecCompoundCollection(
					byId);
			compoundCollection.setID("compounds");
		}
		return compoundCollection;
	}
//
//	public IFDSampleDataAnalysisCollection getSampleDataAnalysisCollection() {
//		if (sampleDataAnalysisCollection == null)
//			associations[SAMPLE_DATA_ANALYSIS_COLLECTION] =
//					sampleDataAnalysisCollection = new IFDSampleDataAnalysisCollection("sampleDataAnalyses");
//		return sampleDataAnalysisCollection;
//	}

	public IFDStructureDataAnalysisCollection getStructureDataAnalysisCollection() {
		if (structureDataAnalysisCollection == null)
			collections[STRUCTURE_DATA_ANALYSIS_COLLECTION] = structureDataAnalysisCollection = new IFDStructureDataAnalysisCollection(
					byId);
		structureDataAnalysisCollection.setID("structure-spectra-analyses");
		return structureDataAnalysisCollection;
	}

	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * [rootname]_IFD_PROPERTY_COLLECTIONSET.[ext] and optionally create an
	 * _IFD_collection.zip in that same directory.
	 * 
	 * @param targetDir  or null for no output
	 * @param rootName   a prefix root to add to the
	 *                   _IFD_PROPERTY_COLLECTIONSET.json (or.xml) finding aid
	 *                   created
	 * @param products   optionally, a list of directories containing the files
	 *                   referenced by the finding aid for creating the
	 *                   IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String createSerialization(File targetDir, String rootName, ArrayList<Object> products, IFDSerializerI serializer,
			long[] t) throws IOException {
		if (serializer == null)
			serializer = new IFDDefaultJSONSerializer(byId);
		// subclasses should be able to use this directly with no changes.
		t[0] = System.currentTimeMillis();
		String serializedFindingAid = serializer.serialize(findingAid).toString();
		t[0] = System.currentTimeMillis() - t[0];
		
		if (targetDir == null)
			return serializedFindingAid;
		
		String aidName = "IFD" + IFDConst.IFD_FINDINGAID_FLAG + serializer.getFileExt();
		String faPath = targetDir.toString().replace('\\', '/') + "/" + aidName;
		FAIRSpecUtilities.writeBytesToFile(serializedFindingAid.getBytes(), new File(faPath));
		if (products != null) {
			products = (ArrayList<Object>) products.clone();
			// zip up the collection and re-create finding aid with updated information
			
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_REF, null);
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_LEN, null);
			String zipName = "IFD" + IFDConst.IFD_COLLECTION_FLAG + "zip";
			String path = targetDir + "/" + zipName;
			
			// creating the zip file is the time-consuming part.
			
			FAIRSpecUtilities.refreshLog();
			System.out.println("FAIRSpecExtractorHelper creating " + path);
			t[1] = System.currentTimeMillis();
			
			// byte[] followed by entry name signals that we already have the bytes; don't open a FileInputStream
			
			
			products.add(0, serializedFindingAid.getBytes());
			products.add(1, aidName);
			long len = FAIRSpecUtilities.zip(path, targetDir.toString().length() + 1, products);
			t[1] = System.currentTimeMillis() - t[1];

			// update external finding aid with length of data and reference
			
			t[2] = System.currentTimeMillis();
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_REF, zipName);
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_LEN, len);
			serializedFindingAid = serializer.serialize(findingAid).toString();
			t[2] = System.currentTimeMillis() - t[2];
			FAIRSpecUtilities.writeBytesToFile(serializedFindingAid.getBytes(), new File(faPath));
		}
		return serializedFindingAid;
	}

	@Override
	public IFDResource addOrSetSource(String dataSource) {
		return currentResource = getFindingAid().addOrSetResource(dataSource);
	}

	@Override
	public void setCurrentResourceByteLength(long len) {
		currentResource.setLength(len);
	}

	@Override
	public IFDSample getSampleByName(String label) {
		return (IFDSample) getSampleCollection().getObjectByLabel(label);
	}

	@Override
	public IFDSampleStructureAssociation associateSampleStructure(IFDSample sample, IFDStructure struc)
			throws IFDException {
		return getSampleStructureCollection().addAssociation(sample, struc);
	}

	@Override
	public String finalizeExtraction() {
		for (int i = 0; i < objects.length; i++)
			if (objects[i] != null)
				findingAid.addCollection(objects[i]);
		for (int i = 0; i < collections.length; i++)
			if (collections[i] != null)
				findingAid.addCollection(collections[i]);
		findingAid.finalizeCollectionSet();
		return dumpSummary();
	}

	private String dumpSummary() {
		IFDCollectionSet cs = findingAid.getCollectionSet();
		int n = 0;
		String s = "";
		for (int i = 0; i < cs.size(); i++) {
			IFDCollection<IFDObject<?>> c = cs.get(i);
			for (Object sd : c) {
				s += ("\t" + sd + "\n");
				n++;
			}
			s += "## " + c.size() + " " + c.getID() + "\n";
		}
		s = "!FAIRSpecExtractionHelper.dumpSummary extraction complete:\n! " + getFindingAid().getResources() + "\n!\n"
				+ (n == 0 ? "!FAIRSpecExtractionHelper.dumpSummary no objects?\n" : s)
				+ "\n!FAIRSpecExtractionHelper.dumpSummary version " + IFDConst.getProp("IFD_VERSION") + "\n";
		return s;
	}

	/**
	 * Clone the spectrum object that has declared a sub-spectrum from it (as from
	 * Mestrenova plugin page-based spectra) and make sure it is part of any
	 * structureDataCollection or sampleDataCollection
	 */
	@Override
	public IFDDataObject cloneData(IFDDataObject localSpec, String idExtension, boolean andReplace) {
		// this will invalidate localSpec -- 1.mnova, for example.
		// TODO clean out invalidated data
		IFDDataObject data = getSpecCollection().cloneData(localSpec, idExtension, andReplace);
		if (compoundCollection != null) {
			for (IFDAssociation a : compoundCollection) {
				IFDStructureDataAssociation assoc = (IFDStructureDataAssociation) a;
				if (assoc.getDataObjectCollection().contains(localSpec))
					assoc.getDataObjectCollection().add(data);
			}
		}
		if (sampleDataCollection != null) {
			for (IFDAssociation a : sampleDataCollection) {
				IFDStructureDataAssociation assoc = (IFDStructureDataAssociation) a;
				if (assoc.getDataObjectCollection().contains(localSpec))
					assoc.getDataObjectCollection().add(data);
			}
		}
		return data;
	}

	@Override
	public void removeInvalidData() {
		if (dataObjectCollection == null)
			return;
		dataObjectCollection.removeInvalidData();
		if (compoundCollection != null) {
			for (IFDAssociation a : compoundCollection) {
				IFDStructureDataAssociation assoc = (IFDStructureDataAssociation) a;
				for (int i = assoc.size(); --i >= 0;) {
					assoc.getDataObjectCollection().removeInvalidData();
				}
				if (assoc.getFirstObj1() == null) {
					IFDObject<?> o = assoc.getFirstObj2();
					extractor.log("! FAIRSpecExtractorHelper association id=" + assoc.getID()
							+ " spec=" + (o == null ? "" : o.getID())
							+ " has no associated structure representation ");
				}
			}
		}
		if (sampleDataCollection != null) {
			for (IFDAssociation a : sampleDataCollection) {
				IFDSampleDataAssociation assoc = (IFDSampleDataAssociation) a;
				for (int i = assoc.size(); --i >= 0;) {
					assoc.getDataObjectCollection().removeInvalidData();
				}
				if (assoc.getFirstObj1() == null)
					extractor.log("! FAIRSpecExtractorHelper association " + assoc.getID()
							+ " has no associated sample representation");
			}
		}
	}


	/**
	 * Get standardized JSON for the _IFD_* files.
	 * 
	 * @param name  "manifest", "rejected", "ignored"
	 * @param fileTarget
	 * @param rootLists 
	 * @param ret number of items
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	@Override
	public String getListJSON(String name, List<FileList> rootLists, String resourceList, String scriptFileName, int[] ret) throws IOException {
		int n = ret[0] = FileList.getListCount(rootLists, name);
		// Date d = new Date();
		// all of a sudden, on 2021.06.13 at 1 PM
		// file:/C:/Program%20Files/Java/jdk1.8.0_251/jre/lib/sunrsasign.jar cannot be
		// found when
		// converting d.toString() due to a check in Date.toString for daylight savings
		// time!

		StringBuffer sb = new StringBuffer();
		sb.append("{\"" + IFDConst.IFD_FLAG + "version\":\"" + IFDConst.IFD_VERSION + "\",\n");
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "version\":\"" + extractor.getVersion() + "\",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "code\":\"" + extractor.getCodeSource() + "\",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "creation_date\":\"" + getFindingAid().getDate().toGMTString() + "\",\n");
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "script\":\"" + scriptFileName+"\",\n");
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "sources\":\"" + resourceList + "\",\n");
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_type\":\"" + name + "\",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_fileCount\":" + n + ",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_byteCount\":" + FileList.getByteCount(rootLists, name) + ",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list\":\n");
		sb.append("[\n");
		for (int i = 0; i < rootLists.size(); i++) {
			if (rootLists.get(i).getName().equals(name))
				rootLists.get(i).serialize(sb);
		}
		sb.append("]\n");
		sb.append("}\n");
		return sb.toString();
	}

}