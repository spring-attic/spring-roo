(function(jQuery){
	jQuery(document).ready(function(){

		//dropdown
		jQuery('ul.dropdown-menu [data-toggle=dropdown]').on('click', function(event) {
			event.preventDefault();
			event.stopPropagation();
			jQuery(this).parent().siblings().removeClass('open');
			jQuery(this).parent().toggleClass('open');
		});

		//tooltip
		jQuery('[data-toggle="tooltip"]').tooltip();
	});
})(jQuery);