package org.springframework.roo.addon.dto.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST_PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.field.addon.FieldCommands;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Commands for the DTO add-on to be used by the ROO shell.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FieldCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private DtoOperations dtoOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private PathResolver pathResolver;
  @Reference
  private FileManager fileManager;
  @Reference
  private LastUsed lastUsed;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @CliAvailabilityIndicator({"dto"})
  public boolean isDtoCreationAvailable() {
    return dtoOperations.isDtoCreationPossible();
  }

  @CliAvailabilityIndicator({"entity projection"})
  public boolean isEntityProjectionAvailable() {
    return dtoOperations.isEntityProjectionPossible();
  }

  @CliCommand(value = "dto", help = "Creates a new DTO class in SRC_MAIN_JAVA")
  public void newDtoClass(
      @CliOption(
          key = "class",
          mandatory = true,
          optionContext = UPDATELAST_PROJECT,
          help = "Name of the DTO class to create, including package and module (if multimodule project)") final JavaType name,
      @CliOption(key = "immutable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Whether the DTO should be inmutable") final boolean immutable,
      @CliOption(key = "utilityMethods", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the DTO should implement 'toString', 'hashCode' and 'equals' methods") final boolean utilityMethods,
      @CliOption(key = "serializable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Whether the DTO should implement Serializable") final boolean serializable,
      ShellContext shellContext) {

    // Check if DTO already exists
    final String entityFilePathIdentifier =
        pathResolver.getCanonicalPath(name.getModule(), Path.SRC_MAIN_JAVA, name);
    if (fileManager.exists(entityFilePathIdentifier) && shellContext.isForce()) {
      fileManager.delete(entityFilePathIdentifier);
    } else if (fileManager.exists(entityFilePathIdentifier) && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "DTO '%s' already exists and cannot be created. Try to use a "
                      + "different DTO name on --class parameter or use --force parameter to overwrite it.",
                  name));
    }

    dtoOperations.createDto(name, immutable, utilityMethods, serializable);

  }

  /**
   * Makes 'entity' option mandatory if 'class' has been defined.
   * 
   * @param shellContext
   * @return true if 'class' has been defined, false otherwise.
   */
  @CliOptionMandatoryIndicator(command = "entity projection", params = {"entity"})
  public boolean isEntityMandatoryForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'fields' option mandatory if 'entity' has been defined.
   * 
   * @param shellContext
   * @return true if 'entity' has been defined, false otherwise.
   */
  @CliOptionMandatoryIndicator(command = "entity projection", params = {"fields"})
  public boolean isFieldsMandatoryForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'all' option visible only if 'class' option is not specified.
   * 
   * @param shellContext
   * @return false if 'class' is specified, true otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"all"},
      help = "Option 'all' can't be used with 'class' option in the same command.")
  public boolean isAllVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'class' option visible only if 'all' option is not specified.
   * 
   * @param shellContext
   * @return false if 'all' is specified, true otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"class"},
      help = "Option 'class' can't be used with 'all' option in the same command.")
  public boolean isClassVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("all")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'entity' option visible only if 'class' option is already specified.
   * 
   * @param shellContext
   * @return true if 'class' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"entity"},
      help = "Option 'entity' can't be used until 'class' option is specified.")
  public boolean isEntityVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'suffix' option visible only if 'all' option is specified.
   * 
   * @param shellContext
   * @return true if 'all' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"suffix"},
      help = "Option 'suffix' can't be used if option 'all' isn't already specified.")
  public boolean isSuffixVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("all")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'fields' option visible only if 'entity' option is specified.
   * 
   * @param shellContext
   * @return true if 'entity' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"fields"},
      help = "Option 'fields' can't be used if option 'entity' isn't already specified.")
  public boolean isFieldsVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity")) {
      return true;
    }

    return false;
  }

  /**
   * Find entities in project and returns a list with their fully qualified names.
   * 
   * @param shellContext
   * @return List<String> with available entity full qualified names.
   */
  @CliOptionAutocompleteIndicator(command = "entity projection", param = "entity",
      help = "Option 'entity' must have an existing entity value. Please, assign it a right value.")
  public List<String> returnEntityValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity fully qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            JpaJavaType.ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliCommand(value = "entity projection",
      help = "Creates new projection classes from entities in SRC_MAIN_JAVA")
  public void newProjectionClass(
      @CliOption(key = "entity", mandatory = true,
          help = "Name of the entity which can be used to create the Projection from") final JavaType entity,
      @CliOption(
          key = "class",
          mandatory = false,
          optionContext = UPDATELAST_PROJECT,
          help = "Name of the Projection class to create, including package and module (if multimodule project)") final JavaType name,
      @CliOption(key = "all", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether should create one Projection per each entity in the project") final boolean all,
      @CliOption(key = "fields", mandatory = true,
          help = "Comma separated list of entity fields to be included into the Projection") final String fields,
      @CliOption(
          key = "suffix",
          mandatory = false,
          unspecifiedDefaultValue = "Projection",
          help = "Suffix added to each Projection class name, builded from each associated entity name.") final String suffix,
      ShellContext shellContext) {

    // Check if Projection already exists
    if (name != null) {
      final String entityFilePathIdentifier =
          pathResolver.getCanonicalPath(name.getModule(), Path.SRC_MAIN_JAVA, name);
      if (fileManager.exists(entityFilePathIdentifier) && shellContext.isForce()) {
        fileManager.delete(entityFilePathIdentifier);
      } else if (fileManager.exists(entityFilePathIdentifier) && !shellContext.isForce()) {
        throw new IllegalArgumentException(
            String
                .format(
                    "Projection '%s' already exists and cannot be created. Try to use a "
                        + "different Projection name on --class parameter or use --force parameter to overwrite it.",
                    name));
      }
    }

    // Check if --fields has a value
    if (entity != null && StringUtils.isBlank(fields)) {
      throw new IllegalArgumentException(
          String
              .format(
                  "Projection '%s' should have at least one field from its associated entity. Please, add a right value for 'fields' option.",
                  name));
    }

    if (entity != null) {
      dtoOperations.createProjection(entity, name, fields, null);
    } else if (all == true) {
      dtoOperations.createAllProjections(suffix, shellContext);
    }
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
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
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
   * Gets a list of fields from an entity.
   * 
   * @param entity the JavaType from which to obtain the field list.
   * @return a List<FieldMetadata> with info of the entity fields.
   */
  private List<FieldMetadata> getEntityFieldList(JavaType entity) {
    List<FieldMetadata> fieldList = new ArrayList<FieldMetadata>();
    ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(entity);
    Validate.notNull(typeDetails, String.format(
        "Cannot find details for class %s. Please be sure that the class exists.",
        entity.getFullyQualifiedTypeName()));
    MemberDetails entityMemberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), typeDetails);
    Validate.notNull(
        entityMemberDetails.getAnnotation(JpaJavaType.ENTITY),
        String.format("%s must be an entity to obtain it's fields info.",
            entity.getFullyQualifiedTypeName()));

    // Get fields and check for other fields from relations
    List<FieldMetadata> entityFields = entityMemberDetails.getFields();
    for (FieldMetadata field : entityFields) {
      fieldList.add(field);
    }

    return fieldList;
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
    // Try to get 'class' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      type = getJavaTypeConverter().convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    // Inform that entity param couldn't be retrieved
    Validate.notNull(type,
        "Couldn't get the entity for 'entity projection' command. Please, be sure that "
            + "param '--entity' is specified with a right value.");

    return type;
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
