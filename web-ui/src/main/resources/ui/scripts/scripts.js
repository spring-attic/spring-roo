// TODO: Move to individual controllers

"use strict";
// Creating Spring Roo App
angular.module("SpringRooApp", ["ui.router", "ngAnimate", "ngDialog"]).config(["$stateProvider", "$urlRouterProvider", function(r, t) {
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
    
}]),


// Creating Dashboard Controller
angular.module("SpringRooApp").controller("DashboardCtrl", ["$scope", "$state", function(r, t) {
    r.$state = t
}]),

//Creating Project Controller
angular.module("SpringRooApp").controller("ProjectCtrl", ["$scope", "$http", function($scope, $http) {
	// Creating form data
	$scope.formData = {};
	
	// Checking if project exists
	$http({
	  method  : 'GET',
	  url     : '/rs-api/project',
	  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
	 })
	  .success(function(data) {
	      if(data.exists){
	    	  $scope.formData.projectName = data.projectName;
	    	  $scope.formData.topLevelPackage = data.topLevelPackage;
	    	  $scope.projectExists = data.exists;
	      }
	  });
	
	
	// process the form
    $scope.processForm = function() {
    	 $http({
		  method  : 'POST',
		  url     : '/rs-api/project',
		  data    : $.param($scope.formData), 
		  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
		 })
		  .success(function(data) {
		      $scope.success = data.success;
		      $scope.message = data.message;
		      if(data.success){
		    	  $scope.messageClass = "alert alert-success";
		    	  $scope.projectExists = true;
		      }else{
		    	  $scope.messageClass = "alert alert-danger";
		      }
		  });
    };
	
}]),


