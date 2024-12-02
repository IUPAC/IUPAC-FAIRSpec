package org.iupac.fairdata.contrib.fairspec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.core.IFDResource;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;

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
public class FAIRSpecExtractorHelper extends FAIRSpecFindingAidHelper implements FAIRSpecExtractorHelperI {

	static {
		FAIRSpecFindingAid.loadProperties();
	}

	// these values are in fairspec.properties
	public static final String FAIRSPEC_EXTRACTOR_FLAG = IFDConst.getProp("FAIRSPEC_EXTRACTOR_FLAG");
	public static final String FAIRSPEC_EXTRACTOR_OBJECT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OBJECT");
	public static final String FAIRSPEC_EXTRACTOR_REPLACEMENTS = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REPLACEMENTS");
	public static final String FAIRSPEC_EXTRACTOR_ACCEPT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_ACCEPT");
	public static final String FAIRSPEC_EXTRACTOR_REJECT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REJECT");
	public static final String FAIRSPEC_EXTRACTOR_IGNORE = IFDConst.getProp("FAIRSPEC_EXTRACTOR_IGNORE");
	public static final String FAIRSPEC_EXTRACTOR_OPTION_FLAG = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OPTION_FLAG");
	public static final String FAIRSPEC_EXTRACTOR_OPTIONS = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OPTIONS");
	public static final String FAIRSPEC_EXTRACTOR_METADATA = IFDConst.getProp("FAIRSPEC_EXTRACTOR_METADATA");
	public static final String FAIRSPEC_EXTRACTOR_METADATA_FILE = IFDConst.getProp("FAIRSPEC_EXTRACTOR_METADATA_FILE");
	public static final String FAIRSPEC_EXTRACTOR_METADATA_KEY = IFDConst.getProp("FAIRSPEC_EXTRACTOR_METADATA_KEY");	
	public static final String FAIRSPEC_EXTRACTOR_METADATA_IGNORE_PREFIX = IFDConst.getProp("FAIRSPEC_EXTRACTOR_METADATA_IGNORE_PREFIX");
	public static final String FAIRSPEC_EXTRACTOR_RELATED_METADATA = IFDConst.getProp("FAIRSPEC_EXTRACTOR_RELATED_METADATA");
	public static final String FAIRSPEC_EXTRACTOR_LOCAL_SOURCE_FILE = IFDConst.getProp("FAIRSPEC_EXTRACTOR_LOCAL_SOURCE_FILE");
	public static final String FAIRSPEC_EXTRACTOR_REFERENCES = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REFERENCES");

	public static final String EXIT = "EXIT";

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
		private List<Long> lengths = new ArrayList<>();
		private Pattern acceptPattern;
		private long byteCount;
	
		public FileList(String rootPath, String name) {
			this.name = name;
			this.rootPath = rootPath;
		}
	
		public static String toJSON(StringBuffer sb, String[] list, String rootPath, boolean withBrackets) {
			boolean returnString = (sb == null);
			if (returnString)
				sb = new StringBuffer();
			if (withBrackets)
				sb.append("[");
			String sep = "";
			for (int i = 0; i < list.length; i++) {
				String fname = list[i];
				sb.append((sep + "\"" 
				+ (rootPath == null ? "" : rootPath + "/") + fname + "\""));
				sep = ",\n";
			}
			sb.append("\n");
			if (withBrackets)
				sb.append("]");
			return (returnString ? sb.toString() : null);
		}

		public int size() {
			return files.size();
		}

		/**
		 * Sort and concatentate the list. Do not add opening and closing [ ] 
		 * @param sb
		 * @return String if sb == null, or null is sb != null
		 */
		public String serialize(StringBuffer sb) {
			lengths = null;
			return getJSON(sb);
		}
	
		private String getJSON(StringBuffer sb) {
			String[] list = files.toArray(new String[files.size()]);
			Arrays.sort(list);
			return toJSON(sb, list, rootPath, false);
		}

		public boolean contains(String fileName) {
			return files.contains(fileName);
		}
	
