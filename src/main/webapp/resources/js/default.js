/**
 * HELPER FUNCTIONS FOR FORM INPUT
 * -----------------------------------------------------------------------------
 */
/**
 * check if the string ends with special chars
 * 
 * @param {type}
 *            str
 * @param {type}
 *            suffix
 * @returns {Boolean}
 */
function endsWith(str, suffix) {
	return str.indexOf(suffix, str.length - suffix.length) !== -1;
}
/**
 * Return a number which can be validated
 * 
 * @param n
 * @returns
 */
function formatNumber(n) {
	return n.replace(",", '.').replace(" ", '');
	;
}
/**
 * true if the input is a number
 * 
 * @param n
 * @returns {Boolean}
 */
function isNumber(n) {
	n = formatNumber(n);
	return !isNaN(parseFloat(n)) && isFinite(n);
}

/**
 * void function to sort select-tags with attribute data-sort="sort" by text
 */
function sortOptionsByText() {
	var selectElement, optionElements, options;
	selectElement = jQuery('select[data-sort="sort"]');
	if (selectElement) {
		optionElements = jQuery(selectElement.find('option'));
		options = jQuery.makeArray(optionElements).sort(function(a, b) {
			return (a.innerHTML > b.innerHTML) ? 1 : -1;
		});
		selectElement.html(options);
	}
};

/*
 * global function to load content via ajax, function use jQuery the callback
 * function get the target and return data
 */
function loadContent(loadURL, target, callback) {
	$.ajax({
		type : "GET",
		url : loadURL,
		cache : false,
		success : function(returndata) {
			if (target) {
				$(target).html(returndata);
			}
			if (callback) {
				setTimeout(callback, 15, target, returndata);
			}
		}
	});
}

/**
 * Validate the value of the imput number used in
 * resources/components/list/batchEditList_singleStatement.xhtml
 * 
 * @param input
 */
function validateInputNumber(input) {
	input.value = formatNumber(input.value);
	if (!isNumber(input.value)) {
		input.value = '';
	}
}



// START - DIALOGS
function openDialog(id) {
	/* set the dialog in center of the screen */
	var dialog = $(document.getElementById(id));
	dialog.css("left", Math.max(0,
			Math
					.round(($(window).width() - $(dialog)
							.outerWidth()) / 2)
					+ $(window).scrollLeft())
			+ "px");
	/* open the dialog */
	dialog.show();
	$(".imj_modalDialogBackground").show();
}
/* close a dialog */
function closeDialog(id) {
	var dialog = $(document.getElementById(id));
	$(".imj_modalDialogBackground").hide();
	dialog.hide();
}

$(window).resize(
		function(evt) {
			var dialog = $('.imj_modalDialogBox:visible');
			if (dialog.length > 0) {
				dialog.css("left", Math.max(0,
						Math
								.round(($(window).width() - $(dialog)
										.outerWidth()) / 2)
								+ $(window).scrollLeft())
						+ "px");
			}
		});

// END - DIALOGS


/**
 * Avoid double click submit for all submit buttons
 * 
 * @param data
 */
function handleDisableButton(data) {
	if (data.source.type !== "submit") {
		return;
	}

	switch (data.status) {
	case "begin":
		data.source.disabled = true;
		break;
	case "complete":
		data.source.disabled = false;
		break;
	}
}

//  START - JSF stuff 
if (typeof jsf !== 'undefined') {
	jsf.ajax.addOnEvent(function(data) {
		if (data.status === "success") {
			fixViewState(data.responseXML);
		}
		handleDisableButton(data);
	});
}

function fixViewState(responseXML) {
	var viewState = getViewState(responseXML);

	if (viewState) {
		for (var i = 0; i < document.forms.length; i++) {
			var form = document.forms[i];

			if (form.method.toLowerCase() === "post") {
				if (!hasViewState(form)) {
					createViewState(form, viewState);
				}
			} else { // PrimeFaces also adds them to GET forms!
				removeViewState(form);
			}
		}
	}
}

function getViewState(responseXML) {
	var updates = responseXML.getElementsByTagName("update");

	for (var i = 0; i < updates.length; i++) {
		var update = updates[i];

		if (update.getAttribute("id").match(
				/^([\w]+:)?javax\.faces\.ViewState(:[0-9]+)?$/)) {
			return update.firstChild.nodeValue;
		}
	}
	return null;
}

function hasViewState(form) {
	for (var i = 0; i < form.elements.length; i++) {
		if (form.elements[i].name == "javax.faces.ViewState") {
			return true;
		}
	}

	return false;
}

function createViewState(form, viewState) {
	var hidden;

	try {
		hidden = document.createElement("<input name='javax.faces.ViewState'>"); // IE6-8.
	} catch (e) {
		hidden = document.createElement("input");
		hidden.setAttribute("name", "javax.faces.ViewState");
	}
	hidden.setAttribute("type", "hidden");
	hidden.setAttribute("value", viewState);
	hidden.setAttribute("autocomplete", "off");
	form.appendChild(hidden);
}

function removeViewState(form) {
	for (var i = 0; i < form.elements.length; i++) {
		var element = form.elements[i];
		if (element.name == "javax.faces.ViewState") {
			element.parentNode.removeChild(element);
		}
	}
}

// END - JSF stuff 

/*******************************************************************************
 * 
 * SIMPLE SEARCH
 * 
 ******************************************************************************/
var selectedSearch;
var numberOfContext = $('.imj_bodyContextSearch li').length;


