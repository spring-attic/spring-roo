(function(module) { // making private all vars in this scope.

	// Create fnController function
	function fnController($scope, $http, ngDialog, projectService,
			persistenceService, entityService) {

		// Creating form data
		$scope.formData = {};

		var currentEntities = [];

		$scope.projectExists = true;
		$scope.persistenceExists = true;
		$scope.entitiesAvailable = false;
		$scope.needsTypeParam = false;
		$scope.createFieldAvailable = false;

		// Checking if persistence configuration is available
		projectService.getProjectInfo().success(function(data) {
			$scope.projectExists = data.exists;
			if (data.exists) {
				persistenceService.getPersistenceInfo().success(function(data) {
					if (data.persistenceProvider != "") {
						$scope.persistenceExists = true;
						$scope.entitiesAvailable = true;

						// Getting current entities
						entityService.getEntities().success(function(data) {
							$scope.entities = data;
						});

					} else {
						$scope.persistenceExists = false;
					}
				});
			}
		});

		// Function to show selected entity fields
		$scope.onChangeEntity = function() {

			entityService.getEntityInfo($scope.formData.entityName).success(
					function(data) {

						// Getting entity fields
						$scope.entityFields = data.fields;

						// Enabling create field form
						$scope.createFieldAvailable = true;

						$scope.success = true;

					});
		};

		// Function to create new field on current entity
		$scope.addNewField = function() {

			var newEntityField = {};

			// Getting current entity
			newEntityField.entityName = $scope.formData.entityName;

			// Getting new field values
			newEntityField.fieldName = $scope.formData.fieldName;
			newEntityField.fieldGroup = jQuery("#fieldType :selected").parent()
					.attr("label");
			newEntityField.fieldType = $scope.formData.fieldType;
			newEntityField.referencedClass = $scope.formData.referencedClass;

			if ((newEntityField.fieldName == undefined || newEntityField.fieldType == undefined)
					|| (newEntityField.fieldName == "" || newEntityField.fieldType == "")) {
				$scope.message = "You must complete field name and field type to add a new field on '"
						+ newEntityField.entityName + "'";
				$scope.messageClass = "alert alert-danger";
				$scope.success = false;
				return;
			}

			entityService.createField($.param(newEntityField)).success(
					function(data) {
						$scope.success = data.success;
						$scope.message = data.message;
						if (data.success) {
							$scope.messageClass = "alert alert-success";

							// Clean create field form
							$scope.formData.fieldName = "";
							$scope.formData.fieldType = "";

							// Reload fields of current entity
							$scope.onChangeEntity();

						} else {
							$scope.messageClass = "alert alert-danger";
						}
					});
		};

		// Function to enable referencedClass when needed
		$scope.onChangeFieldType = function() {

			$scope.needsTypeParam = false;
			$scope.formData.referencedClass = "";

			var fieldType = $scope.formData.fieldType;

			if (fieldType == "reference" || fieldType == "enum"
					|| fieldType == "embedded" || fieldType == "file"
					|| fieldType == "list" || fieldType == "other"
					|| fieldType == "set") {
				$scope.needsTypeParam = true;
			}
		};

		// Function to show UML
		$scope.showUML = function() {
			ngDialog.open({
				template : 'views/dashboard/uml.html'
			})
		};

		// Function to create new entity
		$scope.processCreateEntity = function() {

			entityService.createEntity($.param($scope.formData)).success(
					function(data) {
						$scope.success = data.success;
						$scope.message = data.message;
						if (data.success) {
							$scope.messageClass = "alert alert-success";
							$scope.projectExists = true;
							// Cleaning new entity name
							$scope.formData.entityName = "";
						} else {
							$scope.messageClass = "alert alert-danger";
						}
					});
		};

	}
	// Create EntitiesCtrl with fnController function
	fnController.$inject = [ '$scope', '$http', 'ngDialog', 'projectService',
			'persistenceService', 'entityService' ];
	module.controller("EntitiesCtrl", fnController);

}(window.rooApp)); // Sending rooApp module on function call
