package org.iupac.fairspec.spec.nmr;

import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSProperty;
import org.iupac.fairspec.common.IFSSpecData;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSNMRSpecData extends IFSSpecData {


	{
		super.setProperties(new IFSProperty[] {
				new IFSProperty("nmr.dimension", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.NONE),
				new IFSProperty("nmr.nominalFreq1", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nominalFreq2", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nominalFreq3", IFSConst.PROPERTY_TYPE.INT, IFSConst.UNITS.MHZ),
				new IFSProperty("nmr.nucleus1", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE), 
				new IFSProperty("nmr.nucleus2", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE),
				new IFSProperty("nmr.nucleus3", IFSConst.PROPERTY_TYPE.NUCL, IFSConst.UNITS.NONE) 
		});
	}
}
