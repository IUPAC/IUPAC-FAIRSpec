// ${IFD site}/assets/FAIRSpec-gui.js
// 
// Bob Hanson hansonr@stolaf.edu 2024.11.15

// anonymous function for local functions

;(function() {

	// from https://www.nmrdb.org/service/

	//    1H NMR prediction: https://www.nmrdb.org/service.php?name=nmr-1h-prediction&smiles=c1ccccc1CC
	//    13C NMR prediction: https://www.nmrdb.org/service.php?name=nmr-13c-prediction&smiles=c1ccccc1CC
	//    COSY prediction: https://www.nmrdb.org/service.php?name=cosy-prediction&smiles=c1ccccc1CC
	//    HSQC/HMBC prediction: https://www.nmrdb.org/service.php?name=hmbc-prediction&smiles=c1ccccc1CC
	//    All predictions: https://www.nmrdb.org/service.php?name=all-predictions&smiles=c1ccccc1CC
	
	var MODE_NONE       = "none";
	var MODE_COMPOUNDS  = "compounds";
	var MODE_STRUCTURES = "structures";
	var MODE_SPECTRA    = "spectra";
	var MODEsearch_SAMPLES  = "samples";
	var NMRDB_PREDICT_SMILES = "https://www.nmrdb.org/service.php?name=%TYPE%-prediction&smiles=%SMILES%";
	// where type = 1h, 13c, cosy, hmbc, hsqc
	
	var aLoading = null;
	var divId = 0;
	
	var dirFor = function(aidID) {
		return IFD.properties.baseDir + "/" + aidID;
	}
	
	var fileFor = function(aidID, fname) {
		return dirFor(aidID) + "/" + fname;
	}
	
	//external
	IFD.searchText = function(aidID) {
		alert("not implemented");
	}

	//external
	IFD.searchSpectra = function(aidID) {
		alert("not implemented");
	}

	IFD.showStructuresByAidAndID = function(idList) {
		// not implemented
		s = showCompoundStructures(null, idList, false);
	}
	
	
	// external
	IFD.loadFindingAid = function() {
		return IFD.loadFindingAids();
	}

	// external
	IFD.loadFindingAids = function() {
		var aids = IFD.findingAids || [];
		IFD.aidIDs = [];
		var s = (aids.length == 1 ? null : '<select id=articles onchange="IFD.loadSelected(this.selectedOptions[0].value)"><option value="">Select a Finding Aid</option>')
		// set up for ./name/IFD.findingAid.json
		for (var i = 0; i < aids.length; i++) {
			var name = aids[i].split("#")[0];
			if (!name.endsWith(".json"))
				name += "/IFD.findingaid.json";
      		var aidID = name.split("/");
      		aidID = aidID[aidID.length - 2];
			IFD.aidIDs.push(aidID);
			if (s)
				s += "<option value=\"" + aidID + "\">" + aidID + "</option>";
		}
		if (aids.length == 1) {
			IFD.loadSelected(aidID, null);
		} else {
			s += '</select>'
			$("#selectionBox").html(s);
		}
		IFD.createJmolAndJSME();		
	}	
	
	// external
	IFD.select = function(n, collection) {
		var d = $("#articles")[0];
		if (d) {
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
			n = d.selectedOptions[0].value;
		}
		IFD.loadSelected(n, collection);
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
		var aid = IFD.cacheGet(aidID);
		if (aid == null) {
			$.ajax({url:IFD.findingAidFile, dataType:"json", success:callbackLoaded, error:callbackLoadFailed});
		} else {
			loadAid(aid, collection);
		}
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
	
	IFD.showCollection = function(aidID) {
		window.open(dirFor(aidID), "_blank");
	}

	IFD.showAid = function(aidID) {
		window.open(fileFor(aidID, IFD.properties.findingAidFileName), "_blank");
	}

	IFD.showVersion = function(aid) {
		$("#version").html(aid.version);
	}


	var setMode = function(mode) {
	  IFD.mode = mode;
	  IFD.headers = [];
	}

	//external
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
		IFD.cachePut(IFD.findingAidID, aid);
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
				+ "&nbsp;&nbsp; " + addPathRef(aid.id, aid.collectionSet.properties.ref, aid.collectionSet.properties.len)
			: ""); 
		s = "<h3>" + IFD.shortType(aid.ifdType) + s + " </h3>";
		s += "<table>";
		s += addPubInfo(aid);
		s += addDescription(aid);
		s += addResources(aid);
		s += addTopRow(aid);
		s += "</table>";
		setTop(s);
	}

	var addDescription = function(aid) {
		var s = "";
		if (aid.collectionSet.description)
			s += "<tr><td valign=top>Description</td><td valign=top>" + aid.collectionSet.description + "</td></tr>";
		return s;
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
			var urltype =(url.indexOf("https://doi.org") == 0 ? "DOI" : "URL"); 
			if (url) {
				s += "<tr><td valign=top>";
				s += (isDataCite ? "Data " : "") +urltype+"</td><td valign=top><a target=_blank href=\"" + url + "\">" + url + "</a>";
				s += " (<a target=_blank href=\"" +info.metadataSource.metadataUrl + "\">metadata</a>)";
				s += "</td></tr>";
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

	var addPubInfo = function(aid) {
		var s = "";
		var t = [];
		s += getPubText(aid, "Crossref", t);
		s += "<br>"
		s += getPubText(aid, "DataCite", t);
		return s;
	}

	var addResources = function(aid) {
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

	var addTopRow = function(aid) {
		var dc = IFD.getCollectionSetById(aid);
		var id = aid.id;
		var dItems = IFD.items[id] = {};
		var sep = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" 
			//+ ==&gt;
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		var s = "";
		if (dc.samples) {
			if (s)
				s += sep;
			var items = dItems["samples"] = getIDs(dc.samples);
			s += "<a href=\"javascript:IFD.showSamples('"+aid.id+"')\">Samples(" + items.length + ")</a>";
		}
		if (dc.compounds) {
			var items = dItems["compounds"] = getIDs(dc.compounds);
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showCompounds('"+id+"')\">Compounds(" + items.length + ")</a>";
		}
		if (dc.structures) {
			var items = dItems["structures"] = getIDs(dc.structures);
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showStructures('"+aid.id+"')\">Structures(" + items.length + ")</a>";
		}
		if (dc.spectra) {
			var items = dItems["spectra"] = getIDs(dc.spectra);
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showSpectra('"+id+"')\">Spectra(" + items.length + ")</a>";
		}

	// TODO fold this information into "samples"
		if (dc["sample-spectra associations"]) {
			var items = dItems["samplespectra"] = getIDs(dc["sample-spectra associations"]);
//			s += "<a href=\"javascript:IFD.showSampleSpectraAssociations('"+aid.id+"')\">Sample-Spectra Associations(" + items.length + ")</a>";
		}
		return "<tr><td><b>Collections:</b>&nbsp;&nbsp;</td><td>"
		  + (s ? s : "(none found)") + "</td></tr>";
	}

	var showSample = function(aidID,id) {
		var sample = IFD.collections[aidID].samples[id];
		var specids = IFD.getSpectrumIDsForSample(aidID, id);
		var structureIDs = IFD.getStructureIDsForSpectra(aidID,specids);
		var s = getHeader("Sample " + id); 
		s += showCompoundStructures(aidID,structureIDs, false);
		var smiles = IFD.getSmilesForStructureID(aidID, structureIDs[0]);
		s += showCompoundSpectra(aidID,specids,smiles,true);
		s += "<hr style='color:red'>";
		return s;
	}


	//
	// So "by sample" means:
	//
	// 1. list by sample
	// 2. for each sample:
//	    a. show the originating ID, maybe link to ELN?
//	    b. show all spectra associated with this sample
//	    c. show all structures associated with these spectra -- should be just one? or could be a mixture.
//	    d. highlight any case where compound/spectra relationships are different. 
	//
//	       For example, this is OK:
	//
//	         Sample A:
//	             1H   Compound 1 and Compound 2
//	             13C  Compound 1 and Compound 2
	//
//	       But this is not OK:
	//
//	         Sample B:
//	             1H   Compound 1
//	             13C  Compound 2
	//
//	       Something is amiss here!

	var getObjectProperty = function(o, what) {
		return o.properties && o.properties[what] || "";
	}

	var showSpectrum = function(aidID,id) {
		var spec = IFD.collections[aidID].spectra[id];
		var structureIDs = IFD.getStructureIDsForSpectra(aidID, [id]);
		var sampleID = spec.properties && spec.properties.originating_sample_id;
		var sid = (IFD.byID ? id : spec.id); 
		var s = "<table padding=3><tr><td valign=top>"
			+ getHeader("Spectrum " + sid) + "<h3>" 
			+ (sampleID ? "&nbsp;&nbsp;&nbsp; sample " + sampleID : "")
			+ "</h3></td>"; 
		var title = getObjectProperty(spec, "expt_title");
		if (title)
			s += "<td>&nbsp;&nbsp;</td><td><b>" + title + "</b></td>"
		s += "</tr></table>";
		var smiles = IFD.getSmilesForStructureID(aidID, structureIDs[0]);
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
		var keys = IFD.getCompoundCollectionKeys();
		var structureIDs = cmpd[IFD.itemsKey][keys.structures];
		var spectraIDs = cmpd[IFD.itemsKey][keys.spectra];
		var props = cmpd.properties;
		var params = cmpd.attributes;
		var label = cmpd.label || cmpd.id;
		var s = getHeader(label.startsWith("Compound") ? label : "Compound " + label); 
		s += "<table>" + addPropertyRows("",props, null, false) + "</table>"
		s += "<table>" + addPropertyRows("",params, null, false) + "</table>"

		s += showCompoundStructures(aidID,structureIDs, false);
		var smiles = IFD.getSmilesForStructureID(aidID, structureIDs[0]);
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
		var sid = struc.id;
		var props = struc.properties;
		var reps = struc.representations;

		s += "<td rowspan=2 valign=\"top\">";
		if (showID) {
			var h = (id.indexOf("Structure") == 0 ? removeUnderline(sid) : "Structure " + sid);
			s += "<span class=structurehead>"+ (IFD.mode == MODE_STRUCTURES ? getHeader(h) : h) + "</span><br>";
		}
		s += "from SMILES:<br>" + IFD.getStructureVisual(reps);
		s += IFD.getStructureSpectraPredictions(reps);
		s += "</td>";
		s += "<td>" + addRepresentationTable(false, aidID, reps, "png") + "</td>";
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
		var max = IFD.properties.MAX_IMAGE_DIMENSIONS;
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
				+ (IFD.properties.standalone ? "" 
				: "<br><br><a href=\"javascript:IFD.showCollection('"+aid.id+"')\">Collection Folder</a>") : "");
		s += loadSearchPanel(aid);
		$("#search").html(s);
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

	var addRepresentationTable = function(isData, aidID, reps, firstItem) {
		var s = ""
		for (var i = 0; i < reps.length; i++) {
			var type = IFD.shortType(reps[i].representationType);
			if (type == firstItem) {
				s = addRepresentationRow(isData, aidID, reps[i], type) + s;
			} else {
				s += addRepresentationRow(isData, aidID, reps[i], type);
			}
		}
		return "<table>" + s + "</table>";
	}

	var addRepresentationRow = function(isData, aidID, r, type) {
		var s = ""; 
		var shead = //"";//
			// TODO data type xrd is in the wrong place
		(type == "png" || isData ? "" : "<span class=repname>" + clean(type) + "</span> ");
		if (r.data) {
			if (r.data.indexOf(";base64") == 0) {
				var imgTag = "<img id=img" + (++divId)  + " onload=IFD.checkImage(" + divId + ")" +  " src=\"" + "data:" + r.mediaType + r.data + "\"</img>";
				s += addPathForRep(aidID, r.ref, -1, imgTag);
			} else {
				if (r.data.length > 30) {
					s += anchorHide(shead, r.data);
					shead = "";
				} else {
					s += r.data;
				}
			}
		} else {
			s += " " + addPathForRep(aidID, r.ref, r.len, null);
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
		var s = "<tr><td id='q1' rowspan=2 style=\"width:100px\" valign=top>" 
			+ (IFD.byID ? id : "") 
		+ getSpectrumPrediction(spec.properties, smiles)
		+ "</td>"
		s += "<td id='q2'>";
			s += addRepresentationTable(true, aidID, spec.representations);
		s += "</td></tr>";
		s += "<tr><td><table>";
			s += addPropertyRows("IFD&nbsp;Properties", spec.properties, null, true);
			s += addPropertyRows("More&nbsp;Attributes", spec.attributes, null, true);
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
				s0 = "<tr><td><h4>" + (hideDiv ? "<div class=hiddendiv onclick=IFD.toggleDiv(" + id + ")><u>" + name + "...</u></div>" : name) + "</h4></td></tr>";
			if (key == firstItem) {
				s = addPropertyLine(key, map[key]) + s;
			} else {
				s += addPropertyLine(key, map[key]);
			}
		}
		if (hideDiv) {
			s = "<tr><td colspan=2><div id=prop" + id + " style='display:none'><table><tr><td><u>" + s + "</u></td></tr></table></div></td></tr>"
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

	var addPathForRep = function(aidID, ref, len, value) {
		var shortName = ref.localName || shortFileName(ref.localPath);
		var url = ref.url || ref.doi || (ref.localPath ? fileFor(aidID, ref.localPath) : null);
		if (value) {
			s = "<a target=_blank href=\"" + url + "\">" + value + "</a>"
		} else if (shortName.endsWith(".png")) {
			s = "<img id=img" + (++divId)  + " onload=IFD.checkImage(" + divId + ")" +  " src=\"" + url +"\">"; 
		} else {
			s = "<a target=_blank href=\"" + url + "\">" + shortName + "</a>" + " (" + getSizeString(len) + ")";
		}
		return s;
	}
	
	var addPathRef = function(aidID, path, len) {
		var url = fileFor(aidID, path);
		return "<a target=_blank href=\"" + url + "\">" 
			+ shortFileName(path) + "</a>" + " (" + getSizeString(len) + ")";
	}
		
	var shortFileName = function(f) {
		var pt = f.lastIndexOf("/");
		f = f.substring(pt + 1);
		pt = f.lastIndexOf("..");
		return (pt < 0 ? f : f.substring(pt + 2));
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
	
	var JMEshowSearch = function(fReturn) {

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

	IFD.getStructureVisual = function(reps) {
		var types = IFD.getRepTypes(reps);
		if (types.png && types.png.data || !types.smiles || !types.smiles.data) return "";
		return IFD.getCDKDepictImage(types.smiles.data);
	}
	
	IFD.getCDKDepictImage = function(SMILES) {
		  var w        = IFD.properties.imageDimensions.width; // mm
		  var h        = IFD.properties.imageDimensions.height; // mm
		  var hdisplay = "bridgehead";
		  var annotate = "cip";
		  return "<img  id=img" + (++divId) + " onload=IFD.checkImage(" + divId + ")"
	//		+ " style=\"width:" + w + "px;height:"+h+"px\" "
			+ " src=\"https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=" 
			+ encodeURIComponent(SMILES) + "&w=" + w + "&h=" + h + "&hdisp=" + hdisplay 
			+ "&showtitle=false&zoom=1.7"
			// + &annotate=" + annotate
			+ "\"/>";
		}
	
	
	IFD.getStructureSpectraPredictions = function(reps) {
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
	
	var getPredictAnchor = function(type, smiles, text) {
		var url = NMRDB_PREDICT_SMILES.replace("%TYPE%", type.toLowerCase()).replace("%SMILES%", smiles);
		return "<a target=_blank href=\"" + url + "\">" + (text ? text : type) + "</a>"
	
	}
	
	var getCdkDepictImage = function(SMILES) {
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
	
	
	IFD.showHideDiv = function(d) {
		d = $("#" + d);
		var isVisible = ("block" == d.css("display"));	 
		d.css({display:(isVisible ? "none" : "block")});
	}

})();


