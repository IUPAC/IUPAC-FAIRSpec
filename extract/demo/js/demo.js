demo = {

loadFindingAids: function() {
	$("#nfa").html("" + (FindingAids.length - 1));
	var s = '<select id=articles onchange="demo.loadSelected(this.selectedOptions[0].value)"><option>Select an ACS article</option>'
	for (var i = 1; i < FindingAids.length; i++) {
		s += "<option value=\"" + FindingAids[i]  + "\">" + FindingAids[i] + "</option>"
	}
	s += '</select>'
	$("#leftpanel").html(s);
},

select: function(n) {
	var d = $("#articles")[0];
	d.selectedIndex = n;
	demo.loadSelected(d.selectedOptions[0].value);
	demo.clearMain();
},

loadSelected: function(fa) {
	if (!fa)return;
	demo.findingAidFile = "../ifs/" + fa + "._IFS_findingaid.json";
	$.ajax({url:demo.findingAidFile, dataType:"json", success:demo.loaded});
},

loaded: function(json) {
	demo.loadRightPanel(json["IFS.findingaid"]);
},

loadRightPanel: function(aid) {
	demo.aid = aid;
 	demo.loadTop(aid);
	if (aid.structures){
		demo.loadStructures(aid.structures);
		demo.loadSpecData(aid.specData);
		demo.loadStructureSpecs(aid.structureSpecData);
	} else {
		demo.loadSpecData(aid.specData);
	}

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
	if (aid.urls && aid.urls.length) {
		s += "<tr><td valign=top>Dataset URL(s)</td><td valign=top>"
		var sep = "";
		for (var i = 0; i < aid.urls.length; i++) {
			s += sep + "<a target=_blank href=\"" + aid.urls[i] + "\">" + aid.urls[i] + "</a>";
			sep = ", ";
		}
		s += "</td></tr>";
	}
	s += "</table>";
	$("#top").html(s);

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
	var s = '<select onchange="demo.loadStructure(this.selectedOptions[0].value)"><option>Select a structure ('+list.length+')</option>'
	for (var i = 1; i < list.length; i++) {
		var struc = list[i];
		s += "<option value=\"" + struc.index + "\">" + struc.name + "</option>"
	}
	s += '</select>'
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
	var s = '<select onchange="demo.loadSpec(this.selectedOptions[0].value)"><option>Select a spectrum ('+list.length+')</option>'
	for (var i = 1; i < list.length; i++) {
		var spec = list[i];
		s += "<option value=\"" + i + "\">" + spec.name + "</option>"
	}
	s += '</select>'
	$("#spec").html(s);

},

loadStructureSpecs: function(structureSpecs) {
	var n = structureSpecs.list.length;
	var list = [];
	for (var i = 0; i < n; i++) {
		list[i] = structureSpecs.list[i];
		list[i].index = i;
		var name= demo.aid.structures.list[list[i].struc[0]].name;
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
	var s = '<select onchange="demo.loadStructureSpec(this.selectedOptions[0].value)"><option>Select a structure-spectrum combination ('+list.length+')</option>'
	for (var i = 1; i < list.length; i++) {
		var struc = list[i];
		s += "<option value=\"" + struc.index + "\">" + struc.name + "</option>"
	}
	s += '</select>'
	$("#structurespec").html(s);
},

loadStructure: function(i) {
	var struc = demo.aid.structures.list[+i];
	var s = JSON.stringify(struc, null, 2);
	$("#main").html("<pre>" + s + "</pre>");
},

loadSpec: function(i) {
	var spec = demo.aid.specData.list[+i];
	var s = JSON.stringify(spec, null, 2);
	$("#main").html("<pre>" + s + "</pre>");
},

loadStructureSpec: function(i) {
	var spec = demo.aid.structureSpecData.list[+i];
	var s = JSON.stringify(spec, null, 2);
	$("#main").html("<pre>" + s + "</pre>");
},

showAid: function() {
	window.open(demo.findingAidFile, "_blank");
},

clearMain: function() {
	$("#main").html("");
}






}