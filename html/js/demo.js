
demo = {

loadFindingAids: function() {
    $.ajax({url:rootdir + "_IFS_findingaids.json", dataType:"json", 
    	success:demo.loadLeftPanel,
    	error:function(x){ 
    		alert("failed - do you have your browser enabled for local file reading?");
    		$("#main").html("<a target=_blank href=\"http://wiki.jmol.org/index.php/Troubleshooting/Local_Files\">http://wiki.jmol.org/index.php/Troubleshooting/Local_Files</a>");
    	} 
    });
},

loadLeftPanel: function(json) {
	var aids = demo.findingaids = json.findingaids;
	$("#nfa").html("" + aids.length);
	var s = '<select id=articles onchange="demo.loadSelected(this.selectedOptions[0].value)"><option>Select an ACS article</option>'
	for (var i = 0; i < aids.length; i++) {
		s += "<option value=\"" + aids[i]  + "\">" + aids[i] + "</option>"
	}
	s += '</select>'
	$("#leftpanel").html(s);
	demo.select(1);
},

select: function(n) {
	var d = $("#articles")[0];
	d.selectedIndex = n;
	demo.clearMain();
	demo.loadSelected(d.selectedOptions[0].value);
},

loadSelected: function(fa) {
	if (!fa)return;
	demo.clearMain();
	demo.findingAidFile = rootdir + fa + "._IFS_findingaid.json";
	$.ajax({url:demo.findingAidFile, dataType:"json", success:demo.loaded});
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
		demo.loadSpecData(aid.specData);
	}

},

loadFiles: function() {
	var s = '<a href="javascript:demo.showAid()">Finding Aid</a>&nbsp;&nbsp;&nbsp;';
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
	$("#top").html(s);

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
	var s = '<select onchange="demo.loadStructure(this.selectedOptions[0].value)"><option>Select a structure ('+list.length+')</option>'
	for (var i = 0; i < list.length; i++) {
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
	for (var i = 0; i < list.length; i++) {
		var spec = list[i];
		s += "<option value=\"" + i + "\">" + spec.name + "</option>"
	}
	s += '</select>'
	$("#spec").html(s);

},

loadStructureSpecs: function(structureSpecs) {
	if (!structureSpecs || !structureSpecs.list)return;
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
	for (var i = 0; i < list.length; i++) {
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
	$("#top").html("");
	$("#main").html("");
	$("#structurespec").html(s);
	$("#specData").html(s);
	$("#struc").html(s);
}






}