/**
 * Can be used for an autocomplete field that allows the user to select a uri resource by it's label.
 * The actual uri of the resource is stored in an invisible gui element and saved from there to a 
 * backing bean property.
 * 
 * i.e. 
 *   - select a person (represented by a uri) from a list of names
 *   - select an imeji collection (represented by a uri) by it's name
 *   etc.
 */

// list of labels that user can select from
var autocompleteSource = [];

/**
 * Set a semicolon-separated list of labels and values that the user can select from.
 * Format
 *   label1; value1; label2; value2; etc. 
 * @param src
 * @returns
 */
function setAutocompleteSource(src) {			
	// set only one time
	if(autocompleteSource.length == 0){
		var collectionNames = src.split(";");
		var index = 0;
		for(i = 0; i < collectionNames.length; i=i+2){				 
			autocompleteSource[index] = {
					  label: collectionNames[i],
					  value: collectionNames[i+1]
			};
			index++;
		}
	}
}

/**
 * Get the associated hidden gui element for out autocomplete field.
 * @returns
 */
function getAssociatedHiddenElement(visibleElementsId){
	var idParts = visibleElementsId.split(":");
	
	var idStem = "";
	for(let i = 0; i < idParts.length -1; i++){
			idStem = idStem + idParts[i] + ":";
	}
	// this is based on a naming convention 
	// autocomplete input field has id: label
	// hidden value input field has id: value

	var newLastPart = "value";		
	var idOfHiddenElement = idStem + newLastPart;
	var hiddenElement = document.getElementById(idOfHiddenElement);	
	return hiddenElement;
			
}

/**
 * Sets jQuery autocomplete functionality to all input fields 
 * with style '.autocomplete_link_collection' 
 * @returns
 */
function setAutocompleteToInputFields(){
	
	$(".autocomplete_link_collection").autocomplete({
		source : autocompleteSource,
		minLength : 1,
		delay: 0,
		select : function(event, uiobject) {
			// set value of autocomplete input field to selected item (label)
			this.value = uiobject.item.label;
			// set value of hidden element to selected item (value)
			var associatedHiddenElement = getAssociatedHiddenElement(this.id);
			associatedHiddenElement.value = uiobject.item.value;
			return false;
		}
	});
}

/**
 * Function is automatically called once all the DOM elements of the page are ready to use
 * (after initial page load)
 * 
 * @returns
 */
$(function() {
	setAutocompleteToInputFields();
});


/**
 * Function to be attached to the onevent attribute of f:ajax tag.
 * Results in this function being called 3 times during ajax request:
 * 1. before request is sent (data.status == 'begin')
 * 2. after response has arrived (data.status == 'complete')
 * 3. when the HTML DOM is successfully updated (data.status == 'success')
 * 
 * @returns
 */
function onAjaxEvent(data){
	
	if(data.status == 'success'){
		setAutocompleteToInputFields();
	}
	
}