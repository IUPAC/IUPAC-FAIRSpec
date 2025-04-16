// Bob Hanson hansonr@stolaf.edu
// last revised 2021.07.08

demo = {

aid:null,
findingAids:[],
findingAidFile:null,
findingAidMap:{},
strucspecList:null,
whatSpec:null,
whatStructure:null,
messages:[],
messageCount:0,
query:document.location.search,
loadCount:0,

cacheFindingAid: function(fa) {
	if (!fa)return;
	demo.clearAll();
	demo.findingAidFile = rootdir + fa + "._IFS_findingaid.json";
	$.ajax({url:demo.findingAidFile, dataType:"json", success:function(json) {demo.register(fa, json, false)}});	
},

register: function(fa, json, andLoad) {
	demo.findingAidMap[fa] = json;
	if (andLoad)demo.loaded(json);
	demo.loadCount++;
	if (demo.loadCount == demo.findingAids.length) {
		demo.finalize();
	}
},

finalize: function() {
	var query = demo.query;
	if (query) {
		var pub = demo.getQueryField(query, "pub");
		var struc = demo.getQueryField(query, "struc");
		var spec = demo.getQueryField(query, "spec");
		if (pub || struc || spec) {
			demo.search(pub, struc, spec);
			return;
		}
	}
},

loadFindingAids: function() {
    $.ajax({url:rootdir + "_IFS_findingaids.json", dataType:"json", 
    	success:demo.loadLeftPanel,
    	error:function(x){ 
    		alert("failed - do you have your browser enabled for local file reading?");
    		$("#right_main").html("<a target=_blank href=\"http://wiki.jmol.org/index.php/Troubleshooting/Local_Files\">http://wiki.jmol.org/index.php/Troubleshooting/Local_Files</a>");
    	} 
    });
},

getQueryField: function(query, key) {
	return decodeURIComponent(("&" + query.substring(1) + "&" + key + "=").split(key + "=")[1].split("&")[0]); 
},

loadLeftPanel: function(json) {
	var aids = demo.findingAids = json.findingaids;
	$("#nfa").html("" + aids.length);
	demo.loadLeftTop();
	demo.loadLeftSelect();
},

loadLeftTop: function() {
	var s = "";
	s += "pub search:<br><input id=searchpub type=text style='width:140px' onchange='demo.search(this.value, null, null)'/>";
	s += "structure search:<br><input id=searchstruc type=text style='width:140px' onchange='demo.search(null, this.value, null)'/>";
	s += "spectrum search:<br><input id=searchspec type=text style='width:140px' onchange='demo.search(null, null, this.value)'/>";
	s += '<br><a href="javascript:demo.clearSearch()">Clear Search</a>';
	$("#left_top").html(s);
},

clearSearch: function() {
 $("#searchpub").val("");
 $("#searchstruc").val("");
 $("#searchspec").val("");
 demo.query = null;
 demo.loadFindingAids();
},

search: function(whatPub, whatStruc, whatSpec) {
	demo.clearAll();
	demo.loadLeftSelect(whatPub, whatStruc, whatSpec);
	var d = $("#articles")[0];
	// todo: -1 here for "all"?
	demo.select(d ? 1 : 0);
},

loadLeftSelect: function(whatPub, whatStruc, whatSpec) {
	whatPub = whatPub && (whatPub.toLowerCase());
	whatStruc = whatStruc && (whatStruc.toLowerCase());
	whatSpec = whatSpec && (whatSpec.toLowerCase());
	var aids = demo.findingAids;
	demo.whatSpec = whatSpec || null;
	demo.whatStructure = whatStruc || null;
	var s = "";
	var n = 0;
	var defFound = !(demo.whatSpec || demo.whatStructure);
	s += '<select id=articles onchange="demo.loadSelected(this.selectedOptions[0].value)"><option>Select an ACS article</option>'
	demo.clearFound(defFound);
	for (var ai = 0; ai < aids.length; ai++) {
		var fa = aids[ai];
		var found = false;
		var aid = (demo.findingAidMap[fa] ? demo.findingAidMap[fa]["IFS.findingaid"] : null);
		var structures = (aid && aid.structures && aid.structures.list);
		var spectra = (aid && aid.specData && aid.specData.list);
		if (whatPub) {
			var json = aid.pubInfo;
			var stext = JSON.stringify(json).toLowerCase();
			found = (stext.indexOf(whatPub) >= 0);
		} else if (whatStruc) {
			if (structures) {
				for (var i = 0; i < structures.length; i++) {
					var stext = JSON.stringify(structures[i]).toLowerCase();	
					structures[i]._found = (stext.indexOf(whatStruc) >= 0);
					if (structures[i]._found) {
						found = true;						
					}
				}
				var list = aid.structureSpecData.list;
				if (list) {
					for (var li = 0; li < list.length; li++) {
						if (structures[list[li].struc[0]]._found) {
							var data = list[li].data;
							for (var j = 0; j < data.length; j++) {
								spectra[data[j]]._found = true;
							}
						}
					}
				}						
			}
		} else if (whatSpec) {
			if (spectra) {
				for (var i = 0; i < spectra.length; i++) {
					var stext = JSON.stringify(spectra[i]).toLowerCase();	
					var b = spectra[i]._found = (stext.indexOf(whatSpec) >= 0);
					if (b && !aid.structureSpecData) {
						found = true;
					}
				}
				if (aid.structureSpecData) {
					var list = aid.structureSpecData.list;
					if (list) {
						for (var i = 0; i < list.length; i++) {
							var data = list[i].data;
							for (var j = 0; j < data.length; j++) {
								if (spectra[data[j]]._found) {
									structures[list[i].struc[0]]._found = true;
									found = true;
								}
							}
						}
					}
				}
			}
		} else {
			found = true;
			demo.cacheFindingAid(fa);
		}
		if (found) {
			s += "<option value=\"" + fa  + "\">" + fa + "</option>"
			n++;
		}
	}
	s += '</select>'
	if (n > 0) {
		$("#left_select").html(s);
	} else {
		$("#left_select").html("<br>nothing found");
	}
},

clearFound: function(aid, defFound) {
	var aids = demo.findingAids;
	for (var ai = 0; ai < aids.length; ai++) {
		var fa = aids[ai];
		var aid = (demo.findingAidMap[fa] ? demo.findingAidMap[fa]["IFS.findingaid"] : null);
		if (aid) {
			if (aid.structures) {
				var structures = aid.structures.list;
				for (var i = 0; i < structures.length; i++) {
					structures[i]._found = defFound;
				}
			}
			if (aid.specData) {
				var spectra = aid.specData.list;
				for (var i = 0; i < spectra.length; i++) {
					spectra[i]._found = defFound;
				}
			}
		}
	}
},

select: function(n) {
	var d = $("#articles")[0];
	if (!d)
		return;
		demo.clearAll();
	if (n < 0) {
		d.selectedIndex = 0;
	} else {
		d.selectedIndex = n;
		demo.loadSelected(d.selectedOptions[0].value);
	}
},

loadSelected: function(fa) {
	if (!fa)return;
	if (fa == -1)
		fa = demo.aid.id;
	demo.clearAll();
	demo.findingAidFile = rootdir + fa + "._IFS_findingaid.json";
	var json = demo.findingAidMap[fa];
	if (json) {
		demo.loaded(json);
	} else {
		$.ajax({url:demo.findingAidFile, dataType:"json", success:function(json){demo.register(fa, json, true)}});
	}
},

loaded: function(json) {
	demo.loadRightPanel(json["IFS.findingaid"]);
},

loadRightPanel: function(aid) {
	demo.aid = aid;
 	demo.loadTop(aid);
 	demo.loadFiles();
	if (aid.structures){
		demo.loadStructures(aid.structures);
		demo.loadSpecData(aid.specData);
		demo.loadStructureSpecs(aid.structureSpecData);
	} else {
		demo.strucspecList = null;
		demo.loadSpecData(aid.specData);
	}
	demo.showAll();
},

loadFiles: function() {
	var d = $("#articles")[0];
	if (!d)
		return;
	var s = '<br><a href="javascript:demo.showAid()">Finding Aid</a>&nbsp;&nbsp;&nbsp;';
	s += '<a href="javascript:demo.showAll()">All Data</a>&nbsp;&nbsp;&nbsp;';

	$("#files").html(s);	
},

loadTop: function(aid) {
	var info = aid.pubInfo || "";
	var s = "<h3>IFS Finding Aid " + aid.id + "</h3>";
	s += "<table>";
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
		s += "<tr><td valign=top>url</td><td valign=top><a target=_blank href=\"" + info.url + "\">" + info.url + "</a></td></tr>";
	  } 
	}
	if (aid.sources && aid.sources.length) {
		s += "<tr><td valign=top>Dataset Source(s)</td><td valign=top>"
		var sep = "";
		for (var i = 0; i < aid.sources.length; i++) {
			var ref = aid.sources[i].ref;
			var len = aid.sources[i].len;
			var dir = ref.split('/');
			dir = dir[dir.length - 1];
			s += sep + "<a target=_blank href=\"" + ref + "\">" + ref + "</a> (" + demo.bytesFor(len) + ")"
				+ "&nbsp;&nbsp;<a target=_blank href=\""+rootdir + dir +"\">extracted collection</a>";
			sep = ", ";
		}
		s += "</td></tr>";
	}
	if (aid.properties["IFS.property.collection.ref"]) {
		s += "<tr><td valign=top>FAIRSpec Collection</td><td valign=top>"
		+ "<a target=_blank href=\"" + rootdir + aid.properties["IFS.property.collection.ref"] + "\">" 
		+ aid.properties["IFS.property.collection.ref"] + "</a> (" +  demo.bytesFor(aid.properties["IFS.property.collection.len"]) + ")"
		+"</td></tr>";
	}
	s += "</table>";
	$("#right_top").html(s);

},

