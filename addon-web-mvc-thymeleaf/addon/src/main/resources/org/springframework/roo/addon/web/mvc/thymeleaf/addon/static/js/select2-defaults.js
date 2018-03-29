// IIFE - Immediately Invoked Function Expression
(function(extendSelect2) {

  // The global jQuery object is passed as a parameter
  extendSelect2(window.jQuery, window, document);

}(function($, window, document) {

  // The $ is now locally scoped, it won't collide with other libraries 

  // Listen for the jQuery ready event on the document
  // READY EVENT BEGIN
  $(function() {
   
    // The DOM is ready!
    //console.log('The DOM is ready');

    // Init select simple
    $('.dropdown-select-simple').select2({
      debug : false,
      theme : 'bootstrap',
      allowClear : true,
    });

    $('.dropdown-select-simple[data-ajax--url]').each(function (index, item){
      var $item = $(item);
      console.log(
          "WARNING: Select2 with class 'dropdown-select-simple' which declares 'data-ajax--url' (should be 'dropdown-select-ajax'?): id="
          + $item.attr('id') + " name=" + $item.attr('name'));
    });

    // Init select with AJAX search
    $('.dropdown-select-ajax').select2({
      debug : false,
      theme : 'bootstrap',
      allowClear : true,
      ajax : {
        data : function(params) {
          // set search params names to match with GlobalSearch and
          // Pageable arguments
          var query = {
            'search[value]' : params.term,
            'page' : params.page - 1,
          };
          return query;
        }
      }
    });    
  });
   
  // READY EVENT END
  //console.log('The DOM may not be ready');
   
  // The rest of code goes here!
}));

