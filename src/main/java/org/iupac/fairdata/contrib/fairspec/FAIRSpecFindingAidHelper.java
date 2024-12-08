package org.iupac.fairdata.contrib.fairspec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDCollectionSet;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.core.IFDResource;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.derived.IFDSampleDataAssociationCollection;
import org.iupac.fairdata.derived.IFDSampleStructureAssociation;
import org.iupac.fairdata.derived.IFDSampleStructureAssociationCollection;
import org.iupac.fairdata.derived.IFDStructureDataAnalysisCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.structure.IFDStructureRepresentation;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

import com.integratedgraphics.extractor.DOIInfoExtractor;

/**
 * 
 * This class is tailored to the task of creating an IUPAC FAIRData Collection
 * and its associated IUPAC FAIRData Finding Aid 
 * from scratch, not from zipped data aggregations.
 * 
 * This class is used by DoiCrawler.
 * 
 * It is currently under development and should NOT be considered to be a an
 * IUPAC standard.
 * 
 * 
 * It works by linear iterative sequence of 
 * 
 * 1) creating a compound association
 * 2) adding structures and spectral representations
 * 
 * followed 
 * 
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class FAIRSpecFindingAidHelper implements FAIRSpecFindingAidHelperI {

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


	static {
		FAIRSpecFindingAid.loadProperties();
	}

	public final static int SAMPLE_COLLECTION = 0;
	public final static int STRUCTURE_COLLECTION = 1;
	public final static int DATA_COLLECTION = 2;

	public final static int SAMPLE_STRUCTURE_COLLECTION = 0;
	public final static int SAMPLE_DATA_COLLECTION = 1;
	public final static int STRUCTURE_DATA_COLLECTION = 2;
	public final static int SAMPLE_DATA_ANALYSIS_COLLECTION = 3;
	public final static int STRUCTURE_DATA_ANALYSIS_COLLECTION = 4;

	protected FAIRSpecFindingAid findingAid;

	
	protected IFDStructureCollection structureCollection;
	protected IFDDataObjectCollection dataObjectCollection;
	protected FAIRSpecCompoundCollection compoundCollection;
	protected IFDStructureDataAnalysisCollection structureDataAnalysisCollection;
	protected IFDSampleCollection sampleCollection;
	protected IFDSampleDataAssociationCollection sampleDataCollection;
	protected IFDSampleStructureAssociationCollection sampleStructureCollection;

	protected IFDStructure currentStructure;
	protected IFDDataObject currentDataObject;
	protected IFDAssociation currentAssociation;
	protected IFDSample currentSample;
	protected IFDResource currentResource;

	/**
	 * temporary holding arrays separating objects from associations so that they
	 * can be added later to the finding aid in a desired order.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] objects = new IFDCollection[3];
	@SuppressWarnings("rawtypes")
	protected IFDCollection[] associations = new IFDCollection[5];

	private FAIRSpecCompoundAssociation thisCompound;


	public FAIRSpecFindingAidHelper(String creator) {
		try {
		findingAid = new FAIRSpecFindingAid(null, null, creator);
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * Set associations to be listed by ID, not by index.
	 */
	protected boolean byId;

	@Override
	public void setById(boolean tf) {
		byId = tf;
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
	 * For Phase 2b This list will grow.
	 * 
	 * @param key
	 * @return
	 * @throws IFDException
	 */
	public static String getObjectTypeForPropertyOrRepresentationKey(String key, boolean allowError) throws IFDException {
		if (IFDConst.isIFDProperty(key))
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
			return FAIRSpecFindingAidHelper.ClassTypes.Structure;
		if (key.startsWith("\0sample."))
			return FAIRSpecFindingAidHelper.ClassTypes.Sample;
		if (key.startsWith("\0fairspec.compound."))
			return FAIRSpecFindingAidHelper.ClassTypes.Compound;
		if (key.startsWith("\0association.sampledata"))
			return FAIRSpecFindingAidHelper.ClassTypes.SampleDataAssociation;
		if (key.startsWith("\0analysis.structuredata"))
			return FAIRSpecFindingAidHelper.ClassTypes.StructureDataAnalysis;
		if (key.startsWith("\0analysis.sampledata"))
			return FAIRSpecFindingAidHelper.ClassTypes.SampleDataAnalysis;
		if (key.startsWith("\0dataobject.fairspec.")) {
			// adds next part, e.g "nmr"
			return key.replace('\0', '.').substring(0, key.indexOf(".", 21));
		}
		if (key.startsWith("\0dataobject."))
			return FAIRSpecFindingAidHelper.ClassTypes.DataObject;
		return "Unknown";
	}

	@Override
	public FAIRSpecFindingAid getFindingAid() {
		return findingAid;
	}
	
	@Override
	public String addRelatedInfo(String doi, boolean addPublicationMetadata, List<Map<String, Object>> list,
			String type) throws IOException {
		Map<String, Object> info = DOIInfoExtractor.getPubInfo(doi, addPublicationMetadata, type);
		if (info == null || info.get("metadataSource") == null)
			return "Could not access " + DOIInfoExtractor.getMetadataUrl(doi, type);
		list.add(info);
		findingAid.setRelatedTo(list);
		return null;
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
	public FAIRSpecCompoundAssociation createCompound(String id) throws IFDException {
		FAIRSpecCompoundAssociation c = createCompound(null, null);
		if (id != null)
			c.setPropertyValue(IFDConst.IFD_PROPERTY_ID, id);
		currentAssociation = thisCompound = c;
		getCompoundCollection().add(c);
		currentStructure = null;
		currentDataObject = null;
		return c;
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
			associations[SAMPLE_STRUCTURE_COLLECTION] = sampleStructureCollection = new IFDSampleStructureAssociationCollection(
					byId);
			sampleStructureCollection.setID("sample-structure associations");
		}
		return sampleStructureCollection;
	}

	public IFDSampleDataAssociationCollection getSampleDataCollection() {
		if (sampleDataCollection == null) {
			associations[SAMPLE_DATA_COLLECTION] = sampleDataCollection = new IFDSampleDataAssociationCollection(
					byId);
			sampleDataCollection.setID("sample-spectra associations");
		}
		return sampleDataCollection;
	}

	@Override
	public FAIRSpecCompoundCollection getCompoundCollection() {
		if (compoundCollection == null) {
			associations[STRUCTURE_DATA_COLLECTION] = compoundCollection = new FAIRSpecCompoundCollection(
					byId);
			compoundCollection.setID("compounds");
		}
		return compoundCollection;
	}

	public IFDStructureDataAnalysisCollection getStructureDataAnalysisCollection() {
		if (structureDataAnalysisCollection == null)
			associations[STRUCTURE_DATA_ANALYSIS_COLLECTION] = structureDataAnalysisCollection = new IFDStructureDataAnalysisCollection(
					byId);
		structureDataAnalysisCollection.setID("structure-spectra-analyses");
		return structureDataAnalysisCollection;
	}

	
	@Override
	public IFDStructure createStructure(String id) {
		currentStructure = new IFDStructure();
		if (id != null)
			currentStructure.setID(id);
		if (thisCompound != null)
			thisCompound.addStructure(currentStructure);
		getStructureCollection().add(currentStructure);
		return currentStructure;
	}

	
	@Override
	public IFDStructureRepresentation createStructureRepresentation(IFDReference ref, Object data, long len,
			String ifdStructureType, String mediatype) {
		if (currentStructure == null)
			currentStructure = createStructure(null);
		IFDStructureRepresentation r = (IFDStructureRepresentation) findOrAddRepresentation(currentStructure, null, null, null, ref == null ? null : ref.getLocalName(), data, null);
		if (data == null) {
			r.getRef().setDOI(ref.getDOI());
			r.getRef().setURL(ref.getURL());
		}
		r.setMediaType(mediatype);
		r.setType(ifdStructureType);
		r.setLength(len);
		return r;
	}

	@Override
	public IFDDataObject createDataObject(String id, String type) {
		IFDDataObject o = FAIRSpecDataObject.createFAIRSpecObject(type);
		if (thisCompound != null)
			thisCompound.addDataObject(o);
		getSpecCollection().add(o);
		return currentDataObject = o;
	}

	@Override
	public IFDDataObjectRepresentation createDataObjectRepresentation(IFDReference ref, Object data, long len,
			String ifdDataType, String mediatype) {
		if (currentDataObject == null) {
			System.out.println("no data object for " + ref);
			return null;
		}
		IFDDataObjectRepresentation r = (IFDDataObjectRepresentation) findOrAddRepresentation(currentDataObject, null, null, null, ref == null ? null : ref.getLocalName(), data, null);
		r.setRef(ref);
		r.setMediaType(mediatype);
		r.setType(ifdDataType);
		r.setLength(len);
		return r;
	}

	@Override
	public FAIRSpecCompoundAssociation findCompound(IFDStructure struc, IFDDataObject spec) {
		return (FAIRSpecCompoundAssociation) getCompoundCollection().findAssociation(struc, spec);
	}

	@Override
	public IFDSampleDataAssociation associateSampleSpec(IFDSample sample, IFDDataObject spec) throws IFDException {
		return (IFDSampleDataAssociation) getSampleDataCollection().addAssociation(sample, spec);
	}

	public IFDSampleDataAssociation getSampleAssociation(IFDSample struc, IFDDataObject spec) {
		return (IFDSampleDataAssociation) getSampleDataCollection().findAssociation(struc, spec);
	}

	@Override
	public IFDRepresentation getSpecDataRepresentation(String localizeName) {
		return (dataObjectCollection == null ? null : dataObjectCollection.getRepresentation(currentResource.getID(), localizeName));
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
//
//	public IFDSampleDataAnalysisCollection getSampleDataAnalysisCollection() {
//		if (sampleDataAnalysisCollection == null)
//			associations[SAMPLE_DATA_ANALYSIS_COLLECTION] =
//					sampleDataAnalysisCollection = new IFDSampleDataAnalysisCollection("sampleDataAnalyses");
//		return sampleDataAnalysisCollection;
//	}

	/**
	 * 
	 * Generate the serialization and optionally save it to disk as
	 * _IFD_PROPERTY_COLLECTIONSET.[ext] and optionally create an
	 * _IFD_collection.zip in that same directory.
	 * 
	 * @param targetDir  or null for no output
	 * @param products   optionally, a list of directories containing the files
	 *                   referenced by the finding aid for creating the
	 *                   IFD_collection.zip file
	 * @param serializer optionally, a non-default IFDSerializerI (XML, JSON, etc.)
	 * @return the serialization as a String
	 * @throws IOException
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String createSerialization(File targetDir, ArrayList<Object> products, IFDSerializerI serializer, long[] t) throws IOException {
		if (serializer == null)
			serializer = new IFDDefaultJSONSerializer(byId);
		// subclasses should be able to use this directly with no changes.
		if (t == null) {
			t = new long[3];
		}
		t[0] = System.currentTimeMillis();
		String serializedFindingAid = serializer.serialize(findingAid).toString();
		t[0] = System.currentTimeMillis() - t[0];
		
		if (targetDir == null)
			return serializedFindingAid;
		
		String aidName = "IFD" + IFDConst.IFD_FINDINGAID_FLAG + serializer.getFileExt();
		String faPath = targetDir.toString().replace('\\', '/') + "/" + aidName;
		FAIRSpecUtilities.writeBytesToFile(serializedFindingAid.getBytes(), new File(faPath));
		System.out.println("created " + faPath);
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

	public void finalizeObjects() {
		if (objects == null)
			return;
		for (int i = 0; i < objects.length; i++)
			if (objects[i] != null)
				findingAid.addCollection(objects[i]);
		for (int i = 0; i < associations.length; i++)
			if (associations[i] != null)
				findingAid.addCollection(associations[i]);
		objects = null;
		associations = null;
	}


	public String dumpSummary() {
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

	public void finalizeCollectionSet(Map<String, Map<String, Object>> htURLReferences) {
		findingAid.finalizeCollectionSet(htURLReferences);
	}

	/**
	 * not for Extractor; only for creating finding aid from an archive
	 * or otherwise directly
	 * 
	 * @param fileDir
	 * @return
	 * @throws IOException
	 */
	@Override
	public String generateFindingAid(File fileDir) throws IOException {
		finalizeObjects();
		finalizeCollectionSet(null);
		return createSerialization(fileDir, null, null, null);
	}

	/**
	 * 
	 * @param collection
	 * @param resourceID
	 * @param currentOriginPath
	 * @param rootPath
	 * @param localName
	 * @param object
	 * @param param
	 * @return
	 */
	public IFDRepresentation findOrAddRepresentation(IFDRepresentableObject<? extends IFDRepresentation> collection, 
			String resourceID,
			String currentOriginPath, String rootPath, 
			String localName, Object object, String param) {
		return collection.findOrAddRepresentation(resourceID, currentOriginPath, 
				rootPath, localName, object, param, FAIRSpecUtilities.mediaTypeFromFileName(localName));
	}

	/**
	 * adjust key for backward compatibility.
	 * 
	 * @param key
	 * @return
	 */
	public static String updateKey(String key) {
		int pt = key.indexOf(".xray.");
		if (pt >= 0)
			key = key.substring(0, pt + 1) + "xrd" + key.substring(pt + 5);
		return key;
	}

}