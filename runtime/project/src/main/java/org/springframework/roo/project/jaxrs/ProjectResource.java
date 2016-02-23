package org.springframework.roo.project.jaxrs;

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
import org.springframework.roo.project.MavenOperations;
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
@Path("project")
public class ProjectResource implements ResourceMarker {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(ProjectResource.class);

  private Shell shell;
  private ProjectOperations projectOperations;
  private MavenOperations mavenOperations;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  @GET
  @Produces("application/json")
  public JsonObject getProject() {
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    ProjectOperations prjOperations = getProjectOperations();
    MavenOperations mvnOperations = getMavenOperations();

    // Checking if project exists
    if (!mvnOperations.isCreateProjectAvailable()) {

      // Getting project info
      jsonBuilder.add("projectName", prjOperations.getFocusedProjectName())
          .add("topLevelPackage", getProjectOperations().getFocusedTopLevelPackage().toString())
          .add("exists", true);
    } else {
      String message = "No Spring Roo project has been created yet.";

      // Returning JSON
      jsonBuilder.add("success", true).add("message", message);
    }

    // Returning JSON
    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public JsonObject setupProject(@FormParam("projectName") String projectName,
      @FormParam("topLevelPackage") String topLevelPackage) {

    // Execute project command
    // TODO: Replace with Project command
    boolean status =
        getShell().executeCommand(
            "project setup --projectName " + projectName + " --topLevelPackage " + topLevelPackage);

    String message = "Spring Roo project has been generated correctly!";
    if (!status) {
      message = "Error configuring Spring Roo project";
    }

    // Returning JSON
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    jsonBuilder.add("success", status).add("message", message);

    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
  }

  /**
   * Method to get MavenOperations Service implementation
   * 
   * @return
   */
  public MavenOperations getMavenOperations() {
    if (mavenOperations == null) {
      // Get all Services implement MavenOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MavenOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MavenOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MavenOperations on ProjectConfigurationController.");
        return null;
      }
    } else {
      return mavenOperations;
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
        LOGGER.warning("Cannot load ProjectOperations on ProjectConfigurationController.");
        return null;
      }
    } else {
      return projectOperations;
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
}
