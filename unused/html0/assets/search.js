// IFD site/assets/search.js
// 
// Bob Hanson hansonr@stolaf.edu 2023.01.19


IFD.JMEreadyCallback = function(app,b,c,d) {
	IFD.JMEapp = app;
	IFD.JME = app._applet.app;
}

var JMEshowSearch = function(fReturn) {

  if (IFD.JME) {
	IFD.JME.getTopLevelAncestor$().setVisible$Z(true);
  } else {
	var Info = {
	  args: ["search"],
	  code: null,
	  main: "jme.JMEJmol",
	  core: "NONE",
		width: 850,
		height: 550,
	  readyFunction: IFD.JMEreadyCallback,
	  searchCallback: fReturn,
	  serverURL: 'https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php',
	  j2sPath: IFD.properties.j2sPath,
	  console:'sysoutdiv',
	  allowjavascript: true
	}
	SwingJS.getApplet('testApplet', Info);
  }

}


var JMEfindSubstructure = function(smarts, smilesList) {
	var list = IFD.JME.findMatchingStructures$S$SA$Z(smarts, smilesList, true);
	IFD.JME.getTopLevelAncestor$().setVisible$Z(false);	
	return list;
}

var JMEsmartsReturn = function(smarts, smilesList, idList) {
	IFD.smarts = smarts;
	if (!smarts)
		return;
	var foundList = JMEfindSubstructure(smarts, smilesList);
	var ids = [];
	if (foundList != null) {
		for (var i = 0; i < foundList.length; i++) {
			if (foundList[i] == 1) {
				ids.push(idList[i]);
			}
		}
	}
	alert(ids.length + " structures were found.");
	IFD.showStructures(ids, false);
}

IFD.searchStructures = function(aidID) {
	aidID || (aidID = IFD.findingAidID);
	var smilesList = [];
	var idList = [];
	getSMILESList(aidID, smilesList, idList);
	JMEshowSearch(function(smarts) { JMEsmartsReturn(smarts, smilesList, idList)});
}

var getSMILESList = function(aidID, smilesList, idList) {
	IFD.mode = MODE_STRUCTURES;	
	if (!aidID) {
		for (var i = 0; i < IFD.aidIDs.length; i++) {
			getSMILESList(IFD.aidIDs[i], smilesList, idList);
		}
		return;
	}
	var structureIDs = IFD.items[aidID]["structures"];
	for (var i = 0; i < structureIDs.length; i++) {
		var id = structureIDs[i];
		var struc = IFD.collections[aidID].structures[id];
		var reps = struc.representations;
		var types = IFD.getRepTypes(reps, "smiles");
		if (types.smiles) {
			smilesList.push(types.smiles.data);
			idList.push([aidID, struc.id]);
		}
	}
}

IFD.searchText = function(aidID) {
	alert("not implemented");
}

IFD.searchSpectra = function(aidID) {
	alert("not implemented");

}

