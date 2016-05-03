(function(jQuery){
	
		jQuery('.dropdown-select-simple').select2({
			debug: false,
			theme: "bootstrap"
		});

		jQuery('.dropdown-select-ajax').select2({
			debug: false,
			theme: "bootstrap",
			ajax: {
				    data: function (params) {
				      // set search params names to match with GlobalSearch and Pageable arguments
				      var query = {
				        "search[value]": params.term,
				        "page": params.page -1,
				      }
				      return query;
				    },
			    	// parse the results into the format expected by Select2.
				    processResults: function (data, page) {

				    	// entity attribute names are specified using the Select2 options 
				    	// feature that are setted using data-* attributes in each <select> element
			            var idField = this.options.get('idField');
			            var txtFields = this.options.get('textFields');
			            var fields = txtFields.split(",");

				    	// parse the results into the format expected by Select2.
				    	// The results are inside a Page object, so we have to iterate
				    	// over the entities in the content attribute.
			            var results = [];
			            jQuery.each(data.content, function(i, entity) {
			            	var id = entity[idField];
			            	var text = "";

			            	// compose the text to be rendered from the specified
			            	// entity fields
			            	jQuery.each(fields, function(i, fieldName) {
				            	text = text.concat(" ", entity[fieldName]); 
				            });

			            	// Select2 assumes the data is an array of {id:"",text:""}
			                results.push(
			                {
			                	'id': id, 
			                	'text': text
			                }
			                );
			            });

			            // calc page info
		                var morePages = !data.last;

			            return {
			                results: results,
			                pagination: {
		                      more: morePages
			                }
			            };
			        },
		  },
		});
})(jQuery);
