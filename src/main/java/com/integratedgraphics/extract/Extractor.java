package com.integratedgraphics.extract;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.integratedgraphics.util.Util;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;

public class Extractor {


	private String extractVersion;
	private List<String> objects;

	public List<String> getObjectsForFile(File ifsExtractScript, File targetDir) throws IOException {
		System.out.println("Extracting " + ifsExtractScript.getAbsolutePath() + " to " + targetDir.getAbsolutePath());
		return getObjectsForStream(ifsExtractScript.toURI().toURL().openStream(), targetDir);
	}

	public List<String> getObjectsForStream(InputStream is, File targetDir) throws IOException {
		byte[] bytes = Util.getLimitedStreamBytes(is, -1, null, true);
		String script = new String(bytes);
		return objects = parseScript(script);
	}

	@SuppressWarnings("unchecked")
	private List<String> parseScript(String script) throws IOException {
		Map<String, Object> jp = (Map<String, Object>) new JSJSONParser().parse(script, false);
		System.out.println(jp);
		extractVersion = (String) jp.get("IFS-extract-version");
		System.out.println(extractVersion);
		List<Map<String, Object>> pathway = (List<Map<String, Object>>) jp.get("pathway");
		List<String> objects = getObjects(pathway);
		System.out.println(objects.size() + " objects found");
		return objects;
	}

	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 */
	private List<String> getObjects(List<Map<String, Object>> pathway) {
		
		//input:
		
		//		 {"IFS-extract-version":"0.1.0-alpha","pathway":[
		//         {"hash":"0c00571"},
		//         {"pubid":"acs.orglett.{hash}"},
		//         {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
		//         {"data":"{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
		//
		//         {"path":"{data}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}"},
		//         {"objects":"{path}/{IFS.structure.representation.mol.2d::{id}.mol}"},
		//         {"objects":"{path}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"},
		//         {"objects":"{path}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"},
		//        ]}
		
		//output:
		
		// [
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"
		// ]

		Lst<String> keys = new Lst<>();
		Lst<String> values = new Lst<>();
		List<String> objects = new ArrayList<>(); 
		for (int i = 0; i < pathway.size(); i++) {
			Map<String, Object> def = pathway.get(i);
			for (Entry<String, Object> e : def.entrySet()) {
				String key = e.getKey();
				String val = (String) e.getValue();
				if (val.indexOf("{") >= 0) {
					String s = PT.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						System.out.println(val+"\n"+s+"\n");
						e.setValue(s);
					}
					val = s;
				}
				if (key.equals("objects")) {
					objects.add("{IFS.finding.aid.object::" + val + "}");
				} else {
					keys.addLast("{" + key + "}");
					values.addLast(val);
				}
			}
		}
		return objects;
		
	}
	
	
	
	

}
