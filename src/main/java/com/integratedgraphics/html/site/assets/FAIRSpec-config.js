// IFD site/assets/search.js
// 
// Bob Hanson hansonr@stolaf.edu 2023.01.19

IFD = {		
	properties:{
		baseDir: ".",
		findingAidFileName: "IFD.findingaid.json",
		standalone: true, // we DO NOT have actual data in this demo - all is coming from remote sources in the Finding Aid 
		jmeDiv: "jmediv",
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
	jmeReturn: null,
	smarts: null, // just for reference
	canvas: null,
	contentHeader: null,
	cache: {}
}