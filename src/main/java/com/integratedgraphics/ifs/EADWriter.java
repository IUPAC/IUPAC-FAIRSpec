package com.integratedgraphics.ifs;

import java.io.File;

import org.iupac.fairspec.api.IFSFindingAidWriterI;
import org.iupac.fairspec.object.IFSFindingAid;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 */

public class EADWriter implements IFSFindingAidWriterI {

	private File targetDir;
	private String rootPath;

	public EADWriter(File targetDir, String rootPath, IFSFindingAid aid) {
		this.targetDir = targetDir;
		this.rootPath = rootPath;
		System.out.println(aid.getURLs());
		
	}

	public void write() {
		// TODO Auto-generated method stub
		
	}
	
}