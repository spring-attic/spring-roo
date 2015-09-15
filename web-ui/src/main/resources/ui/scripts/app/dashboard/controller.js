(function(module){ // making private all vars in this scope.

	// Create fnController function
	function fnController($scope, $state) {
		$scope.$state = $state;
	}
	
	// Create DashboardCtrl with fnController function
	fnController.$inject = ['$scope', '$state'];
	module.controller("DashboardCtrl", fnController);

}(window.rooApp)); // Sending rooApp module on function call