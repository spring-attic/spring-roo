package org.springframework.roo.addon.jpa.addon.jaxrs;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.annotations.RooJavaBean;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.rest.publisher.ResourceMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * TODO
 * 
 * @author Juan Carlos García
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
@Component
@Service
@Path("entities")
public class JpaEntityResource implements ResourceMarker {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(JpaEntityResource.class);

  private Shell shell;
  private TypeLocationService typeLocationService;
  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  /**
   * Method to obtain all created entities on current project
   * 
   * @param out
   */
  @GET
  @Produces("application/json")
  public JsonArray getAllEntities() {

    // JSON Array to store the info about all entities
    JsonArrayBuilder jsonAllEntitiesBuilder = Json.createArrayBuilder();

    // Getting all entities
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            new JavaType(RooJavaBean.class));

    Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();

    while (it.hasNext()) {

      // Getting entity
      ClassOrInterfaceTypeDetails entity = it.next();

      // Get the JsonObjectBuilder for this entity
      JsonObjectBuilder jsonEntityBuilder = createJsonObjectBuilder(entity);

      // Add the JSON Object of this entity to the list of entities info
      jsonAllEntitiesBuilder.add(jsonEntityBuilder);
    }

    // Returning info
    JsonArray jsonAllEntities = jsonAllEntitiesBuilder.build();
    return jsonAllEntities;
  }

  /**
   * Create the JSON that contains all the info about the given entity.
   * 
   * @param entity
   * @return
   */
  private JsonObjectBuilder createJsonObjectBuilder(ClassOrInterfaceTypeDetails entity) {

    // Entity Name
    String entityName = entity.getName().getFullyQualifiedTypeName();
    String topLevelPackage = getProjectOperations().getFocusedTopLevelPackage().toString();

    // Replacing topLevelPackage with ~
    entityName = entityName.replace(topLevelPackage, "~");

    // JSON Array to store the info about the fields of this entity
    JsonArrayBuilder jsonFieldsBuilder = Json.createArrayBuilder();

    for (FieldMetadata field : entity.getDeclaredFields()) {

      // Getting fields values
      String fieldName = field.getFieldName().getSymbolName();
      String fieldType = field.getFieldType().getSimpleTypeName();

      // Getting referenced class
      String referencedClass = "";
      if (field.getFieldType().getSimpleTypeName().equals("Set")
          || field.getFieldType().getSimpleTypeName().equals("List")) {
        referencedClass = field.getFieldType().getParameters().get(0).getSimpleTypeName();
      } else {
        AnnotationMetadata manyToOneAnnotation =
            field.getAnnotation(new JavaType("javax.persistence.ManyToOne"));
        if (manyToOneAnnotation != null) {
          referencedClass = field.getFieldType().getFullyQualifiedTypeName();
          referencedClass = referencedClass.replace(topLevelPackage, "~");
          fieldType = "Reference";
        }
      }

      // JSON Object to store the info about this field
      JsonObjectBuilder jsonFieldBuilder = Json.createObjectBuilder();
      jsonFieldBuilder.add("fieldName", fieldName).add("type", fieldType)
          .add("referencedClass", referencedClass);

      // Add the JSON Object of this field to the list of fields info
      jsonFieldsBuilder.add(jsonFieldBuilder);
    }

    // Checking if current entity is abstract
    boolean isAbstractEntity = entity.isAbstract();

    // JSON Array to store the info about the parent classes of this entity
    JsonArrayBuilder jsonExtendsBuilder = Json.createArrayBuilder();
    for (JavaType extendsType : entity.getExtendsTypes()) {
      jsonExtendsBuilder.add(extendsType.getFullyQualifiedTypeName().replace(topLevelPackage, "~"));
    }

    // JSON Object to store the info about this entity
    JsonObjectBuilder jsonEntityBuilder = Json.createObjectBuilder();
    jsonEntityBuilder.add("entityName", entityName).add("extendsTypes", jsonExtendsBuilder)
        .add("isAbstract", isAbstractEntity).add("fields", jsonFieldsBuilder);

    return jsonEntityBuilder;
  }

  /**
   * Method to obtain details of some entity
   * 
   * @param out
   */
  @GET
  @Path("{entityName}")
  @Produces("application/json")
  public JsonObject getEntityDetails(@PathParam("entityName") String entityName) {

    // Getting all entities
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            new JavaType(RooJavaBean.class));
    String topLevelPackage = getProjectOperations().getFocusedTopLevelPackage().toString();

    Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();

    JsonObjectBuilder jsonEntityBuilder = null;
    boolean entityFound = false;

    while (it.hasNext()) {
      // Getting entity
      ClassOrInterfaceTypeDetails entity = it.next();

      // Entity Name
      String currentEntityName = entity.getName().getFullyQualifiedTypeName();

      // Replacing topLevelPackage with ~
      currentEntityName = currentEntityName.replace(topLevelPackage, "~");

      // If current entity is not the requested entity, continue to
      // next entity
      if (!currentEntityName.toLowerCase().endsWith(entityName.toLowerCase())) {
        continue;
      }

      // Get the JsonObjectBuilder for this entity
      jsonEntityBuilder = createJsonObjectBuilder(entity);
      entityFound = true;
    }

    // Returning JSON
    if (!entityFound) {
      String message = "Entity '" + entityName + "' not found!";

      jsonEntityBuilder = Json.createObjectBuilder();
      jsonEntityBuilder.add("success", false).add("message", message);
    }

    JsonObject jsonEntity = jsonEntityBuilder.build();
    return jsonEntity;
  }

  /**
   * Method to handle POST request of /entities service
   * 
   * @param request
   * @param response
   */
  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public JsonObject createEntity(@FormParam("entityName") String entityName,
      @FormParam("extends") String extendsType, @FormParam("isAbstract") String isAbstract) {

    // Execute entity command
    // TODO: Replace with JPA command
    String entityCommand = "entity jpa --class " + entityName;

    // Adding abstract if needed
    if (isAbstract != null) {
      entityCommand = entityCommand.concat(" --abstract ").concat(isAbstract);
    }

    // Adding extends if needed
    if (extendsType != null && !"".equals(extendsType)) {
      entityCommand = entityCommand.concat(" --extends ").concat(extendsType);
    }

    boolean status = getShell().executeCommand(entityCommand);

    String message = "Created entity '" + entityName + "'!";
    if (!status) {
      message = "Error creating entity.";
    }

    // Returning JSON
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    jsonBuilder.add("success", status).add("message", message);

    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
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
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) context.getService(ref);
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
        ServiceReference<?>[] references =
            context.getAllServiceReferences(Shell.class.getName(), null);

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
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (ProjectOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on EntitiesController.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }
}
