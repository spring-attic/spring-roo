/**
 * Project service that will provide different information about project status
 * on current project
 * 
 * @param module
 */
(function(module) {

	function fnService($http) {

		/**
		 * Function to obtain information about current project
		 */
		this.getProjectInfo = function() {

			var request = {
				method : 'GET',
				url : '/rs-api/project',
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to generate new project
		 */
		this.generateProject = function(data) {

			var request = {
				method : 'POST',
				url : '/rs-api/project',
				data : data,
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}
	}

	fnService.$inject = [ "$http" ];
	module.service("projectService", fnService)

}(window.rooApp));