package com.integratedgraphics.ifs;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSJSONDefaultSerializer;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: 
 * <code>
	acs.joc.0c00770
	acs.orglett.0c00571
	acs.orglett.0c00624
	acs.orglett.0c00755
	acs.orglett.0c00788
	acs.orglett.0c00874
	acs.orglett.0c00967
	acs.orglett.0c01022
	acs.orglett.0c01043
	acs.orglett.0c01153
	acs.orglett.0c01197
	acs.orglett.0c01277
	acs.orglett.0c01297
 </code>
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	public ExtractorTest(String key, File ifsExtractScriptFile, File targetDir, String localSourceDir) throws IOException, IFSException {
		// first create super.objects, a List<String>
		
		super.initialize(ifsExtractScriptFile);
		
		// now actually do the extraction.
		
		if (localSourceDir != null)
			super.setLocalSourceDir(localSourceDir);
		
		super.setCachePattern(null);
		super.setRezipCachePattern(null, null);
	
		//IFSSpecDataFindingAid aid = 
				super.extractObjects(targetDir);
				
		System.out.println(
				"extracted " + manifestCount + " files (" + extractedByteCount + " bytes)"
				+ "; ignored " + ignoredCount + " files (" + ignoredByteCount + " bytes)");
//		System.out.println(aid.getURLs() + " " + aid.getParams());
//		IFSStructureSpecCollection ssc = aid.getStructureSpecCollection();
//		new EADWriter(targetDir, rootPath, aid).write();
		String s = new IFSJSONDefaultSerializer().serialize(findingAid);
		writeStringToFile(s, new File(targetDir + "/" + (key == null ? "" : key + ".") + "_IFS_findingaid.json"));
	}


	/**
	 * ACS/FigShare codes   /21947274  
	 * 
	 * for example: https://ndownloader.figshare.com/files/21947274
	 */
	private final static String[] testSet = {
			"acs.orglett.0c01153/22284726,22284729", // 9 bruker dirs
			"acs.joc.0c00770/22567817",  // 0 727 files; zips of bruker dirs + mnovas
			"acs.orglett.0c00624/21947274", // 1 1143 files; MANY bruker dirs
			"acs.orglett.0c00788/22125318", // 2 jdfs
			"acs.orglett.0c00874/22233351", // 3 bruker dirs
			"acs.orglett.0c00967/22111341", // 4 bruker dirs + jdfs
			"acs.orglett.0c01022/22195341", // 5  many mnovas
			"acs.orglett.0c01197/22491647", // 6 many mnovas
			"acs.orglett.0c01277/22613762", // 7 bruker dirs
			"acs.orglett.0c01297/22612484", // 8 bruker dirs
			// these next four are too large to save at GitHub (> 100 MB)
			//"acs.orglett.0c00571/21975525", // 1 3212 files; zips of bruker zips and HRMS
			//"acs.orglett.0c00755/22150197", // 3 MANY bruker dirs
			//"acs.orglett.0c01043/22232721", // 8  single 158-MB mnova
	};
	
	public static void main(String[] args) {

		int i0 = 0;
		int i1 = 0; // 12 max
		
		debugging = false;//true; // verbose listing of all files
		
		int failed = 0;
		
		for (int itest = (args.length == 0 ? i0 : 0); 
				itest <= (args.length == 0 ? i1 : 0); 
				itest++) {
			
//		String s = "test/ok/1c.nmr";
//		Pattern p = Pattern.compile("^\\Qtest/ok/\\E(.+)\\Q.nmr\\E");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find());
//		String v = m.group(1);
//		
			
//		
			String key = null;
			String script, targetDir = null, sourceDir = null;
			switch (args.length) {
			default:
			case 3:
				sourceDir = args[2];
			case 2:
				targetDir = args[1];
			case 1:
				script = args[0];
				break;
			case 0:
				key = testSet[itest];
				System.out.println("\n!\n! found Test " + itest + " " + key);
				int pt = key.indexOf("/");
				if (pt >= 0)
					key = key.substring(0, pt);
				script = "./extract/" + key + "/IFS-extract.json";
				sourceDir = "file:///c:/temp/iupac/zip";
				targetDir = "c:/temp/iupac/ifs";
			}

			// ./extract/ should be in the main Eclipse project directory.

			try {
				new ExtractorTest(key, new File(script), new File(targetDir), sourceDir);
			} catch (Exception e) {
				failed++;
				System.err.println("Exception " + e + " for test " + itest);
				e.printStackTrace();
			}

		}
		System.out.println ("DONE failed=" + failed);
	}
	
