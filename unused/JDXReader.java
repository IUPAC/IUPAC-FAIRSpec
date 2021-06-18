package com.vendor.jcampdx;

import jspecview.exception.JSVException;
import jspecview.source.GenericJDXDataObject;
import jspecview.source.GenericJDXReader;

public class JDXReader extends GenericJDXReader {

	private int lastSpec;
	private int nSpec;

	public JDXReader(String filePath, int iSpecFirst, int iSpecLast) {
		super(filePath, false, iSpecFirst, 1F, false);
	    lastSpec = iSpecLast;
	}

	@Override
	protected boolean checkCustomTags(GenericJDXDataObject spectrum, String label, String value) throws JSVException {
		return false;
	}

	@Override
	protected boolean addSpectrum(GenericJDXDataObject spectrum, boolean forceSub) {
	    if (!Float.isNaN(nmrMaxY))
	        spectrum.doNormalize(nmrMaxY);
	      else if (spectrum.getMaxY() >= 10000)
	        spectrum.doNormalize(1000);
	      nSpec++;
	      if (firstSpec > 0 && nSpec < firstSpec)
	        return true;
	      if (lastSpec > 0 && nSpec > lastSpec)
	        return !(done = true);
	      spectrum.setBlockID(blockID);
	      source.addJDXSpectrum(null, spectrum, forceSub);
	      return true;
	}
	
}