//Creating Persistence Controller
angular.module("SpringRooApp").controller("PersistenceCtrl", ["$scope", "$http", function($scope, $http) {
    
	// Creating form data
	$scope.formData = {};
	
	// Checking if persistence configuration is available
	$http({
	  method  : 'GET',
	  url     : '/rs-api/project',
	  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
	 })
	  .success(function(data) {
    	 $scope.persistenceAvailable = data.exists;
    	 if(data.exists){
    		// Getting available ORM Providers
			$http({
			  method  : 'GET',
			  url     : '/rs-api/persistence/providers',
			  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			 })
			  .success(function(data) {
			      $scope.ormProviders = data;
			      
			      // Getting available Databases
			      $http({
			        method  : 'GET',
			        url     : '/rs-api/persistence/databases',
			        headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			       })
			        .success(function(data) {
			            $scope.databases = data;
			            
			            // Getting persistence info
			        	$http({
			        	  method  : 'GET',
			        	  url     : '/rs-api/persistence',
			        	  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			        	 })
			        	  .success(function(data) {
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
    	 
    	 if(providerName == undefined || database == undefined){
    		 $scope.message = "You must select a valid Persistence Provider and a valid Database.";
    		 $scope.messageClass = "alert alert-danger";
    		 $scope.success = false;
    		 return;
    	 }
    	
    	 $http({
		  method  : 'POST',
		  url     : '/rs-api/persistence',
		  data    : $.param($scope.formData), 
		  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
		 })
		  .success(function(data) {
		      $scope.success = data.success;
		      $scope.message = data.message;
		      if(data.success){
		    	  $scope.messageClass = "alert alert-success";
		    	  $scope.projectExists = true;
		      }else{
		    	  $scope.messageClass = "alert alert-danger";
		      }
		  });
    };
	
}]),


//Creating Entities Controller
angular.module("SpringRooApp").controller("EntitiesCtrl", ["$scope", "$http", "ngDialog", function($scope, $http, ngDialog) {

	// Creating form data
	$scope.formData = {};
	
	var currentEntities = [];
	
	$scope.projectExists = true;
	$scope.persistenceExists = true;
	$scope.entitiesAvailable = false;
	$scope.needsTypeParam = false;
	$scope.createFieldAvailable = false;
	
	// Checking if persistence configuration is available
	$http({
	  method  : 'GET',
	  url     : '/rs-api/project',
	  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
	 })
	  .success(function(data) {
    	 $scope.projectExists = data.exists;
    	 if(data.exists){
    		 $http({
			  method  : 'GET',
			  url     : '/rs-api/persistence',
			  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			 })
			  .success(function(data) {
		    	 if(data.persistenceProvider != ""){
		    		 $scope.persistenceExists = true;
		    		 $scope.entitiesAvailable = true;
		    		 
		    		 // Getting current entities
		    		 $scope.getCurrentEntities();
		    		 
		    	 }else{
		    		 $scope.persistenceExists = false;
		    	 }
			  });
    	 }
	  });
	
	
	  // Function to create new entity
	  $scope.processCreateEntity = function() {
		  $http({
			  method  : 'POST',
			  url     : '/rs-api/entities',
			  data    : $.param($scope.formData), 
			  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			 })
			  .success(function(data) {
			      $scope.success = data.success;
			      $scope.message = data.message;
			      if(data.success){
			    	  $scope.messageClass = "alert alert-success";
			    	  $scope.projectExists = true;
			    	  // Cleaning new entity name
			    	  $scope.formData.entityName = "";
			      }else{
			    	  $scope.messageClass = "alert alert-danger";
			      }
			  });
	  };
	  
	  // Function to show selected entity fields
	  $scope.onChangeEntity = function(){

		// Getting current entities
 		 $http({
		  method  : 'GET',
		  url     : '/rs-api/entities/'+$scope.formData.entityName,
		  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
		 })
		  .success(function(data) {
			  
			  // Getting entity fields
			  $scope.entityFields = data.fields;
			  
			  // Enabling create field form
			  $scope.createFieldAvailable = true;
			  
			  $scope.success = true;
	  	});
	  };
	  
	  
	  // Function to get current entities
	  $scope.getCurrentEntities = function(){
 		 $http({
		  method  : 'GET',
		  url     : '/rs-api/entities',
		  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
		 })
		  .success(function(data) {
			  $scope.entities = data;
		  });
	  }
	  
	  
	  // Function to create new field on current entity
	  $scope.addNewField = function(){
		  
		  var newEntityField = {};
		  
		  // Getting current entity
		  newEntityField.entityName = $scope.formData.entityName;
		  
		  // Getting new field values
		  newEntityField.fieldName = $scope.formData.fieldName;
		  newEntityField.fieldGroup = jQuery("#fieldType :selected").parent().attr("label");
		  newEntityField.fieldType = $scope.formData.fieldType;
		  newEntityField.referencedClass = $scope.formData.referencedClass;
		  
		  
		  if((newEntityField.fieldName == undefined || newEntityField.fieldType == undefined) || 
				  (newEntityField.fieldName == "" || newEntityField.fieldType == "")){
			 $scope.message = "You must complete field name and field type to add a new field on '" + newEntityField.entityName + "'";
    		 $scope.messageClass = "alert alert-danger";
    		 $scope.success = false;
    		 return;
		  }
		  
		  $http({
			  method  : 'POST',
			  url     : '/rs-api/fields',
			  data    : $.param(newEntityField), 
			  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
			 })
			  .success(function(data) {
			      $scope.success = data.success;
			      $scope.message = data.message;
			      if(data.success){
			    	  $scope.messageClass = "alert alert-success";

			    	  // Clean create field form
			    	  $scope.formData.fieldName = "";
			    	  $scope.formData.fieldType = "";
			    	  
			    	  // Reload fields of current entity
			    	  $scope.onChangeEntity();
			    	  
			      }else{
			    	  $scope.messageClass = "alert alert-danger";
			      }
			  });
	  };
	  
	  // Function to enable referencedClass when needed
	  $scope.onChangeFieldType = function(){
		  
		  $scope.needsTypeParam = false;
		  $scope.formData.referencedClass = "";
		  
		  var fieldType = $scope.formData.fieldType;
		  
		  if(fieldType == "reference" || fieldType == "enum" || 
				  fieldType == "embedded" || fieldType == "file" || 
				  fieldType == "list" || fieldType == "other" || 
				  fieldType == "set"){
			  $scope.needsTypeParam = true;  
		  }
	  };
	  
	  // Function to show UML
	  $scope.showUML = function(){
		  ngDialog.open({ template: 'views/dashboard/uml.html'})
	  };
	
}]),


//Creating UML Controller
angular.module("SpringRooApp").controller("UmlCtrl", ["$scope", "$http", "ngDialog", function($scope, $http, ngDialog) {
	// Getting all entities
	 $http({
	  method  : 'GET',
	  url     : '/rs-api/entities',
	  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
	 })
	  .success(function(data) {
		  	// Generating graph and paper
			var graph = new joint.dia.Graph;
			var paper = new joint.dia.Paper({
			    el: jQuery('#paper'),
			    width: jQuery("#paper").width(),
			    model: graph
			});
			var uml = joint.shapes.uml;
			
			// Generating classes
			var classes = {};
			var height = 50;
			
			for(var i = 0; i < data.length; i++){
				
				// Getting fields
				var fields = [];
				for(var x = 0; x < data[i].fields.length; x++){
					var fieldName = data[i].fields[x].fieldName;
					var type = data[i].fields[x].type;
					var referencedClass = data[i].fields[x].referencedClass;
					if(referencedClass !== ""){
						referencedClass = "<"+referencedClass+">";
					}
					fields.push("-" + fieldName + ": " + type + referencedClass);
					height+=25;
				}
				
				// Getting random position
				var xPosition = Math.floor(Math.random() * (jQuery("#paper").width() - 300)) + 100
				var yPosition = Math.floor(Math.random() * (jQuery("#paper").height() - height)) + 100
				
				
				if(data[i]["abstract"]){
					height+=25;
					classes[data[i].entityName] = new uml.Abstract({
						position: { x: xPosition , y: yPosition },
				        size: { width: 300, height: height },
				        name: data[i].entityName,
				        attributes: fields
				    });
				}else{
					classes[data[i].entityName] = new uml.Class({
						position: { x: xPosition , y: yPosition },
				        size: { width: 300, height: height },
				        name: data[i].entityName,
				        attributes: fields
				    });
				}
				height = 50;
				
			}
			
			// Adding classes
			_.each(classes, function(c) { graph.addCell(c); });
			
			
			// Generating relations
			var relations = [];
			
			for(var i = 0; i < data.length; i++){
				
				// Getting fields
				for(var x = 0; x < data[i].fields.length; x++){
					var fieldName = data[i].fields[x].fieldName;
					var type = data[i].fields[x].type;
					var referencedClass = data[i].fields[x].referencedClass;
					
					if(type == "Reference" && referencedClass !== ""){
						relations.push(
								new uml.Aggregation({ source: { id: classes[referencedClass].id }, target: { id: classes[data[i].entityName].id }}));
					}
				}
				
				
				// Getting extends types
				var extendsTypes = [];
				for(var z = 0; z < data[i].extendsTypes.length; z++){
					var extendType = data[i].extendsTypes[z];
					relations.push(
							new uml.Generalization({ source: { id: classes[data[i].entityName].id }, target: { id: classes[extendType].id }}));
				}
				
				
			}
			
			
			_.each(relations, function(r) { graph.addCell(r); }); 
		  
	  });
	
}]),

//Creating Shell Controller
angular.module("SpringRooApp").controller("ShellCtrl", ["$scope", "$http", "$interval", function($scope, $http, $interval) {

	// Creating form data
	$scope.formData = {};
	
	// Function to get shell content
	$scope.getShellContent = function(){
		$http({
		  method  : 'GET',
		  url     : '/rs-api/shell',
		  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
		 })
		  .success(function(data) {
			  var currentHtml = jQuery(".shell-content").html();
			  var re = new RegExp("roo&gt;", 'g');
			  currentHtml = currentHtml.replace(re, "roo>");
			  var re2 = new RegExp("\"", 'g');
			  currentHtml = currentHtml.replace(re2, "'");
			  var re3 = new RegExp("<br>", 'g');
			  currentHtml = currentHtml.replace(re3, "<br/>");
			  
			  // If html changes, refresh web shell
			  if(currentHtml != data){
				  jQuery(".shell-content").html(data);
				  jQuery(".shell-content").animate({ scrollTop: jQuery(".shell-content")[0].scrollHeight }, 500);
			  }
		  });
	}
	
	// Event to execute when user press intro
	$scope.onPressIntro = function(key){
		if(key == 13){
			var commandToExecute = $scope.formData.command;
			
			if(commandToExecute !== "" ){
				$http({
				  method  : 'POST',
				  url     : '/rs-api/shell',
				  data    : $.param($scope.formData), 
				  headers : { 'Content-Type': 'application/x-www-form-urlencoded' } 
				 })
				  .success(function(data) {
				      if(data.success){
				    	  // Cleaning command input
				    	  $scope.formData.command = "";
				      }else{
				    	  jQuery(".shell-content").append("<font color='red'>"+data.message+"</font>");
				    	  jQuery(".shell-content").animate({ scrollTop: jQuery(".shell-content")[0].scrollHeight }, 500);
				      }
				  });
			}
			
		}
	}
	
	  // Loading Shell
	  $interval(function(){
		  // Reload shell content every 5 second
		  $scope.getShellContent();
	  },5000);
	
}]);