loadStructures: function(structures) {
	if (!structures)return;
	var n = structures.list.length;
	var list = [];
	for (var i = 0; i < n; i++) {
		list[i] = structures.list[i];
		list[i].index = i;
	}
	list.sort(demo.structureSorter);
	var n = 0;
	var s = "";
	for (var i = 0; i < list.length; i++) {
		var struc = list[i];
		if (struc._found !== false) {
			s += "<option value=\"" + struc.index + "\">" + struc.name + "</option>";
			n++;
		}
	}
	s = '<select onchange="demo.loadStructure(this.selectedOptions[0].value)"><option>Structure Metadata ('+n+')</option>' + s + '</select>';
	$("#struc").html(s);
},

structureSorter: function(a, b) {
	var aname = a.name;
	var bname = b.name;
	var r = /([\D]*)(\d+)([\D]*)/;
	var an = aname.match(r) || aname;
	var bn = bname.match(r) || bname;
	var na = 1000 + +an[2];
	var nb = 1000 + +bn[2];
	an = "" + na + "" + an[1] + na + an[3];
	bn = "" + nb + "" + bn[1] + nb + bn[3];
	return (an < bn ? -1 : an > bn ? 1 : 0);
},

loadSpecData: function(specData) {
	if (!specData)return;
	var n = specData.list.length;
	var list = [];
	for (var i = 0; i < n; i++) {
		list[i] = specData.list[i];
		list[i].index = i;
	}
	list.sort(demo.specDataSorter || demo.structureSorter);
	var s = [];
	var n = 0
	var nameLast = null;
	var nlast = 1;
	for (var i = 0; i < list.length; i++) {
		var spec = list[i];
		if (spec._found !== false) {
			var name = spec.name;
			if (name == nameLast) {
				s.pop();
				s.push("<option value=\"" + (i-1) + "\">" + name + "(" + nlast + ")</option>")
				name += "(" + ++nlast + ")";
			} else {
				nameLast = name;
				nlast = 1;
			}
			s.push("<option value=\"" + i + "\">" + name + "</option>");
			n++;
		}		
	}
	s = '<select onchange="demo.loadSpec(this.selectedOptions[0].value)"><option>SpecData Metadata ('+n+')</option>' + s + '</select>'
	$("#spec").html(s);

},