//	found Test 0 acs.joc.0c00770/22567817
//	2 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22567817}|{id=IFS.structure.param.compound.id::*}.zip|{IFS.spec.nmr.representation.vendor.dataset::{id}_{IFS.spec.nmr.param.expt::*}/}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22567817}|{id=IFS.structure.param.compound.id::*}.zip|{IFS.spec.nmr.representation.vendor.dataset::{id}_mnova}}
//	found 727 files
//	found Test 1 acs.orglett.0c00571/21975525
//	3 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}-NMR.zip}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|**/{IFS.spec.hrms.representation.pdf::*.pdf}}
//	found 3212 files
//	found Test 2 acs.orglett.0c00624/21947274
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/21947274}|primary NMR data/{id=IFS.structure.param.compound.id::*}/{IFS.spec.nmr.representation.vendor.dataset::{id}-{IFS.spec.nmr.param.expt::*}/}}
//	found 1143 files
//	found Test 3 acs.orglett.0c00755/22150197
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22150197}|NMR/{id=IFS.structure.param.compound.id::*}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}/}}
//	found 2150 files
//	found Test 4 acs.orglett.0c00788/22125318
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22125318}|primary NMR data/{IFS.spec.nmr.representation.vendor.dataset::{IFS.structure.param.compound.id::*}-{IFS.spec.nmr.param.expt::*}.jdf}}
//	found 82 files
//	found Test 5 acs.orglett.0c00874/22233351
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22233351}|NMR FID files/{id=IFS.structure.param.compound.id::*}/{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}/}*}
//	found 1598 files
//	found Test 6 acs.orglett.0c00967/22111341
//	2 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22111341}|{id=IFS.structure.param.compound.id::*}//{IFS.spec.nmr.representation.vendor.dataset::{id}-{IFS.spec.nmr.param.expt::*}/}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22111341}|{id=IFS.structure.param.compound.id::*}/{IFS.spec.nmr.representation.vendor.dataset::{id}-{IFS.spec.nmr.param.expt::*}.jdf}}
//	found 1354 files
//	found Test 7 acs.orglett.0c01022/22195341
//	2 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22195341}|NMR DATA/product/{IFS.spec.nmr.representation.vendor.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.spec.nmr.param.expt::*}.mnova}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22195341}|NMR DATA/starting material/{IFS.spec.nmr.representation.vendor.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.spec.nmr.param.expt::*}.mnova}}
//	found 66 files
//	found Test 8 acs.orglett.0c01043/22232721
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22232721}|metadataNMR/{IFS.spec.nmr.representation.vendor.dataset::NMR spectra.mnova}}
//	found 1 files
//	found Test 9 acs.orglett.0c01153/22284726,22284729
//	4 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22284726}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}/}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22284726}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.structure.representation.cdx::*.cdx}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22284729}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.spec.nmr.representation.vendor.dataset::{IFS.spec.nmr.param.expt::*}/}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22284729}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.structure.representation.cdx::*.cdx}}
//	found 3476 files
//	found Test 10 acs.orglett.0c01197/22491647
//	2 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22491647}|NMR DATA/products/{IFS.spec.nmr.representation.vendor.dataset::{id=IFS.structure.param.compound.id::*}.mnova}}
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22491647}|NMR DATA/Substrate/{IFS.spec.nmr.representation.vendor.dataset::{id=IFS.structure.param.compound.id::*}.mnova}}
//	found 61 files
//	found Test 11 acs.orglett.0c01277/22613762
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.spec.nmr.representation.vendor.dataset::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22613762}|alkylation original FID submitted/{id=IFS.structure.param.compound.id::*}-{IFS.spec.nmr.param.expt::*}/**/*}}
//	found 5372 files
//	found Test 12 acs.orglett.0c01297/22612484
//	1 objects found
//	found object {IFS.findingaid.object::{IFS.findingaid.source.data.uri::https://ndownloader.figshare.com/files/22612484}|NMR fids/{IFS.spec.nmr.representation.vendor.dataset::{id=IFS.structure.param.compound.id::*}/{IFS.spec.nmr.param.expt::*}/}}
//	found 1495 files

	
//FigShare searching:
//import requests as rq
//from pprint import pprint as pp
//
//HEADERS = {'content-type': 'application/json'}
//
//r = rq.post('https://api.figshare.com/v2/articles/search', data='{"search_for": "university of sheffield", "page_size":20}', headers=HEADERS)
//print(r.status_code)
//results = r.json()
//pp(results)
	
}