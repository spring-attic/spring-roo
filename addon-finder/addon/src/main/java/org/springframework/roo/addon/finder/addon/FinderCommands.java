package org.springframework.roo.addon.finder.addon;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.parser.PartTree;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the 'finder' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class FinderCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(FinderCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private FinderOperations finderOperations;

  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;
  private MemberDetailsScanner memberDetailsScanner;
  private Map<String, MemberDetails> memberDetails;
  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    memberDetails = new HashMap<String, MemberDetails>();
  }

  @CliOptionVisibilityIndicator(command = "finder add", params = {"name"},
      help = "name parameter is not available if class parameter is not specified.")
  public boolean isNameVisible(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If mandatory parameter class is not defined, name parameter is not
    // visible
    String entity = params.get("class");
    if (StringUtils.isBlank(entity)) {
      return false;
    }
    try {
      if (getMemberDetails(entity) == null) {
        return false;
      }
    } catch (Exception e) {
      LOGGER.warning(e.getLocalizedMessage());
      return false;
    }

    return true;
  }

  @CliOptionAutocompleteIndicator(
      command = "finder add",
      includeSpaceOnFinish = false,
      param = "name",
      help = "The option 'name' constructs a Spring Data query based on what is already defined. Please, write a right value for 'name'")
  public List<String> returnOptions(ShellContext shellContext) {

    Map<String, String> contextParameters = shellContext.getParameters();
    String name = contextParameters.get("name");

    try {
      PartTree part =
          new PartTree(name, getMemberDetails(contextParameters.get("class")),
              getTypeLocationService());
      return part.getOptions();
    } catch (Exception e) {
      LOGGER.warning(e.getLocalizedMessage());
      return null;
    }

  }

  @CliCommand(value = "finder add",
      help = "Install a finder in the given target (must be an entity)")
  public void installFinders(@CliOption(key = "class", mandatory = true,
      unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT,
      help = "The entity for which the finders are generated") final JavaType typeName,
      @CliOption(key = "name", mandatory = true,
          help = "The finder string defined as a Spring Data query") final String finderName) {

    PartTree partTree =
        new PartTree(finderName, getMemberDetails(typeName.getFullyQualifiedTypeName()),
            getTypeLocationService());

    if (!partTree.isValid()) {
      LOGGER.warning("Query not valid");
    }

  }

  @CliAvailabilityIndicator({"finder add"})
  public boolean isFinderCommandAvailable() {
    return finderOperations.isFinderInstallationPossible();
  }

  public MemberDetails getMemberDetails(String entityName) {

    if (memberDetails.containsKey(entityName)) {
      return memberDetails.get(entityName);
    }

    // Getting JavaType for entityName
    // Check first if contains base package (~)
    if (entityName.contains("~")) {
      entityName =
          entityName.replace("~", getProjectOperations().getFocusedTopLevelPackage()
              .getFullyQualifiedPackageName());
    }
    JavaType entityType = new JavaType(entityName);


    // We know the file exists, as there's already entity metadata for it
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entityType);

    if (cid == null) {
      throw new RuntimeException("ERROR: Cannot locate source for '" + entityType + "'");
    }

    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) == null) {
      LOGGER.warning("Unable to find the entity annotation on '" + entityType + "'");
      return null;
    }

    memberDetails.put(entityName,
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), cid));
    return memberDetails.get(entityName);

  }

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
        LOGGER.warning("ERROR: Cannot load ProjectOperations on FinderOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeLocationService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on FinderOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeManagementService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on FinderOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }


  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MemberDetailsScanner) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on FinderOperationsImpl.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }

}
