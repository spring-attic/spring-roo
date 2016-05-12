(function(jQuery){
	jQuery(document).ready(function(){
		
		/* jQuery Utilities
		================================================== */
		/**
		 * Convert Java's SimpleDateFormat to momentJS formatDate.
		 * Takes a Java pattern
		 * (http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)
		 * and turns it into the expected momentJS formatDate
		 * (http://momentjs.com/docs/#/parsing/string-format/).
		 *
		 * @param pattern SimpleDateFormat pattern
		 * @return moment pattern (if 'pattern' is ommited return defautl pattern)
		 */
		function momentDateFormat(pattern) {
			if (pattern) {
				// Year
				if (pattern.search(/y{3,}/g) >= 0) {
					pattern = pattern.replace(/y{3,}/g, "YYYY"); // yyyy to yy
				} else if (pattern.search(/y{2}/g) >= 0) { // yy to YY
					pattern = pattern.replace(/y{2}/g, "YY");
				}

				// Day
				if (pattern.search(/d{2,}/g) >= 0) { // dd to DD
					pattern = pattern.replace(/d{2,}/g, "DD");
				} else if (pattern.search(/d{1}/g) >= 0) { // d to D
					pattern = pattern.replace(/d{1}/g, "D");
				} else if (pattern.search(/D{1,}/g) >= 0) { // D,DD, DDD to DDD
					pattern = pattern.replace(/D{1,}/g, "DDD");
				}

				// Day in week
				if (pattern.search(/E{4,}/g) >= 0) { // EEEE to dddd
					pattern = pattern.replace(/E{4,}/g, "dddd");
				} else if (pattern.search(/E{2,3}/g) >= 0) { // EEE to ddd
					pattern = pattern.replace(/E{2,3}/g, "ddd");
				}

				// Day in week (number)
				if (pattern.search(/F{1}/g) >= 0) { // F to e
					pattern = pattern.replace(/F{1}/g, "e");
				}

				// week of the year
				if (pattern.search(/w{1,}/g) >= 0) { // ww to WW
					pattern = pattern.replace(/w{1,}/g, "WW");
				}
			} else {
				var pattern = "YYYY/MM/DD HH:mm";
			}

		  return pattern;
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
		 * Checks if an object in JavaScript is undefined, null or empty string
		 * 
		 * @param obj
		 * @returns {Boolean}
		 */
		function isEmpty(obj) {
		  if(jQuery.isPlainObject(obj)) {
		    return jQuery.isEmptyObject(obj);
		  }
		  else if(jQuery.isArray(obj)) {
		    return 0 === obj.length;
		  }
		  else if ( typeof obj === "string" ) {
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
		  if( typeof obj === "undefined" || obj == null ) {
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
		 * Informs if date format (momentJS) includes date information
		 *
		 * @param format string
		 * @returns true if !format or format contains ('YQDMdw')
		 */
		function isDateFormatDate(format) {
		    if (!format) {
		       return true;
			}
		    return format.search(/[YQDMdw]/) > -1;
		}

		/**
		 * Informs if date format (ISO 8601) includes time information
		 *
		 * @param format string
		 * @returns true if !format or format contains ('HmAasSZ')
		 */
		function isDateFormatTime(format) {
		    if (!format) {
		       return true;
			}
		    return format.search(/[HhmAasSZ]/) > -1;
		    ;
		}

		/**
		 * Select the most switchable time format for time selectod
		 * related to requiered format
		 *
		 * @param format
		 * @returns time format
		 */
		function getSelectorTimeFormat(format) {
			//
			if (format.search(/h{1,2}/) > -1 && format.search(/[aA]/) > -1) {
				if (format.search(/[A]/) > -1) {
					return "hh:mm A";
				} else {
					return "hh:mm a";
				}
			}
			return "HH:mm";
		}

		
		// Define parse/format date using moment library
		Date.parseDate = function( input, format ){
		    return moment(input,format).toDate();
		};
		Date.prototype.dateFormat = function( format ){
		    return moment(this).format(format);
		};

		jQuery(".datetimepicker").each(function( index ) {
	      var $input = jQuery(this);
	      var pattern = $input.attr("data-dateformat");
	      var timeStep = $input.attr("data-timestep");
	      try {
	        timeStep = parseInt(timeStep);
	      } catch (e) {
	        timeStep = 5;
	      }

	      if(isNotEmpty(pattern)) {
	        var momentPattern = momentDateFormat(pattern);
	        $input.datetimepicker({format: momentPattern,
	            datepicker: isDateFormatDate(momentPattern),
	            timepicker: isDateFormatTime(momentPattern),
	            step: timeStep,
	            formatDate: "YYYY/MM/DD",
	            formatTime : getSelectorTimeFormat(momentPattern)});
	      }
	      else {
	        var momentPattern = momentDateFormat();
	        $input.datetimepicker({step: timeStep,
	            format: momentPattern,
	            formatDate: "YYYY/MM/DD",
	            formatTime : "HH:mm" });
	      }
	    });
	});
})(jQuery);	