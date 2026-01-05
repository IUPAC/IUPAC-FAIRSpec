package com.integratedgraphics.ifd.analysisobject.nmrml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.api.DataObjectVendorPluginI;
import com.integratedgraphics.ifd.analysisobject.DefaultAnalysisPlugin;
import com.integratedgraphics.ifd.analysisobject.NMRAnalysisPlugin;
import com.integratedgraphics.ifd.util.VendorUtils;
import com.integratedgraphics.ifd.dataobject.NMRVendorPlugin;

import jspecview.source.JDXReader;

public class NMRMLAnalysisObjectPlugin extends NMRAnalysisPlugin {

	static {
		register(com.integratedgraphics.ifd.analysisobject.nmrml.NMRMLAnalysisObjectPlugin.class);
	}
	
	@Override
	public String getAnalysisName() {
		return "NMRML";
	}




}