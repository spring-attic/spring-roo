package org.springframework.roo.web.ui.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.addon.javabean.annotations.RooJavaBean;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.web.ui.domain.Entity;
import org.springframework.roo.web.ui.domain.Field;
import org.springframework.roo.web.ui.domain.Status;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class EntitiesServlet extends HttpServlet {

	private BundleContext context;
	private static final Logger LOGGER = HandlerUtils
			.getLogger(EntitiesServlet.class);

	private Shell shell;
	private TypeLocationService typeLocationService;
	private ProjectOperations projectOperations;

	public EntitiesServlet(BundleContext bContext) {
		this.context = bContext;
	}

	public void init() throws ServletException {

	}

	/**
	 * Method to handle GET method request of /entities service
	 * 
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		String method = request.getPathInfo();

		if (method == null || method.equals("/")) {
			getAllEntities(out);
		}else{
			// Removing URL / from entity name
			getEntityDetails(method.substring(1), out);
		}
	}

	/**
	 * Method to handle POST request of /entities service
	 * 
	 * @param request
	 * @param response
	 * 
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		// Getting params
		String entityName = request.getParameter("entityName").trim();
		String extendsType = request.getParameter("extends");
		String isAbstract = request.getParameter("isAbstract");

		// Execute entity command
		// TODO: Replace with JPA command
		String entityCommand = "entity jpa --class " + entityName;
		
		// Adding abstract if needed
		if(isAbstract != null){
			entityCommand = entityCommand.concat(" --abstract ").concat(isAbstract);
		}
		
		// Adding extends if needed
		if(extendsType != null && !"".equals(extendsType)){
			entityCommand = entityCommand.concat(" --extends ").concat(extendsType);
		}

		boolean status = getShell().executeCommand(entityCommand);

		String message = "Created entity '" + entityName + "'!";
		if (!status) {
			message = "Error creating entity.";
		}

		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer()
				.withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(new Status(status, message));
		out.println(json);

	}

	public void destroy() {
		// do nothing.
	}

	/**
	 * Method to obtain all created entities on current project
	 * 
	 * @param out
	 */
	private void getAllEntities(PrintWriter out) throws ServletException,
			IOException {

		List<Entity> allEntities = new ArrayList<Entity>();

		// Getting all entities
		Set<ClassOrInterfaceTypeDetails> entities = getTypeLocationService()
				.findClassesOrInterfaceDetailsWithAnnotation(
						new JavaType(RooJavaBean.class));

		Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();

		while (it.hasNext()) {
			// Getting entity
			ClassOrInterfaceTypeDetails entity = it.next();
			// Entity Name
			String entityName = entity.getName().getFullyQualifiedTypeName();
			String topLevelPackage = getProjectOperations().getFocusedTopLevelPackage().toString();
			
			// Replacing topLevelPackage with ~
			entityName = entityName.replace(topLevelPackage, "~");
			List<Field> entityFields = new ArrayList<Field>();
			for (FieldMetadata field : entity.getDeclaredFields()) {
				
				// Getting fields values
				String fieldName = field.getFieldName().getSymbolName();
				String fieldType = field.getFieldType().getSimpleTypeName();
				
				// Getting referenced class
				String referencedClass = "";
				if(field.getFieldType().getSimpleTypeName().equals("Set") ||
						field.getFieldType().getSimpleTypeName().equals("List")){
					referencedClass = field.getFieldType().getParameters().get(0).getSimpleTypeName();
				}else{
					AnnotationMetadata manyToOneAnnotation = field.getAnnotation(new JavaType("javax.persistence.ManyToOne"));
					if(manyToOneAnnotation != null){
						referencedClass = field.getFieldType().getFullyQualifiedTypeName();
						referencedClass = referencedClass.replace(topLevelPackage, "~");
						fieldType = "Reference";
					}
				}
				
				// Creating entityField Object
				Field entityField = new Field(fieldName, fieldType, referencedClass);
				// Adding field to entity 
				entityFields.add(entityField);

			}
			
			// Checking if current entity is abstract
			boolean isAbstractEntity = entity.isAbstract();
			
			// Adding extends type
			List<String> extendsTypes = new ArrayList<String>();
			for(JavaType extendsType : entity.getExtendsTypes()){
				extendsTypes.add(extendsType.getFullyQualifiedTypeName().replace(topLevelPackage, "~"));
			}
			
			// Creating Entity Object
			allEntities.add(new Entity(entityName, extendsTypes, isAbstractEntity, entityFields));
		}

		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer()
				.withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(allEntities);
		out.println(json);

	}
	
	/**
	 * Method to obtain details of some entity
	 * 
	 * @param out
	 */
	private void getEntityDetails(String entityName, PrintWriter out) throws ServletException,
			IOException {

		List<Entity> allEntities = new ArrayList<Entity>();

		// Getting all entities
		Set<ClassOrInterfaceTypeDetails> entities = getTypeLocationService()
				.findClassesOrInterfaceDetailsWithAnnotation(
						new JavaType(RooJavaBean.class));

		Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();

		while (it.hasNext()) {
			// Getting entity
			ClassOrInterfaceTypeDetails entity = it.next();
			
			// Entity Name
			String currentEntityName = entity.getName().getFullyQualifiedTypeName();
			String topLevelPackage = getProjectOperations().getFocusedTopLevelPackage().toString();
			
			// Replacing topLevelPackage with ~
			currentEntityName = currentEntityName.replace(topLevelPackage, "~");
			
			// Getting info only about selected entity
			if(entityName.equals(currentEntityName)){
				List<Field> entityFields = new ArrayList<Field>();
				for (FieldMetadata field : entity.getDeclaredFields()) {
					
					// Getting fields values
					String fieldName = field.getFieldName().getSymbolName();
					String fieldType = field.getFieldType().getSimpleTypeName();
					
					// Getting referenced class
					String referencedClass = "";
					if(field.getFieldType().getSimpleTypeName().equals("Set") ||
							field.getFieldType().getSimpleTypeName().equals("List")){
						referencedClass = field.getFieldType().getParameters().get(0).getSimpleTypeName();
					}else{
						AnnotationMetadata manyToOneAnnotation = field.getAnnotation(new JavaType("javax.persistence.ManyToOne"));
						if(manyToOneAnnotation != null){
							referencedClass = field.getFieldType().getFullyQualifiedTypeName();
							referencedClass = referencedClass.replace(topLevelPackage, "~");
							fieldType = "Reference";
						}
					}
					
					// Creating entityField Object
					Field entityField = new Field(fieldName, fieldType, referencedClass);
					// Adding field to entity 
					entityFields.add(entityField);

				}
				
				// Checking if current entity is abstract
				boolean isAbstractEntity = entity.isAbstract();
				
				// Adding extends type
				List<String> extendsTypes = new ArrayList<String>();
				for(JavaType extendsType : entity.getExtendsTypes()){
					extendsTypes.add(extendsType.getFullyQualifiedTypeName().replace(topLevelPackage, "~"));
				}
				
				// Creating Entity Object
				allEntities.add(new Entity(currentEntityName, extendsTypes, isAbstractEntity, entityFields));
			}
		}

		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer()
				.withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(allEntities);
		out.println(json);

	}

	/**
	 * Method to get TypeLocationService Service implementation
	 * 
	 * @return
	 */
	public TypeLocationService getTypeLocationService() {
		if (typeLocationService == null) {
			// Get all TypeLocationService implement Shell interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								TypeLocationService.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					typeLocationService = (TypeLocationService) context
							.getService(ref);
					return typeLocationService;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load TypeLocationService on ProjectConfigurationController.");
				return null;
			}
		} else {
			return typeLocationService;
		}
	}

	/**
	 * Method to get Shell Service implementation
	 * 
	 * @return
	 */
	public Shell getShell() {
		if (shell == null) {
			// Get all Services implement Shell interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(Shell.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					shell = (Shell) context.getService(ref);
					return shell;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load Shell on ProjectConfigurationController.");
				return null;
			}
		} else {
			return shell;
		}
	}
	
	/**
	 * Method to get ProjectOperations Service implementation
	 * 
	 * @return
	 */
    public ProjectOperations getProjectOperations() {
        if (projectOperations == null) {
            // Get all Services implement ProjectOperations interface
            try {
                ServiceReference<?>[] references = this.context
                        .getAllServiceReferences(
                        		ProjectOperations.class.getName(), null);

                for (ServiceReference<?> ref : references) {
                    return (ProjectOperations) this.context.getService(ref);
                }

                return null;

            }
            catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load ProjectOperations on EntitiesController.");
                return null;
            }
        }
        else {
            return projectOperations;
        }
    }
}