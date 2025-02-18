package com.integratedgraphics.extractor;

import org.iupac.fairdata.common.IFDConst;

/**
 * 
 * @author Bob Hanson
 *
 */

	public class ICLDOICrawler2 extends ICLDOICrawler {

		public ICLDOICrawler2(DOICrawler crawler) {
			super(crawler);
		}
		
		/**
		 * check ignore list -- note DOIs are case insensitive
		 */
		@Override
		public boolean ignoreURL(String url) {
			return super.ignoreURL(url);
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
		case DOICrawler.DATACITE_SUBJECT:
		case DOICrawler.DATACITE_DESCRIPTION:
			return super.customizeText(key, val);
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
			case "NMR":
				crawler.setDataObjectType("nmr");
				int pt = val.toLowerCase().indexOf("compound ");
				if (pt > 0) {
					String id = val.substring(pt + 9, val.indexOf('.', pt));
					id = crawler.newCompound(id);
					crawler.addAttr(DOICrawler.FAIRSPEC_COMPOUND_ID, id);
					String s = val.substring(pt + 9);
					crawler.addAttr(IFDConst.IFD_PROPERTY_LABEL, s);
					return true;
				}
				break;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
		  System.out.println("java -jar ICLDOICrawler2.jar 10.14469/hpc/14635 <outputdir>");
		}
		DOICrawler crawler = new DOICrawler(args);
		crawler.setCustomizer(new ICLDOICrawler2(crawler));
		crawler.crawl();
	}
}