loadStructureSpecs: function(structureSpecs) {
	demo.strucspecList = null;
	if (!structureSpecs || !structureSpecs.list)return;
	var n = structureSpecs.list.length;
	var list = [];
	for (var i = 0; i < n; i++) {
		list[i] = structureSpecs.list[i];
		list[i].index = i;
		var struc = demo.aid.structures.list[list[i].struc[0]];
		list[i]._found = struc._found;
		var name= struc.name;
		var sep = " (";
		var specs = list[i].data;
		for (var j = 0; j < specs.length; j++) {
			var spec = demo.aid.specData.list[specs[j]];
			if (name.indexOf(spec.name) >= 0)
				continue;
			name += sep + spec.name;
			sep = ", ";
		}
		name += ")";
		list[i].name = name;
	}
	list.sort(demo.structureSorter);
	demo.strucspecList = list;
	var s = "";
	var n = 0;
	for (var i = 0; i < list.length; i++) {
		if (list[i]._found !== false) {
			s += "<option value=\"" + list[i].index + "\">" + list[i].name + "</option>";
			n++;
		}
	}
	s = '<select onchange="demo.loadStructureSpec(this.selectedOptions[0].value)"><option>Select a structure-spectrum combination ('+n+')</option>' + s + '</select>';
	$("#structurespec").html(s);
},

