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

	var MAIN_SEARCH_SUB = "main_search_sub";
	var MAIN_SEARCH_TEXT = "main_search_text";
	var MAIN_SEARCH_PROP = "main_search_prop";
	var MAIN_SEARCH     = "main_search";
	var MAIN_SUMMARY    = "main_summary";
	
	var NMRDB_PREDICT_SMILES = "https://www.nmrdb.org/service.php?name=%TYPE%-prediction&smiles=%SMILES%";
	// where type = 1h, 13c, cosy, hmbc, hsqc
	
	var aLoading = null;
	var divId = 0;
	
	var dirFor = function(aidID) {
		return IFD.properties.baseDir + (IFD.findingAidID == '.' ? "" : "/" + aidID);
	}
	
	var fileFor = function(aidID, fname) {
		return dirFor(aidID) + "/" + fname;
	}
	
	//external
	IFD.searchText = function(aidID) {
		IFD.toggleDiv(MAIN_SEARCH_SUB,"none");
		var text = prompt("Text to search for?");
		if (text) {
			var indexes = IFD.getCompoundIndexesForText(aidID, text);
			if (indexes)
				IFD.showCompounds(aidID, indexes);
		}
	}

	//external
	IFD.searchProperties = function(aidID) {
		IFD.toggleDiv(MAIN_SEARCH_SUB,"none");
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
		var aids = IFD.findingAids || ["."];
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
			$("#faselectiondiv").html(s);
		}
		IFD.createJmol();
	}	
	
	// external
	IFD.select = function(n, collection) {
		IFD.mainMode = MAIN_SUMMARY;
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
		IFD.mainText = null;
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
		if (IFD.findingAidID == '.' || IFD.findingAidID == aidID) return;
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
			$.ajax({url:IFD.findingAidFile, dataType:"json", success:callbackLoaded, error:function(){callbackLoadFailed()}});
		} else {
			loadAid(aid, collection);
		}
	}

	//external
	IFD.showSamples = function(aidID, ids) {
		IFD.select(aidID);
		setMode(IFD.MODE_SAMPLES);
		ids || (ids = IFD.items[aidID][IFD.MODE_SAMPLES]);
		var s = "<table>";
		for (var i = 0; i < ids.length; i++) {
			s += "<tr class=\"tableRow" + (i%2) + "\">" + showSample(aidID,ids[i]) + "</tr>";
		}
		s += "</table>";
		setResults(s);
	}
	
	//external
	IFD.showCompounds = function(aidID, ids) {
		loadMainSummary(IFD.aid, false);
		IFD.select(aidID);
		setMode(IFD.MODE_COMPOUNDS);
		ids || (ids = IFD.items[aidID][IFD.MODE_COMPOUNDS]);
		var s;
		if (ids.length == 0) {
			s = "no compounds found";
		} else {
			s = "<table>";
			for (var i = 0; i < ids.length; i++) {
				s += "<tr class=\"tablerow" + (i%2) + "\">" + showCompound(aidID,ids[i]) + "</tr>";
			}
			s += "</table>";
		}
		setResults(s);
	} 
	
	//external
	IFD.showSpectra = function(aidID, ids) {
		loadMainSummary(IFD.aid, false);
		IFD.select(aidID);
		setMode(IFD.MODE_SPECTRA);
		ids || (ids = IFD.items[aidID][IFD.MODE_SPECTRA]);
		var s = "<table>";
		for (var i = 0; i < ids.length; i++) {
			s += "<tr class=\"tableRow" + (i%2) + "\">" + showSpectrum(aidID,ids[i]) + "</tr>";
		}
		s += "</table>";
		setResults(s);
	} 
	
	//external
	IFD.showStructures = function(aidID, ids) {
		loadMainSummary(IFD.aid, false);
		IFD.select(aidID);
		setMode(IFD.MODE_STRUCTURES);
		ids || ids === false || (ids = IFD.getItems(aidID, IFD.MODE_STRUCTURES));
		var s = showCompoundStructures(aidID,ids, false, true);
		setResults(s);
	} 	

	IFD.getItems = function(aidID, mode) {
		var ids = IFD.items[aidID][mode];
		return ids;
	}
	
	IFD.showCollection = function(aidID) {
		window.open(dirFor(aidID), "_blank");
	}

	IFD.showAid = function(aidID) {
		window.open(fileFor(IFD.findingAidID, IFD.properties.findingAidFileName), "_blank");
	}

	IFD.showVersion = function(aid) {
		$("#version").html(aid.version);
	}


	var setMode = function(mode) {
	  IFD.resultsMode = mode;
	  IFD.headers = [];
	}

	//external
	// local methods

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

	var callbackLoadFailed = function(x) {
		alert([x,y,z]);
		return;
	}


	var loadAid = function(aid, collection) {
		if (!aLoading) {
			IFD.mainMode = MAIN_SUMMARY;
			setTop("");
			setMain("");
		}
		setResults("");
		setMoreLeft("");
		IFD.cachePut(IFD.findingAidID, aid);
		IFD.aid = aid;
		loadTop(aid);
		loadAidPanels(aid);
		if (aLoading) {
			if (aLoading.length > 0) {
				IFD.loadSelected(aLoading);
				return;
			}
			aLoading = null;
			IFD.findingAidID = null;
			setMoreLeft("");
		} else {
			IFD.showVersion(aid);
		}
	}

	var loadAidPanels = function(aid) {
	 	loadMain(aid);
		setMoreLeft(aid);
	}

	var loadTop = function(aid) {
		var s = "&nbsp;&nbsp;&nbsp;" + aid.id 
		+ (aLoading ? 
			"&nbsp;&nbsp; <a target=_blank href=\"" + IFD.findingAidFile + "\">view "+IFD.findingAidFile.split("/").pop()+"</a>"
			+ "&nbsp;&nbsp; " + addPathRef(aid.id, aid.collectionSet.properties.ref, aid.collectionSet.properties.len)
		: ""); 
		s = "<h3>" + IFD.shortType(aid.ifdType) + s + " </h3>";
		setTop(s);
	}

	var loadMain = function(aid) {
		aid || (aid = IFD.aid);
		setResults("");
		clearJQ("#contents");
		IFD.toggleDiv(MAIN_SEARCH_SUB,"none");
		switch (IFD.mainMode) {
		case MAIN_SUMMARY:
			return loadMainSummary(aid, true);
		case MAIN_SEARCH:
			return loadMainSearch(aid.id);
		}
	}
	
	var loadMainSummary = function(aid, isAll) {
//		if (!getInnerHTML(MAIN_SUMMARY)) {
		var s;
		if (isAll && IFD.mainText) {
			s = IFD.mainText;
		} else {
			s = "<table>";
			s += addTopRow(aid);
			if (isAll) {
				s += addPubInfo(aid);
				s += addDescription(aid.collectionSet);
				s += addResources(aid);
			}
			s += "</table>";
			if (isAll) {
				IFD.mainText = s;
			}
		}
		setMain(s);
		showMain();
	}

	var loadMainSearch = function(aidID) {
		if (!getInnerHTML(MAIN_SEARCH)) {
			var s = "";
			aidID || (aidID = "");
			s += "<h3>Search</h3>";
			s += `<a href="javascript:IFD.searchStructures('${aidID}')">substructure</a>`;
			s += `&nbsp;&nbsp;&nbsp;<a href="javascript:IFD.searchText('${aidID}')">text</a>`;
			s += `&nbsp;&nbsp;&nbsp;<a href="javascript:IFD.searchProperties('${aidID}')">properties</a>`;
			s += `<div id=${MAIN_SEARCH_TEXT} style="display:none"></div>`;
			s += `<div id=${MAIN_SEARCH_PROP} style="display:none"></div>`;
			setMain(s);
		}
		showMain();
	}
	
	var showMain = function() {
		var altmode = (IFD.mainMode == MAIN_SUMMARY ? MAIN_SEARCH : MAIN_SUMMARY);
		$("#" + altmode).css({display:"none"});
		$("#" + IFD.mainMode).css({display:"block"});
	}
	
	var addDescription = function(element) {
		var s = "";
		if (element.description)
			s += "<tr><td valign=top>Description</td><td valign=top>" + element.description + "</td></tr>";
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
		for (var i = 0; i < resources.length; i++) {
			var ref = resources[i].ref;
			if (ref.indexOf("http") == 0) {
				var size = getSizeString(resources[i].len);
				ref = "<a target=_blank href=\"" + ref + "\">" + ref + (size ? " ("+size+")":"") + "</a>"
				s += "<tr><td>Data&nbsp;Origin</td><td>"+ref+"</td></tr>";
			}
	      }
		return s;
	}

	var addTopRow = function(aid) {
		var dItems = IFD.getCollectionSetItems(aid);
		var id = aid.id;
		var sep = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" 
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		var s = "";
		var items = dItems[IFD.MODE_SAMPLES];
		if (items) {
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showSamples('"+aid.id+"')\">Samples(" + items.length + ")</a>";
		}
		items = dItems[IFD.MODE_COMPOUNDS];
		if (items) {
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showCompounds('"+id+"')\">Compounds(" + items.length + ")</a>";
		}
		items = dItems[IFD.MODE_STRUCTURES];
		if (items) {
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showStructures('"+aid.id+"')\">Structures(" + items.length + ")</a>";
		}
		items = dItems[IFD.MODE_SPECTRA];
		if (items) {
			if (s)
				s += sep;
			s += "<a href=\"javascript:IFD.showSpectra('"+id+"')\">Spectra(" + items.length + ")</a>";
		}

		items = dItems[IFD.MODE_SAMPLESPECTRA];
		if (items) {
			// TODO fold this information into "samples"
//			s += "<a href=\"javascript:IFD.showSampleSpectraAssociations('"+aid.id+"')\">Sample-Spectra Associations(" + items.length + ")</a>";
		}
		return "<tr><td><b>Collections:</b>&nbsp;&nbsp;</td><td>"
		  + (s ? s : "(none found)") + "</td></tr>";
	}

	var showSample = function(aidID,id) {
		var sample = IFD.getCollection(aidID).samples[id];
		var specids = IFD.getSpectrumIDsForSample(aidID, id);
		var structureIDs = IFD.getStructureIDsForSpectra(aidID,specids);
		var s = getHeader("Sample/s", "Sample " + id); 
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
		var spec = IFD.getCollection(aidID).spectra[id];
		var structureIDs = IFD.getStructureIDsForSpectra(aidID, [id]);
		var sampleID = spec.properties && spec.properties.originating_sample_id;
		var sid = (IFD.byID ? id : spec.id); 
		var s = "<table padding=3><tr><td valign=top>"
			+ getHeader("Spectrum/a  ", "Spectrum " + sid) + "<h3>" 
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

	var getHeader = function(types, name, description) {
		IFD.contentHeader = types;
		var key = removeSpace(name) + "_" + ++divId
		IFD.headers.push([key,name]);
		return "<a name=\"" + key + "\"><h3>" + name + "</h3></a>"
		+ (description ? description + "<p>" : "<p>");
	}

	var showCompound = function(aidID,id) {
		var cmpd = IFD.getCollection(aidID).compounds[id];
		var keys = IFD.getCompoundCollectionKeys();
		var structureIDs = cmpd[IFD.itemsKey][keys.structures];
		var spectraIDs = cmpd[IFD.itemsKey][keys.spectra];
		var props = cmpd.properties;
		var params = cmpd.attributes;
		var label = cmpd.label || cmpd.id;
		var s = getHeader("Compound/s", label.startsWith("Compound") ? label : "Compound " + label, cmpd.description); 
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
					s += showCompoundStructures(thisaid, ids,false,i + 1);
					ids = [];
					thisaid = aid;
				}
			}		
		} else {
			var showID = (ids.length > 1);
			for (var i = 0; i < ids.length; i++) {	
				if (aidID) 	{
					s += "<tr>" + showCompoundStructure(aidID, ids[i], showID, i + 1) + "</tr>";
				} else {
					s += "<tr>" + showCompoundStructure(ids[i][0], ids[i][1], showID, i + 1) + "</tr>";
				}
			}
		}
		s += (haveTable ? "" : "</table>");
		return s;
	}


	var showCompoundStructure = function(aidID, id, showID, tableRow) {
		var cl = (tableRow > 0 ? " class=tablerow" + (tableRow%2) : "");
		var s = "<td" + cl + "><table cellpadding=10><tr>";
		var struc = IFD.getCollection(aidID).structures[id];
		var sid = struc.id;
		var props = struc.properties;
		var reps = struc.representations;

		s += "<td rowspan=2 valign=\"top\">";
		if (showID) {
			var h = (id.indexOf("Structure") == 0 ? removeUnderline(sid) : "Structure " + sid);
			s += "<span class=structurehead>"+ (IFD.resultsMode == IFD.MODE_STRUCTURES ? getHeader("Structure/s", h) : h) + "</span><br>";
		}
		v = IFD.getStructureVisual(reps);
		if (v){
			s += "<table border=1><tr><td>";
			s += "from SMILES:<br>" + v;
			s += "</td></tr></table>"
		}
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
		ids || (ids = IFD.items[aidID][IFD.MODE_SPECTRA]);
		var s = "<table>"
			if (withTitle)
				s += "<tr><td style=\"width:100px\" valign=top><span class=spectitle>Spectra</span></td></tr>";
		for (var i = 0; i < ids.length; i++) {
			s += addCompoundSpectrumRow(aidID, ids[i], smiles, i + 2);
		}
		s += "</table>";
		return s;
	}

	var setTop = function(s) {
		s += '<a href="javascript:IFD.showSummary()">summary</a>&nbsp;&nbsp;&nbsp;<a href="javascript:IFD.showSearch()">search</a>'
		addOrAppendJQ("#top",s, true);
	}

	var getInnerHTML = function(id) {
		return $("#" + id).html();
	}

	var setMain = function(s) {
		addOrAppendJQ("#" + IFD.mainMode,s, true);
	}

	var setResults = function(s) {
		addOrAppendJQ("#results",s, true);
		loadContents(s && !s.startsWith("no "));
	}

	var loadContents = function(hasContent) {
		clearJQ("#contents");
		if (!hasContent)
			return;
		var n = IFD.headers.length;
		var type = IFD.contentHeader.split("/");
		type = (n == 1 ? type[0] : type[0].substring(0, type[0].length + 1 - type[1].length) + type[1]);
		var s = "<b>" + n + " " + type + "</b><br><table>";
		for (var i = 0; i < n; i++) {
			var h = IFD.headers[i];
			var key = h[0];
			var val = h[1];
			s += "<tr><td><a href=#" + key + ">"+val+"</a></td></tr>"	
		}
		s += "</table>"
		$("#contents").html(s);

	}

	IFD.showSummary = function() {
		IFD.mainMode = MAIN_SUMMARY;
		loadMain();
	}

	IFD.showSearch = function() {
		IFD.mainMode = MAIN_SEARCH;
		loadMain();		
	}
	
	var setMoreLeft = function(aid) {
		var s = (aid ? "<br><a href=\"javascript:IFD.showAid('"+aid.id+"')\">Show Finding Aid</a><hr>"
				+ (IFD.properties.standalone ? "" 
				: "<br><br><a href=\"javascript:IFD.showCollection('"+aid.id+"')\">Collection Folder</a>") : "");
		$("#moreleftdiv").html(s);
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

	var addOrAppendJQ = function(jqid, s, andClear) {
		if (!s || andClear)
			clearJQ(jqid);
		if (s) {
			$(jqid).append("<hr>");
			$(jqid).append(s);
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
				s += addPathForRep(aidID, r.ref, -1, imgTag, null);
			} else {
				if (r.data.length > 30) {
					s += anchorHide(shead, r.data);
					shead = "";
				} else {
					s += r.data;
				}
			}
		} else {
			s += " " + addPathForRep(aidID, r.ref, r.len, null, r.mediaType);
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

	var addCompoundSpectrumRow = function(aidID, id, smiles, tableRow) {
		var spec = IFD.getCollection(aidID).spectra[id];
		var cl = (tableRow > 0 ? " class=tablerow" + (tableRow%2) : "");
		var s = "<tr><td"+ cl + " id='q1' rowspan=2 style=\"width:100px\" valign=top>" 
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
		if (hideDiv) {
			for (var key in map)
				n++;
			if (n < 6)
				hideDiv = false;
		}
	    n = 0;
		for (var key in map) {
			if (n++ == 0 && name)
				s0 = "<tr><td><h4>" + (hideDiv ? "<div class=hiddendiv onclick=IFD.toggleDiv(\"prop" + id + "\")><u>" + name + "...</u></div>" : name) + "</h4></td></tr>";
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

	IFD.toggleDiv = function(id, mode) {
		var d = document.getElementById(id);
		switch (mode) {
		case "block":
		case "none":
			return d.style.display = mode;
		default:
			return d.style.display = (d.style.display == "none" ? "block" : "none");
		}
	}

	var addPathForRep = function(aidID, ref, len, value, mediaType) {
		var shortName = ref.localName || shortFileName(ref.localPath);
		var url = ref.url || ref.doi || (ref.localPath ? fileFor(aidID, ref.localPath) : null);
		mediaType = null;// nah. Doesn't really add anything || (mediaType = "");
		if (value) {
			s = "<a target=_blank href=\"" + url + "\">" + value + "</a>"
		} else if (shortName.endsWith(".png")) {
			s = "<img id=img" + (++divId)  + " onload=IFD.checkImage(" + divId + ")" +  " src=\"" + url +"\">"; 
		} else {
			s = "<a target=_blank href=\"" + url + "\">" + shortName + "</a>" + " (" + getSizeString(len) + (mediaType ? " " + mediaType : "") + ")";
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
		IFD.toggleDiv(MAIN_SEARCH_SUB,"block");
		IFD.jmeReturn = fReturn;
		if (!IFD.JME) {
			IFD.createJSME();			
		}
	}

	var JMESmartsReturn = function(aidID) {
		aidID || (aidID = IFD.findingAidID);
		var ids = IFD.jmolGetSmartsMatch(aidID);
		var indexes = IFD.getCompoundIndexesForStructures(aidID, ids);
		IFD.showCompounds(aidID, indexes);
	}
	
	IFD.searchStructures = function() {
		JMEshowSearch(JMESmartsReturn); 
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
		  var code = encodeURIComponent(SMILES);
		  var dim = IFD.cacheGet(code);
		  var onload;
		  divId++;
		  if (dim) {
			  w = dim.w;
			  h = dim.h;
			  onload = "";  
		  } else {
			  onload = " onload=IFD.checkImage(" + divId + ",true)";
			  IFD.cachePut("img" + divId, code);
		  }
		  var src =
		  		"https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=" 
				+ code + "&w=" + w + "&h=" + h + "&hdisp=" + hdisplay 
				+ "&showtitle=false&zoom=1.7";
		  var s = "<img  id=\"img" + divId + "\"" + onload 
			+ " src=\"" + src + "\"/>";
//		  if (!data){
//			  IFD.cachePut(code, "img" + divId);
//			  IFD.cachePut("img" + divId, code);
//		  }
		  return s;
		}
		

	IFD.checkImage = function(id, doCache) {
		id = "img" + id;
		var max = IFD.properties.MAX_IMAGE_DIMENSIONS;
		var image = document.getElementById(id);
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
		var code = IFD.cacheGet(id);
		if (doCache && code && IFD.cacheGet(code) == id) {
			IFD.cachePut(code, {w:w, h:h});
			IFD.cachePut(id, null);
		}
		return image;
	}

// ah... but one cannot cache an image from another server.
//	IFD.getImageData = function(img) {		
//		var canvas = document.createElement('canvas');
//	    var ctx = canvas.getContext('2d');   
//	    canvas.width = img.width;
//	    canvas.height = img.height;   
//	    ctx.drawImage(img, 0, 0);
//	    return canvas.toDataURL('image/jpeg');
//	}
	
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
	
})();


