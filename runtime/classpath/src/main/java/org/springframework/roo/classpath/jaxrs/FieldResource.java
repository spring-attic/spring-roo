package org.springframework.roo.classpath.jaxrs;

import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
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
@Path("fields")
public class FieldResource implements ResourceMarker {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(FieldResource.class);

  private Shell shell;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  /**
   * Method to handle GET method request of /fields service
   * 
   * @param request
   * @param response
   */
  @GET
  @Produces("text/html")
  public String getFields() {

    // Return message
    return "<b>GET method on /fields context is not available</b>";
  }

  /**
   * Method to handle POST request of /fields service
   */
  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public JsonObject setupJpa(@FormParam("entityName") String entityName,
      @FormParam("fieldName") String fieldName, @FormParam("fieldGroup") String fieldGroup,
      @FormParam("fieldType") String fieldType, @FormParam("referencedClass") String referencedClass) {

    // Execute fieldcommand
    // TODO: Replace with field command
    String fieldCommand =
        constructFieldCommand(entityName, fieldName, fieldGroup, fieldType, referencedClass);

    boolean status = getShell().executeCommand(fieldCommand);

    String message =
        "New field '".concat(fieldName).concat("' created on '").concat(entityName).concat("'!");
    if (!status) {
      message = "Error creating field.";
    }

    // Returning JSON
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    jsonBuilder.add("success", status).add("message", message);

    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
  }

  /**
   * Method to construct field command.
   * 
   * @param entityName
   * @param fieldName
   * @param fieldGroup
   * @param fieldType
   * @param referencedClass
   * @return
   */
  private String constructFieldCommand(String entityName, String fieldName, String fieldGroup,
      String fieldType, String referencedClass) {

    StringBuilder sb = new StringBuilder();

    sb.append("field ");

    // Depends of field group, command changes
    if ("".equals(fieldGroup)) {
      sb.append(fieldType.toLowerCase()).append(" ");
    } else {
      sb.append(fieldGroup.toLowerCase()).append(" --type ").append(fieldType.toLowerCase())
          .append(" ");
    }

    // Adding field name
    sb.append("--fieldName ").append(fieldName).append(" ");

    // Adding entity where field will be added
    sb.append("--class ").append(entityName);

    // Adding type param
    if (!"".equals(referencedClass)) {
      sb.append(" --type ").append(referencedClass);
    }

    return sb.toString();

  }

  public void destroy() {
    // do nothing.
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
}