loadStructure: function(i) {
	var struc = demo.aid.structures.list[+i];
	var s = JSON.stringify(struc, null, 2);
	$("#right_main").html("<pre>" + s + "</pre>");
},


loadSpec: function(i) {
	var spec = demo.aid.specData.list[+i];
	var s = JSON.stringify(spec, null, 2);
	$("#right_main").html("<pre>" + s + "</pre>");
},

loadStructureSpec: function(i) {
	demo.query = null;
	demo.clearFound(true);
	var s = "<table>" + demo.getStructureSpecHTML(demo.aid.structureSpecData.list[+i], false) + "</table>";
	$("#right_main").html(s);
//	var s = JSON.stringify(sspec, null, 2);
//	$("#right_main").html("<pre>" + s + "</pre>");
},

fixSmiles: function(smiles) {
// more here?
	return smiles.replaceAll("#","%23").replaceAll("%","%25").replaceAll("Xx","Xe");
},
	
getStructureHTML: function(struc) {
	var props = struc.properties;
	var smiles = props["IFS.property.struc.smiles"];
	var s = '<a href="javascript:demo.loadSelected(-1)">' + demo.aid.id + '</a> <b>' + struc.name + '</b> ' 
		+ demo.alertHref("InChI", props["IFS.property.struc.inchi"]) 
		+ demo.alertHref("InChIKey", props["IFS.property.struc.inchikey"])  
		+ demo.alertHref("SMILES", smiles);
	if (smiles) {
		s += '&nbsp;&nbsp;<a target=_blank href="https://chemapps.stolaf.edu/jmol/jmol.php?model='+demo.fixSmiles(smiles)+'">3D model</a>&nbsp;&nbsp;'
	}
	var imageRep = null;
	var cdxRep = null;
	if (struc.list) {
		
		for (var i = 0; i < struc.list.length; i++) {
			var rep = struc.list[i];
			switch (rep.type) {
			case "IFS.representation.struc.mol.2d":
				s += demo.repHref(rep, "mol-2d","");			
				break;
			case "IFS.representation.struc.cdx":
				if (cdxRep == null) {
					s += demo.repHref(rep, "cdx", "");
					cdxRep = rep;
				}
				break;
			case "IFS.representation.struc.png":
				if (imageRep == null) {
					imageRep = rep;
				}
				break;
			}
		}
	}
	if (smiles || imageRep) {
	  if (imageRep) {
		var path = demo.pathTo(imageRep.ref);
		s += '<br><img title="'+imageRep.source+'" src="'+path+'" style="width:250px"/>';
	  }
	  if (smiles) {
		s += (imageRep ? "" : "<br>") + '<img title="image from SMILES" src="https://cactus.nci.nih.gov/chemical/structure/'+demo.fixSmiles(smiles)+'/image?width=250&height=250"/>';
	  }
	}
	return s;
},


