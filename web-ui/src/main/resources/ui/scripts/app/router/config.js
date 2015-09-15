(function(module){ // making private all vars in this scope.

	module.config(["$stateProvider", "$urlRouterProvider", function(r, t) {
	    t.when("", "/dashboard/home"), 
	    r.state("dashboard", {
	        url: "/dashboard",
	        templateUrl: "views/dashboard.html",
	        controller: "DashboardCtrl"
	    }).state("home", {
	        url: "/home",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/home.html"
	    }).state("project", {
	        url: "/project",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/project.html",
	        controller: "ProjectCtrl"
	    }).state("persistence", {
	        url: "/persistence",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/persistence.html",
	        controller: "PersistenceCtrl"
	    }).state("entities", {
	        url: "/entities",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/entities.html",
	        controller: "EntitiesCtrl"
	    }).state("create-entity", {
	        url: "/create-entity",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/create-entity.html",
	        controller: "EntitiesCtrl"
	    }).state("uml", {
	        url: "/uml",
	        parent: "dashboard",
	        templateUrl: "views/dashboard/uml.html",
	        controller: "UmlCtrl"
	    })
	    
	}]);

}(window.rooApp)); // Sending rooApp module on function call