// ${IUPAC FAIRSpec}/src/html/site/FAIRSpec-config.js
// 
// Bob Hanson hansonr@stolaf.edu 2023.01.19

IFD = {		
	properties:{
		baseDir: ".",
		aLoading: null,
		corsOK: null,
		findingAidPath: "./",
		findingAidFileName: "IFD.findingaid.json",
		standalone: true, // we DO NOT have actual data in this demo - all is coming from remote sources in the Finding Aid 
		jmeDiv: "jme-app-frame0-div",
		readyFunction : function(){IFD.jmolReadyCallback()},
		imageDimensions:{width:40,height:40},
		MAX_IMAGE_DIMENSIONS: {width:400, height:400}
	},
	collections: {},
	collectionsKeys: null,
	byID: true,
	itemsKey: "itemsByID",
	items: {},
	aidIDs: [],
	headers: [],
	mainMode: "none",
	resultsMode: "none",
	findingAid: null,
	JME: null,
	smarts: null, // just for reference
	canvas: null,
	contentHeader: null,
	cache: {},
	imageSet: new Set()
}

IFD.MODE_NONE       = "none";
IFD.MODE_COMPOUNDS  = "compounds";
IFD.MODE_STRUCTURES = "structures";
IFD.MODE_SPECTRA    = "spectra";
IFD.MODE_SAMPLES  = "samples";
IFD.MODE_SAMPLESPECTRA = "samplespectra";
IFD.searchType = IFD.MODE_NONE;


IFD.configSetFindingAidPath = function(url, base) {
	base || (base = IFD.properties.baseDir);
	if (!base.endsWith("/"))
		base += '/';
	IFD.findingAidURL = url;
	IFD.properties.findingAidPath = (url ? url.substring(0, url.lastIndexOf("/") + 1) : "./");
	IFD.properties.baseDir = (base.startsWith(".") ? IFD.properties.findingAidPath + base : base);
}

