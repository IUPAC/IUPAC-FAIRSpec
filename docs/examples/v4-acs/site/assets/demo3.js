// IUPAC FAIRSpec site/assets/demo.js
//
// see https://chemapps.stolaf.edu/iupac/ifd4/site/demo.htm
//
// using data in https://chemapps.stolaf.edu/ifd4/
//
// Simple methods to work with a JSON IUPAC FAIRSpec Finding Aid
//
// Bob Hanson, 2023.01.17
//
 

IFD = {
	properties:{
		baseDir: "..",
		collectionDir: "https://chemapps.stolaf.edu/iupac/site/ifd4",
		findingAidFileName: "IFD.findingaid.json",
		standalone: false, // we DO have actual data in this demo
		imageDimensions:{width:40,height:40} //mm??
	},
	collections: {},
	items: {},
	cache: {}
}

;(function() {

var aLoading = null;

var dirFor = function(aidID, isCollection) {
	return IFD.properties[isCollection ? "collectionDir" : "baseDir"] + "/" + aidID;
}

var fileFor = function(aidID, fname, isCollection) {
	return dirFor(aidID, isCollection) + "/" + fname;
}

IFD.showCollection = function(aidID) {
	window.open(dirFor(aidID, true), "_blank");
}

IFD.showAid = function(aidID) {
	window.open(fileFor(aidID, IFD.properties.findingAidFileName), "_blank");
}

IFD.showVersion = function(aid) {
	$("#version").html(aid.version);
}


IFD.loadFindingAids = function() {
	var aids = IFD.findingaids = FindingAids.findingaids;
	$("#nfa").html("" + (aids.length - 1));
	var s = '<select id=articles onchange="IFD.loadSelected(this.selectedOptions[0].value)"><option value="">Select an ACS article</option>'
	for (var i = 1; i < aids.length; i++) {
	      var txt = aids[i].split("#")[0].split("/")[2];
		s += "<option value=\"" + txt + "\">" + txt + "</option>";
	}
	s += '</select>'
	$("#selectionBox").html(s);
}	

IFD.setTop = function(s) {
	addOrAppendJQ("#top",s);
}

IFD.setResults = function(s) {
	addOrAppendJQ("#results",s);
}

IFD.select = function(n, collection) {
	var d = $("#articles")[0];
	if (typeof n == "string") {
		var i;
		for (var i = d.options.length; --i >= 0;) {
			if (d.options[i].value == n) {
				break;
			}
		}
		if (i < 0)
			return;
		n = i;
	}
	d.selectedIndex = n;
	IFD.loadSelected(d.selectedOptions[0].value, collection);
}

IFD.loadSelected = function(aidID, collection) {
	if (typeof aidID == "object") {
		var next = aidID.pop();
		if (!next)return;
		aLoading = aidID;
		IFD.loadSelected(next);
		return;
	}
	aidID || (aidID = "");
	IFD.setResults("");
	var dir = dirFor(aidID);
	if (IFD.findingAidID == aidID) return;
	IFD.findingAidID = aidID;
	if (IFD.findingAidDir == dir) return;
	IFD.findingAidDir = dir;
	if (!aidID) {
		loadAll();
		return;
	}
	IFD.findingAidFile = fileFor(aidID, IFD.properties.findingAidFileName);
	var aid = cacheGet(aidID);
	if (aid == null) {
		$.ajax({url:IFD.findingAidFile, dataType:"json", success:callbackLoaded});
	} else {
		loadAid(aid, collection);
	}
}

var loadAidPanels = function(aid) {
 	loadTop(aid);
	setLowerLeft(aid);
}

var loadTop = function(aid) {
	var s = (aLoading ? " <a href=\"javascript:IFD.select('"  + aid.id +"','compounds')\">" + aid.id + "</a>": aid.id); 
	s = "<h3>" + shortType(aid.ifdType) + " for " + s + " </h3>";
	s += "<table>";
	s += IFD.loadPubInfo(aid);
	s += IFD.loadResources(aid);
	s += IFD.loadContents(aid);
	s += "</table>";
	IFD.setTop(s);
}

IFD.loadPubInfo = function(aid) {
	var info = (aid.isRelatedTo || "")[0] || "";
	var s = "";
	if (info) {
	  if (info.title) {
		s += "<tr><td valign=top>Title</td><td valign=top>" + info.title + "</td></tr>";
	  }
	  if (info.authors) {
		s += "<tr><td valign=top>Authors</td><td valign=top>" + info.authors + "</td></tr>";
	  }
	  if (info.desc) {
		s += "<tr><td valign=top>Description</td><td valign=top>" + info.desc+ "</td></tr>"
	  }
	  if (info.url) {
		s += "<tr><td valign=top>Publication</td><td valign=top><a target=_blank href=\"" + info.url + "\">" + info.url + "</a></td></tr>";
	  } 
	}
	return s;
}

IFD.loadResources = function(aid) {
	var resources = aid.resources;
	var s = "";
	for (var r in resources) {
		var ref = resources[r].ref;
		if (ref.indexOf("http") == 0) {
			var size = getSizeString(resources[r].len);
			ref = "<a target=_blank href=\"" + ref + "\">" + ref + (size ? " ("+size+")":"") + "</a>"
			s += "<tr><td>Data&nbsp;Origin</td><td>"+ref+"</td></tr>";
		}
      }
	return s;
}

IFD.getIDs = function(map) {
	var ids = [];
	for (var id in map) {
		ids.push(id);
	}
	return ids;
}

IFD.loadContents = function(aid) {
	var collections = aid.collectionSet.itemsByID;
	var s = "<tr><td>Collections</td><td>";
	var sep = "";
	var dc = IFD.collections[aid.id] = {};
	for (var i in collections) {
		dc[i] = collections[i].itemsByID;
	}
	var id = aid.id;
	var dItems = IFD.items[aid.id] = {};
	if (dc.compounds) {
		var items = dItems["compounds"] = IFD.getIDs(dc.compounds);
		s += "<a href=\"javascript:IFD.showAllCompounds('"+id+"')\">Compounds(" + items.length + ")</a>&nbsp;&nbsp;&nbsp;"
	}
	if (dc.spectra) {
		var items = dItems["spectra"] = IFD.getIDs(dc.spectra);
		s += "<xa href=\"javascript:IFD.showAllSpectra('"+id+"')\">Spectra(" + items.length + ")</xa>&nbsp;&nbsp;&nbsp;"
	}
	if (dc.structures) {
		var items = dItems["structures"] = IFD.getIDs(dc.structures);
		s += "<xa href=\"javascript:IFD.showAllStructures('"+aid.id+"')\">Structures(" + items.length + ")</xa>&nbsp;&nbsp;&nbsp;"
	}
	return s + "</td></tr>";
}



// showing items



IFD.showAllCompounds = function(aidID, ids) {
IFD.select(aidID);
	ids || (ids = IFD.items[aidID]["compounds"]);
	var s = "<table>";
	for (var i = 0; i < ids.length; i++) {
		s += "<tr class=\"tableRow" + (i%2) + "\">" +IFD.showCompound(aidID,ids[i]) + "</tr>";
	}
	s += "</table>";
	IFD.setResults(s);
} 

IFD.showCompound = function(aidID,id) {
	var cmpd = IFD.collections[aidID].compounds[id];
	var structureIDs = cmpd.itemsByID["structures"];
	var spectraIDs = cmpd.itemsByID["spectra"];
	var s = "<h3>Compound " + id + "</h3>"; 
	s += IFD.showCompoundStructures(aidID,structureIDs);
	s += IFD.showCompoundSpectra(aidID,spectraIDs);
	return s;
}


IFD.showCompoundStructures = function(aidID,ids) {
		var s = "<table>";
	var showID = (ids.length > 1);
	for (var i = 0; i < ids.length; i++) {
		s += "<tr>" + IFD.showCompoundStructure(aidID, ids[i], showID) + "</tr>";
	}
	s += "</table>";
	return s;
}

IFD.showCompoundStructure = function(aidID, id, showID) {
	var s = "<td><table><tr>";
	var struc = IFD.collections[aidID].structures[id];
	var props = struc.properties;
	var reps = struc.representations;

	s += "<td rowspan=2 valign=\"top\">";
	if (showID) {
		s += "Structure " + id + "<br>";
	}
	s += getStructureVisual(reps);
	s += "</td>";
 	s += "<td><table>";
		s += addMapRows("",props);
 		s += "</table></td>";
		s += "</tr>";
		s += "<tr><td>";
		s += addRepresentationTable(aidID, reps);
 		s += "</td>";
		s += "</tr></table>"
	s += "</td>";
	return s;
}



IFD.showCompoundSpectra = function(aidID,ids) {
	ids || (ids = IFD.items[aidID]["spectra"]);
	var s = "<table><tr><td style=\"width:100px\"></td><td valign=top><span class=spectitle>Spectra</span></td><td><div class=spectratable><table>";
	for (var i = 0; i < ids.length; i++) {
		s += addCompoundSpectrumRow(aidID,ids[i]);
	}
	s += "</table><div></td></tr></table>";
	return s;
}

var addMapRows = function(name, map, prefix) {
	var prefix = (prefix ? prefix.substring(prefix.lastIndexOf(".") + 1) + "." : "");
	var s = "";
	var n = 0;
	for (var key in map) {
		if (n++ == 0 && name)
			s += "<tr><td><h4>" + name + "</h4></td></tr>";
		s += "<tr><td>" + prefix + key + "</td><td>" + map[key] + "</td></tr>";
	}
	return s;
}

addCompoundSpectrumRow = function(aidID, id) {
	var spec = IFD.collections[aidID].spectra[id];
	var s = "<tr><td><table><tr><td valign=top>" + id + "</td></tr>"
	s += "<tr><td>";
		s += addRepresentationTable(aidID, spec.representations);
	s += "</td></tr>";
	s += "<tr><td><table>";
		s += addMapRows("IFD&nbsp;Properties", spec.properties, spec.propertyPrefix);
		s += addMapRows("More&nbsp;Parameters", spec.parameters);
	s += "</table></td></tr>";
	s += "</table>";
	return s;	
}

var getStructureVisual = function(reps) {
	var types = {};
	for (var i = 0; i < reps.length; i++) {
		types[shortType(reps[i].representationType)] = reps[i];
	}
	var s = "";
	var sep= "";
	if (types.image_png) {
		s += addPathRef(aidID, r.ref.path, shortFileName(r.ref.path))
		sep = "<br>"
	}
	if (types.smiles) {
		s += sep + "from SMILES:<br>" + cdkDepict(types.smiles.data);
	}	
	return s;	
}

// local methods


var getSizeString = function(n) {
	if (!n) return "";
	if (n > 1000000) return Math.round(n/100000)/10 + " MB";
	if (n > 1000) return Math.round(n/100)/10 + " KB";
	return n + " bytes";
}
var setLowerLeft = function(aid) {
	var s = (aid ? "<br><a href=\"javascript:IFD.showAid('"+aid.id+"')\">Show Finding Aid</a>"
		+ "<br><br><a href=\"javascript:IFD.showCollection('"+aid.id+"')\">Collection Folder</a>" : "")
	$("#lowerleft").html(s);
}

var addOrAppendJQ = function(jqid, s) {
	if (s) {
		$(jqid).append("<hr>");
		$(jqid).append(s);
	} else {
		$(jqid).html("");
	}
}



var cdkDepict = function(SMILES) {
  var w        = IFD.properties.imageDimensions.width; // mm
  var h        = IFD.properties.imageDimensions.height; // mm
  var hdisplay = "bridgehead";
  var annotate = "cip";
  return "<image "
//	+ "style=\"width:" + w + "px;height:"+h+"px\" "
	+ "src=\"https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=" 
	+ encodeURIComponent(SMILES) + "&w=" + w + "&h=" + h + "&hdisp=" + hdisplay 
	+ "&showtitle=false&zoom=1.7"
	// + &annotate=" + annotate
	+ "\"/>";
}

var addRepresentationTable = function(aidID, reps) {
	var s = "<table>"
	for (var i = 0; i < reps.length; i++) {
		s += addRepresentationRow(aidID, reps[i]);
	}
	s += "</table>"
	return s;
}

var addRepresentationRow = function(aidID, r) {
	var s = "<tr><td>"; 
	var type = shortType(r.representationType);
	s += "<span class=repname>" + type + "</span> ";
	if (r.data) {
		s += " " + r.data;
	} else {
		s += " " + addPathRef(aidID, r.ref.path, shortFileName(r.ref.path), r.len);
	}
	s += "</td></tr>";
	return s;
}

var shortFileName = function(f) {
	var pt = f.lastIndexOf("_");
	f = f.substring(pt + 1);
	pt = f.lastIndexOf("..");
	return (pt < 0 ? f : f.substring(pt + 2));
}

var addPathRef = function(aidID, path, shortName, len) {
	var url = fileFor(aidID, path, true);
	var s = shortName;
	if (shortName.endsWith(".png")) {
		s = "<image src=\"" + url +"\">"; 
	} else {
		s = "<a target=_blank href=\"" + url + "\">" + s + "</a>" + " (" + getSizeString(len) + ")";
	}
	return s;
}

var loadAll = function() {
		aLoading = [];
		var d = $("#articles")[0];
		for (var i = d.options.length; --i >= 1;) {
			aLoading.push(d.options[i].value);
		}
		IFD.loadSelected(aLoading);
}

var callbackLoaded = function(json) {
	var aid = json["IFD.findingaid"];
	loadAid(aid);
}

var loadAid = function(aid, collection) {
	if (!aLoading) {
		IFD.setTop("");
	}
	IFD.setResults("");
	setLowerLeft("");
	cachePut(IFD.findingAidID, aid);
	loadAidPanels(aid);
	if (aLoading) {
		if (aLoading.length > 0) {
			IFD.loadSelected(aLoading);
			return;
		}
		aLoading = null;
		IFD.findingAidID = null;
		setLowerLeft("");
	} else {
		IFD.showVersion(aid);
	}
}

var cachePut = function(key, value) {
	IFD.cache[key] = value;
}

var cacheGet = function(key) {
	return IFD.cache[key];
}

var shortType = function(type) {
	return type.substring(type.lastIndexOf(".") + 1);
}


})();
