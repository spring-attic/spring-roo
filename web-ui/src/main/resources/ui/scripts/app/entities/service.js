/**
 * Entity service that will provide different information about entity model of
 * current project
 * 
 * @param module
 */
(function(module) {

	function fnService($http) {

		/**
		 * Function to obtain information about current entities
		 */
		this.getEntities = function() {

			var request = {
				method : 'GET',
				url : '/rs-api/entities',
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to get info about an specific entity
		 */
		this.getEntityInfo = function(entityName) {

			var request = {
				method : 'GET',
				url : '/rs-api/entities/' + entityName,
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to generate new entity
		 */
		this.createEntity = function(data) {

			var request = {
				method : 'POST',
				url : '/rs-api/entities',
				data : data,
				headers : {
					'Content-Type' : 'application/x-www-form-urlencoded'
				}
			};

			var promise = $http(request);
			return promise;
		}

		/**
		 * Function to generate new entity field
		 */
		this.createField = function(data) {

			var request = {
				method : 'POST',
				url : '/rs-api/fields',
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
	module.service("entityService", fnService)

}(window.rooApp));