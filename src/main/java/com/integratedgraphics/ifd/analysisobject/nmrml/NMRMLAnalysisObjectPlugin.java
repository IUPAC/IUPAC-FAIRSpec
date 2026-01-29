package com.integratedgraphics.ifd.analysisobject.nmrml;

import com.integratedgraphics.ifd.analysisobject.NMRAnalysisPlugin;

public class NMRMLAnalysisObjectPlugin extends NMRAnalysisPlugin {

	static {
		register(com.integratedgraphics.ifd.analysisobject.nmrml.NMRMLAnalysisObjectPlugin.class);
	}
	
	@Override
	public String getAnalysisName() {
		return "NMRML";
	}




}