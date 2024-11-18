// from https://chemapps.stolaf.edu/jmol/jsmol/jsmol.htm


var jmol="jmol";
var jme = "jme";
var s = document.location.search;

Jmol._isAsync = false;
Jmol._debugCode = (s.indexOf("debugcode") >= 0);

jmol_isReady = function(applet) {
}		


var JmolInfo = {
	width: 300,
	height: 300,
	debug: false,
	color: "white",
    	//addSelectionOptions: true,
	serverURL: "https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php",
	j2sPath: "https://chemapps.stolaf.edu/jmol/jsmol/j2s",
	readyFunction : function(){IFD.jmolReadyCallback()},
	console: "none",
	script: "frank off"
}

var JMEInfo = {     
	visible: true,
  	divId: "jmediv"
  //optional parameters
  ,"options" : "nocanonize"//"query,hydrogens"
  //,"jme" : startingStructure   
}

// JME
//jsmeOnLoad = IFD.JMEreadyCallback = function(app,a,b,c,d) {
//	//IFD.JMEapp = app;
//	//IFD.JME = app._applet.app;
////	setTimeout(function() {IFD.showHideDiv(IFD.properties.jmeDiv)}, 100);
//}

IFD.jmolReadyCallback = function() {
	IFD.showHideDiv(IFD.properties.jmolDiv);
}

IFD.createJSME = function() {
	// SwingJS version -- not implementing yet
//	  if (IFD.JME) {
//		IFD.JME.getTopLevelAncestor$().setVisible$Z(true);
//	  } else {
//		var Info = {
//		  args: ["search"],
//		  code: null,
//		  main: "jme.JMEJmol",
//		  core: "NONE",
//			width: 850,
//			height: 550,
//		  readyFunction: IFD.JMEreadyCallback,
//		  searchCallback: fReturn,
//		  serverURL: 'https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php',
//		  j2sPath: IFD.properties.j2sPath,
//		  console:'sysoutdiv',
//		  allowjavascript: true
//		}
//		SwingJS.getApplet('testApplet', Info);
//	  }
	JMEInfo.divId = "jmediv";
	IFD.JME = Jmol.getJMEApplet("jme", JMEInfo);	
	Jmol._JMEApplet.onload();
}

IFD.createJmol = function() {
	$("#jmoldiv").html(Jmol.getAppletHtml(jmol, JmolInfo));
	IFD.jmol = Jmol._applets.jmol;
}

IFD.jmeGetSmiles = function() {
	return IFD.JME._applet.smiles();
}

IFD.jmolFindSubstructure = function(smarts, smilesList) {
	// swingjs? 
	//	var list = IFD.jmol.JME.findMatchingStructures$S$SA$Z(smarts, smilesList, true);
	//	IFD.JME.getTopLevelAncestor$().setVisible$Z(false);	
	var list = IFD.jmol._applet.viewer.getSmilesMatcher().hasStructure(smarts, smilesList);
	return list;
}

IFD.jmolCheckSmiles = function(data){
	try {
		IFD.jmol._applet.viewer.getSmilesMatcher().getMolecularFormula(data);
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
