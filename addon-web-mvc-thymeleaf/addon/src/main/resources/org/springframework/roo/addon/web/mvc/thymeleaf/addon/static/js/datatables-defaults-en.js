$.extend( $.fn.dataTable.defaults, {
	'language': {
		'buttons': {
			'add': 'Add',
			'colvis': 'Columns',
			'pageLength': 'Show %d rows'	
		},
		'select': {
            'rows': {
                _: "%d selected rows",
                0: "",
                1: "1 selected row"
            }
        },		
		'url': "//cdn.datatables.net/plug-ins/1.10.11/i18n/English.json",
	}
} );