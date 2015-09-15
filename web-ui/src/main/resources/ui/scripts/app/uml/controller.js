(function(module){ // making private all vars in this scope.
	
	// Create fnController function
	function fnController($scope, $http, ngDialog, entityService) {
		// Getting all entities
		entityService.getEntities().success(function(data) {
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
	}
	// Create UmlCtrl with fnController function
	fnController.$inject = ['$scope', '$http', "ngDialog", "entityService"];
	module.controller("UmlCtrl", fnController);
	
}(window.rooApp)); // Sending rooApp module on function call