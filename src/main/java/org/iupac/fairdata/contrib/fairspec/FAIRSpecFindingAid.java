package org.iupac.fairdata.contrib.fairspec;

import java.io.File;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDFindingAid;

/**
 * A class used by IFDFAIRSpecHelper to identify this as an IUPAC FAIRData
 * Finding Aid that is specialized for spectroscopic data.
 * 
 * This finding aid adds citations and the type for the IFDFindingAid.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecFindingAid extends IFDFindingAid {

	static boolean propertiesLoaded;
	
	public static void loadProperties() {
		if (!propertiesLoaded) {
			File f = new File(FAIRSpecFindingAid.class.getName().replace('.', '/'));
			String propertyFile = f.getParent().replace('\\', '/') + "/fairspec.properties";
			IFDConst.addProperties(propertyFile);
			propertiesLoaded = true;
		}
	}

	static {
		loadProperties();
	}
	
	public FAIRSpecFindingAid(String label, String type, String creator) throws IFDException {
		super(label, type, creator, null);
		add(new FAIRSpecCollection());
	}

	@Override
	public String getVersion() {
		return super.getVersion() + ";FAIRSpec " + IFDConst.getProp("FAIRSPEC_VERSION") ;
	}
	

}