getSpectrumHTML: function(spec) {
	var name = spec.name;
	var s = "";
	var zip = "";
	for (var i = 0; i < spec.list.length; i++) {
		var rep = spec.list[i];
		var path = demo.pathTo(rep.ref);
		switch (rep.subtype) {
			case "application/octet-stream (mnova)":
				name = demo.repHref(rep, name, "");
				break;
			case "application/zip":
				name = demo.repHref(rep, name, "zip");
				break;
			case "image/png":
				s += "<br><img src=\"" + path + "\">";
				break;
			case "application/pdf":
				s += "<br>" + demo.repHref(rep, "pdf", "");
				break;
		}
	}
	var dim = "1D",nuc1, nuc2, pulseProg,solvent,temp,freq,manuf;
	
	for (prop in spec.properties) {
		var val = spec.properties[prop];
		switch (prop) {
			case "IFS.property.spec.nmr.expt.dim":
				dim = val;
				break;
			case "IFS.property.spec.nmr.expt.nucl.1":
				nuc1 = val;
				break;
			case "IFS.property.spec.nmr.expt.nucl.2":
				nuc2 = val;
				break;
			case "IFS.property.spec.nmr.expt.pulse.prog":
				pulseProg = val;
				break;
			case "IFS.property.spec.nmr.expt.solvent":
				solvent = val;
				break;
			case "IFS.property.spec.nmr.expt.temperature.K":
				temp = val;
				break;
			case "IFS.property.spec.nmr.instr.freq.nominal":
				freq = val;
				break;
			case "IFS.property.spec.nmr.instr.manufacturer.name":
				manuf = val;
				break;
			case "IFS.property.spec.nmr.instr.probe.id":
			case "IFS.property.spec.nmr.expt.id":
				break;
		}
	}
	if (freq) {
		var sp = "<br>" + dim + (!nuc1 ? "" : " " + nuc1 + (dim == "2D" && nuc2 && nuc2 != nuc1 ? "/" + nuc2 : "")) 
			+ "<br>" + manuf + " " + freq + "<br>";
		if (solvent)
			sp += solvent + " ";
		if (temp)
			sp += temp + " K";
		s += sp;
	}
	return name + s;
},

pathTo: function(ref) {
	return rootdir + ref.localPath + "/" + ref.localName;
},

showAid: function() {
	window.open(demo.findingAidFile, "_blank");
},

getStructureSpecHTML: function(sspec, isAll) {
	var structures = demo.aid.structures.list;
	var spectra = demo.aid.specData.list;
	var strucs = sspec.struc;
	var specs = sspec.data;
	if (isAll && structures[strucs[0]]._found === false) {
		return "";
	}
	var s = "<tr><td><table>";		
	s += "<tr><td><table cellspacing=0 cellpadding=10><tr>";
	for (var j = 0; j < strucs.length; j++) {
		s += "<td valign=top>" + demo.getStructureHTML(structures[strucs[j]]) + "</td>";
	}
	s += "</tr></table></td></tr>";
	s += "<tr><td><table cellspacing=0 cellpadding=10><tr>"
	for (var  j= 0; j < specs.length; j++) {
		s += "<td valign=top class=td" + (j%2) + ">" + demo.getSpectrumHTML(spectra[specs[j]]) + "</td>";
	}
	s += "</tr></table></td></tr>";
	s += "</table></td></tr>";
	return s;
},

showAll: function(){
	var s = "<table border=1>";
	var spectra = demo.aid.specData.list;
	if (demo.strucspecList) {
		for (var i = 0; i < demo.strucspecList.length; i++) {
			s += demo.getStructureSpecHTML(demo.strucspecList[i], true);
		}
	} else {
		var specs = demo.aid.specData.list;
		for (var i = 0; i < specs.length; i++) {
			s += "<tr><td><table>";
			s += "<tr><td><table cellspacing=0 cellpadding=10><tr>"
			for (var  j= 0; j < specs.length; j++) {
				s += "<td valign=top class=td" + (j%2) + ">" + demo.getSpectrumHTML(specs[j]) + "</td>";
			}
			s += "</tr></table></td></tr>";
			s += "</table></td></tr>";
		}
	}
	s += "</table>";
	$("#right_main").html(s);
},

alertHref: function(key, val) {
	if (!val) return "";
	var n = demo.messageCount++;
	demo.messages[n] = val;
	return '&nbsp;&nbsp;<a href="javascript:demo.alertMsg('+n+')">' + key + '</a>&nbsp;&nbsp;';
},

alertMsg: function(n) {
	alert(demo.messages[n]);
},

bytesFor: function(len) {
	if (len > 1000000000)
		return (Math.round(len/100000000)/10) + " GB";
	if (len > 1000000)
		return (Math.round(len/100000)/10) + " MB";
	if (len > 1000)
		return (Math.round(len/100)/10) + " KB";
	return len + " bytes";
},

repHref: function(rep, name, type) {
	return "<a target=_blank href=\"" + demo.pathTo(rep.ref) + "\">" + name 
		+ (type === null ? "" : " (" + (type === "" ? "" : type + "&nbsp;") + demo.bytesFor(rep.len) +")")
		+ "</a>";
},

clearMain: function() {
	$("#right_main").html("");
},

clearAll: function() {
	demo.clearMain();
	$("#right_top").html("");
	$("#structurespec").html("");
	$("#struc").html("");
	$("#spec").html("");
	$("#files").html("");
}






}