		public void add(String fileName, long len) {
			files.add(fileName);
			if (len < 0)
				len = 0;
			
			lengths.add(Long.valueOf(len));
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
				if (list != null && list.getName().equals(name))
					n += list.size();
			}
			return n;
		}
	
		public static long getByteCount(List<FileList> lists, String name) {
		    long n = 0;
			for (int i = lists.size(); --i >= 0;) {
				FileList list = lists.get(i);
				if (list != null && list.getName().equals(name))
					n += list.getByteCount();
			}
			return n;
		}
	
		@Override
		public String toString() {
			return getJSON(null);
		}

		public long getLength(String fileName) {
			int i = files.indexOf(fileName);
			return (i < 0 || lengths == null ? 0 : lengths.get(i).longValue());
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
	public static final String defaultCachePattern = "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
	;

	public static final String FAIRSPEC_EXTRACT_VERSION = IFDConst.getProp("FAIRSPEC_EXTRACT_VERSION");
	public static final String DATAOBJECT_FAIRSPEC_FLAG = IFDConst.getProp("DATAOBJECT_FAIRSPEC_FLAG");
	
	public static final String DATAOBJECT_ORIGINATING_SAMPLE_ID = IFDConst.getProp(IFDConst.IFD_PROPERTY_DATAOBJECT_ORIGINATING_SAMPLE_ID);
	private static final String IFD_PROPERTY_SAMPLE_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_SAMPLE_FLAG, IFDConst.IFD_ID_FLAG);

	public static final String IFD_PROPERTY_STRUCTURE_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_STRUCTURE_FLAG, IFDConst.IFD_ID_FLAG);
	public static final String IFD_PROPERTY_DATAOBJECT_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG,
			IFDConst.IFD_DATAOBJECT_FLAG, IFDConst.IFD_ID_FLAG);

	public static final String IFD_PROPERTY_FAIRSPEC_COMPOUND_ID = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, "fairspec.compound.id");
	
