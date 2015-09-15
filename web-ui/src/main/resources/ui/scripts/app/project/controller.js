/**
 * Project controller
 * 
 * @param module
 */
(function(module) { // making private all vars in this scope.

	// Create fnController function
	function fnController($scope, $http, projectService) {
		// Creating form data
		$scope.formData = {};

		// Checking if project exists using project service
		projectService.getProjectInfo().success(function(data) {
			if (data.exists) {
				$scope.formData.projectName = data.projectName;
				$scope.formData.topLevelPackage = data.topLevelPackage;
				$scope.projectExists = data.exists;
			}
		});

		// process the form
		$scope.processForm = function() {

			// Generating project using project service
			projectService.generateProject($.param($scope.formData)).success(function(data) {
				$scope.success = data.success;
				$scope.message = data.message;
				if (data.success) {
					$scope.messageClass = "alert alert-success";
					$scope.projectExists = true;
				} else {
					$scope.messageClass = "alert alert-danger";
				}
			});

		};

	}

	// Create ProjectCtrl with fnController function
	fnController.$inject = [ '$scope', '$http', 'projectService' ];
	module.controller("ProjectCtrl", fnController);

}(window.rooApp)); // Sending rooApp module on function call
