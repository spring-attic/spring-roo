jQuery.extend($.fn.dataTable.ext.buttons, {
	'add': function ( dt, conf ) {
		return {
			'action' : function(e, dt, node, config) {
				var jQueryTable = $(dt.table().node());
				var urlFunction = jQueryTable.data('create-url-function');
				var url = urlFunction ? jQuery[urlFunction]() : jQueryTable.data('create-url');
				if (url) {
					location.href = url;
				}
			},
			'className' : 'btn-accion agregar',
			'text': dt.i18n( 'buttons.add', 'Añadir' )
		};
	}
});

jQuery.extend(jQuery.fn.dataTable.defaults, {
	'ajax' : {
		'headers' : {
			'Accept' : 'application/vnd.datatables+json'
		},
	},
	'buttons' : [
	    'add',
	    {
			'extend' : 'colvis',
			'className' : 'btn-accion',
	    }, 
	    {
			'extend' : 'pageLength',
			'className' : 'btn-accion',
	    } 
	],
	'deferRender' : true,
	'dom' : 'Bfrtip',
	'processing' : true,
	'responsive' : true,
	'serverSide' : true,
	'stateSave' : true,
});

jQuery.extend({
	'createUri': 'create-form',
	'editUri': 'edit-form',
	'deleteUri': 'delete-form'
});
