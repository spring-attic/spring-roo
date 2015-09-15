/**
 * Persistence service that will provide different information about persistence
 * status on current project
 * 
 * @param module
 */
(function(module) {

	function fnService($http) {

		/**
		 * Function to obtain available ORMs from Spring Roo Shell
		 */
		this.getAvailableORMs = function() {

			var request = {
				method : 'GET',
				url : '/rs-api/persistence/providers',
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to obtain available Databases from Spring Roo Shell
		 */
		this.getAvailableDatabases = function() {

			var request = {
				method : 'GET',
				url : '/rs-api/persistence/databases',
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to obtain persistence info from generated project
		 */
		this.getPersistenceInfo = function() {

			var request = {
				method : 'GET',
				url : '/rs-api/persistence',
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to save persistence info
		 */
		this.savePersistence = function(data) {

			var request = {
				method : 'POST',
				url : '/rs-api/persistence',
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
	module.service("persistenceService", fnService)

}(window.rooApp));