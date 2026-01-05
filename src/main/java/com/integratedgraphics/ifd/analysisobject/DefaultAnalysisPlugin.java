package com.integratedgraphics.ifd.analysisobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.api.AnalysisObjectPluginI;

public abstract class DefaultAnalysisPlugin implements AnalysisObjectPluginI {

	protected static void register(Class<? extends DefaultAnalysisPlugin> plugin) {
		AnalysisObjectPluginI.registerAnalysisPlugin(plugin);
	}

	private int index;
	protected MetadataReceiverI extractor;
	private boolean processing;

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
	public boolean isDerived() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public abstract String getAnalysisName();

	@Override
	public void initializeAnalysisObject(MetadataReceiverI extractor) {
		this.extractor = extractor;
		reportAnalysis();
		processing = true;
	}

	public abstract void reportAnalysis();

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public Object[] getExtractTypeInfo(MetadataReceiverI extractor, String baseName, String entryName) {
		return null;
	}

	protected static String getProp(String name) {
		return IFDConst.getProp(name);
	}

}
