(function(jQuery) {
	jQuery(document).ready(
			function() {

				// dropdown
				jQuery('ul.dropdown-menu [data-toggle=dropdown]').on(
						'click',
						function(event) {
							event.preventDefault();
							event.stopPropagation();
							jQuery(this).parent().siblings()
									.removeClass('open');
							jQuery(this).parent().toggleClass('open');
						});

				// tooltip
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
 * Genera y ejecuta una petición ajax con el objetivo de cargar los datos de un
 * elemento datatables.
 * 
 * @param data
 *            Datos del objeto datatables
 * @param callback
 *            Función a la cual llamar con los datos obtenidos del servidor tras
 *            la petición ajax
 * @param settings
 *            Opciones del objeto datatables
 * @param url
 *            Url sobre la cual se realiza la petición ajax
 * @param oDatatable
 *            Objeto datatable sobre el que se realiza la llamada
 * 
 */
function loadData(data, callback, settings, url, oDatatable) {
	if (url) {
		jQuery.ajax({
			url : url,
			type : 'GET',
			data : data,
			dataType : 'json',
			headers : {
				Accept : "application/vnd.datatables+json",
			},
			success : jQuery
					.proxy(
							function(dataReceived) {
								callback(dataReceived);
								if (this.DataTable().state.loaded()) {
									var rowSelectedId = this.DataTable().state
											.loaded().rowSelectedId;
									if (rowSelectedId) {
										var rowSelected = this.DataTable().row(
												'#' + rowSelectedId);
										if (rowSelected.length > 0) {
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
 * Genera un objeto json con los datos necesarios que permiten indicar a un
 * objeto datatables que no se han obtenido elementos.
 * 
 * @param draw
 *            Contador de peticiones de datatables
 * @returns {json} Objeto json que indica un listado de datatables vacío
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