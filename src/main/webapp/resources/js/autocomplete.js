/*
 * make use Jquery UI auto-complete component
 * official sample see: http://jqueryui.com/autocomplete/#multiple-remote
 */

var datasourceUrl;
var result;
var servlet;
// After how many character the suggest is started
var offset = 2;
var id;
/*
 * Update remote calling source url Called when input field focus:
 * onfocus="getDatasourceUrl('#{statement.vocabulary}')"
 */
function getDatasourceUrl(url, servlet) {
	datasourceUrl = url;
	this.servlet = servlet;
	offset = 2;
}
/*
 * Same a getDatasourceUrl, but with a fixed offset. This allow to force the
 * number of character to wait for before starting the suggest
 * 
 */
function getDatasourceUrlWithFixedDelay(url, servlet, startAfter) {
	getDatasourceUrl(url, servlet);
	offset = startAfter;
}
function split(val) {
	return val.split(/,\s*/);
}
function extractLast(term) {
	return split(term).pop();
}
function setInputValue(inputId, value) {
	if (document.getElementById(inputId) != null) {
		if (value != null) {
			document.getElementById(inputId).value = value;
		} else {
			document.getElementById(inputId).value == '';
		}
	}
}
$(function() {

	// This add auto-complete to all input fields on page,
	//$(":input[type=text], textarea")
	$(".autocomplete_js")
	// don't navigate away from the field on tab when selecting an item
	.bind(
			"keydown",
			function(event) {
				if (datasourceUrl != null && datasourceUrl != ''
						&& event.keyCode === $.ui.keyCode.TAB
						&& $(this).data("autocomplete").menu.active) {
					event.getPreventDefault();
				}
			})

	.autocomplete({
		// source: datasourceUrl,
		// source retrieve data to display popup

		/*
		 * jsonData is result returned from servlet /imeji/autocompleter
		 * According to http://api.jqueryui.com/autocomplete/#option-source
		 * jsonData has to be either Array:[ "Choice1", "Choice2" ] Or An array
		 * of objects with label and value properties: [ { label: "Choice1",
		 * value: "value1" }, ... ] In our case, servlet add label&value into
		 * result.
		 */
		source : function(request, response) {
			$.getJSON(servlet, {
				searchkeyword : request.term,
				datasource : datasourceUrl
			}, function(jsonData) {
				result = jsonData;
				response(result);
			});
		},
		minLength : 0,
		messages : {
			noResults : '',
			results : function() {
				return '';
			}
		},
		// this search event fired before search beginning
		// and it is used to cancel "unqualified" search,
		// i.e.,return false;
		search : function() {
			if (datasourceUrl == null || datasourceUrl == '') {
				return false;
			}
			// custom minLength, currently start query after
			// entering x
			// characters,
			var term = extractLast(this.value);
			if (term.length < offset) {
				return false;
			}
		},
		focus : function() {
			// prevent value inserted on focus
			return false;
			
		},
		/**
		 * Action triggered when a value is selected in the autocomplete. Fill
		 * in the input values
		 * 
		 * @param event
		 * @param ui
		 * @returns {Boolean}
		 */
		select : function(event, ui) {
			/*
			 * User the id of the current input to set the values of the others
			 * input
			 */
			var idEls = this.id.split(":");
			var inputId = "";
			for ( var i = 0; i < idEls.length - 1; i++) {
				inputId = inputId + idEls[i] + ":";
			}
			// Write the value of the current input
			setInputValue(this.id, ui.item.value);
			setInputValue(inputId + "identifier", ui.item.id);
			setInputValue(inputId + "given", ui.item.givenname);
			setInputValue(inputId + "family", ui.item.family);
			setInputValue(inputId + "identifier", ui.item.id);
			setInputValue(inputId + "organization:0:name", ui.item.organization);
			setInputValue(inputId + "latitude", ui.item.latitude);
			setInputValue(inputId + "longitude", ui.item.longitude);
			return false;
		}
	}).focus(function() {
		// if the offset is 0, then show results on focus
		if (offset == 0 && datasourceUrl != null && datasourceUrl != '') {
			this.value = " ";
			$(this).autocomplete("search");
			this.value = "";
			// force an error (event is null) to avoid more calls on focus
			event.getPreventDefault();
			
		}

	});
});