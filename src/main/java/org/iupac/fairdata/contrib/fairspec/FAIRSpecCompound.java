package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;

/**
 * A class that identifies this as an IFDStructureDataAssociation 
 * specifically of type type FAIRSpecCompound.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCompound extends IFDStructureDataAssociation {

	private final static String cmpdPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.getProp("FAIRSPEC_COMPOUND_FLAG"));

	@Override
	protected String getPropertyPrefix() {
		return cmpdPrefix;
	}

	protected FAIRSpecCompound() throws IFDException {
		super();
	}
}
