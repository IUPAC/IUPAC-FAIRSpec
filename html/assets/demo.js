// Bob Hanson hansonr@stolaf.edu

demo = {

aid:null,
findingAids:[],
findingAidFile:null,
findingAidMap:{},
strucspecList:null,
whatSpec:null,
whatStructure:null,

cacheFindingAid: function(fa) {
	if (!fa)return;
	demo.clearMain();
	demo.findingAidFile = rootdir + fa + "._IFS_findingaid.json";
	$.ajax({url:demo.findingAidFile, dataType:"json", success:function(json) {demo.findingAidMap[fa] = json;}});	
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

loadLeftPanel: function(json) {
	var aids = demo.findingAids = json.findingaids;
	$("#nfa").html("" + aids.length);
	demo.loadLeftTop();
	demo.loadLeftSelect();
	demo.select(1);
},

loadLeftTop: function() {
	var s = "";
	s += "pub search:<br><input id=search type=text style='width:140px' onchange='demo.search(this.value, null, null)'/>";
	s += "structure search:<br><input id=search type=text style='width:140px' onchange='demo.search(null, this.value, null)'/>";
	s += "spectrum search:<br><input id=search type=text style='width:140px' onchange='demo.search(null, null, this.value)'/>";
	$("#left_top").html(s);
},

search: function(whatPub, whatStruc, whatSpec) {
	demo.clearMain();
	demo.loadLeftSelect(whatPub, whatStruc, whatSpec);
	var d = $("#articles")[0];
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
	for (var ai = 0; ai < aids.length; ai++) {
		var fa = aids[ai];
		var found = false;
		var aid = (demo.findingAidMap[fa] ? demo.findingAidMap[fa]["IFS.findingaid"] : null);
		var structures = null, spectra = null;
		if (aid) {
			if (aid.structures) {
				structures = aid.structures.list;
				for (var i = 0; i < structures.length; i++) {
					structures[i]._found = defFound;
				}
			}
			if (aid.specData) {
				spectra = aid.specData.list;
				for (var i = 0; i < spectra.length; i++) {
					spectra[i]._found = defFound;
				}
			}
		}
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

select: function(n) {
	var d = $("#articles")[0];
	if (!d)
		return;
	d.selectedIndex = n;
	demo.clearMain();
	demo.loadSelected(d.selectedOptions[0].value);
},

loadSelected: function(fa) {
	if (!fa)return;
	demo.clearMain();
	demo.findingAidFile = rootdir + fa + "._IFS_findingaid.json";
	var json = demo.findingAidMap[fa];
	if (json) {
		demo.loaded(json);
	} else {
		$.ajax({url:demo.findingAidFile, dataType:"json", success:function(json){demo.findingAidMap[fa] = json;demo.loaded(json)}});
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
			s += sep + "<a target=_blank href=\"" + ref + "\">" + ref + "</a> (" + demo.bytesFor(len) + ") "
				+ "<a target=_blank href=\""+rootdir + dir +"\">extracted collection</a>";
			sep = ", ";
		}
		s += "</td></tr>";
	}
	if (aid.properties["IFS.collection.ref"]) {
		s += "<tr><td valign=top>IFS collection</td><td valign=top><a target=_blank href=\"" 
			+ rootdir + aid.properties["IFS.collection.ref"] + "\">" 
+ aid.properties["IFS.collection.ref"] + "</a> (" +  demo.bytesFor(aid.properties["IFS.collection.len"]) + ")</td></tr>";
		
	}
	s += "</table>";
	$("#right_top").html(s);

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
	s = '<select onchange="demo.loadStructure(this.selectedOptions[0].value)"><option>Select a structure ('+n+')</option>' + s + '</select>';
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
	var s = "";
	var n = 0
	for (var i = 0; i < list.length; i++) {
		var spec = list[i];
		if (spec._found !== false) {
			s += "<option value=\"" + i + "\">" + spec.name + "</option>";
			n++;
		}		
	}
	s = '<select onchange="demo.loadSpec(this.selectedOptions[0].value)"><option>Select a spectrum ('+n+')</option>' + s + '</select>'
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
	var spec = demo.aid.structureSpecData.list[+i];
	var s = JSON.stringify(spec, null, 2);
	$("#right_main").html("<pre>" + s + "</pre>");
},

getStructureHTML: function(struc) {
	var props = struc.properties;
	return struc.name;
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
				name = "<a href=\"" + path + "\">" + name +" (" + demo.bytesFor(rep.len) +")</a>";
				break;
			case "application/zip":
				name = "<a href=\"" + path + "\">" + name +" (zip " + demo.bytesFor(rep.len) +")</a>";
				break;
			case "image/png":
				s += "<br><img src=\"" + path + "\">";
				break;
			case "application/pdf":
				s += "<br><a href=\"" + path + "\">pdf (" + demo.bytesFor(rep.len) +")</a>";
				break;
		}
	}
	return name + s;
},

pathTo: function(ref) {
	return rootdir + ref.localPath + "/" + ref.localName;
},

showAid: function() {
	window.open(demo.findingAidFile, "_blank");
},

showAll: function(){
	var s = "<table border=1>";
	var spectra = demo.aid.specData.list;
	if (demo.strucspecList) {
		var structures = demo.aid.structures.list;
		for (var i = 0; i < demo.strucspecList.length; i++) {
			var sspec = demo.strucspecList[i];
			var strucs = sspec.struc;
			var specs = sspec.data;
			if (structures[strucs[0]]._found === false) {
				continue;
			}
			s += "<tr><td><table>";		
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

clearMain: function() {
	$("#right_top").html("");
	$("#right_main").html("");
	$("#structurespec").html(s);
	$("#struc").html(s);
	$("#spec").html(s);
	$("#files").html(s);
}






}