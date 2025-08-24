package com.integratedgraphics.extractor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.common.IFDUtil;

import com.integratedgraphics.extractor.DOICrawler.DOICustomizer;

/**
 * 
 * Crawler for ICL repository 10.14469/hpc/10386 (2024.12)
 * @author Bob Hanson
 *
 */

	public class ICLDOICrawler implements DOICustomizer {

		private static String[] ignoreURLs = new String[] {
//				"10.14469/hpc/14300",
//				"10.14469/HPC/11652",
//				"https://data.hpc.imperial.ac.uk/resolve/?doi=11597&file=1",
//				"https://data.hpc.imperial.ac.uk/resolve/?doi=11597&file=2"
		};		

		private static Map<String, String> hackMap = new HashMap<>();
		
		{
			hackMap.put("INCHI", DOICrawler.IFD_INCHI);
			hackMap.put("SMILES", DOICrawler.IFD_SMILES);
			hackMap.put("INCHIKEY", DOICrawler.IFD_INCHIKEY);
			hackMap.put("NMR_SOLVENT", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_solvent");
			hackMap.put("NMR_NUCLEUS", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl1");
			hackMap.put("NMR_NUCLEUS1", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl1");
			hackMap.put("NMR_NUCLEUS2", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_nucl2");
			hackMap.put("NMR_EXPT", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "nmr.expt_description");
			hackMap.put("IFD.IR", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "ir.description");
			hackMap.put("IFD.XRAY", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "xrd.description");
			hackMap.put("IFD.COMP", DOICrawler.FAIRSPEC_DATAOBJECT_FLAG + "comp.description");
		};
		
		protected DOICrawler crawler;

		private final static String[] defaultIgnoreKeys = {
				"compound.id",
				"IFD.property.dataobject.fairspec.comp.description",
				"IFD.property.dataobject.fairspec.xrd.description", 
				"IFD.property.dataobject.fairspec.ir.description", 
				"IFD.Comp.IR", 
				"IFD.Comp.NMR",
		};

		public ICLDOICrawler(DOICrawler crawler) {
			this.crawler = crawler;
			crawler.setIgnoreKeys(defaultIgnoreKeys);
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
				crawler.log("! RelatedIdentifier.References value ignored: " + val + " in " + crawler.doiRecord);
				break;
			case DOICrawler.DATACITE_SUBJECT:
				switch (val) {
				// from ccdc DOI
				case "Crystal Structure":
					crawler.setDataObjectType("xrd");
					break;
				}
				break;
			case DOICrawler.DATACITE_DESCRIPTION:
				break;
			case DOICrawler.DATACITE_TITLE:
				System.out.println("TITLE: " + val);
				switch (val.substring(0, 3)) {
				default:
					if (val.indexOf("-ray") >= 0 || val.indexOf("rystal") >= 0)
						crawler.setDataObjectType("xrd");
					break;
				case "IR ":
					crawler.setDataObjectType("ir");
					break;
				case "Com":
					break;
				case "NMR":
					crawler.setDataObjectType("nmr");
					break;
				}
				int pt = val.toLowerCase().indexOf("compound ");
				if (pt >= 0) {
					String id = crawler.newCompound("" + IFDUtil.parsePositiveInt(val.substring(pt + 9).replace('.',' ').trim()));
					crawler.addAttr(DOICrawler.FAIRSPEC_COMPOUND_ID, id);
//					String id = val.substring(pt + 9).replace('.', ' ') + " ";
//					id = id.substring(0, id.indexOf(" "));
//					id = crawler.newCompound(id);
//					crawler.addAttr(DOICrawler.FAIRSPEC_COMPOUND_ID, id);
//					String s = val.substring(pt + 9);
//					crawler.addAttr(IFDConst.IFD_PROPERTY_LABEL, s);
					return true;
				}
			}
			return false;
		}

	private static boolean debug = false;

	public static void main(String[] args) {
		if (debug) {
			args = new String[] { "10.14469/hpc/14635", "c:/temp/iupac/crawler"};
//			args = new String[] { "10.14469/hpc/14635", "c:/temp/iupac/crawler", "-insitu"};
//			args = new String[] { "10.14469/hpc/14635", "c:/temp/iupac/crawler", "-insitu", "-extractSpecProperies"};
		}
		if (args.length == 0) {
			System.out.println("format: java -jar ICLDOICrawler.jar <doi> <outputdir> <options>\nwhere <doi> is like \"10.14469/hpc/14635\",\nand options include one or more of: -insitu, -extractSpecProperties");
			return;
		}
		String strArgs = Arrays.toString(args);
		DOICrawler crawler = new DOICrawler(args);
		crawler.setCustomizer(new ICLDOICrawler(crawler));
		crawler.crawl();
		System.err.println("proccessed "+ strArgs);
		if (debug) {
			System.err.println("DEBUG IS TRUE!");
		}
	}
	
}
