/**
 * Script that manages the concurrency control form
 */
$(document).ready(function () {

    // Obtain the Concurrency Control Form
    var $occForm = $("#concurrency-control-form");

    // Check if exists in the current page. If not, is not
    // necessary to apply any action.
    if ($occForm.length > 0) {

        // Register function that will apply concurrency
        function fnApplyConcurrency(occForm, event) {
            event.preventDefault();
            // Obtain the selected option
            var option = occForm.find("input[type='radio']:checked").val();
            // If discard, reload the form with the provided URL
            if(option === "discard"){
                window.location.href = occForm.data("editFormUrl");
                return false;
            }else if(option === "apply") {
                // If not, update the version field id to ensure that the new version is the last one
                $("input[name='version']").val(occForm.data("newVersion"));
                // Submit the form
                occForm.closest("form").submit();
            }

        }

        // Obtain all submit buttons and register click
        // event
        $("button[type='submit']").on("click", $.proxy(fnApplyConcurrency, this, $occForm));
    }

});