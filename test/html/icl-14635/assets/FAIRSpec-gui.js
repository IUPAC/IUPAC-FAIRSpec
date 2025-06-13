// ${IUPAC FAIRSpec}/src/html/site/assets/FAIRSpec-gui.js
// 
// Bob Hanson hansonr@stolaf.edu 2024.12.13

;(function() {

	// from https://www.nmrdb.org/service/

	//    1H NMR prediction: https://www.nmrdb.org/service.php?name=nmr-1h-prediction&smiles=c1ccccc1CC
	//    13C NMR prediction: https://www.nmrdb.org/service.php?name=nmr-13c-prediction&smiles=c1ccccc1CC
	//    COSY prediction: https://www.nmrdb.org/service.php?name=cosy-prediction&smiles=c1ccccc1CC
	//    HSQC/HMBC prediction: https://www.nmrdb.org/service.php?name=hmbc-prediction&smiles=c1ccccc1CC
	//    All predictions: https://www.nmrdb.org/service.php?name=all-predictions&smiles=c1ccccc1CC

	const MAIN_SEARCH_SUB = "main_search_sub";
	const MAIN_SEARCH_TEXT = "main_search_text";
	const MAIN_SEARCH_PROP = "main_search_prop";
	const MAIN_SEARCH     = "main_search";
	const MAIN_SUMMARY    = "main_summary";

	const INVALID = "invalid!"; 
	
	const NMRDB_PREDICT_SMILES = "https://www.nmrdb.org/service.php?name=%TYPE%-prediction&smiles=%SMILES%";
	// where type = 1h, 13c, cosy, hmbc, hsqc
	
	var aLoading = null;
	var divId = 0;
	
	var dirFor = function(aidID) {
		return IFD.properties.baseDir + (IFD.findingAidID == '.' ? "" : "/" + aidID);
	}
	
	var fileFor = function(aidID, fname) {
		return dirFor(aidID) + "/" + fname;
	}


	const sanitizeUserInput = (string) =>{
		const map = {
			'&': '&amp;',
			'<': '&lt;',
			'>': '&gt;',
			'"': '&quot;',
			"'": '&#x27;',
			"/": '&#x2F;',
		}
		const reg = /[&<>"'/]/g;
		return string.replace(reg, (match)=>(map[match]));
	  }


	

	//external
	IFD.searchText = function(aidID) {
		// hide the main search sub 
		IFD.toggleDiv(MAIN_SEARCH_SUB,"none");
		var text = prompt("Text to search for?");
		var highlightEnabled = document.getElementById("highlightToggle").checked; 
		if (text) {
			// clear previous search state
			//IFD.clearSearchState();
			//sanitize user input text
			text = sanitizeUserInput(text)

			// Save the search text & the state of the highlight toggle
			localStorage.setItem("searchText", text);
			localStorage.setItem("highlightEnabled", highlightEnabled);

			var indexes = IFD.getCompoundIndexesForText(aidID, text);
			if (indexes)
				IFD.showCompounds(aidID, indexes);
				if (highlightEnabled)
					IFD.highlightText(text);
		}
	}



	IFD.highlightText = function(text){
		// reference to the DOM element
		var resultsDiv = document.getElementById("results");
		var contentsDiv = document.getElementById("contents");


		// change contents
		var frequency_contents = replaceInnerHtmlText(text, contentsDiv);
		// change results
		var frequency_results = replaceInnerHtmlText(text, resultsDiv);
		displayMatchCount(frequency_results);


	}

	function displayMatchCount(count) {
		// Select or create an element to display the count
		let countDisplay = document.getElementById('highlightCount');
		if (!countDisplay) {
			// If the element doesn't exist, create it
			countDisplay = document.createElement('div');
			countDisplay.id = 'highlightCount';
			}
		// Update the content of the element
		countDisplay.innerHTML = `<br> <b> <mark> Highlighted ${count} match${count === 1 ? '' : 'es'} </b> </mark>`;

		// Insert before the results div
		let result = document.getElementById('results');
		result.parentNode.insertBefore(countDisplay, result); // Insert before the first child of the body
	}

	function replaceInnerHtmlText(text, div){

		//			  using capture groups ; gi- global + case insensitive
		var regex= new RegExp(`(${text})`, "gi");
		

		// treeWalker to traverse text nodes only
		var treeWalker = document.createTreeWalker(div, NodeFilter.SHOW_TEXT, null);
		var currentNode = treeWalker.nextNode();
		var count = 0;
		
		while (currentNode){
			let nextNode = treeWalker.nextNode(); // Save the next node before modifying the DOM
			if(currentNode.textContent.match(regex)){
				//	before replacemenet
				//console.log(div.innerHTML);
				
				count += 1
				var highlightText = currentNode.textContent.replace(regex, `<mark>${text}</mark>`)
				var newNode = document.createElement(`span`);
				newNode.innerHTML = highlightText;
				// ... being the spread operator
				currentNode.replaceWith(...newNode.childNodes);
				
				//	after replacement
				//console.log(div.innerHTML);
			}
			currentNode  = nextNode;
		}

		return count;
	}


	IFD.clearSearchState = function(){
		localStorage.removeItem("searchText");
		localStorage.removeItem("highlightEnabled");
	}

	IFD.propertyMap = {
		"compounds": {}, 
		"structures": {},
		"spectra": {}
	}

	//external
	IFD.searchProperties = function(aidID) {													
		prepDOM_searchProperties();
		let initiliazed_keys = new Set(); // do avoid repeated key setting
		const searchTypeArr = document.querySelectorAll('input[name = "searchPropOption"]');
		searchTypeArr.forEach(radioBttn =>{
			radioBttn.addEventListener("change", function(){
				if(this.checked){
					IFD.searchType = this.value;

					// set the key in the map if needed
					if(!initiliazed_keys.has(IFD.searchType)){
						//update property map
						IFD.propertyMap[IFD.searchType] = IFD.getPropertyMap(aidID, IFD.searchType)
						initiliazed_keys.add(IFD.searchType);

					}
				}
				buildCheckboxContainer();
				getPropIDsFromGUI(aidID);
				
				
			})

			}
			
		)

	}

	const getPropIDsFromGUI = function(aidID){
		let searchKeys = [];
		let setList = [];
		let idSet = new Set();
		let propertyDiv = document.getElementById(`${MAIN_SEARCH_PROP}`);
		let form = propertyDiv.querySelector("#propertySearch");
		
		userSelectedOptions = {}
		form.addEventListener("submit", function(event){
			event.preventDefault();                     // ^= ~ starts with
			childBoxes = form.querySelectorAll(`input[class^="ifd-search-value-checkbox"]`);	

			
			childBoxes.forEach(box =>{
				if(box.checked){
					currParentProperty = box.dataset.parentBoxProperty;
					if(!(currParentProperty in userSelectedOptions)){
						userSelectedOptions[currParentProperty] = IFD.propertyMap[IFD.searchType][currParentProperty + "$" + box.value];
				
					}else{
						userSelectedOptions[currParentProperty] = userSelectedOptions[currParentProperty].union(IFD.propertyMap[IFD.searchType][currParentProperty + "$" + box.value]);
					}
				}
			})

			// if something to display
			if(Object.values(userSelectedOptions).length > 0){
				setColelction = Object.values(userSelectedOptions);
				// Intersection/&& operation between all the set IDs
				idSet = setColelction[0];
				for(let setItem of setColelction){
					idSet = idSet.intersection(setItem);

				}
			
				
				//console.log("Final Set:", idSet);
				var f;
				switch(IFD.searchType){
				case "structures":
					f = IFD.showStructures;
					break;
				case "spectra":
					f = IFD.showSpectra;
					break;
				case "compounds":
					f = IFD.showCompounds;
					break;					
				}
				f(aidID, [...idSet]);
			}
		})	
		}




	const checkboxHelper_prop = function(property, value, visibility = "visible"){
		let checkbox = document.createElement('input');
		checkbox.id = `${property}_${value}`;
		checkbox.dataset.parentBoxProperty = property;
		checkbox.type = 'checkbox';
		checkbox.name = 'propertyVal';
		checkbox.value = value;
		checkbox.className = 'ifd-search-value-checkbox_' + visibility;


		return checkbox;

	}

	const labelHelper_prop = function(id, value, count, visibility = "visible"){
		let label = document.createElement('label');
		label.setAttribute('for', id);
		label.setAttribute('class', "ifd-search-value-checkbox_" + visibility);
		if(count > 1){
			label.textContent = value + ' (' + count + ')';
		}
		else{
			label.textContent = value;
		}

		return label;

	}

	const buildCheckboxContainer = function(){
		const MAX_CHECKBOXES = 5;
		let checkboxContainer = document.getElementById('checkboxContainerPropSearch');
		 

		// clean the container if pre-populated
		checkboxContainer.innerHTML = '';
		propMap = IFD.propertyMap[IFD.searchType];
		if(Object.keys(propMap).length == 0){
			textNode = document.createTextNode("Nothing to show here");
			checkboxContainer.appendChild(textNode);
			checkboxContainer.style.display = "block";

			return 
		}


		// track properties and the count of their values
		uniqueProperties = {};

		hiddenBoxesMap = {} 

		for(const prop in propMap){
			const parentCheckbox = document.createElement(`input`);
			parentCheckbox.type = 'checkbox';
			let count = propMap[prop].size;
			
			let [property, value] = prop.split("$");
			

			if(!(property in uniqueProperties)){
			
				let parentCheckboxDiv = document.createElement('div');
				parentCheckboxDiv.id =	property + " Div";
				parentCheckboxDiv.className = "ifd-property-parent";


				checkboxContainer.appendChild(document.createElement('br'));
				uniqueProperties[property] = 1;

				parentCheckbox.id = `property_${property}`;
				parentCheckbox.name = `propertyType`;
				parentCheckbox.value = property;
				parentCheckbox.className = "ifd-search-key-checkbox";
				
				// Create the label element for the checkbox
				const label = document.createElement('label');
				label.setAttribute('for', parentCheckbox.id);
				label.textContent = property;
				
				// Append the checkbox and label to the main div container
				parentCheckboxDiv.appendChild(parentCheckbox);
				parentCheckboxDiv.appendChild(label);
				checkboxContainer.appendChild(parentCheckboxDiv);

				// for each parent property, create a corresponding child div
				let childCheckboxDiv = document.createElement('div');
				childCheckboxDiv.id =	property + " ChildDiv";
				childCheckboxDiv.className = "ifd-search-val-div";

				
				let childCheckbox = checkboxHelper_prop(property, value);

				// Create the label element for the child checkbox
				const childLabel = labelHelper_prop(childCheckbox.id, value, count);
			
				
				// add the child checkbox and label to the childDiv
				childCheckboxDiv.appendChild(childCheckbox);
				childCheckboxDiv.append(childLabel);

				childCheckboxDiv.appendChild(document.createElement('br'));

				parentCheckboxDiv.appendChild(childCheckboxDiv)
				checkboxContainer.append(parentCheckboxDiv);
				
				

			}else { // we've seen this property before
				let visibilityClass;
				uniqueProperties[property] += 1;
				if(uniqueProperties[property] > MAX_CHECKBOXES){
					visibilityClass = "hidden";
					let hiddenWrapperId = property + "_HiddenDiv";
					var hiddenWrapper = document.getElementById(hiddenWrapperId);
					if(!hiddenWrapper){
						hiddenWrapper = document.createElement('div');
						hiddenWrapper.id = hiddenWrapperId;
						hiddenWrapper.className = 'hidden-checkbox-group';
						hiddenWrapper.style.display = "none";
					}
						if(hiddenBoxesMap[hiddenWrapperId]){
							hiddenBoxesMap[hiddenWrapperId] += 1;
						}
						else{
							hiddenBoxesMap[hiddenWrapperId] = 1;
						}
				}
				else{
					visibilityClass = "visible";
				}

				let childDiv = document.getElementById(property + " ChildDiv");

				let childCheckbox = checkboxHelper_prop(property, value, visibilityClass);

				// Create the label element for the child checkbox
				let childLabel = labelHelper_prop(childCheckbox.id, value, count, visibilityClass);

				if(visibilityClass == "hidden"){
					hiddenWrapper.appendChild(childCheckbox);
					hiddenWrapper.append(childLabel);
					hiddenWrapper.appendChild(document.createElement('br'));
					childDiv.appendChild(hiddenWrapper);

				}else{
					// add the child checkbox and label to the childDiv 
					childDiv.appendChild(childCheckbox);
					childDiv.append(childLabel);
					childDiv.appendChild(document.createElement('br'));
				}
				
			}
		}

		parentContainers = checkboxContainer.querySelectorAll('div[class=ifd-property-parent]');
			
		parentContainers.forEach(div => {
			hiddenDiv = div.querySelector('div[class=hidden-checkbox-group]');

			if(hiddenDiv){
				showBttn = document.createElement('button');
				showBttn.setAttribute('class', 'showHiddenBttn');
				showBttn.setAttribute("type", "button");
				showBttn.dataset.assocDiv = hiddenDiv.id;
				showBttn.textContent = "Show More " + `(${hiddenBoxesMap[hiddenDiv.id]})`;
				showBttn.addEventListener('click', function(){
					assocDiv = document.getElementById(this.dataset.assocDiv);
					assocDiv.parentElement.parentElement.scrollIntoView({
						 behavior: "smooth",
						 block: "start"
						});
					if(assocDiv.style.display == "none"){
						assocDiv.style.display = "block";
						this.textContent = "Hide";
					}
					else{
						// get all the selected checkboxes in the hidden div
						let checkedinHidden = [];
						assocDiv.childNodes.forEach(box => {
							if(box.checked){
								checkedinHidden.push(box);
							}
						});

						let checkedinVisible = [];
						let uncheckedinVisible = [];
						assocDiv.parentElement.childNodes.forEach(elem =>{
							if(elem.type == "checkbox"){
								if(elem.checked){
									checkedinVisible.push(elem);
								}
								else{
									uncheckedinVisible.push(elem);
								}
							}
						})
						
						// bring checkedinHidden to the visible div
						if(checkedinVisible.length + checkedinHidden.length <= MAX_CHECKBOXES){
							//checkedinVisible
							//checkedinHidden
							let fragment = document.createDocumentFragment();

							checkedinHidden.forEach(cb => {
								let label = cb.nextElementSibling;
								label.className = label.className.split("_")[0] + "_visible";
								cb.className = cb.className.split("_")[0] + "_visible";
								let br = label.nextElementSibling;
								fragment.appendChild(cb);
								fragment.appendChild(label);
								fragment.appendChild(br);
							});

							//update the counts
							hiddenBoxesMap[assocDiv.id] -= checkedinHidden.length;			
							assocDiv.parentElement.insertBefore(fragment, uncheckedinVisible[0]);

							//enforce 5 elem limit in the visible Div
							let inVisibleCh = checkedinVisible.length + checkedinHidden.length;
							let inVisibleUnch = uncheckedinVisible.length;
							idx = 0;
							while(inVisibleCh + inVisibleUnch  > MAX_CHECKBOXES){
								//move the unchecked boxes to hidden
								fragment = document.createDocumentFragment();
								
								let cb = uncheckedinVisible[idx];
								cb.className = cb.className.split("_")[0] + "_hidden";
								let label = cb.nextElementSibling;
								label.className = label.className.split("_")[0] + "_hidden";
								let br = label.nextElementSibling;
								fragment.appendChild(cb);
								fragment.appendChild(label);
								fragment.appendChild(br);

								assocDiv.insertBefore(fragment, assocDiv.children[0]);
								hiddenBoxesMap[assocDiv.id] += 1;
								inVisibleUnch -= 1;
								idx++;	 
							}
							
						}
						else{
							// more than MAX_CHECKBOXES were checked
							// show all of them 
							let fragment = document.createDocumentFragment();

							checkedinHidden.forEach(cb => {
								let label = cb.nextElementSibling;
								label.className = label.className.split("_")[0] + "_visible";
								cb.className = cb.className.split("_")[0] + "_visible";
								let br = label.nextElementSibling;
								fragment.appendChild(cb);
								fragment.appendChild(label);
								fragment.appendChild(br);
							});
							
							//update the counts
							hiddenBoxesMap[assocDiv.id] -= checkedinHidden.length;			
							assocDiv.parentElement.insertBefore(fragment, assocDiv.parentElement.children[0]);

							//move all the unchecked boxes to hidden div
							fragment = document.createDocumentFragment();

							uncheckedinVisible.forEach(cb => {
								let label = cb.nextElementSibling;
								label.className = label.className.split("_")[0] + "_hidden";
								cb.className = cb.className.split("_")[0] + "_hidden";
								let br = label.nextElementSibling;
								fragment.appendChild(cb);
								fragment.appendChild(label);
								fragment.appendChild(br);
							});

							//update the counts
							hiddenBoxesMap[assocDiv.id] += uncheckedinVisible.length;			
							assocDiv.insertBefore(fragment, assocDiv.children[0]);
						}

						// console.log("checkedinHidden: ", checkedinHidden);
						// console.log("checkedinVisible: ", checkedinVisible);
						// console.log("uncheckedinVisible: ", uncheckedinVisible);
						
						assocDiv.style.display = "none";
						this.textContent = "Show More " + `(${hiddenBoxesMap[assocDiv.id]})`;
					}

				})
				div.appendChild(showBttn);

				
			}
		});


		// padding before the search button
		checkboxContainer.appendChild(document.createElement('br'));
		const submitButton = document.createElement('input');
		submitButton.type = 'submit';
		submitButton.value = 'search';
		checkboxContainer.appendChild(submitButton);


		//add event listener to the checkbox div

		checkboxContainer.addEventListener("change", function(event){
			
			// records any input targetted inside the div
			let target = event.target;

			// if a parent checkbox is clicked
			if(target.classList.contains("ifd-search-key-checkbox")){
				// get the child boxes
				let childBoxes = this.querySelectorAll(`input[data-parent-box-property = "${target.value}"]`);
				childBoxes.forEach(box => box.checked = target.checked);
			}
			else if(target.classList[0].startsWith("ifd-search-value-checkbox")){
				// get the parent box
				let parentValue = target.dataset.parentBoxProperty;
				let parentBox = this.querySelector(`input.ifd-search-key-checkbox[value = "${parentValue}"]`);
				let childBoxes = this.querySelectorAll(`input[data-parent-box-property = "${parentValue}"]`);
				let orBool = false;
				childBoxes.forEach(box => 
					orBool = orBool || box.checked
				);

				if(orBool){
					parentBox.checked = true;
				}
				else{
					parentBox.checked = false;
				}
				
			}
		})


		checkboxContainer.style.display = "block";
	
		// put unspecified property val to the top (if it exists)
		unspecified_checkboxes = document.querySelectorAll('input[value="Unspecified"]');
	
		unspecified_checkboxes?.forEach(unspecifiedBox => {
			let parentDiv = unspecifiedBox.parentElement;

			let propertyValDivId = unspecifiedBox.dataset.parentBoxProperty + " ChildDiv";
			let unspecifiedLabel = unspecifiedBox.nextElementSibling;
			// swap with the first checkbox in the parentDiv
			let visibleDiv = document.getElementById(propertyValDivId);
			
			//temp for hidden box's class names
			let temp = unspecifiedBox.className;

			let firstBox = visibleDiv.firstChild;
			let firstLabel = firstBox.nextSibling;

			unspecifiedLabel.className = firstLabel.className;
			unspecifiedBox.className = firstBox.className;
			const br = document.createElement('br');
			
			visibleDiv.insertBefore(unspecifiedBox, firstBox);
			visibleDiv.insertBefore(unspecifiedLabel, firstBox);
			visibleDiv.insertBefore(br, firstBox);

			firstBox.className = temp;
			firstLabel.className = temp;

			// if the unspecified checkbox was inside the hidden div
			if(parentDiv.className.includes("hidden")){

				//remove the break after the first label
				visibleDiv.removeChild(firstLabel.nextElementSibling);

				// insert the previous first box as the first elem in the hidden div
				const br = document.createElement('br');
				parentDiv.insertBefore(br, parentDiv.firstChild);
				parentDiv.insertBefore(firstLabel, br);
				parentDiv.insertBefore(firstBox, firstLabel);
			}
		});
		
		
	
	}

	const prepDOM_searchProperties = function(){
			//change the title
			const title_h3 = document.querySelector("#main_search h3"); // Select the <h3> inside the <div> with id="container"
			title_h3.textContent = "Property Search";
	
			const mainSearch = document.getElementById("main_search");
	
			// Hide the anchors, hightlight toggle, & label text inside #main_search div
			mainSearch.querySelectorAll("a, #highlightToggle, label[for='highlightToggle']").forEach(a => a.style.display = "none");
			
			IFD.toggleDiv(MAIN_SEARCH_PROP, "block");

	}


	IFD.showStructuresByAidAndID = function(idSet) {
		// not implemented
		s = showCompoundStructures(null, idSet, false);
	}

	IFD.documentReady = function(){

	}

	IFD.pageLoaded = function(){
		IFD.loadFindingAid();
	}
	
	// external
	IFD.loadFindingAid = function() {
		if(!getFindingAid()){;
			IFD.loadFindingAids();
		}	
	}
	// external
	IFD.loadFindingAids = function() {
		var aids = (IFD.findingAids ? IFD.findingAids.findingaids : ["."]);
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
		
		if (!aidID)
			return;
		
		
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


		// retain highlight
		var highlightEnabled = localStorage.getItem("highlightEnabled") === "true";
    	var searchText = localStorage.getItem("searchText");
		if (highlightEnabled && searchText) {
			IFD.highlightText(searchText);
		}

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

		// retain highlight
		var highlightEnabled = localStorage.getItem("highlightEnabled") === "true";
    	var searchText = localStorage.getItem("searchText");
		if (highlightEnabled && searchText) {
			IFD.highlightText(searchText);
		}
	} 
	
	//external
	IFD.showStructures = function(aidID, ids) {
		loadMainSummary(IFD.aid, false);
		IFD.select(aidID);
		setMode(IFD.MODE_STRUCTURES);
		ids || ids === false || (ids = IFD.getItems(aidID, IFD.MODE_STRUCTURES));
		var s = showCompoundStructures(aidID,ids, false, true);
		setResults(s);

		// retain highlight
		var highlightEnabled = localStorage.getItem("highlightEnabled") === "true";
    	var searchText = localStorage.getItem("searchText");
		if (highlightEnabled && searchText) {
			IFD.highlightText(searchText);
		}
	} 	

	IFD.getItems = function(aidID, mode) {
		var ids = IFD.items[aidID][mode];
		return ids;
	}
	
	IFD.showCollection = function(aidID) {
		window.open(dirFor(aidID), "_blank");
	}

	IFD.showAid = function(aidID) {
		var url = (IFD.findingAidURL || fileFor(IFD.findingAidID, IFD.properties.findingAidFileName));
		window.open(url, "_blank");
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

	var callbackLoadFailed = function(x,y,z) {
		alert([x,y,z]);
		return;
	}

	var getField = function(name){
		var search = "&"+document.location.search.substring(1)+"&" + name + "=";
		search = search.split("&" + name + "=")[1];
		search = search.split("&")[0];
		if(!search){
			return ""
		}else{
			return decodeURIComponent(search);
		}
	
	}
	
	var getFindingAid = function(url){
		if(!url){
			url = getField("url");
		}
		var faJson = J2S.getFileData(url);
		if(faJson.startsWith('{"IFD.findingaid"')){
			IFD.findingAidURL = url;
			var aid = JSON.parse(faJson)["IFD.findingaid"];
			IFD.findingAidID = aid.id || "?" ;
			loadAid(aid);
			return true;
		}	
		return false;
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
		IFD.isCrawler = (aid.createdBy && aid.createdBy.indexOf("Crawler") >= 0);
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
		var s = (aid.id == '.' ? "" : "&nbsp;&nbsp;&nbsp;" + aid.id) 
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
		clearPDFCache();
		
		// remove the highlight count
		div = document.getElementById("highlightCount")
		if(div){
			div.remove();
		}

		var s;
		if (isAll && IFD.mainText) {
			s = IFD.mainText;
		} else {
			s = getMainTable(aid, isAll);
			if (isAll) {
				IFD.mainText = s;
			}
		}
		setMain(s);

		showMain();
	}

	var getMainTable = function(aid, isAll) {		
		var s = "<table>";
		s += addTopRow(aid);
		if (isAll) {
			s += addPubInfo(aid);
			s += addDescription(aid.collectionSet);
			s += addResources(aid);
		}
		s += "</table>";
		return s;
	}
	
	var loadMainSearch = function(aidID) {
		// remove the highlight count
		highlight_div = document.getElementById("highlightCount");
		if(highlight_div){
			highlight_div.remove();
		}
			var s = "";
			aidID || (aidID = "");
			s += "<h3>Search</h3>";
			s += `<a href="javascript:IFD.searchStructures('${aidID}')">substructure</a>`;
			s += `&nbsp;&nbsp;&nbsp;<a href="javascript:IFD.searchText('${aidID}')">text</a>`;
			s += `&nbsp;&nbsp;&nbsp;<a href="javascript:IFD.searchProperties('${aidID}')">properties</a>`;
			s += `<div id=${MAIN_SEARCH_TEXT} style="display:none"></div>`;
			s +=  buildSearchPropertyDiv(); 
			s += `&nbsp;<input type="checkbox" id="highlightToggle" name="highlight"> <label for = "highlightToggle"> enable highlight </label>`;
			setMain(s);
	
		showMain();
	}

	var buildSearchPropertyDiv = function(){
		s = 	`<div id=${MAIN_SEARCH_PROP} style="display:none">`
		s += 	`<form id = propertySearch>`
		s += 	`<input id = "searchProp_Compounds" type = "radio" name = "searchPropOption" value = "${IFD.MODE_COMPOUNDS}"> 
					<label for = "searchProp_Compounds">compounds</label>&nbsp;&nbsp;&nbsp;`
		s += 	`<input id = "searchProp_Structures" type = "radio" name = "searchPropOption" value = "${IFD.MODE_STRUCTURES}">
					<label for = "searchProp_Structures">structures</label>&nbsp;&nbsp;&nbsp;`
		
		s += 	`<input id = "searchProp_Spectra" type = "radio" name = "searchPropOption" value = "${IFD.MODE_SPECTRA}">
					<label for = "searchProp_Spectra">spectra</label>&nbsp;&nbsp;&nbsp;`
		s +=	`<hr>`

		s +=	`<div id = checkboxContainerPropSearch style = "display:none">
				</div>`
		s += 	`</form>`
		s += `</div>`
		return s
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
		s += showCompoundSpectra(aidID,specids,smiles,true,false);
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

	var getSpecialText = function(spec) {
		var label = (spec.label || "");
		var desc = (spec.description || "");
		var s = (spec.doi ? getDOIAnchor("DOI", spec.doi) : "");
		if (label)
			s += (s ? "<br>": "") + label;
		if (desc)
			s += (s ? "<br>": "") + desc;
		return s;
	}
	
	var showSpectrum = function(aidID,id) {
		var isAll = (IFD.resultsMode == IFD.MODE_SPECTRA);
		var spec = IFD.getCollection(aidID).spectra[id];
		var structureIDs = IFD.getStructureIDsForSpectra(aidID, [id]);
		var sampleID = spec.properties && spec.properties.originating_sample_id;
		var sid = (IFD.byID ? id : spec.id); 
		var s = "<table padding=3><tr><td valign=top>"
			+ getHeader("Spectrum/a  ", "Spectrum " + sid) + "<h3>" 
			+ (sampleID ? "&nbsp;&nbsp;&nbsp; sample " + sampleID : "")
			+ "</h3>"
			+ "</td>"; 
		var title = getObjectProperty(spec, "expt_title");
		if (title)
			s += "<td>&nbsp;&nbsp;</td><td><b>" + title + "</b></td>"
		s += "</tr></table>";
		var smiles = IFD.getSmilesForStructureID(aidID, structureIDs[0]);
		s += showCompoundStructures(aidID,structureIDs, false);
		s += showCompoundSpectra(aidID,[id],smiles,false, isAll);
		s += "<hr style='color:blue'>";
		return s;
	}

	var getHeader = function(types, name, description) {
		IFD.contentHeader = types;
		var key = toAlphanumeric(name) + "_" + ++divId
		IFD.headers.push([key,name]);
		return "<a name=\"" + key + "\"><h3>" + name + "</h3></a>"
		+ (false && description ? description + "<p>" : "<p>");
	}

	var showCompound = function(aidID,id) {
		var cmpd = IFD.getCollection(aidID).compounds[id];
		var keys = IFD.getCompoundCollectionKeys();
		var structureIDs = cmpd[IFD.itemsKey][keys.structures];
		var spectraIDs = cmpd[IFD.itemsKey][keys.spectra];
		var props = cmpd.properties;
		var params = cmpd.attributes;
		var label = cmpd.label || cmpd.id;
		var s = getHeader("Compound/s", label.startsWith("Compound") ? label : "Compound " + label, null);// cmpd.description); 
		s += getSpecialText(cmpd);
		s += "<table>" + addPropertyRows("",props, null, false) + "</table>"
		s += "<table>" + addPropertyRows("",params, null, false) + "</table>"

		s += showCompoundStructures(aidID,structureIDs, false);
		var smiles = IFD.getSmilesForStructureID(aidID, structureIDs[0]);
		s += showCompoundSpectra(aidID,spectraIDs,smiles,false,false);
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
		var isAll = (IFD.resultsMode == IFD.MODE_STRUCTURES);
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
		if (v && isAll){
			s += "<table border=1><tr><td>";
			s += "from SMILES:<br>" + v;
			s += "</td></tr></table>"
		}
		s += IFD.getStructureSpectraPredictions(reps);
		s += "</td>";
		s += "<td>" + addRepresentationTable(false, aidID, reps, "png", isAll) + "</td>";
		s += "</tr>";
		if (isAll) {
			s += "<tr>";
		 	s += "<td><table>" + addPropertyRows("",props, null, false) + "</table></td>";
			s += "</tr>";
		}
		s += "</table>";
		s += "</td>";
		return s;
	}

	var showCompoundSpectra = function(aidID,ids,smiles,withTitle,isAll) {
		ids || (ids = IFD.items[aidID][IFD.MODE_SPECTRA]);
		var s = "<table>"
			if (withTitle)
				s += "<tr><td style=\"width:100px\" valign=top><span class=spectitle>Spectra</span></td></tr>";
		for (var i = 0; i < ids.length; i++) {
			s += addCompoundSpectrumRow(aidID, ids[i], smiles, i + 2, isAll);
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
		startImageMonitor();
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
		IFD.clearSearchState();
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

	var addRepresentationTable = function(isData, aidID, reps, specialItem, isAll) {
		var justImage = (!isData && IFD.resultsMode == IFD.MODE_SPECTRA);
		var s = ""
		for (var i = 0; i < reps.length; i++) {
			var type = IFD.shortType(reps[i].representationType);
			if (type == specialItem) {
				s = addRepresentationRow(isData, aidID, reps[i], type) + s;
			} else if (!justImage && "!" + type != specialItem) {
				s += addRepresentationRow(isData, aidID, reps[i], type);
			}
		}
		return "<table>" + s + "</table>";
	}

	var addRepresentationRow = function(isData, aidID, r, type) {
		var s; 
		var shead = //"";//
			// TODO data type xrd is in the wrong place
		(type == "png" || isData ? "" : "<span class=repname>" + (type = cleanKey(type)) + "</span> ");
		if (r.data) {
			if (r.data.indexOf(";base64") == 0) {
                                if (type == "png" || "image/png" == r.mediaType) {
                                        var imgTag = getImageTag((r.ref ? r.ref.localPath : "image.png"),(r.note ? cleanText(r.note) : null), "data:" + r.mediaType + r.data);
					s = addPathForRep(aidID, r.ref, -1, imgTag, null, r.note);
				} else {
					s = anchorBase64(r.ref.localPath, r.data, r.mediaType);
				}
			} else {
				if (r.data.indexOf(INVALID) >= 0) {
					s = "<span class='invalid'>" + r.data + "</span>";
				} else if (r.data.length > 30 || type == "inchikey") {
					s = anchorHide(shead, r.data, r.note);
					shead = "";
				} else {
					s = r.data;
				}
			}
		} else {
			s = " " + addPathForRep(aidID, r.ref, r.len, null, r.mediaType, r.note);
		}
		s = "<tr><td>" + shead + s + "</td></tr>";
		return s;
	}

	var heads = [];

	var anchorHide = function(shead, sdata, title) {
		heads.push(sdata);
		return "<a " +(title ? "title=\"" + cleanText(title) + "\" ": "")
			+ "class=hiddenhead href=javascript:IFD.showHead(" + (heads.length - 1) + ")>" + shead + "</a>";
	}

	IFD.showHead = function(i) {alert(heads[i])}

	var clearPDFCache = function() {
		IFD.pdfData = [];
		IFD.pdfDataPt = 0;		
	}
	
	clearPDFCache();
	
	var anchorBase64 = function(label, sdata, mediaType) {
		mediaType || (mediaType = "application/octet-stream");
		var s = "<a href=\"data:" + mediaType + sdata + "\")>" + label + "</a>";
		if (mediaType.indexOf("/pdf") >= 0) {
			s += getPDFLink("data:application/pdf" + sdata);
		}
		return s;
	}

	var getPDFLink = function(uri) {
		var s = "&nbsp;&nbsp;<span id=pdf" + IFD.pdfDataPt + "><a href=\"javascript:IFD.viewPDF("+IFD.pdfDataPt+")\">VIEW</a></div>";
		IFD.pdfData[IFD.pdfDataPt++] = "<object data=\"" + uri + "\" type=\"application/pdf\" width=\"800\" height=\"600\"></object>";
		return s;
	}
	
	IFD.viewPDF = function(i) {
		$("#pdf" + i).html(IFD.pdfData[i]);
	}

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

	var addCompoundSpectrumRow = function(aidID, id, smiles, tableRow, isAll) {
		var spec = IFD.getCollection(aidID).spectra[id];
		var cl = (tableRow > 0 ? " class=tablerow" + (tableRow%2) : "");
		var s = "<tr><td"+ cl + " id='q1' rowspan=2 style=\"width:100px\" valign=top>" 
			+ (IFD.byID ? id : "") 
		+ (isAll ? getSpectrumPrediction(spec.properties, smiles) : "")
		+ "</td>"
		s += "<td id='q2'>";
		s += getSpecialText(spec);
			s += addRepresentationTable(true, aidID, spec.representations, (isAll ? null : "pdf"), isAll);
		s += "</td></tr>";
		s += "<tr><td><table>";
			s += addPropertyRows("IFD&nbsp;Properties", spec.properties, null, !isAll);
			s += addPropertyRows("More&nbsp;Attributes", spec.attributes, null, !isAll);
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
		key = cleanKey(key);
		if ((key.endsWith("_PID") || key.endsWith("_DOI")) && val.startsWith("10.")) {
			val = getDOIAnchor(key, val);
		}
		return "<tr><td><b>" + key + "<b></td><td>" + val + "</td></tr>";
	}

	var getDOIAnchor = function(key, val) {
		// PID 10.xxxxxx
		if (!key.endsWith("DOI")) {
			return "<a class=pidref target=_blank href=\"" + val + "\">"+val+"</a>";
		}
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

	var getImageTag = function(title, note, url) {
		if (!IFD.isCrawler && url.indexOf("data:") < 0 && url.indexOf("https://") < 0)
			return "";
		divId++;
		if (note && title && title.indexOf(note) < 0)
			title += " " + note;
		startImageMonitor(divId);
		return "<img width=50% height=50% id=img" + divId  
			+  (title ? " title=\"" + title + "\"" : "")
			+ " onload=IFD.checkImage(" + divId + ")" 
			+  " src=\"" + url +"\">";			
	}

        var NOREF = {"localName":"?"};

        var addPathForRep = function(aidID, ref, len, value, mediaType, note) {
                ref || (ref = NOREF);
		var shortName = ref.localName || shortFileName(ref.localPath);
		var url = ref.url || ref.doi || (ref.localPath ? fileFor(aidID, ref.localPath) : ref.localName);
		mediaType = null;// nah. Doesn't really add anything || (mediaType = "");
		var s;
		if (value) {
			s = (url == "?" ? value : "<a target=_blank href=\"" + url + "\">" + value + "</a>");
			if (value.indexOf("<img") >= 0)
				return s;
		} else if (shortName.endsWith(".png")) {
			return getImageTag(url, null, url); 
		} else {
			if (url.startsWith(";base64,"))
				url = "data:application/octet-stream" + url;
			s = "<a target=_blank href=\"" + url + "\">" + shortName + "</a>" + " (" + getSizeString(len) + (mediaType ? " " + mediaType : "") + ")";				
			if (IFD.resultsMode == IFD.MODE_SPECTRA && shortName.endsWith(".pdf")) {
				s +=  getPDFLink(url);
			}
		}
		note = (note && note.startsWith("Warning") ? note : null);
		if (note)
			s += "<br><span class=warning>" + note + "</span>";
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

	var cleanKey = function(key){
		return (key == "unknown" ? "" : key || "");//.replace(/_/g, ' ');
	}

	var removeUnderline = function(s){
		return s.replace(/_/g, ' ');
	}

	var toAlphanumeric = function(s){
		return s.replace(/[^a-zA-Z0-9_]/g, '_');
	}
	
	var cleanText = function(text) {
		return removeUnderline(toAlphanumeric(text));
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

return ""; // no longer necessary

		var types = IFD.getRepTypes(reps);
		if (!types.smiles || !types.smiles.data) return "";
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
			startImageMonitor(divId);
			onload = " onload=IFD.checkImage(" + divId + ",true)";
			IFD.cachePut("img" + divId, code);
		  }
		  var src =	"https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=" 
				+ code + "&w=" + w + "&h=" + h + "&hdisp=" + hdisplay 
				+ "&showtitle=false&zoom=1.7";
		  var s = getImageTag(null, null, src);
//		  if (!data){
//			  IFD.cachePut(code, "img" + divId);
//			  IFD.cachePut("img" + divId, code);
//		  }
		  return s;
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
		console.log("image loaded " + image.id + " " + w + " " + h);
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
		IFD.imageSet.delete(id);
		return image;
	}

	var startImageMonitor = function(id) {
		if (!IFD.isCrawler)
			return;
		if (!id) {
			// hide
			if (IFD.imageSet.size != 0) {
				$("#spinner").show();
				$("#results").css("visibility","hidden");
			}
			return;
		}
		IFD.imageSet.add("img" + id);
		if (!IFD.imageMonitor) {
			IFD.imageMonitor = function() {
				if (IFD.imageSet.size == 0) {
					IFD.imageMonitor = null;
					$("#spinner").hide();
					$("#results").css("visibility","visible");
				}
			};
			setInterval(IFD.imageMonitor, 50);
		}
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
	
	
})();


