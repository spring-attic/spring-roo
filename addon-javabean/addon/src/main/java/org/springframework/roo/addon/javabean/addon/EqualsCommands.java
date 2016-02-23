package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the Equals add-on to be used by the ROO shell.
 * 
 * @author Alan Stewart
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class EqualsCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(EqualsCommands.class);

  @Reference
  private EqualsOperations equalsOperations;

  private ProjectOperations projectOperations;

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @CliAvailabilityIndicator("equals")
  public boolean isEqualsAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @CliCommand(value = "equals", help = "Add equals and hashCode methods to a class")
  public void addEquals(
      @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class") final JavaType javaType,
      @CliOption(key = "appendSuper", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether to call the super class equals and hashCode methods") final boolean appendSuper,
      @CliOption(
          key = "excludeFields",
          mandatory = false,
          specifiedDefaultValue = "",
          optionContext = "exclude-fields",
          help = "The fields to exclude in the equals and hashcode methods. Multiple field names must be a double-quoted list separated by spaces") final Set<String> excludeFields) {

    equalsOperations.addEqualsAndHashCodeMethods(javaType, appendSuper, excludeFields);
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Shell implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on EqualsCommands.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }
}
