package com.integratedgraphics.extractor;

/**
 * 
 * Second ICL repository crawler, for 10.14469/hpc/14635 (2025.04)
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
		return super.customizeText(key, val);
	}

	private static boolean debug = true;
	public static void main(String[] args) {		
//		String f = "c:\\temp\\iupac\\crawler\\10.14469_hpc_14635\\files\\14749\\5-H.zip";
//		try {
//			FileInputStream fis = new FileInputStream(f);
//			ZipInputStream zis = new ZipInputStream(fis);
//			ZipEntry e = zis.getNextEntry();
//			System.out.println(e);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		

		if (args.length == 0) {
		  System.out.println("java -jar ICLDOICrawler2.jar 10.14469/hpc/14635 <outputdir>");
		  if (debug) {
			  args = new String[] { "10.14469/hpc/14635", "c:/temp/iupac/crawler2" };
			  
			  //args = new String[] { "10.14469/hpc/10386", "c:/temp/iupac/crawler2" };
		  }
		}
		DOICrawler crawler = new DOICrawler(args);
		crawler.setCustomizer(new ICLDOICrawler2(crawler));
		crawler.crawl();
	}
}
