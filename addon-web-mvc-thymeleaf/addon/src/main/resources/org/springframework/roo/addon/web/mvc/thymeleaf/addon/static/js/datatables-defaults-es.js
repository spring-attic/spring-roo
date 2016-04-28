// Definiciones de textos multiidioma añadiendo algunas a las ya proporcionadas
// por defecto de Datatables
$.extend( $.fn.dataTable.defaults, {
	'language': {
		'buttons': {
			'add': 'Añadir',
			'colvis': 'Columnas',
			'pageLength': 'Mostrar %d filas'	
		},
		'select': {
            'rows': {
                _: "%d filas seleccionadas",
                0: "",
                1: "1 fila seleccionada"
            }
        },		
		'url': "//cdn.datatables.net/plug-ins/1.10.11/i18n/Spanish.json",
	}
} );