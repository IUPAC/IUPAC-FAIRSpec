// ${IUPAC FAIRSpec}/src/html/site/FAIRSpec-get.js
// 
// Bob Hanson hansonr@stolaf.edu 2024.12.05


// samples --- associated via originating_sample property of a spectrum -- 1 sample : N spectra
// structures --- associated with
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

;(function() {

IFD.getCollection = function(aidID) {
	return IFD.collections[aidID];
} 

IFD.getSmilesForStructureID = function(aidID, id) {
	var struc = IFD.getCollection(aidID).structures[id];
	if (!struc) return "";
	var reps = struc.representations;
	var types = IFD.getRepTypes(reps);
	return types.smiles && types.smiles.data;
}

IFD.getSpectrumIDsForSample = function(aidID,id) {
	var samplespecs = IFD.getCollection(aidID)["sample-spectra associations"];
	var items = IFD.items[aidID].samplespectra;
	for (var i = 0; i < items.length; i++) {
		var assoc = samplespecs[items[i]];
		if (assoc.itemsByID.samples[0] == id) {
			return assoc.itemsByID.spectra;
		}
	}
	return [];	
}

IFD.getPropertyMap = function(aidID, searchType, nKeyType, map){
	var idObj = IFD.getCollection(aidID)[searchType]
	var n = nKeyType[0] + "_";
	var keyType = nKeyType.substring(1);
	for(const item in idObj){
		var propertyObj = idObj[item][keyType];
		// go into a deeper loop if the obj has properties
		//console.log(propertyObj);
		if(propertyObj){
			for(const propKey in propertyObj){
				//console.log(item, propKey);
				var propVal = propertyObj[propKey];
				if(!map[n + propKey + '$' + propVal]){
					map[n + propKey + '$' + propVal] = new Set();
				}
				
				//add the index to the set
				map[n + propKey + '$' + propVal].add(item);
			}	
		}	
	}

		// check for unspecified property values
	var generalizedMap = {}
	Object.keys(map).forEach(key=>{
		var generalKey = key.split("$")[0];
		if(!generalizedMap[generalKey]){
			generalizedMap[generalKey] = new Set();
		}
		generalizedMap[generalKey] = (generalizedMap[generalKey]).union(map[key]);
	})
	var collection = IFD.collections[IFD.findingAidID];
	var completeSet = new Set(Object.keys(collection[searchType]));
	Object.keys(generalizedMap).forEach(key =>{
		var setDiff = completeSet.difference(generalizedMap[key]);
		// an unspecified value has been detected
		if(setDiff.size != 0){
			map[key + "$\0unspecified"] = setDiff;
		}
	})
	return map;
}


IFD.getCompoundIndexesForText = function(aidID, text) {
	var compounds = IFD.getCollection(aidID).compounds;
	var keys = IFD.getCompoundCollectionKeys();
	var ids = [];
	text = text.toLowerCase();
	var citems = IFD.items[aidID].compounds;
	for (var i = 0; i < citems.length; i++) {
		var assoc = compounds[citems[i]];
		var found = false;
		while (!found) {
			found = testText(assoc.label + "|" + assoc.description + "|", text);
			break;
		}
		if (found)
			ids.push(citems[i]);
	}
	return ids;
}

var testText = function(s, text) {
	return (s.toLowerCase().indexOf(text) >= 0);
}

IFD.getCompoundIndexesForStructures = function(aidID, strucIDs) {
	var compounds = IFD.getCollection(aidID).compounds;
	var structures = IFD.getCollection(aidID).structures;
	var keys = IFD.getCompoundCollectionKeys();
	var ids = [];
	var citems = IFD.items[aidID].compounds;
	for (var is = 0; is < strucIDs.length; is++){
		var id = strucIDs[is];
		out: for (var i = 0; i < citems.length; i++) {
			var assoc = compounds[citems[i]];
			var sitems = assoc[IFD.itemsKey][keys.structures];
			// all by ID now
			for (var j = 0; j < sitems.length; j++) {
				if(sitems[j] == id) {
					ids.push(citems[i]);
					break out;
				}
			}	
		}
	}
	return ids;		
}

IFD.getStructureIDsForSpectra = function(aidID, specIDs) {
	var compounds = IFD.getCollection(aidID).compounds;
	var keys = IFD.getCompoundCollectionKeys();
	var ids = [];
	var items = IFD.items[aidID].compounds;
	for (var is = 0; is < specIDs.length; is++){
		var specid = specIDs[is];
		for (var i = 0; i < items.length; i++) {
			var assoc = compounds[items[i]];
			var spectra = assoc[IFD.itemsKey][keys.spectra];
			for (var j = 0; j < spectra.length; j++) {
				if(spectra[j] == specid) {
					addUnique(assoc[IFD.itemsKey][keys.structures], ids);
					break;
				}
			}	
		}
	}
	return ids;	
}

IFD.getRepTypes = function(reps, type) {
	var types = {};
	for (var i = 0; i < reps.length; i++) {
		var t = IFD.shortType(reps[i].representationType);
		if (!type || t == type)
			types[t] = reps[i];
	}
	return types;
}


IFD.getIDs = function(map) {
	var ids = [];
	if (map.length == null) {
		// actual map
		for (var id in map) {
			ids.push(id);
		}
		sortIDs(ids);
	} else {
		// array
		for (var id = 0; id < map.length; id++) {
			ids.push("" +id);
		}
	}
	return ids;
}

var sortIDs = function(ids) {
	var a = [];
	for (i = 0; i < ids.length; i++) {
		a[i] = [getSortKey(ids[i]), ids[i]];
	}
	a.sort(idSorter);
	for (i = 0; i < ids.length; i++) {
		ids[i] = a[i][1];
	}
}

var getSortKey = function(id) {
  var ret = new Array(2);
  id = id.toUpperCase();
  var val = getBestNumber(id, ret);
  if (val == 0)
    return id + "__________";
  var sval = "" + val;
  sval = ("0000000000" + sval).substring(sval.length);
  return id.substring(0, ret[0]) + sval + id.substring(ret[1]);
}

////	TESTING__________
////	COMPOUND 0000000033
////	COMPOUND 0000000033A
////	0000000015B
////	0000000015C
//}

  var getBestNumber = function(id, ret) {
	var pt1 = -1, n = id.length;
	var val = 0;
	for (var i = 0; i < n; i++) {
		var c = id.charCodeAt(i);
		if (c >= 48 && c <= 57) {
			if (pt1 < 0)
				pt1 = i;
			val = val * 10 + (c - 48);
		} else {
			if (pt1 >= 0) {
				n = i;
				break;
			}
		}
	}
	ret[0] = pt1;
	ret[1] = n;
	return val;
}


var idSorter = function(a, b) {
	return (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0);	
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


IFD.cachePut = function(item, value) {
	if (value)
		IFD.cache[item] = value;
	else
		delete IFD.cache[item]
}

IFD.cacheGet = function(item) {
	return IFD.cache[item];
}

IFD.shortType = function(type) {
	return (!type ? "" : type.substring(type.lastIndexOf(".") + 1));
}

IFD.getCollectionSetById = function(aid) {
	IFD.byID = !!aid.collectionSet.itemsByID;
	IFD.itemsKey = (IFD.byID ? "itemsByID" : "items");
	IFD.collectionKeys = {};
	var collections = aid.collectionSet.itemsByID || aid.collectionSet.items;
	var dc = IFD.collections[aid.id] = {};
	if (IFD.findingAidID == '.')
		IFD.collections['.'] = dc;
	for (var id in collections) {
		c = collections[id];
		dc[c.id || id] = c.items || c.itemsByID;
		// deprecated not byID
		if (!IFD.byID) {
			IFD.collectionKeys[c.id] = c.collections; // compounds only
		}
	}
	return dc;
}

IFD.getCollectionSetItems = function(aid) {
	var dc = IFD.getCollectionSetById(aid);
	var dItems = IFD.items[aid.id] = {};
	if (IFD.findingAidID == '.')
		IFD.items['.'] = dItems;
	if (dc[IFD.MODE_SAMPLES]) {
		dItems[IFD.MODE_SAMPLES] = IFD.getIDs(dc[IFD.MODE_SAMPLES]);
	}
	if (dc[IFD.MODE_COMPOUNDS]) {
		dItems[IFD.MODE_COMPOUNDS] = IFD.getIDs(dc[IFD.MODE_COMPOUNDS]);
	}
	if (dc[IFD.MODE_STRUCTURES]) {
		dItems[IFD.MODE_STRUCTURES] = IFD.getIDs(dc[IFD.MODE_STRUCTURES]);
	}
	if (dc[IFD.MODE_SPECTRA]) {
		dItems[IFD.MODE_SPECTRA] = IFD.getIDs(dc[IFD.MODE_SPECTRA]);
	}
	if (dc["sample-spectra associations"]) {
		dItems[IFD.MODE_SAMPLESPECTRA] = IFD.getIDs(dc["sample-spectra associations"]);
	}
	return dItems;
}

IFD.getCompoundCollectionKeys = function() {
	var ckeys = IFD.collectionKeys.compounds;
	if (ckeys && !ckeys[0]) {
		return ckeys;
	}
	if (!ckeys) {
		return ckeys = {structures: "structures", spectra:"spectra"};
	}
	var keys = {};
	for (var i = ckeys.length; --i >= 0;) {
		keys[ckeys[i]] = i; 
	}
	return IFD.collectionKeys.compounds = keys;
}

IFD.getSMILES = function(aidID, retIDs, retSMILES, allowReactions, withAidID) {
	if (!aidID) {
		// across all Finding Aids
		for (var i = 0; i < IFD.aidIDs.length; i++) {
			IFD.getSMILES(IFD.aidIDs[i], retIDs, retSMILES, allowReactions, true);
		}
		return;
	}
	var structureIDs = IFD.items[aidID]["structures"];
	for (var i = 0; i < structureIDs.length; i++) {
		var id = structureIDs[i];
		var struc = IFD.getCollection(aidID).structures[id];
		var reps = struc.representations;
		var types = IFD.getRepTypes(reps, "smiles");
		if (types.smiles) {
			var data = types.smiles.data;
			if (!allowReactions)
				data = data.replace("&gt;&gt;", ".");
			if (IFD.jmolCheckSmiles(data)) {
				retIDs.push(withAidID ? [aidID, id] : id);
				retSMILES.push(data);
			} else {
				System.out.println("INVALID SMILES! " + data);
			}
		}
	}
}


})();