/**
 * Trigger the simple search, according to the currently selected context
 * @returns {Boolean}
 */
function submitSimpleSearch() {
	if ($('#simpleSearchInputText').val() != '') {
		goToSearch(selectedSearch);
	}
	return false;
};

$(".imj_bodyContextSearch li").click(function(){
	 goToSearch($(this).index() + 1);
});
/**
 * Open a search page according to the type 
 * @param type
 */
function goToSearch(index) {
	var appendChar="?";
	var url=$('.imj_bodyContextSearch li:nth-child('+ index +')').data('url');
	if(url.indexOf("?") >= 0){
		appendChar="&";
	}
	window.open(url + appendChar+'q=' + encodeURIComponent($('#simpleSearchInputText').val()),
	"_self");
};

/**
 * Actions for the search menu: open, navigate with array keys
 */
$("#simpleSearchInput").focusin(function() {
	$(".imj_menuSimpleSearch").show();
}).keyup(function(event) {
	if (event.which == 40) {
		incrementSelectedSearch();
		highlightSearch();
	}
	else if (event.which == 38) {
		decrementSelectedSearch();
		highlightSearch();
	}
	else if ($(this).val() != '') {
		$(".imj_menuSimpleSearch").show();
	}
});

// Set the correct context for the search according to the current page
$( document ).ready(function() {
	selectedSearch = 1;
	
	var path = window.location.pathname;
	$("ul.imj_bodyContextSearch li" ).each(function( index ) {
		if($(this).data('url').indexOf(path) !== -1){
			selectedSearch = index + 1;
			return false;
		}
	});
	highlightSearch();
});

function changePlaceholder(){
	var placeholder = $("ul.imj_bodyContextSearch li:nth-child(" + selectedSearch + ")").data('placeholder');
	$("#simpleSearchInputText").attr("placeholder", placeholder);
}

/**
 * Close the search menu
 */
$(".imj_simpleSearch").focusout(function() {
	$(".imj_menuSimpleSearch").delay(200).hide(0);
	
});
/**
 * On mouse over, unselect the previously selected menu
 */
$("ul.imj_bodyContextSearch li").mouseover(function() {
	//$(".hovered").removeClass("hovered");
	//selectedSearch = $(this).index() +1;
});

$("ul.imj_bodyContextSearch li").mouseout(function() {
	highlightSearch();
});

/**
 * Highlight the currently selected search
 */
function highlightSearch() {
	$("ul.imj_bodyContextSearch li").removeClass("hovered");
	$("ul.imj_bodyContextSearch li:nth-child(" + selectedSearch + ")").addClass("hovered");
	changePlaceholder();
}
/**
 * Select the next search 
 */
function incrementSelectedSearch() {
	if (selectedSearch < numberOfContext) {
		selectedSearch = selectedSearch + 1;
	}
}
/**
 * Select the previous search
 */
function decrementSelectedSearch() {
	if (selectedSearch > 1) {
		selectedSearch = selectedSearch - 1;
	}
}

/*******************************************************************************
 * 
 * END - SIMPLE SEARCH
 * 
 ******************************************************************************/

// START- loader methods
function lockPage(){
	$(".loaderWrapper").show();
}

function unlockPage(){
	$(".loaderWrapper").hide();
}

function startLoader(){
	$(".loaderWrapper").show();
	$(".loader").show();
}

function stopLoader(){
	$(".loaderWrapper").hide();
	$(".loaderWrapper").removeClass("show");
	$(".loader").hide();
	$(".loader").removeClass("show");
}
// END - loader methods

// JSF AJAX EVENTS
jsf.ajax.addOnEvent(function(data) {
    var ajaxstatus = data.status; // Can be "begin", "complete" and "success"
    switch (ajaxstatus) {
        case "begin":
            break;
        case "complete":
        	stopLoader();
        	break;
        case "success":
            break;
    }
});

// Edit selected items table
$(function(){
    $(".scrollbarTopWrapper").scroll(function(){
        $(".edit_selected_table_wrapper")
            .scrollLeft($(".scrollbarTopWrapper").scrollLeft());
    });
    $(".edit_selected_table_wrapper").scroll(function(){
        $(".scrollbarTopWrapper")
            .scrollLeft($(".edit_selected_table_wrapper").scrollLeft());
    });
});

//Close success message after 4s
setTimeout(function() {
    $('.imj_messageSuccess').slideUp(200);
}, 4000);

function closeSuccessMessage(){
setTimeout(function() {
    $('.imj_messageSuccess').slideUp(200);
}, 4000);
	
}


//END - set number of items pro line cookie

// Define datepicker
$(function() {
	$(".datepicker").datepicker({
		changeMonth : true,
		changeYear : true,
		dateFormat : "yy-mm-dd",
		firstDay : 1
	});
});

// Responsive menu show/hide
$(".responsiveMenuBtn").click(function() {
		$("#" +  $(this).data('menu')).slideToggle('slow', function() {
			$(this).toggleClass("show");
		});
	});

// SECTIONS
//show
$(".showSection").click(function() {
	var section = $(this).data('section');
	console.log(section);
	$("." +  section).slideToggle('fast');
	$("*[data-section='" + section + "']").toggle();
});
// hide
$(".hideSection").click(function() {
	var section = $(this).data('section');
	console.log(section);
	$("." +  section).slideToggle('fast');
	$("*[data-section='" + section + "']").toggle();
});
