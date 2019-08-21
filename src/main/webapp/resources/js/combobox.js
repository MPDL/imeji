/**
 * JQuery UI Widget for a text input field with autocomplete and dropdown
 * 
 */


    $.widget( "custom.combobox", {
      
    	options : {
    		source : [],
    		showListByFocus : false
    	},
    	
    	_create: function() {

    	  
    	  
    	  
         /* 
        this.wrapper = $( "<span>" )
          .addClass( "custom-combobox" )
          .insertAfter( this.element );
 */
        //this.element.hide();
        this._createAutocomplete();
        if(this.options.showListByFocus){
        	this._createShowAllByFocusAndButton();
        }else {
        	this._createShowAllByButton();
		}
        
      },
 
      _createAutocomplete: function() {
        var selected = this.element.val();
 
        this.input = this.element
         
          .autocomplete({
            delay: 0,
            minLength: 0,
            source: this.options.source
          });
        

        /*
        this._on( this.input, {
          autocompleteselect: function( event, ui ) {
            ui.item.option.selected = true;
            this._trigger( "select", event, {
              item: ui.item.option
            });
          },
 
          autocompletechange: "_removeIfInvalid"
        });
        */
      },
      
 
      _createShowAllByButton: function() {
        var input = this.input,
          wasOpen = false;
        
        $( "<a>" )
          .attr( "tabIndex", -1 )
          .addClass("imj_submitButton")
          .tooltip()
          .insertAfter( this.input )
          .append( '<span class="fa fa-angle-down">')
          .on( "mousedown", function() {
            wasOpen = input.autocomplete( "widget" ).is( ":visible" );
          })
          .on( "click", function() {
            input.trigger( "focus" );
            // Close if already visible
            if ( wasOpen ) {
              return;
            }
 
            // Pass empty string as value to search for, displaying all results
            input.autocomplete( "search", "" );
          });
      },
 
      
      
      _createShowAllByFocusAndButton: function() {
        var input = this.input,
          wasOpen = false;
          
        $( "<a>" )
	      .attr( "tabIndex", -1 )
	      .addClass("imj_submitButton")
	      .tooltip()
	      .insertAfter( this.input )
	      .append( '<span class="fa fa-angle-down">')
	      .on( "mousedown", function() {
	    	wasOpen = input.autocomplete( "widget" ).is( ":visible" );
          })
          .on( "click", function() {
            // Close if already visible
            if ( wasOpen ) {
              return;
            }
            
            // Set focus on input
            input.trigger( "focus" );
          });
      
        input
          .focus(function() {
			input.autocomplete( "search", "" );
			//FIXME: Actions: Set focus on input element (list opens) -> click outside the browser -> click on a position where the list was visible before
        	// => list is briefly displayed and element on this position is selected
		  });
      },

 
      _removeIfInvalid: function( event, ui ) {
 
        // Selected an item, nothing to do
        if ( ui.item ) {
          return;
        }
 
        // Search for a match (case-insensitive)
        var value = this.input.val(),
          valueLowerCase = value.toLowerCase(),
          valid = false;
        this.element.children( "option" ).each(function() {
          if ( $( this ).text().toLowerCase() === valueLowerCase ) {
            this.selected = valid = true;
            return false;
          }
        });
 
        // Found a match, nothing to do
        if ( valid ) {
          return;
        }
 
        // Remove invalid value
        this.input
          .val( "" )
          .attr( "title", value + " didn't match any item" )
          .tooltip( "open" );
        this.element.val( "" );
        this._delay(function() {
          this.input.tooltip( "close" ).attr( "title", "" );
        }, 2500 );
        this.input.autocomplete( "instance" ).term = "";
      },
 
      _destroy: function() {
        //this.wrapper.remove();
        //this.element.show();
      }
    });
 
    /*
    $( "#combobox" ).combobox();
    
    $( "#toggle" ).on( "click", function() {
      $( "#combobox" ).toggle();
    });
    */
