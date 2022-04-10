package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.core.IFDCollectionSet;

/**
 * A class that identifies this as an IUPAC FAIRSpec Collection within the IUPAC
 * FAIRData Finding Aid.
 * 
 * It is specialized for spectroscopic data via the property and representations
 * described in the accompanying fairspec.properties file.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCollection extends IFDCollectionSet {

	public FAIRSpecCollection() {
		super(null, null);
	}

}
