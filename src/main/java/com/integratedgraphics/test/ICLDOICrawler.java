package com.integratedgraphics.test;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDUtil;

import com.integratedgraphics.extractor.DOICrawler;

/**
 * 
 * @author Bob Hanson
 *
 */

public class ICLDOICrawler extends DOICrawler {


	public static class ICLCustomizer implements DOICustomizer {

		private static String[] ignoreURLs = new String[] {
//				"10.14469/hpc/14300",
//				"10.14469/HPC/11652",
//				"https://data.hpc.imperial.ac.uk/resolve/?doi=11597&file=1",
//				"https://data.hpc.imperial.ac.uk/resolve/?doi=11597&file=2"
		};		

		private static Map<String, String> hackMap = new HashMap<>();
		
		{
			hackMap.put("INCHI", IFD_INCHI);
			hackMap.put("SMILES", IFD_SMILES);
			hackMap.put("INCHIKEY", IFD_INCHIKEY);
			hackMap.put("NMR_SOLVENT", FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_solvent");
			hackMap.put("NMR_NUCLEUS", FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl1");
			hackMap.put("NMR_NUCLEUS1", FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl1");
			hackMap.put("NMR_NUCLEUS2", FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl2");
			hackMap.put("NMR_EXPT", FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_name");
			hackMap.put("IFD.IR", FAIRSPEC_DATAOBJECT_FLAG + "ir.description");
			hackMap.put("IFD.XRAY", FAIRSPEC_DATAOBJECT_FLAG + "xrd.description");
			hackMap.put("IFD.COMP", FAIRSPEC_DATAOBJECT_FLAG + "comp.description");
		};
		
		private ICLDOICrawler crawler;

		public ICLCustomizer(DOICrawler crawler) {
			this.crawler = (ICLDOICrawler) crawler;
		}
		
		/**
		 * check ignore list -- note DOIs are case insensitive
		 */
		@Override
		public boolean ignoreURL(String url) {
			for (int i = ignoreURLs .length; --i >= 0;) {
				if (url.equalsIgnoreCase(ignoreURLs[i]))
					return true;
			}
			return false;
		}

		/**
		 * return mapped key or key
		 */
		@Override
		public String customizeSubjectKey(String key) {
			String mappedKey = hackMap.get(key.toUpperCase());
			return (mappedKey == null ? key : mappedKey);
		}

		/**
		 * Process a hack for ICL preliminary repository files.
		 * 
		 * description DOI:.... to relatedIdentifier
		 * 
		 * title "Compound xx:..." adds subject IFD.property.fairspec.compound.id
		 * 
		 * @param key
		 * @param val
		 * @return
		 */
		@Override
		public boolean customizeText(String key, String val) {
			if (val.length() < 3)
				return false;
			switch (key) {
			case "References":
				crawler.addError("!RelatedIdentifier.References value ignored: " + val + " in " + crawler.doiRecord);
				break;
			case DATACITE_SUBJECT:
				switch (val) {
				// from ccdc DOI
				case "Crystal Structure":
					crawler.setDataObjectType("xrd");
					break;
				}
				break;
			case DATACITE_DESCRIPTION:
				break;
			case DATACITE_TITLE:
				switch (val.substring(0, 3)) {
				default:
					if (val.indexOf("-ray") >= 0 || val.indexOf("rystal") >= 0)
						crawler.setDataObjectType("xrd");
					break;
				case "IR ":
					crawler.setDataObjectType("ir");
					break;
				case "Com":
					if (val.startsWith("Compound ")) {
						String id = crawler.newCompound("" + IFDUtil.parsePositiveInt(val.substring(9)));
						crawler.addAttr(FAIRSPEC_COMPOUND_ID, id);
					}
					break;
				}
			}
			return false;
		}

	}

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { TEST_PID, DEFAULT_OUTDIR, };//"-dodownload" };
//			args = new String[] { "10.14469/hpc/14443" , DEFAULT_OUTDIR, "-dodownload -bycompound" };
		}
		ICLDOICrawler crawler = new ICLDOICrawler();
		crawler.setCustomizer(new ICLCustomizer(crawler));
		crawler.crawl();
	}
}
