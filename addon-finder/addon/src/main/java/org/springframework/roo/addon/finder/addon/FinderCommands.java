package org.springframework.roo.addon.finder.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.parser.FinderAutocomplete;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.finder.addon.parser.PartTree;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Commands for the 'finder' add-on to be used by the ROO shell.
 * 
 * These commands allow developers to generate Spring Data finders 
 * on existing repositories.
 * 
 * @author Stefan Schmidt
 * @author Paula Navarro
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class FinderCommands implements CommandMarker, FinderAutocomplete {

  private static final Logger LOGGER = HandlerUtils.getLogger(FinderCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private FinderOperations finderOperations;
  @Reference
  private LastUsed lastUsed;

  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;
  private MemberDetailsScanner memberDetailsScanner;
  private ProjectOperations projectOperations;
  private Converter<JavaType> javaTypeConverter;

  // Map where entity details will be cached
  private Map<JavaType, MemberDetails> entitiesDetails;


  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    this.entitiesDetails = new HashMap<JavaType, MemberDetails>();
  }

  @CliAvailabilityIndicator({"finder add"})
  public boolean isFinderCommandAvailable() {
    return finderOperations.isFinderInstallationPossible();
  }

  @CliOptionVisibilityIndicator(command = "finder add", params = {"name"},
      help = "You must define --entity to be able to define --name parameter.")
  public boolean isNameVisible(ShellContext shellContext) {

    // Getting all defined parameters on autocompleted command
    Map<String, String> params = shellContext.getParameters();

    // If mandatory parameter entity is not defined, name parameter should not
    // be visible
    String entity = params.get("entity");
    if (StringUtils.isBlank(entity)) {
      return false;
    }

    // Get current entity member details to check if is a valid Spring Roo entity
    MemberDetails entityDetails = getEntityDetails(entity);

    // If not entity details, is not a valid entity, so --name parameter is not visible
    if (entityDetails == null) {
      return false;
    }

    return true;
  }

  /**
   * This indicator says if --defaultReturnType parameter should be visible or not.
   * 
   * @param context ShellContext
   * @return false if domain entity specified in --entity parameter has no associated Projections.
   */
  @CliOptionVisibilityIndicator(params = "defaultReturnType", command = "finder add",
      help = "--defaultReturnType parameter is not visible if --entity parameter hasn't "
          + "been specified before or if there aren't exist any Projection class associated "
          + "to the current entity.")
  public boolean isDefaultReturnTypeParameterVisible(ShellContext shellContext) {

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);
    if (entity == null) {
      return false;
    }

    Set<ClassOrInterfaceTypeDetails> projectionsInProject =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
    for (ClassOrInterfaceTypeDetails projection : projectionsInProject) {

      // Add only projections associated to the entity specified in the command
      if (projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
          .getValue().equals(entity)) {
        return true;
      }
    }

    return false;
  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "name",
      help = "--name parameter must follow Spring Data nomenclature. Please, write a "
          + "valid value using autocomplete feature (TAB or CTRL + Space)")
  public List<String> returnOptions(ShellContext shellContext) {

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all defined parameters on autocompleted command
    Map<String, String> contextParameters = shellContext.getParameters();

    // Getting current name value
    String name = contextParameters.get("name");

    try {

      // Use PartTree class to obtain all possible values
      PartTree part = new PartTree(name, getEntityDetails(contextParameters.get("entity")), this);

      // Check if part has value
      if (part != null) {
        allPossibleValues = part.getOptions();
      }

      return allPossibleValues;

    } catch (Exception e) {
      LOGGER.warning(e.getLocalizedMessage());
      return allPossibleValues;
    }

  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "entity", help = "--entity option should be an entity.")
  public List<String> getClassPossibleResults(ShellContext shellContext) {

    // ROO-3763: Clear current cache during --entity autocompletion.
    // With that, Spring Roo will maintain cache during --name autocompletion
    // but if --entity is autocomplete, cache should be refreshed to obtain 
    // last changes on entities
    entitiesDetails = new HashMap<JavaType, MemberDetails>();

    // Get current value of entity
    String currentText = shellContext.getParameters().get("entity");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(
      command = "finder add",
      includeSpaceOnFinish = false,
      param = "defaultReturnType",
      help = "--defaultReturnType option should be a Projection class related with the specified entity.")
  public List<String> getReturnTypePossibleResults(ShellContext shellContext) {

    // Get current value of defaultReturnType
    String currentText = shellContext.getParameters().get("defaultReturnType");

    List<String> allPossibleValues = new ArrayList<String>();

    // Get current value of 'entity'
    JavaType entity = getTypeFromEntityParam(shellContext);

    Set<ClassOrInterfaceTypeDetails> entityProjections =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_ENTITY_PROJECTION);
    for (ClassOrInterfaceTypeDetails projection : entityProjections) {
      if (entity != null
          && projection.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION).getAttribute("entity")
              .getValue().equals(entity)) {
        String name = replaceTopLevelPackageString(projection, currentText);
        if (!allPossibleValues.contains(name)) {
          allPossibleValues.add(name);
        }
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "finder add", includeSpaceOnFinish = false,
      param = "formBean", help = "--formBean option should be a DTO.")
  public List<String> getFormBeanPossibleResults(ShellContext shellContext) {

    // Get current value of entity
    String currentText = shellContext.getParameters().get("formBean");

    List<String> allPossibleValues = new ArrayList<String>();

    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails dto : dtosInProject) {
      String name = replaceTopLevelPackageString(dto, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  /**
   * Indicates if 'formBean' option should be visible. It checks the existence of 
   * DTO's in project.
   * 
   * @param shellContext
   * @return true if project contains at least one DTO, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "finder add",
      help = "--formBean parameter is not visible if --entity parameter hasn't been specified "
          + "before or if there aren't exist any DTO in generated project", params = {"formBean"})
  public boolean isFormBeanVisible(ShellContext shellContext) {
    Set<ClassOrInterfaceTypeDetails> dtosInProject =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    if (dtosInProject.isEmpty()) {
      return false;
    }
    return true;
  }

  @CliCommand(value = "finder add",
      help = "Install a finder in the given target (must be an entity)")
  public void installFinders(
      @CliOption(key = "entity", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = PROJECT, help = "The entity for which the finders are generated") final JavaType typeName,
      @CliOption(key = "name", mandatory = true,
          help = "The finder string defined as a Spring Data query") final JavaSymbolName finderName,
      @CliOption(key = "formBean", mandatory = false,
          help = "The finder's search parameter. Should be a DTO.") final JavaType formBean,
      @CliOption(key = "defaultReturnType", mandatory = false, optionContext = PROJECT,
          help = "The finder's results return type. Should be a Projection class related "
              + "with the specified entity in --entity parameter.") JavaType defaultReturnType) {

    // Check if specified finderName follows Spring Data nomenclature
    PartTree partTree = new PartTree(finderName.getSymbolName(), getEntityDetails(typeName), this);

    // If generated partTree is not valid, shows an exception
    Validate
        .isTrue(
            partTree.isValid(),
            "--name parameter must follow Spring Data nomenclature. Please, write a valid value using autocomplete feature (TAB or CTRL + Space)");

    // Validate if formBean DTO has the necessary parameters
    if (formBean != null) {
      List<FinderParameter> parameters = partTree.getParameters();
      ClassOrInterfaceTypeDetails formBeanDetails =
          getTypeLocationService().getTypeDetails(formBean);
      List<FieldMetadata> formBeanFields =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), formBeanDetails)
              .getFields();

      // Check for DTO's fields against finder params
      for (FinderParameter param : parameters) {
        boolean fieldFoundInDto = false;
        for (FieldMetadata field : formBeanFields) {
          if (param.getName().equals(field.getFieldName())
              && param.getType().equals(field.getFieldType())) {
            fieldFoundInDto = true;
            break;
          }
        }
        Validate.isTrue(fieldFoundInDto, "Field names and types of DTO %s used in "
            + "'formBean' param must have the same name and type of Entity %s fields "
            + "or its relations fields.", formBean.getSimpleTypeName(),
            typeName.getSimpleTypeName());
      }
    }

    finderOperations.installFinder(typeName, finderName, formBean, defaultReturnType);

  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   * 
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(getProjectOperations().getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          getProjectOperations().getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          getProjectOperations().getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value  
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

  /**
   * Tries to obtain JavaType indicated in command or which has the focus 
   * in the Shell
   * 
   * @param shellContext the Roo Shell context
   * @return JavaType or null if no class has the focus or no class is 
   * specified in the command
   */
  private JavaType getTypeFromEntityParam(ShellContext shellContext) {
    // Try to get 'entity' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      type = getJavaTypeConverter().convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    return type;
  }

  /**
   * Method that obtains entity details from entity name
   * 
   * @param entityName
   * @return MemberDetails
   */
  public MemberDetails getEntityDetails(String entityName) {

    String moduleName = getProjectOperations().getFocusedModuleName();

    if (entityName.contains(":")) {
      moduleName = StringUtils.split(entityName, ":")[0];
      entityName = StringUtils.split(entityName, ":")[1];
    }

    // Getting JavaType for entityName
    // Check first if contains base package (~)
    if (entityName.contains("~")) {
      entityName =
          entityName.replace("~", getProjectOperations().getTopLevelPackage(moduleName)
              .getFullyQualifiedPackageName());
    }
    JavaType entityType = new JavaType(entityName, moduleName);

    return getEntityDetails(entityType);

  }

  @Override
  public MemberDetails getEntityDetails(JavaType entity) {

    Validate.notNull(entity, "ERROR: Entity should be provided");

    if (entitiesDetails.containsKey(entity)) {
      return entitiesDetails.get(entity);
    }

    // We know the file exists, as there's already entity metadata for it
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entity);

    if (cid == null) {
      return null;
    }

    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) == null) {
      LOGGER.warning("Unable to find the entity annotation on '" + entity + "'");
      return null;
    }

    entitiesDetails.put(entity,
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), cid));
    return entitiesDetails.get(entity);
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
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
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
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
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
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
          memberDetailsScanner = (MemberDetailsScanner) this.context.getService(ref);
          return memberDetailsScanner;
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

  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {

      // Get all Services implement JavaTypeConverter interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, PROJECT)) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FieldCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }

}
