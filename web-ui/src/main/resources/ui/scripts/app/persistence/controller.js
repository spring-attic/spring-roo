/**
 * Persistence controller
 * 
 * @param module
 */
(function(module) { // making private all vars in this scope.

	// Create fnController function
	function fnController($scope, $http, projectService, persistenceService) {

		// Creating form data
		$scope.formData = {};

		// Checking if persistence configuration is available
		projectService.getProjectInfo().success(function(data) {
			$scope.persistenceAvailable = data.exists;
			if (data.exists) {

			// Getting available ORM Providers
			persistenceService.getAvailableORMs().success(function(data) {
				$scope.ormProviders = data;
	
				// Getting available Databases
				persistenceService.getAvailableDatabases().success(function(data) {
					$scope.databases = data;

					// Getting persistence info
					persistenceService.getPersistenceInfo().success(
						function(data) {
							$scope.formData.database = data.database;
							$scope.formData.providerName = data.persistenceProvider;
							$scope.formData.username = data.username;
							$scope.formData.password = data.password;
						});
					});

				});
			}
		});

		// process the form
		$scope.processForm = function() {

			var providerName = $scope.formData.providerName;
			var database = $scope.formData.database;

			if (providerName == undefined || database == undefined) {
				$scope.message = "You must select a valid Persistence Provider and a valid Database.";
				$scope.messageClass = "alert alert-danger";
				$scope.success = false;
				return;
			}

			persistenceService.savePersistence($.param($scope.formData)).success(function(data) {
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

	// Create PersistenceCtrl with fnController function
	fnController.$inject = [ '$scope', '$http', 'projectService',
			'persistenceService' ];
	module.controller("PersistenceCtrl", fnController);

}(window.rooApp)); // Sending rooApp module on function call
