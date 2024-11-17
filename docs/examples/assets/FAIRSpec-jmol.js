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
	script: "frank off;"
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

IFD.createJmolAndJSME = function() {
	var adiv = IFD.properties.appletsDiv;
	if (!adiv)
		return;
	var jmediv = IFD.properties.jmeDiv;
	var jmoldiv = IFD.properties.jmolDiv;
	var s = (jmediv ? `
	<a href="javascript:IFD.showHideDiv('${jmediv}')">show/hide 2D</a>
	<div id="${jmediv}" style="display:block;width:300px;height:300px;border:1px solid green"></div>`
			: "")
			+ (jmoldiv ? 
	`<br><a href="javascript:IFD.showHideDiv('${jmoldiv}')">show/hide 3D</a>
	<div id="${jmoldiv}" style="display:block;width:300px;height:300px;border:1px solid blue"></div>
		` : "");
	$("#"+adiv).html(s);
	$("#"+jmoldiv).html(Jmol.getAppletHtml(jmol, JmolInfo));
	Jmol.getJMEApplet(jme, JMEInfo);	
}
