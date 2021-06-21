package org.iupac.fairspec.api;

public interface IFSPropertyManagerI {

	String getParamRegex();

	boolean doExtract(String entryName);

	boolean accept(IFSExtractorI extractor, String fname, byte[] bytes);

	String getDatasetType(String refName);

	static void addProperty(IFSExtractorI extractor, String key, Object val) {
		if (val == null)
			return;
		System.out.println(key + " = " + val);
		if (extractor != null)
			extractor.addProperty(key, val);
	}


}
