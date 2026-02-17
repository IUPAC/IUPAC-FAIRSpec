package com.integratedgraphics.ifd.analysisobject.nmredata;

import com.integratedgraphics.ifd.analysisobject.NMRAnalysisPlugin;

public class NMReDATAAnalysisObjectPlugin extends NMRAnalysisPlugin {

	static {
		register(com.integratedgraphics.ifd.analysisobject.nmredata.NMReDATAAnalysisObjectPlugin.class);
	}

	@Override
	public String getAnalysisName() {
		return "NMReDATA";
	}

}