;

	/**
	 * current state of extraction
	 * 
	 */
	protected String currentOriginPath;
	protected List<Object[]> currentDataProps;

	private FAIRSpecExtractorI extractor;

	/**
	 * 
	 * @param extractor
	 * @param creator
	 * @throws IFDException in name only; necessary here, but will not be thown
	 */
	public FAIRSpecExtractorHelper(FAIRSpecExtractorI extractor, String creator) {
		super(creator);
		if (extractor == null)
			throw new RuntimeException("FAIRSpecExtractorHelper: extractor cannot be null");
		this.extractor = extractor;
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
		// from MetadataExtractorLevel2
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
			if (isNew) {
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
		case FAIRSpecFindingAidHelper.ClassTypes.Sample:
			currentSample = (IFDSample) checkAddNewObject(getSampleCollection(), type, rootPath, param, value,
					localizedName, null, len, true);
			if (currentAssociation != null && currentAssociation instanceof IFDSampleDataAssociation) {
				((IFDSampleDataAssociation) currentAssociation).getSampleCollection().add(currentSample);
			}
			return currentSample;
		case FAIRSpecFindingAidHelper.ClassTypes.Structure:
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
		case FAIRSpecFindingAidHelper.ClassTypes.DataObject:
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
		case FAIRSpecFindingAidHelper.ClassTypes.Compound:
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
		case FAIRSpecFindingAidHelper.ClassTypes.SampleDataAssociation:
			currentAssociation = getSampleDataCollection().getObjectByID(value);
			if (currentAssociation == null) {
				getSampleDataCollection().add(currentAssociation = new FAIRSpecCompoundAssociation());
				currentAssociation.setPropertyValue(param, value);
			}
			return null;
		case FAIRSpecFindingAidHelper.ClassTypes.SampleDataAssociationCollection:
		case FAIRSpecFindingAidHelper.ClassTypes.CompoundCollection:
		case FAIRSpecFindingAidHelper.ClassTypes.StructureDataAnalysisCollection:
		case FAIRSpecFindingAidHelper.ClassTypes.StructureDataAnalysis:
			System.out.println("FAIRSpecExtractionHelper.addObject " + type + " not implemented");
			// TODO
			return null;
		case FAIRSpecFindingAidHelper.ClassTypes.StructureCollection:
		case FAIRSpecFindingAidHelper.ClassTypes.SampleCollection:
			// should not be generic
		default:
			throw new IFDException("FAIRSpecFindingAidHelper.addObject raw collection -- could not add " + param + " "
					+ value + " for " + currentOriginPath + " type " + type);
		}
	}

	public static void addProperties(IFDObject<?> o, List<Object[]> props) {
		for (Object[] s : props) {
			Object val = s[1];
			if (val == null)
				continue;
			String key = (String) s[0];
			// "*" prefix indicates private, not to be included in Finding Aid
			if (key.startsWith(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_METADATA_IGNORE_PREFIX))
				continue;
			o.setPropertyValue(key, val);
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
			case FAIRSpecFindingAidHelper.ClassTypes.SampleDataAssociation:
			case FAIRSpecFindingAidHelper.ClassTypes.Compound:
				currentAssociation.setPropertyValue(param, value);
				return null;
			case FAIRSpecFindingAidHelper.ClassTypes.Sample:
				key = rootPath + "::" + (originPath == null ? localName : value);
				break;
			case FAIRSpecFindingAidHelper.ClassTypes.Structure:
				key = rootPath + "::" + (originPath == null ? localName : value);
				break;
			case FAIRSpecFindingAidHelper.ClassTypes.DataObject:
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
			case FAIRSpecFindingAidHelper.ClassTypes.Sample:
				o = new IFDSample();
				o = ((IFDSampleCollection) c).addWithPath(key, (IFDSample) o);
				break;
			case FAIRSpecFindingAidHelper.ClassTypes.Structure:
				o = new IFDStructure();
				o = ((IFDStructureCollection) c).addWithPath(key, (IFDStructure) o);
				break;
			case FAIRSpecFindingAidHelper.ClassTypes.DataObject:
			default:
				o = FAIRSpecDataObject.createFAIRSpecObject(type);
				o = ((IFDDataObjectCollection) c).addWithPath(key, (IFDDataObject) o);
				break;
			}
			if (o == null)
				throw new IFDException("FAIRSpecExtractorHelper.addNewObject object not found for path=" + rootPath
						+ " and originPath=" + currentOriginPath);
		}
		checkAddRepOrSetParam(o, param, value, localName, len);
		if (isNew && isID)
			extractor.setNewObjectMetadata(o, param);
		return o;
	}

	protected void checkAddRepOrSetParam(IFDRepresentableObject<? extends IFDRepresentation> o, String param,
			String value, String localName, long len) {
		if (IFDConst.isRepresentation(param)) {
			findOrAddRepresentation(o, currentResource.getID(), currentOriginPath, currentResource.getRootPath(), localName, null, param).setLength(len);
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
			@SuppressWarnings("unchecked")
			IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>> dataCollection = (IFDCollection<IFDRepresentableObject<? extends IFDRepresentation>>) assoc.getObject(1);
			for (IFDRepresentableObject<? extends IFDRepresentation> d : dataCollection) {
				if (d.size() == 0) {
					extractor.log("! FAIRSpecExtractionHelper.removed " + d);
					empty.add((IFDDataObject) d);
				}
			}
			n += empty.size();
			dataCollection.removeAll(empty);
			if (dataCollection.size() == 0) {
				lstRemove.add(assoc);
				IFDStructure st = (IFDStructure) assoc.getFirstObj1();
				if (st != null) {
					extractor.log("! FAIRSpecExtractionHelper.removeStructuresWithNoAssociation removing structure "
						+ st.getID());
					getStructureCollection().remove(st);
				}
				n++;
			}
		}
		n += lstRemove.size();
		strucData.removeAll(lstRemove);
		return n;
	}

	@Override
	public IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String originPath,
			String localName, String name) throws IFDException {
		// from MetadataExtractorLayer2
		IFDStructure struc = getStructureCollection().getStructureFromLocalName(currentResource.getID(), localName);
		if (struc == null) {
			if (name == null)
				name = "Structure_" + ++lastStructureName;
			struc = (IFDStructure) checkAddNewObject(getStructureCollection(), FAIRSpecFindingAidHelper.ClassTypes.Structure, rootPath,
					IFD_PROPERTY_STRUCTURE_ID, name, localName, null, 0, true);
			struc.findOrAddRepresentation(currentResource.getID(), originPath, rootPath, localName, null, ifdRepType,
					FAIRSpecUtilities.mediaTypeFromFileName(localName));
			getStructureCollection().add(struc);
		}
		if (spec != null) {
			if (getSpecCollection().indexOf(spec) < 0)
				getSpecCollection().add(spec);
			IFDStructureDataAssociation ss = (IFDStructureDataAssociation) getCompoundCollection()
					.getAssociationForSingleObj2(spec);
			if (ss == null) {
				ss = getCompoundCollection().addAssociation(struc, spec);
			} else {
				ss.getStructureCollection().add(struc);
			}
		}
		return struc;
	}

	private int lastStructureName;
	private int lastSampleName; // TODO

	@Override
	public IFDSample addSpecOriginatingSampleRef(String rootPath, IFDDataObject spec, String id) throws IFDException {
			// from MetadataExtractorLayer2
			if (getSpecCollection().indexOf(spec) < 0)
				getSpecCollection().add(spec);
			IFDSample sample = (IFDSample) checkAddNewObject(getSampleCollection(), FAIRSpecFindingAidHelper.ClassTypes.Sample, rootPath,
					IFD_PROPERTY_SAMPLE_ID, id, null, null, 0, true);
			getSampleCollection().add(sample);
			IFDSampleDataAssociation ss = (IFDSampleDataAssociation) getSampleDataCollection()
					.getAssociationForSingleObj1(sample);
			if (ss == null) {
				ss = getSampleDataCollection().addAssociation(sample, spec);
			} else {
				ss.getDataObjectCollection().add(spec);
			}
			return sample;
	}

	@Override
	public IFDResource addOrSetSource(String dataSource, String rootPath) {
		return (currentResource = getFindingAid().addOrSetResource(dataSource, rootPath));
	}

	@Override
	public IFDResource getCurrentSource() {
		return currentResource;
	}
	
	@Override
	public void setCurrentResourceByteLength(long len) {
		currentResource.setLength(len);
	}


	private Map<String, Map<String, Object>> htURLReferences;
	
	@Override
	public void setCompoundRefMap(Map<String, Map<String, Object>> htCompoundFileReferences) {
		this.htURLReferences = htCompoundFileReferences;
	}

	@Override
	public String finalizeExtraction() {
		finalizeObjects();
		finalizeURLRefs();
		finalizeCollectionSet(htURLReferences);
		return dumpSummary();
	}

	private void finalizeURLRefs() {
		if (htURLReferences == null)
			return;
		IFDStructureDataAssociationCollection cc = getCompoundCollection();
		for (int i = cc.size(); --i >= 0;) {
			IFDAssociation c = cc.get(i);
			// add repository reference
			Map<String, Object> ref = htURLReferences.get(c.getID());
			if (ref == null)
				continue;
			String url = (String) ref.get("url");
			if (url != null) {
				c.setPropertyValue(IFDConst.IFD_PROPERTY_URL, url);
			}
			String doi = (String) ref.get("doi");
			if (doi != null) {
				c.setPropertyValue(IFDConst.IFD_PROPERTY_DOI, doi);
			}
		}
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
				IFDSampleDataAssociation assoc = (IFDSampleDataAssociation) a;
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
				IFDStructure struc = (IFDStructure) assoc.getFirstObj1(); 
				IFDObject<?> spec = assoc.getFirstObj2();
				if (assoc.get(1).size() > 0 && (struc == null || struc.size() == 0)) {
					extractor.log("! FAIRSpecExtractorHelper association id=" + assoc.getID()
							+ " spec=" + (spec == null ? "" : spec.getID())
							+ " has no associated structure representation ");
				}
				if (assoc.get(0).size() > 0 && (spec == null || spec.size() == 0)) {
					extractor.log("! FAIRSpecExtractorHelper association id=" + assoc.getID()
							+ " struc=" + (struc == null ? "" : struc.getID())
							+ " has no associated spectrum representation ");
					assoc.setValid(false);
				}
			}
			compoundCollection.removeInvalidData();
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
	public String getFileListJSON(String name, List<FileList> rootLists, String resourceList, String scriptFileName, int[] ret) throws IOException {
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
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "sources\":\n");
		FileList.toJSON(sb, resourceList.split(";"), null, true); 
		sb.append(",\n");
		sb.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_type\":\"" + name + "\",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_fileCount\":" + n + ",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list_byteCount\":" + FileList.getByteCount(rootLists, name) + ",\n")
				.append("\"" + FAIRSPEC_EXTRACTOR_FLAG + "list\":\n");
		sb.append("[\n");
		String sep = "";
		for (int i = 0; i < rootLists.size(); i++) {
			FileList list = rootLists.get(i);
			if (list != null && list.getName().equals(name) && list.size() > 0) {
				sb.append(sep);
				list.serialize(sb);
				sep = ",";
			}
		}
		sb.append("]\n");
		sb.append("}\n");
		return sb.toString();
	}


	@Override
	public Object setPropertyValueNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath) {
		if (IFDConst.isIFDProperty(key)) {
			// not a parameter and not forcing NULL
			Object v = obj.getPropertyValue(key);
			if (value.equals(v))
				return null;
			if (v != null && value != IFDProperty.NULL) {
				String objSource = obj.getPropertySource(key);
				if (originPath != null && !originPath.equals(objSource)) {
				return v;
				} else {
					System.out.println("OK!!");
				}
			}
		}
		obj.setPropertyValue(key, value, originPath);
		return null;
	}

}