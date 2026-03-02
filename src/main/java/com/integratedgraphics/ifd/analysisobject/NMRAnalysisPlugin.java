package com.integratedgraphics.ifd.analysisobject;

import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.api.AnalysisObjectPluginI;

public class NMRAnalysisPlugin extends DefaultAnalysisPlugin {

	protected final static String IFD_REP_ANALYSISOBJECT_FAIRSPEC_NMR_ANALYSIS = getProp(
			"IFD_REP_ANALYSISOBJECT_FAIRSPEC_NMR.ANALYSIS");

	protected final static String IFD_PROPERTY_ANALYSISOBJECT_FAIRSPEC_NMR_ANALYSIS_TYPE = getProp(
			"IFD_PROPERTY_ANALYSISOBJECT_FAIRSPEC_NMR.ANALYSIS_TYPE");


	
	@Override
	public void setExtractor(MetadataReceiverI extractor) {
		this.extractor = extractor;
	}

	@Override
	public String getParamRegex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean doExtract(String entryName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVendorDataSetKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAnalysisName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeAnalysisObject(MetadataReceiverI extractor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setIndex(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void reportAnalysis() {
		addProperty(IFD_PROPERTY_ANALYSISOBJECT_FAIRSPEC_NMR_ANALYSIS_TYPE, getAnalysisName());
	}

	/**
	 * Pass back the standardized key/val pair to the IFDMetadataReceiverI class.
	 * 
	 * @param key
	 * @param val a String or Double; IFD.Property.NULL to remove value
	 */
	public void addProperty(String key, Object val) {
		if (val == null || extractor == null)
			return;
		if (val instanceof Double) {
			if (Double.isNaN((Double) val))
				return;
		} else if (val instanceof String && val != IFDProperty.NULL) {
			val = ((String) val).trim();
			if (((String) val).indexOf('\r') >= 0) {
				val = ((String) val).replace('\r', '\n').replace("\n\n", "\n ");
			}
		}
		extractor.addProperty(key, val);
	}


}
