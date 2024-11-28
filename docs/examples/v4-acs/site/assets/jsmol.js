// from https://chemapps.stolaf.edu/jmol/jsmol/jsmol.htm

Jmol._isAsync = false;
var jmolApplet0;
var s = document.location.search;
Jmol._debugCode = (s.indexOf("debugcode") >= 0);

jmol_isReady = function(applet) {
//	document.title = (applet._id + " - Jmol " + Jmol.___JmolVersion)
//	Jmol._getElement(applet, "appletdiv").style.border="1px solid blue"
}		

var Info = {
	width: 420,
	height: 300,
	debug: false,
	color: "0xFFFFFF",
	addSelectionOptions: true,
	use: "HTML5",   // JAVA HTML5 WEBGL are all options
	j2sPath: "../../jmol/jsmol/j2s", // this needs to point to where the j2s directory is.
	jarPath: "./java",// this needs to point to where the java directory is.
	jarFile: "JmolAppletSigned.jar",
	isSigned: true,
	script: "set zoomlarge false;set antialiasDisplay;load data/caffeine.mol",
	serverURL: "https://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php",
	readyFunction: jmol_isReady,
	disableJ2SLoadMonitor: true,
  disableInitialConsole: true,
  allowJavaScript: true
	//defaultModel: "$dopamine",
	//console: "none", // default will be jmolApplet0_infodiv, but you can designate another div here or "none"
}
