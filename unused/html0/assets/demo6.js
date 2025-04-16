// IUPAC FAIRSpec site/assets/demo.js
//
// see https://chemapps.stolaf.edu/iupac/ifd4/site/demo.htm
//
// using data in https://chemapps.stolaf.edu/ifd4/
//
// Simple methods to work with a JSON IUPAC FAIRSpec Finding Aid
//
// Bob Hanson, 2023.01.23, 2023.08.12
//


var MODE_NONE       = "none";
var MODE_COMPOUNDS  = "compounds";
var MODE_STRUCTURES = "structures";
var MODE_SPECTRA    = "spectra";
var MODE_SAMPLES  = "samples";

// from https://www.nmrdb.org/service/

//    1H NMR prediction: https://www.nmrdb.org/service.php?name=nmr-1h-prediction&smiles=c1ccccc1CC
//    13C NMR prediction: https://www.nmrdb.org/service.php?name=nmr-13c-prediction&smiles=c1ccccc1CC
//    COSY prediction: https://www.nmrdb.org/service.php?name=cosy-prediction&smiles=c1ccccc1CC
//    HSQC/HMBC prediction: https://www.nmrdb.org/service.php?name=hmbc-prediction&smiles=c1ccccc1CC
//    All predictions: https://www.nmrdb.org/service.php?name=all-predictions&smiles=c1ccccc1CC


var NMRDB_PREDICT_SMILES = "https://www.nmrdb.org/service.php?name=%TYPE%-prediction&smiles=%SMILES%";
// where type = 1h, 13c, cosy, hmbc, hsqc

IFD = {
	properties:{
		baseDir: ".",
		j2sPath: "../site/swingjs/j2s",
		findingAidFileName: "IFD.findingaid.json",
		standalone: false, // we DO have actual data in this demo
		imageDimensions:{width:40,height:40} //mm??
	},
	collections: {},
	items: {},
	aidIDs: [],
	headers: [],
	mode: MODE_NONE,
	MAX_IMAGE_DIMENSIONS: {width:400, height:400},
	
	cache: {}
}

