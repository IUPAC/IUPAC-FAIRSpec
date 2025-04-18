// ${IUPAC FAIRSpec}/src/html/site/FAIRSpec-jmol.js
// from https://chemapps.stolaf.edu/jmol/jsmol/jsmol.htm

var s = document.location.search;

//Jmol._isAsync = false;
//Jmol._debugCode = (s.indexOf("debugcode") >= 0);

var SMILESInfo = {
  code: null,
  main: "org.jmol.smiles.SmilesMatcher",
  core: "assets/core_fairspec.z.js",
	width: 1,
	height: 1,
	readyFunction: function(app) { IFD.pageLoaded();},
	serverURL: 'https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php',
	j2sPath: 'https://chemapps.stolaf.edu/swingjs/chem/swingjs/j2s',
	console:'sysoutdiv',
	allowjavascript: true
}
SwingJS.getApplet("smiles",SMILESInfo);

var JMEInfo = {     
	visible: true,
  	divId: "jme-app-frame0-div"
  //optional parameters
  ,"options" : "canonize"// that is, created c1ccccc1 for benzene search//"query,hydrogens"
  //,"jme" : startingStructure   
}


IFD.jmolReadyCallback = function() {
	IFD.showHideDiv(IFD.properties.jmolDiv);
}


IFD.createJSME = function() {
	// SwingJS version -- not implementing yet
	  if (IFD.JME) {
		IFD.JME.getTopLevelAncestor$().setVisible$Z(true);
	  } else {
		var Info = {
		  args: ["search"],
		  code: null,
		  main: "jme.JME",
		  core: "NONE",
			width: 350,
			height: 350,
		  readyFunction: function(app) { IFD.JMEapp = app; IFD.JME = app._applet.app; },
		  searchCallback: IFD.jmeReturn,
		  serverURL: 'https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php',
		  j2sPath: '../assets/swingjs/j2s',
		  console:'sysoutdiv',
		  allowjavascript: true
		}
		SwingJS.getApplet('jme-app', Info);
	  }
}

IFD.jmeGetSmiles = function() {
	return IFD.JME.getSmiles$();
}

IFD.jmolFindSubstructure = function(smarts, smilesList) {
	var list = IFD.getSmilesMatcher().hasStructure(smarts, smilesList);
	return list;
}

IFD.getSmilesMatcher = function() {
	if (!IFD.smilesMatcher)
		IFD.smilesMatcher = new org.jmol.smiles.SmilesMatcher();
	return IFD.smilesMatcher;
}

IFD.jmolCheckSmiles = function(data){
	try {
		IFD.getSmilesMatcher().getMolecularFormula(data);
		return true; 
	} catch(e) {
		return false;
	}
}

IFD.jmolGetSmartsMatch = function(aidID) {
	var smarts = IFD.smarts = IFD.jmeGetSmiles();
	if (!smarts)
		return;
	var smilesList = [];
	var idList = [];
	IFD.getSMILES(aidID, idList, smilesList);
	var foundList = IFD.jmolFindSubstructure(smarts, smilesList);
	var ids = [];
	if (foundList != null) {
		for (var i = 0; i < foundList.length; i++) {
			if (foundList[i] == 1) {
				ids.push(idList[i]);
			}
		}
	}
	return ids;
}
