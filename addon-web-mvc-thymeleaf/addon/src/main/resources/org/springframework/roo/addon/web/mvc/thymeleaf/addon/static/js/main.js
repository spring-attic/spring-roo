(function(jQuery) {
  jQuery(document).ready(
      function() {

        //dropdown
        jQuery('ul.dropdown-menu [data-toggle=dropdown]').on(
            'click',
            function(event) {
              event.preventDefault();
              event.stopPropagation();
              jQuery(this).parent().siblings()
                  .removeClass('open');
              jQuery(this).parent().toggleClass('open');
            });

        //tooltip
        jQuery('[data-toggle="tooltip"]').tooltip();

      });
})(jQuery);

/**
 * Checks if an object in JavaScript is undefined, null or empty string
 *
 * @param obj
 * @returns {Boolean}
 */
function isEmpty(obj) {
  if (jQuery.isPlainObject(obj)) {
    return jQuery.isEmptyObject(obj);
  } else if (jQuery.isArray(obj)) {
    return 0 === obj.length;
  } else if (typeof obj === "string") {
    return (isNull(obj) || 0 === obj.length);
  }
  return isNull(obj);
}

/**
 * Checks if an object in JavaScript is undefined or null
 *
 * @param obj
 * @returns {Boolean}
 */
function isNull(obj) {
  if (typeof obj === "undefined" || obj == null) {
    return true;
  }
  return false;
}

/**
 * Checks if an object in JavaScript is defined and not null
 *
 * @param obj
 * @returns {Boolean}
 */
function isNotNull(obj) {
  return !isNull(obj);
}

/**
 * Checks if an object in JavaScript is undefined, null or empty string
 *
 * @param obj
 * @returns {Boolean}
 */
function isNotEmpty(obj) {
  return !isEmpty(obj);
}

/**
 * Generates and executes an ajax request whose goal is to load data from a 
 * DataTable element.
 *
 * @param data DataTable object data
 * @param callback Name of the function to call with the server data obtained 
 * 			once the ajax request has been completed
 * @param settings DataTable object options
 * @param url Url to use for ajax request
 * @param oDatatable DataTable on which the calling should act upon
 *
 */
function loadData(data, callback, settings, url, oDatatable) {
  if (url) {
	var token = jQuery("meta[name='_csrf']");
	var header = jQuery("meta[name='_csrf_header']");
    jQuery.ajax({
      url : url,
      type : 'GET',
      data : data,
      dataType : 'json',
      headers : {
        Accept : "application/vnd.datatables+json",
      },
      beforeSend: function (request) {
      	  if(token != null && token.length > 0 && header != null && header.length > 0) {
	          request.setRequestHeader(header.attr("content"), token.attr("content"));
      	  }
      },
      success : jQuery.proxy(function(dataReceived) {
        callback(dataReceived);
        if(this.DataTable().state.loaded()){
          var rowSelectedId = this.DataTable().state.loaded().rowSelectedId;
          if(rowSelectedId){
            var rowSelected = this.DataTable().row('#' + rowSelectedId);
            if(rowSelected.length > 0){
              rowSelected.select();
            }
          }
        }
      }, oDatatable),
      error : function(dataReceived) {
        jQuery('#datatablesErrorModal').modal('show');
        callback(getEmptyDataDatatables(data.draw));
      }
    });
  } else {
    callback(getEmptyDataDatatables(data.draw));
  }
}

/**
 * Generates a JSON object with the necessary data for indicating a DataTable 
 * object that any elements has been found.
 *
 * @param draw DataTable request counter
 * @returns {json} JSON object which indicates a DataTable empty list
 */
function getEmptyDataDatatables(draw) {

  var emptyData = {
    'data' : new Array(),
    'draw' : draw,
    'error' : null,
    'recordsFiltered' : '0',
    'recordsTotal' : '0'
  };

  return emptyData;
}