;(function() {

var aLoading = null;
var divId = 0;

var dirFor = function(aidID) {
	return IFD.properties.baseDir + "/" + aidID;
}

var fileFor = function(aidID, fname) {
	return dirFor(aidID) + "/" + fname;
}

IFD.showCollection = function(aidID) {
	window.open(dirFor(aidID), "_blank");
}

IFD.showAid = function(aidID) {
	window.open(fileFor(aidID, IFD.properties.findingAidFileName), "_blank");
}

IFD.showVersion = function(aid) {
	$("#version").html(aid.version);
}


// external
IFD.loadFindingAids = function() {
	var aids = IFD.findingaids = FindingAids.findingaids;
	IFD.aidIDs = [];
	$("#nfa").html("" + (aids.length - 1));
	var s = '<select id=articles onchange="IFD.loadSelected(this.selectedOptions[0].value)"><option value="">Select a Finding Aid</option>'
	for (var i = 0; i < aids.length; i++) {
	      var aidID = aids[i].split("#")[0].split("/");
		aidID = aidID[aidID.length - 2];
		IFD.aidIDs.push(aidID);
		s += "<option value=\"" + aidID + "\">" + aidID + "</option>";
	}
	s += '</select>'
	$("#selectionBox").html(s);
}	

// external
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

// external
IFD.loadSelected = function(aidID, collection) {
	if (typeof aidID == "object") {
		var next = aidID.pop();
		if (!next)return;
		aLoading = aidID;
		IFD.loadSelected(next);
		return;
	}
	aidID || (aidID = "");
	setResults("");
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
		$.ajax({url:IFD.findingAidFile, dataType:"json", success:callbackLoaded, error:callbackLoadFailed});
	} else {
		loadAid(aid, collection);
	}
}

var setMode = function(mode) {
  IFD.mode = mode;
  IFD.headers = [];
}

//external
IFD.showSamples = function(aidID, ids) {
	IFD.select(aidID);
	setMode(MODE_SAMPLES);
	ids || (ids = IFD.items[aidID]["samples"]);
	var s = "<table>";
	for (var i = 0; i < ids.length; i++) {
		s += "<tr class=\"tableRow" + (i%2) + "\">" + showSample(aidID,ids[i]) + "</tr>";
	}
	s += "</table>";
	setResults(s);
}

//external
IFD.showCompounds = function(aidID, ids) {
	IFD.select(aidID);
	setMode(MODE_COMPOUNDS);
	ids || (ids = IFD.items[aidID]["compounds"]);
	var s = "<table>";
	for (var i = 0; i < ids.length; i++) {
		s += "<tr class=\"tableRow" + (i%2) + "\">" + showCompound(aidID,ids[i]) + "</tr>";
	}
	s += "</table>";
	setResults(s);
} 

//external
IFD.showSpectra = function(aidID, ids) {
	IFD.select(aidID);
	setMode(MODE_SPECTRA);
	ids || (ids = IFD.items[aidID]["spectra"]);
	var s = "<table>";
	for (var i = 0; i < ids.length; i++) {
		s += "<tr class=\"tableRow" + (i%2) + "\">" + showSpectrum(aidID,ids[i]) + "</tr>";
	}
	s += "</table>";
	setResults(s);
} 

//external
IFD.showStructures = function(aidID, ids) {
	IFD.select(aidID);
	setMode(MODE_STRUCTURES);
	ids || ids === false || (ids = IFD.items[aidID]["structures"]);
	var s = showCompoundStructures(aidID,ids, false, true);
	setResults(s);
} 


IFD.showStructuresByAidAndID = function(idList) {
	s = showCompoundStructures(null, idList, false);
}

// local methods

var loadSearchPanel = function(aid) {
	var s = "";
	s += "<h3>Search</h3>";
	s += "<a href=\"javascript:IFD.searchStructures('"+(aid ? aid.id : "")+"')\">Substructure</a><br><br>";
	//s += "<br><a href=\"javascript:IFD.searchText('"+(aid ? aid.id : "")+"')\">Text</a>";
	//s += "<br><a href=\"javascript:IFD.searchSpectra('"+(aid ? aid.id : "")+"')\">Spectra</a>";
	return s;
}

var loadAll = function() {
	aLoading = [];
	for (var i = 0; i < IFD.aidIDs.length; i++) {
		aLoading[i] = IFD.aidIDs[i];
	}
	IFD.loadSelected(aLoading);
}

var callbackLoaded = function(json) {
	var aid = json["IFD.findingaid"];
	aid.id || (aid.id = IFD.findingAidID);
	loadAid(aid);
}

var callbackLoadFailed = function(x,y,z) {
	alert([x,y,z]);
	return;
}


var loadAid = function(aid, collection) {
	if (!aLoading) {
		setTop("");
	}
	setResults("");
	setSearch("");
	cachePut(IFD.findingAidID, aid);
	loadAidPanels(aid);
	if (aLoading) {
		if (aLoading.length > 0) {
			IFD.loadSelected(aLoading);
			return;
		}
		aLoading = null;
		IFD.findingAidID = null;
		setSearch("");
	} else {
		IFD.showVersion(aid);
	}
}

var loadAidPanels = function(aid) {
 	loadTop(aid);
	setSearch(aid);
}

var loadTop = function(aid) {
	var s = "&nbsp;&nbsp;&nbsp;" + aid.id 
		+ (aLoading ? 
			"&nbsp;&nbsp; <a target=_blank href=\"" + IFD.findingAidFile + "\">view "+IFD.findingAidFile.split("/").pop()+"</a>"
			+ "&nbsp;&nbsp; " + addPathRef(aid.id, aid.collectionSet.properties.ref, shortFileName(aid.collectionSet.properties.ref), aid.collectionSet.properties.len)
		: ""); 
	s = "<h3>" + shortType(aid.ifdType) + s + " </h3>";
	s += "<table>";
	s += loadPubInfo(aid);
	s += loadResources(aid);
	s += loadTopRow(aid);
	s += "</table>";
	setTop(s);
}

var getPubText = function(aid, agency, t) {
	var items = aid.relatedItems;
	if (!items)
		return "";
	for (var i = items.length; --i >= 0;) {
		var info = items[i];
		if (!info || !info.metadataSource || info.metadataSource.registrationAgency != agency)
			continue; 
		var s = "";
		var isDataCite = (agency == "DataCite");
		var d = info.dataTitle || info.title; 
		if (d != t[0]) {
			s += "<tr><td valign=top>" + (isDataCite ? "Data Title" : "Title") + "</td><td valign=top><i>" + d + "</i></td></tr>";
			t[0] = d;
		}
		d = info.dataContributors || info.authors;
		if (d) {
			s += "<tr><td valign=top>" + (isDataCite ? "Data Contributors" :  "Authors" ) +"</td><td valign=top><b>" + cleanOrcids(d) + "</b></td></tr>";
		}
		var url = info.dataDoiLink || info.dataUrl || info.doiLink || info.url;
		if (url) {
			s += "<tr><td valign=top>"+(isDataCite ? "Data URL" : "URL")+"</td><td valign=top><a target=_blank href=\"" + url + "\">" + url + "</a></td></tr>";
		}
		return s;
	}
	return "";
}

var cleanOrcids = function(d) {
  // Thomas Mies (https://orcid.org/0000-0002-3296-6817);...
	var a = d.split(";");
	var s = "";
	for (var i = 0; i < a.length; i++) {
		var d = a[i].split("[https://orcid.org");
		var name = d[0];
		if (d.length > 1) {
			name = "<a target=_blank href=https://orcid.org" + d[1].substring(0, d[1].indexOf("]")) + ">" + name.trim() + "</a>";
		}
		s += ", " + name.trim();
	}
	return s.substring(2);
}

var loadPubInfo = function(aid) {
	var s = "";
	var t = [];
	s += getPubText(aid, "Crossref", t);
	s += "<br>"
	s += getPubText(aid, "DataCite", t);
	return s;
}

var loadResources = function(aid) {
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

var loadTopRow = function(aid) {
	var collections = aid.collectionSet.itemsByID;
	var s = "<tr><td><b>Collections:</b>&nbsp;&nbsp;</td><td>";
	var sep = "";
	var dc = IFD.collections[aid.id] = {};
	for (var i in collections) {
		dc[i] = collections[i].itemsByID;
	}
	var id = aid.id;
	var dItems = IFD.items[aid.id] = {};
	if (dc.samples) {
		var items = dItems["samples"] = getIDs(dc.samples);
		s += "<a href=\"javascript:IFD.showSamples('"+aid.id+"')\">Samples(" + items.length + ")</a>";
	} else {
		s += "samples";
	}
	s += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;==&gt;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
	if (dc.spectra) {
		var items = dItems["spectra"] = getIDs(dc.spectra);
		s += "<a href=\"javascript:IFD.showSpectra('"+id+"')\">Spectra(" + items.length + ")</a>";
	} else {
		s += "spectra";
	}
	s += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;==&gt;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
	if (dc.structures) {
		var items = dItems["structures"] = getIDs(dc.structures);
		s += "<a href=\"javascript:IFD.showStructures('"+aid.id+"')\">Structures(" + items.length + ")</a>";
	} else {
		s += "structures";
	}
	s += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;==&gt;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
	if (dc.compounds) {
		var items = dItems["compounds"] = getIDs(dc.compounds);
		s += "<a href=\"javascript:IFD.showCompounds('"+id+"')\">Compounds(" + items.length + ")</a>";
	} else {
		s += "compounds";
	}

// fold this information into "samples"
	if (dc["sample-spectra associations"]) {
		var items = dItems["samplespectra"] = getIDs(dc["sample-spectra associations"]);
//		s += "<a href=\"javascript:IFD.showSampleSpectraAssociations('"+aid.id+"')\">Sample-Spectra Associations(" + items.length + ")</a>";
	}
	return s + "</td></tr>";
}

// samples --- associated via originating_sample property of a spectrum -- 1 sample : N spectra
// structures --- associated with spectra as "compounds" -- N structures : N' spectra
//
// the logical experimental progression is
//  
//    sample --> spectrum --> structure
//
// NOT   
// 
//    sample --> structure --> spectrum 
//
// So "by sample" means:
//
// 1. list by sample
// 2. for each sample:
//    a. show the originating ID, maybe link to ELN?
//    b. show all spectra associated with this sample
//    c. show all compounds associated with these spectra -- should be just one? or could be a mixture.
//    d. highlight any case where compound/spectra relationships are different. 
//
//       For example, this is OK:
//
//         Sample A:
//             1H   Compound 1 and Compound 2
//             13C  Compound 1 and Compound 2
//
//       But this is not OK:
//
//         Sample B:
//             1H   Compound 1
//             13C  Compound 2
//
//       Something is amiss here!

// Structure/Spectra checks:
//
// 1. "by compound"
// 2. for each compound:
//    a. show Compound ID
//    b. show structure, other identifiers?
//    c. show associated spectra

var getSmilesForStructureID = function(aidID, id) {
	var struc = IFD.collections[aidID].structures[id];
	if (!struc) return "";
	var reps = struc.representations;
	var types = IFD.getRepTypes(reps);
	return types.smiles && types.smiles.data;
}

var getSpectrumIDsForSample = function(aidID,id) {
	var samplespecs = IFD.collections[aidID]["sample-spectra associations"];
	var items = IFD.items[aidID].samplespectra;
	for (var i = 0; i < items.length; i++) {
		var assoc = samplespecs[items[i]];
		if (assoc.itemsByID.samples[0] == id) {
			return assoc.itemsByID.spectra;
		}
	}
	return [];	
}

var addUnique = function(afrom, ato) {
	for (var i = 0; i < afrom.length; i++) {
		var v = afrom[i];
		for (var j = 0; j < ato.length; j++) {
			if (v == ato[j]) {
				v = null;
				break;
			}
		}
		v && ato.push(v);
	}
}

var getStructureIDsForSpectra = function(aidID, specIDs) {
	var compounds = IFD.collections[aidID]["compounds"];
	var ids = [];
	var items = IFD.items[aidID].compounds;
	for (var is = 0; is < specIDs.length; is++){
		var specid = specIDs[is];
		for (var i = 0; i < items.length; i++) {
			var assoc = compounds[items[i]];
			var spectra = assoc.itemsByID.spectra;
			for (var j = 0; j < spectra.length; j++) {
				if(spectra[j] == specid)
					addUnique(assoc.itemsByID.structures, ids);
			}	
		}
	}
	return ids;	
}

var showSample = function(aidID,id) {
	var sample = IFD.collections[aidID].samples[id];
	var specids = getSpectrumIDsForSample(aidID, id);
	var structureIDs = getStructureIDsForSpectra(aidID,specids);
	var s = getHeader("Sample " + id); 
	s += showCompoundStructures(aidID,structureIDs, false);
	var smiles = getSmilesForStructureID(aidID, structureIDs[0]);
	s += showCompoundSpectra(aidID,specids,smiles,true);
	s += "<hr style='color:red'>";
	return s;
}


//
// So "by sample" means:
//
// 1. list by sample
// 2. for each sample:
//    a. show the originating ID, maybe link to ELN?
//    b. show all spectra associated with this sample
//    c. show all structures associated with these spectra -- should be just one? or could be a mixture.
//    d. highlight any case where compound/spectra relationships are different. 
//
//       For example, this is OK:
//
//         Sample A:
//             1H   Compound 1 and Compound 2
//             13C  Compound 1 and Compound 2
//
//       But this is not OK:
//
//         Sample B:
//             1H   Compound 1
//             13C  Compound 2
//
//       Something is amiss here!

var getObjectProperty = function(o, what) {
	return o.properties && o.properties[what] || "";
}

var showSpectrum = function(aidID,id) {
	var spec = IFD.collections[aidID].spectra[id];
	var structureIDs = getStructureIDsForSpectra(aidID, [id]);
	var sampleID = spec.properties && spec.properties.originating_sample_id;
	var s = "<table padding=3><tr><td valign=top>"
		+ getHeader("Spectrum " + id) + "<h3>" 
		+ (sampleID ? "&nbsp;&nbsp;&nbsp; sample " + sampleID : "")
		+ "</h3></td>"; 
	var title = getObjectProperty(spec, "expt_title");
	if (title)
		s += "<td>&nbsp;&nbsp;</td><td><b>" + title + "</b></td>"
	s += "</tr></table>";
	var smiles = getSmilesForStructureID(aidID, structureIDs[0]);
	s += showCompoundSpectra(aidID,[id],smiles,false);
	s += showCompoundStructures(aidID,structureIDs, false);
	s += "<hr style='color:blue'>";
	return s;
}

var getHeader = function(name) {
	var key = removeSpace(name) + "_" + ++divId
	IFD.headers.push([key,name]);
	return "<a name=\"" + key + "\"><h3>" + name + "</h3></a>"
}

var showCompound = function(aidID,id) {
	var cmpd = IFD.collections[aidID].compounds[id];
	var structureIDs = cmpd.itemsByID["structures"];
	var spectraIDs = cmpd.itemsByID["spectra"];
	var props = cmpd.properties;
	var params = cmpd.parameters;
	var label = cmpd.label || cmpd.id;
	var s = getHeader("Compound " + label); 
	s += "<table>" + addPropertyRows("",props, null, false) + "</table>"
	s += "<table>" + addPropertyRows("",params, null, false) + "</table>"

	s += showCompoundStructures(aidID,structureIDs, false);
	var smiles = getSmilesForStructureID(aidID, structureIDs[0]);
	s += showCompoundSpectra(aidID,spectraIDs,smiles);
	s += "<hr style='color:red'>";
	return s;
}

var showCompoundStructures = function(aidID,ids,haveTable,isSample) {
	var s = (haveTable ? "" : "<table>");
	if (!aidID || !aidID[0])return;
	if (ids === false) {
		ids = [];
		var thisaid = aidID[0][0];
		for (var i = 0; i <= aidID.length; i++) { // yes, <= here
			var a = aidID[i];
			var aid = (a ? a[0] : null);
			if (aid == thisaid) {
				ids.push(a[1]);
			} else {
				s += showCompoundStructures(thisaid, ids,false);
				ids = [];
				thisaid = aid;
			}
		}		
	} else {
		var showID = (ids.length > 1);
		for (var i = 0; i < ids.length; i++) {	
			if (aidID) 	{
				s += "<tr>" + showCompoundStructure(aidID, ids[i], showID) + "</tr>";
			} else {
				s += "<tr>" + showCompoundStructure(ids[i][0], ids[i][1], showID) + "</tr>";
			}
		}
	}
	s += (haveTable ? "" : "</table>");
	return s;
}


var showCompoundStructure = function(aidID, id, showID) {
	var s = "<td><table cellpadding=10><tr>";
	var struc = IFD.collections[aidID].structures[id];
	var props = struc.properties;
	var reps = struc.representations;

	s += "<td rowspan=2 valign=\"top\">";
	if (showID) {
		var h = (id.indexOf("Structure") == 0 ? removeUnderline(id) : "Structure " + id);
		s += "<span class=structurehead>"+ (IFD.mode == MODE_STRUCTURES ? getHeader(h) : h) + "</span><br>";
	}
	s += getStructureVisual(reps);
	s += getStructureSpectraPredictions(reps);
	s += "</td>";
	s += "<td>" + addRepresentationTable(aidID, reps, "png") + "</td>";
	s += "</tr>";
		s += "<tr>";
	 	s += "<td><table>" + addPropertyRows("",props, null, false) + "</table></td>"
		s += "</tr>";
		s += "</table>";
	s += "</td>";
	return s;
}



var showCompoundSpectra = function(aidID,ids,smiles,withTitle) {
	ids || (ids = IFD.items[aidID]["spectra"]);
	var s = "<table>"
		if (withTitle)
			s += "<tr><td style=\"width:100px\" valign=top><span class=spectitle>Spectra</span></td></tr>";
	for (var i = 0; i < ids.length; i++) {
		s += addCompoundSpectrumRow(aidID, ids[i], smiles);
	}
	s += "</table>";
	return s;
}


var setTop = function(s) {
	clearJQ("#top");
	addOrAppendJQ("#top",s);
}

var setResults = function(s) {
	clearJQ("#results");
	addOrAppendJQ("#results",s);
	loadContents(!!s);
}

var loadContents = function(hasContent) {
	clearJQ("#contents");
	var s = "<table>";
	for (var i = 0; i < IFD.headers.length; i++) {
		var h = IFD.headers[i];
		var key = h[0];
		var val = h[1];
		s += "<tr><td><a href=#" + key + ">"+val+"</a></td></tr>"	
	}
	s += "</table>"
	$("#contents").html(s);

}


IFD.checkImage = function(id) {
	var max = IFD.MAX_IMAGE_DIMENSIONS;
	var image = document.getElementById("img" + id);
	if (!image)
		return;
	var w = image.clientWidth;
	var h = image.clientHeight;
	if (!w || !h) {
		console.log("image not ready " + image.id);
		return;
	}
	var wh = (w > h ? w : h);
	var f = Math.max(w/max.width, h/max.height);
	if (f > 1) {
		w = Math.round(w/f);
		h = Math.round(h/f);
		image.style.width = w + "px";
		image.style.height = h + "px";
		console.log("image set to " + w + "x" + h + " for " + image.id);
	} 
}

var setSearch = function(aid) {
	var s = (aid ? "<br><a href=\"javascript:IFD.showAid('"+aid.id+"')\">Show Finding Aid</a>"
		+ "<br><br><a href=\"javascript:IFD.showCollection('"+aid.id+"')\">Collection Folder</a>" : "");
	s += loadSearchPanel(aid);
	$("#search").html(s);
}



// shared

IFD.getRepTypes = function(reps, type) {
	var types = {};
	for (var i = 0; i < reps.length; i++) {
		var t = shortType(reps[i].representationType);
		if (!type || t == type)
			types[t] = reps[i];
	}
	return types;
}

var getStructureVisual = function(reps) {
	var types = IFD.getRepTypes(reps);
	if (types.png && types.png.data || !types.smiles || !types.smiles.data) return "";
	return "from SMILES:<br>" + cdkDepict(types.smiles.data);
}

var getPredictAnchor = function(type, smiles, text) {
	var url = NMRDB_PREDICT_SMILES.replace("%TYPE%", type.toLowerCase()).replace("%SMILES%", smiles);
	return "<a target=_blank href=\"" + url + "\">" + (text ? text : type) + "</a>"

}

var getStructureSpectraPredictions = function(reps) {
	var types = IFD.getRepTypes(reps);
	var smiles = (types.smiles && types.smiles.data);
      if (!smiles)return "";
	var s = "<br><br><b>Predicted Spectra</b>"
	s += "<br>" + getPredictAnchor("nmr-1H", smiles, "1H");
	s += "&nbsp;&nbsp;" + getPredictAnchor("nmr-13C", smiles, "13C");
	s += "<br>" + getPredictAnchor("COSY", smiles);
	s += "&nbsp;&nbsp;" + getPredictAnchor("HMBC", smiles);
	return s;	
}

var cdkDepict = function(SMILES) {
  var w        = IFD.properties.imageDimensions.width; // mm
  var h        = IFD.properties.imageDimensions.height; // mm
  var hdisplay = "bridgehead";
  var annotate = "cip";
  return "<img  id=img" + (++divId) + " onload=IFD.checkImage(" + divId + ")"
//	+ " style=\"width:" + w + "px;height:"+h+"px\" "
	+ " src=\"https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=" 
	+ encodeURIComponent(SMILES) + "&w=" + w + "&h=" + h + "&hdisp=" + hdisplay 
	+ "&showtitle=false&zoom=1.7"
	// + &annotate=" + annotate
	+ "\"/>";
}

var getSizeString = function(n) {
	if (!n) return "";
	if (n > 1000000) return Math.round(n/100000)/10 + " MB";
	if (n > 1000) return Math.round(n/100)/10 + " KB";
	return n + " bytes";
}

var clearJQ = function(jqid) {
		$(jqid).html("");
}

var addOrAppendJQ = function(jqid, s) {
	if (s) {
		$(jqid).append("<hr>");
		$(jqid).append(s);
	} else {
		$(jqid).html("");
	}
}

var addRepresentationTable = function(aidID, reps, firstItem) {
	var s = ""
	for (var i = 0; i < reps.length; i++) {
		var type = shortType(reps[i].representationType);
		if (type == firstItem) {
			s = addRepresentationRow(aidID, reps[i], type) + s;
		} else {
			s += addRepresentationRow(aidID, reps[i], type);
		}
	}
	return "<table>" + s + "</table>";
}

var addRepresentationRow = function(aidID, r, type) {
	var s = ""; 
	var shead = (type == "png" ? "" : "<span class=repname>" + clean(type) + "</span> ");
	if (r.data) {
		if (r.data.indexOf(";base64") == 0) {
			var value = "<img id=img" + (++divId)  + " onload=IFD.checkImage(" + divId + ")" +  " src=\"" + "data:" + r.mediaType + r.data + "\"</img>";
			s += addPathRef(aidID, r.ref.path, shortFileName(r.ref.path), -1, value);
		} else {
			if (r.data.length > 30) {
				s += anchorHide(shead, r.data);
				shead = "";
			} else {
				s += r.data;
			}
		}
	} else {
		s += " " + addPathRef(aidID, r.ref.path, shortFileName(r.ref.path), r.len);
	}
	s = "<tr><td>" + shead + s + "</td></tr>";
	return s;
}

var heads = [];

var anchorHide = function(shead, sdata) {
	heads.push(sdata);
	return "<a class=hiddenhead href=javascript:IFD.showHead(" + (heads.length - 1) + ")>" + shead + "</a>";
}

IFD.showHead = function(i) {alert(heads[i])}

var getSpectrumPrediction = function(props, smiles) {
	if (!props || !smiles) return "";
	var s = "<br>";
	var dim = props.expt_dimension;
	var nuc = props.expt_nucl1;
	if (dim == "1D" && nuc == "1H")
		s += "<br>" + getPredictAnchor("nmr-1H",smiles, "predicted 1H");
	else if (dim == "1D" && nuc == "13C")
		s += "<br>" + getPredictAnchor("nmr-13C",smiles, "predicted 13C");
        else if (dim == "2D" && nuc == "1H" && props.expt_nucl2 == "1H")
		s += "<br>" + getPredictAnchor("COSY",smiles, "predicted COSY");
        else if (dim == "2D" && nuc == "1H" && props.expt_nucl2 == "13C")
		s += "<br>" + getPredictAnchor("HMBC",smiles, "predicted HMBC");
	return s;
}

var addCompoundSpectrumRow = function(aidID, id, smiles) {
	var spec = IFD.collections[aidID].spectra[id];
	var s = "<tr><td rowspan=2 style=\"width:100px\" valign=top>" + id 
	+ getSpectrumPrediction(spec.properties, smiles)
	+ "</td>"
	s += "<td>";
		s += addRepresentationTable(aidID, spec.representations);
	s += "</td></tr>";
	s += "<tr><td><table>";
		s += addPropertyRows("IFD&nbsp;Properties", spec.properties, null, true);
		s += addPropertyRows("More&nbsp;Parameters", spec.parameters, null, true);
	s += "</table></td></tr>";
	return s;	
}

var addPropertyRows = function(name, map, firstItem, hideDiv) {
	var s = "";
	var s0 = "";
	var n = 0;
	var id = ++divId;
	for (var key in map) {
		if (n++ == 0 && name)
			s0 = "<tr><td><h4>" + (hideDiv ? "<div class=hiddendiv onclick=IFD.toggleDiv(" + id + ")>" + name + "...</div>" : name) + "</h4></td></tr>";
		if (key == firstItem) {
			s = addPropertyLine(key, map[key]) + s;
		} else {
			s += addPropertyLine(key, map[key]);
		}
	}
	if (hideDiv) {
		s = "<tr><td colspan=2><div id=prop" + id + " style='display:none'><table><tr><td>" + s + "</td></tr></table></div></td></tr>"
	}
	return s0 + s;
}

var addPropertyLine = function(key, val) {
	key = clean(key);
	if ((key.endsWith("_PID") || key.endsWith("_DOI")) && val.startsWith("10.")) {
		val = getDOIAnchor(key, val);
	}
	return "<tr><td><b>" + key + "<b></td><td>" + val + "</td></tr>";
}

var getDOIAnchor = function(key, val) {
	// PID 10.xxxxxx
	if (val.startsWith("https://doi.org/"))
		val = val.split("doi.org/")[1];
	return "<a class=doiref target=_blank href=\"https://doi.org/" + val + "\">"+val+"</a>";
}

IFD.toggleDiv = function(id) {
	var d = document.getElementById("prop" + id);
	d.style.display = (d.style.display == "none" ? "block" : "none");
}

var shortFileName = function(f) {
	var pt = f.lastIndexOf("/");
	f = f.substring(pt + 1);
	pt = f.lastIndexOf("..");
	return (pt < 0 ? f : f.substring(pt + 2));
}

var addPathRef = function(aidID, path, shortName, len, value) {
	var url = fileFor(aidID, path);
	var s = shortName;
	if (value) {
		s = "<a target=_blank href=\"" + url + "\">" + value + "</a>"
	} else if (shortName.endsWith(".png")) {
		s = "<img id=img" + (++divId)  + " onload=IFD.checkImage(" + divId + ")" +  " src=\"" + url +"\">"; 
	} else {
		s = "<a target=_blank href=\"" + url + "\">" + s + "</a>" + " (" + getSizeString(len) + ")";
	}
	return s;
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

var getIDs = function(map) {
	var ids = [];
	for (var id in map) {
		ids.push(id);
	}
	return ids;
}


var clean = function(id){
	return id;//.replace(/_/g, ' ');
}

var removeUnderline = function(id){
	return id.replace(/_/g, ' ');
}

var removeSpace = function(id){
	return id.replace(/[^a-zA-Z0-9_]/g, '_');
}

})();
