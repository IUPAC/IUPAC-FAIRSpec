package org.iupac.fairdata.contrib.fairspec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.structure.IFDStructureRepresentation;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;

/**
 * 
 * This class is tailored to the task of creating an IUPAC FAIRData Collection
 * and its associated IUPAC FAIRData Finding Aid from zipped data aggregations,
 * particularly from supporting information packages.
 * 
 * It is not a fully automated system -- the starting point is an "IUPAC
 * FAIRSpec Data and Metadata Extraction Template" (see the extract/ folder)
 * 
 * This class is best instantiated by third-party extractors. 
 * (see com.integratedgraphics.ifd.Extractor and ExtractorTest)
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
	
	public static final String IFD_EXTRACTOR_OBJECT = IFDConst.getProp("FAIRSPEC_EXTRACTOR_OBJECT");



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

		public final static String StructureDataAssociation = "org.iupac.fairdata.derived.IFDStructureDataAssociation";
		public final static String StructureDataAssociationCollection = "org.iupac.fairdata.derived.IFDStructureDataAssociationCollection";

		public final static String SampleDataAnalysis = "org.iupac.fairdata.derived.IFDSampleDataAnalysis";
		public final static String SampleDataAnalysisCollection = "org.iupac.fairdata.derived.IFDSampleDataAnalysisCollection";

		public final static String StructureDataAnalysis = "org.iupac.fairdata.derived.IFDStructureDataAnalysis";
		public final static String StructureDataAnalysisCollection = "org.iupac.fairdata.derived.IFDStructureDataAnalysisCollection";

	}

	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = "(MACOSX)|(desktop\\.ini)|(\\.DS_Store)";

	/**
	 * the files we want extracted -- just PDF and PNG here; all others are taken
	 * care of by individual IFDVendorPluginI classes
	 */
	public static final String defaultCachePattern = "" + "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
	;
	
	public final static int SAMPLE_COLLECTION    = 0;
	public final static int STRUCTURE_COLLECTION = 1;
	public final static int DATA_COLLECTION      = 2;

	public final static int SAMPLE_STRUCTURE_COLLECTION        = 0;
	public final static int SAMPLE_DATA_COLLECTION             = 1;
	public final static int STRUCTURE_DATA_COLLECTION          = 2;
	public final static int SAMPLE_DATA_ANALYSIS_COLLECTION    = 3;
	public final static int STRUCTURE_DATA_ANALYSIS_COLLECTION = 4;

	public static final String FAIRSPEC_EXTRACT_VERSION = IFDConst.getProp("FAIRSPEC_EXTRACT_VERSION");
	public static final String DATAOBJECT_FAIRSPEC_FLAG = IFDConst.getProp("FAIRSPEC_DATAOBJECT_FLAG");

	public static final String IFD_PROPERTY_SAMPLE_LABEL = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_SAMPLE_FLAG, IFDConst.IFD_LABEL_FLAG);

	private static final String IFD_PROPERTY_STRUCTURE_LABEL = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_STRUCTURE_FLAG, IFDConst.IFD_LABEL_FLAG);

	protected final FAIRSpecFindingAid findingAid;


	protected IFDStructureCollection structureCollection;
	protected IFDDataObjectCollection specDataCollection;
	protected IFDStructureDataAssociationCollection structureDataCollection;
	protected IFDStructureDataAnalysisCollection structureDataAnalysisCollection;
	protected IFDSampleCollection sampleCollection;
	protected IFDSampleDataAssociationCollection sampleDataCollection;
	protected IFDSampleStructureAssociationCollection sampleStructureCollection;


	/**
	 * current state of extraction
	 * 
	 */
	protected String currentOriginPath;
	protected IFDStructure currentStructure;
	protected IFDDataObject currentDataObject;
	protected IFDSample currentSample;
	protected IFDResource currentResource;


	
	/**
	 * temporary holding arrays separating objects from associations
	 * so that they can be added later to the finding aid in a desired order.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] objects = new IFDCollection[3];
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] associations = new IFDCollection[5];


	public FAIRSpecExtractorHelper(String creator) {
		FAIRSpecFindingAid fa = null;
		try {
			fa = new FAIRSpecFindingAid(null, null, creator);
		} catch (IFDException e) {
			// not possible
		}
		findingAid = fa;
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
	 * @param propName
	 * @return
	 * @throws IFDException
	 */
	public static String getObjectTypeForName(String propName, boolean allowError) throws IFDException {
		if (IFDConst.isProperty(propName))
			propName = FAIRSpecUtilities.rep(propName, IFDConst.IFD_PROPERTY_FLAG, "\0");
		else if (IFDConst.isRepresentation(propName))
			propName = FAIRSpecUtilities.rep(propName, IFDConst.IFD_REPRESENTATION_FLAG, "\0");
		else if (allowError)
			return "Unknown";
		else
			throw new IFDException("bad IFD identifier: " + propName);
		if (propName.startsWith("\0structure."))
			return ClassTypes.Structure;
		if (propName.startsWith("\0sample."))
			return ClassTypes.Sample;
		if (propName.startsWith("\0analysis.structuredata"))
			return ClassTypes.StructureDataAnalysis;
		if (propName.startsWith("\0analysis.sampledata"))
			return ClassTypes.SampleDataAnalysis;
		if (propName.startsWith("\0dataobject.fairspec.")) {
			// adds next part, e.g "nmr"
			return propName.replace('\0','.').substring(0, propName.indexOf(".", 21)); 
		}
		if (propName.startsWith("\0dataobject."))
			return ClassTypes.DataObject;
		return "Unknown";
	}

	/**
	 * Add the object to the appropriate collection.
	 * 
	 * @param rootPath
	 * @param param
	 * @param value
	 * @param localName
	 * @return
	 * @throws IFDException
	 */
	@Override
	public IFDRepresentableObject<?> addObject(String rootPath, String param, String value, String localName)
			throws IFDException {
		if (!isAddingObjects())
			throw new IFDException("addObject " + param + " " + value + " called with no current object file name");

		String type = getObjectTypeForName(param, false);
		if (type.startsWith(DATAOBJECT_FAIRSPEC_FLAG)) {
			return currentDataObject = (IFDDataObject) addNewObject(getDataObjectCollection(), type, rootPath, param, value, localName);
		}
		switch (type) {
		case ClassTypes.Sample:
			return currentSample = (IFDSample) addNewObject(getSampleCollection(), type, rootPath, param, value, localName);
		case ClassTypes.Structure:
			return currentStructure = (IFDStructure) addNewObject(getStructureCollection(), type, rootPath, param, value, localName);
		case ClassTypes.DataObject:
			if (currentDataObject == null) {
				System.err.println("FAIRSpecFindingAidHelper.addObject data object property found before data object initialized "
						+ param + " " + value + " for " + currentOriginPath);
			} else {
				currentDataObject.setPropertyValue(param, value);
			}
			return currentDataObject;
		case ClassTypes.SampleDataAssociationCollection:
		case ClassTypes.StructureDataAssociationCollection:
		case ClassTypes.StructureDataAnalysisCollection:
		case ClassTypes.StructureDataAnalysis:
			System.out.println("FAIRSpecExtractionHelper.addObject " + type + " not implemented");
			// TODO
			return null;
		case ClassTypes.StructureCollection:
		case ClassTypes.SampleCollection:
			// should not be generic
		default:
			System.err.println(
					"FAIRSpecFindingAidHelper.addObject raw collection -- could not add " + param + " " + value + " for " + currentOriginPath);
			break;
		}
		return null;
	}
	
	private IFDRepresentableObject<? extends IFDRepresentation> addNewObject(IFDCollection<?> c, String type,
			String rootPath, String param, String value, String localName) throws IFDException {
		String key = rootPath + "::" + currentOriginPath;
		@SuppressWarnings("unchecked")
		IFDRepresentableObject<? extends IFDRepresentation> o = (IFDRepresentableObject<? extends IFDRepresentation>) c
				.getPath(key);
		if (o == null) {
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
			default:
				o = FAIRSpecDataObject.createFAIRSpecObject(type);
				o.setPath(rootPath);
				o = ((IFDDataObjectCollection) c).addWithPath(key, (IFDDataObject) o);
				break;
			}
		}
		if (o == null)
			throw new IFDException("FAIRSpecExtractorHelper.addNewObject object not found for path=" + rootPath
					+ " and originPath=" + currentOriginPath);
		o.setResource(currentResource);
		if (IFDConst.isRepresentation(param)) {
			o.findOrAddRepresentation(currentOriginPath, localName, null, param,
					FAIRSpecUtilities.mediaTypeFromFileName(localName));
		} else {
			o.setPropertyValue(param, value);
		}
		return o;
	}

	@Override
	public IFDObject<?> endAddingObjects() {
		if (!isAddingObjects())
			return null;
		try {
			if (currentStructure != null && currentDataObject != null)
				return getStructureDataCollection().addAssociation(currentStructure, currentDataObject);
			if (currentSample != null && currentDataObject != null)
				return getSampleDataCollection().addAssociation(currentSample, currentDataObject);
			// how does this work?
			return (currentStructure != null ? currentStructure
					: currentSample != null ? currentSample
					: currentDataObject);
		} catch (IFDException e) {
			// not possible
			return null;
		} finally {
			currentOriginPath = null;
			currentStructure = null;
			currentSample = null;
			currentDataObject = null;
		}
	}

	/**
	 * Remove structures for which there are no data associations.
	 * This, of course, is a completely optional step. 
	 * 
	 * @return
	 */
	@Override
	public int removeStructuresWithNoAssociations() {
		List<IFDAssociation> lstRemove = new ArrayList<>();
		IFDStructureDataAssociationCollection strucData = getStructureDataCollection();
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
				System.err.println("FAIRSpecExtractionHelper.removeStructuresWithNoAssociation removing structure " + st.getLabel());
				getStructureCollection().remove(st);
				n++;
			}
		}
		n += lstRemove.size();
		strucData.removeAll(lstRemove);
		return n;
	}

	/**
	 * This method will return the FIRST structure associated with a spectrum
	 * and optionally remove it if found
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	@Override
	public IFDStructure getFirstStructureForSpec(IFDDataObject spec, boolean andRemove) {
		return (IFDStructure) getStructureDataCollection().getFirstObj1ForObj2(spec, andRemove);
	}

	/**
	 * This method will return the FIRST sample associated with a spectrum
	 * and optionally remove it if found
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	@Override
	public IFDSample getFirstSampleForSpec(IFDDataObject spec, boolean andRemove) {
		return (IFDSample) getSampleDataCollection().getFirstObj1ForObj2(spec, andRemove);
	}

	@Override
	public IFDStructureDataAssociation associateStructureSpec(IFDStructure struc, IFDDataObject spec) throws IFDException {
		return getStructureDataCollection().addAssociation(struc, spec);
	}

	@Override
	public IFDSampleDataAssociation associateSampleSpec(IFDSample sample, IFDDataObject spec) throws IFDException {
		return (IFDSampleDataAssociation) getSampleDataCollection().addAssociation(sample, spec);
	}

	@Override
	public IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String originPath, String localName, String name)
			throws IFDException {
		if (getDataObjectCollection().indexOf(spec) < 0)
			getDataObjectCollection().add(spec);			
		IFDStructure struc = (IFDStructure) addNewObject(getStructureCollection(), ClassTypes.Structure, rootPath, IFD_PROPERTY_STRUCTURE_LABEL, name, localName);
		struc.findOrAddRepresentation(originPath, localName, null, ifdRepType, FAIRSpecUtilities.mediaTypeFromFileName(localName));
		getStructureCollection().add(struc);
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) getStructureDataCollection().getAssociationForSingleObj2(spec);
		if (ss == null) {
			ss = getStructureDataCollection().addAssociation(struc, spec);
		} else {
			ss.getStructureCollection().add(struc);
		}
		return struc;
	}

	@Override
	public IFDStructureDataAssociation getStructureAssociation(IFDStructure struc, IFDDataObject spec) {
		return (IFDStructureDataAssociation) getStructureDataCollection().findAssociation(struc, spec);
	}
	
	public IFDSampleDataAssociation getSampleAssociation(IFDSample struc, IFDDataObject spec) {
		return (IFDSampleDataAssociation) getSampleDataCollection().findAssociation(struc, spec);
	}
	
	@Override
	public IFDRepresentation getSpecDataRepresentation(String zipName) {
		return (specDataCollection == null ? null : specDataCollection.getRepresentation(zipName));
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
	public IFDDataObjectCollection getDataObjectCollection() {
		if (specDataCollection == null) {
			objects[DATA_COLLECTION] = specDataCollection = new IFDDataObjectCollection();
			specDataCollection.setID("spectra");
		}
		return specDataCollection;
	}
	
	public IFDSampleStructureAssociationCollection getSampleStructureCollection() {
		if (sampleStructureCollection == null) {
			associations[SAMPLE_STRUCTURE_COLLECTION] = sampleStructureCollection = new IFDSampleStructureAssociationCollection();
			sampleStructureCollection.setID("sample-structure-associations");
		}
		return sampleStructureCollection;
	}

	public IFDSampleDataAssociationCollection getSampleDataCollection() {
		if (sampleDataCollection == null) {
			associations[SAMPLE_DATA_COLLECTION] = sampleDataCollection = new IFDSampleDataAssociationCollection();
			sampleDataCollection.setID("sample-spectra-associations");
		}
		return sampleDataCollection;
	}

	@Override
	public IFDStructureDataAssociationCollection getStructureDataCollection() {
		if (structureDataCollection == null) {
			associations[STRUCTURE_DATA_COLLECTION] =
					structureDataCollection = new IFDStructureDataAssociationCollection();
			structureDataCollection.setID("structure-spectra-associations");
		}
		return structureDataCollection;
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
			associations[STRUCTURE_DATA_ANALYSIS_COLLECTION] =
					structureDataAnalysisCollection = new IFDStructureDataAnalysisCollection();
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
	 * @param rootName   a prefix root to add to the _IFD_PROPERTY_COLLECTIONSET.json
	 *                   (or.xml) finding aid created
	 * @param products   optionally, a list of directories containing the files
	 *                   referenced by the finding aid for creating the
	 *                   IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	@Override
	public String createSerialization(File targetDir, String rootName, List<Object> products, IFDSerializerI serializer)
			throws IOException {
		if (serializer == null)
			serializer = new IFDDefaultJSONSerializer();
		// subclasses should be able to use this directly with no changes.
		String s = serializer.serialize(findingAid);
		if (targetDir == null)
			return s;
		String aidName = "IFD" + IFDConst.IFD_FINDINGAID_FLAG + serializer.getFileExt();
		if (products != null) {
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_REF, null);
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_LEN, null);
			// byte[] followed by entry name
			products.add(0, s.getBytes());
			products.add(1, aidName);
			String zipName = rootName + ".IFD" + IFDConst.IFD_COLLECTION_FLAG + "zip";
			String path = targetDir + "/" + zipName;
			long len = FAIRSpecUtilities.zip(path, targetDir.toString().length() + 1, products);
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_REF, zipName);
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_LEN, len);
			products.remove(1);
			products.remove(0);
			// update external finding aid
			s = serializer.serialize(findingAid);
		}
		String faPath = targetDir.toString().replace('\\', '/') + "/" + rootName + "." + aidName;
		FAIRSpecUtilities.writeBytesToFile(s.getBytes(), new File(faPath));
		return s;
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
	public IFDSampleStructureAssociation associateSampleStructure(IFDSample sample, IFDStructure struc) throws IFDException {
		return getSampleStructureCollection().addAssociation(sample, struc);
	}

	@Override
	public void finalizeExtraction() {
		for (int i = 0; i < objects.length; i++)
			if (objects[i] != null)
				findingAid.addCollection(objects[i]);
		for (int i = 0; i < associations.length; i++)
			if (associations[i] != null)
				findingAid.addCollection(associations[i]);
		findingAid.finalizeCollectionSet();
		dumpSummary();
	}

	protected void dumpSummary() {
		IFDCollectionSet cs = findingAid.getCollectionSet();
		int n = 0;
		String s = "";
		for (int i = 0; i < cs.size(); i++) {
			IFDCollection<IFDObject<?>> c = cs.get(i);
			for (Object sd : c) {
				System.out.println("\t" + sd);
				n++;
			}
			s += "## " + c.size() + " " + c.getLabel() + "\n";
		}
		if (n == 0)
			System.out.println("FAIRSpecExtractionHelper.dumpSummary no objects?");
		System.out.println("!FAIRSpecExtractionHelper.dumpSummary extraction complete:\n! " 
				+ getFindingAid().getResources() + "\n!\n"+ s);
		System.out.println("!FAIRSpecExtractionHelper.dumpSummary version " + IFDConst.getProp("IFD_VERSION"));
	}


}