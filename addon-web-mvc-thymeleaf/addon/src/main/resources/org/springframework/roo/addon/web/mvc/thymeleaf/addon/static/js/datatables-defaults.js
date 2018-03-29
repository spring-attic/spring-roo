// IIFE - Immediately Invoked Function Expression
(function(extendDatatables) {

  // The global jQuery object is passed as a parameter
  extendDatatables(window.jQuery, window, document);

}(function($, window, document) {

  // The $ is now locally scoped, it won't collide with other libraries

  // Listen for the jQuery ready event on the document
  // READY EVENT BEGIN
  $(function() {
    // Initialize all datatables in current page
    $('table[data-datatables="true"]').each(function(){
      // Use the advanced extension to auto-configure all
      // advanced features (ajax, export, add, edit, show, delete, etc.)
      $(this).DataTable({
          mark: {
              "each": function(node){
                  // PREVENT THAT NOT SEARCHABLE COLUMNS
                  // INCLUDE THE MARK
                  var $td = $(node).parent();
                  var $th = $td.closest('table').find('th').eq($td.index());
                  var searchable = $th.data("searchable");
                  var htmlNoMark = $td.html().replaceAll("<mark data-markjs=\"true\">","").replaceAll("</mark>", "");
                  var existsNextMatch = $td.html().substring($td.html().lastIndexOf("</mark>")+7).toLowerCase().indexOf($(node).html().toLowerCase());
                  if(searchable != undefined && searchable == false &&
                      (existsNextMatch == -1 || existsNextMatch == htmlNoMark.length)){
                      // Change the cell content when all matches has been selected
                      $td.html(htmlNoMark);
                  }
              }
          },
          advanced: true
      });
    });
  });

  // READY EVENT END
  //console.log('The DOM may not be ready');

  // The rest of code goes here!
}));