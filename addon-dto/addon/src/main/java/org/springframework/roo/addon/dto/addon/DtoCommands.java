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

  /**
   * Attempts to obtain entity specified in 'entity' option and returns an auto-complete 
   * list with the entity fields, separated by comma's.  
   * 
   * @param shellContext
   * @return a List<String> with the possible values.
   */
  @CliOptionAutocompleteIndicator(
      command = "entity projection",
      param = "fields",
      help = "Option fields must have a comma-separated list of existing fields. Please, assign it a right value.",
      includeSpaceOnFinish = false)
  public List<String> returnFieldValues(ShellContext shellContext) {
    List<String> fieldValuesToReturn = new ArrayList<String>();
    JavaType lastFieldType = null;
    boolean partialField = false;

    // Get entity JavaType
    JavaType entity = getTypeFromEntityParam(shellContext);

    // Get current fields in --field value
    String currentFieldValue = shellContext.getParameters().get("fields");
    String[] fields = StringUtils.split(currentFieldValue, ",");

    // Check for bad written separators and return no options
    if (currentFieldValue.contains(",.") || currentFieldValue.contains(".,")) {
      return fieldValuesToReturn;
    }

    // VALIDATION OF CURRENT SPECIFIED VALUES UNTIL LAST MEMBER
    String completedValue = "";
    JavaType entityToSearch = entity;
    for (int i = 0; i < fields.length; i++) {
      entityToSearch = entity;

      // Split field by ".", in case it was a relation field
      String[] splittedByPeriod = StringUtils.split(fields[i], ".");

      // Build auto-complete values
      for (int t = 0; t < splittedByPeriod.length; t++) {

        // Find the field in entity fields
        List<FieldMetadata> entityFields = getEntityFieldList(entityToSearch);
        boolean fieldFound = false;
        for (FieldMetadata entityField : entityFields) {
          if (splittedByPeriod[t].equals(entityField.getFieldName().getSymbolName())) {
            fieldFound = true;

            // Add auto-complete value
            if (completedValue.equals("")) {
              completedValue = entityField.getFieldName().getSymbolName();
            } else {
              completedValue = completedValue.concat(entityField.getFieldName().getSymbolName());
            }

            // Record last field JavaType for auto-completing last value
            lastFieldType = entityField.getFieldType();

            if (t != splittedByPeriod.length - 1) {

              // Field goes through other relation, so JavaType it's an entity
              entityToSearch = entityField.getFieldType();
              completedValue = completedValue.concat(".");
            } else if (i != fields.length - 1) {
              completedValue = completedValue.concat(",");
            }
            break;
          }
        }
        if (i == fields.length - 1 && fieldFound == false && splittedByPeriod.length == 1) {
          partialField = true;
          completedValue = StringUtils.removeEnd(completedValue, ",");
        }
      }
    }

    // ADDITION OF NEW VALUES

    // Build auto-complete values for last member
    String autocompleteValue = "";
    String lastMember = "";
    if (!completedValue.equals("")) {
      lastMember = fields[fields.length - 1];
    }
    String[] splittedByPeriod = StringUtils.split(lastMember, ".");

    // Check if last member was a relation field
    if (lastFieldType != null
        && typeLocationService.getTypeDetails(lastFieldType) != null
        && typeLocationService.getTypeDetails(lastFieldType).getAnnotation(
            RooJavaType.ROO_JPA_ENTITY) != null && partialField == false
        && !currentFieldValue.endsWith(",")) {

      // Field is a relation field
      List<FieldMetadata> relationEntityFields = getEntityFieldList(lastFieldType);

      // Build last relation field completed
      String lastRelationField = "";
      if (splittedByPeriod.length > 0) {
        lastRelationField = splittedByPeriod[splittedByPeriod.length - 1];
      }
      if (currentFieldValue.endsWith(".")) {

        // currentFieldValue finished by ".", so autocomplete with next relation fields
        for (FieldMetadata field : relationEntityFields) {
          autocompleteValue =
              completedValue.concat(".").concat(field.getFieldName().getSymbolName());

          // Check if value already exists
          String additionalValueToAdd = StringUtils.substringAfterLast(autocompleteValue, ",");
          if (!fieldValuesToReturn.contains(autocompleteValue)
              && (!currentFieldValue.contains(additionalValueToAdd) || additionalValueToAdd
                  .equals(""))) {
            fieldValuesToReturn.add(autocompleteValue);
          }
        }
      } else {

        // Add completions for the relation field itself, with main entity fields
        List<FieldMetadata> mainEntityFields = getEntityFieldList(entityToSearch);
        for (FieldMetadata field : mainEntityFields) {
          if (field.getFieldName().getSymbolName().equals(lastRelationField)) {

            // Field name was already completed, so autocomplete with "." if an entity
            if (typeLocationService.getTypeDetails(field.getFieldType()) != null
                && typeLocationService.getTypeDetails(field.getFieldType()).getAnnotation(
                    RooJavaType.ROO_JPA_ENTITY) != null && !currentFieldValue.endsWith(".")) {
              fieldValuesToReturn.add(completedValue.concat("."));
            }

            // Also, auto-complete with main entity remaining fields
            for (FieldMetadata mainEntityField : mainEntityFields) {
              boolean alreadySpecified = false;
              for (int i = 0; i < fields.length; i++) {
                if (mainEntityField.getFieldName().getSymbolName().equals(fields[i])) {
                  alreadySpecified = true;
                }
              }
              if (!alreadySpecified) {
                autocompleteValue =
                    completedValue.concat(",").concat(
                        mainEntityField.getFieldName().getSymbolName());
                fieldValuesToReturn.add(autocompleteValue);
              }
            }
            break;
          }
          if (!field.getFieldName().getSymbolName().equals(lastRelationField)
              && field.getFieldName().getSymbolName().startsWith(lastRelationField)) {

            // Field  name isn't completed, so auto-complete it
            autocompleteValue =
                StringUtils.substringBeforeLast(currentFieldValue, ".").concat(".")
                    .concat(field.getFieldName().getSymbolName());
            fieldValuesToReturn.add(autocompleteValue);
          }
        }
      }
    } else {

      // Not completing a relation field. Add entity fields as auto-complete values
      List<FieldMetadata> mainEntityFields = getEntityFieldList(entity);
      for (FieldMetadata mainEntityField : mainEntityFields) {

        // Field  name isn't completed, so auto-complete it
        if (completedValue.equals("")) {

          // Is first field to complete
          fieldValuesToReturn.add(mainEntityField.getFieldName().getSymbolName());
        } else {
          if (mainEntityField.getFieldName().getSymbolName().equals(lastMember)
              || mainEntityField.getFieldName().getSymbolName().startsWith(lastMember)) {

            // Check if field is specified and add it if not
            boolean alreadySpecified = false;
            boolean relationField = false;
            for (int i = 0; i < fields.length; i++) {
              if (mainEntityField.getFieldName().getSymbolName().equals(fields[i])) {
                alreadySpecified = true;
              }
              if (typeLocationService.getTypeDetails(mainEntityField.getFieldType()) != null
                  && typeLocationService.getTypeDetails(mainEntityField.getFieldType())
                      .getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
                relationField = true;
              }
            }
            if (!alreadySpecified && partialField) {

              // Add completion
              autocompleteValue =
                  completedValue.concat(",").concat(mainEntityField.getFieldName().getSymbolName());
              fieldValuesToReturn.add(autocompleteValue);
            }

            // Special case for relation fields
            if (relationField) {
              autocompleteValue =
                  completedValue.concat(",").concat(
                      mainEntityField.getFieldName().getSymbolName().concat("."));
              fieldValuesToReturn.add(autocompleteValue);
            }
          } else {

            // Add remaining entity fields
            if (typeLocationService.getTypeDetails(mainEntityField.getFieldType()) != null
                && typeLocationService.getTypeDetails(mainEntityField.getFieldType())
                    .getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

              // Relation field, check if it has been added already
              boolean relationFieldAdded = false;
              for (int i = 0; i < fields.length; i++) {
                if (fields[i].equals(mainEntityField.getFieldName().getSymbolName())) {
                  relationFieldAdded = true;
                }
              }
              if (relationFieldAdded) {

                // Relation field should appear only to add its relations (with ".")
                autocompleteValue =
                    StringUtils.substringBeforeLast(currentFieldValue, ",").concat(",")
                        .concat(mainEntityField.getFieldName().getSymbolName().concat("."));
                if (!fieldValuesToReturn.contains(autocompleteValue)) {
                  fieldValuesToReturn.add(autocompleteValue);
                }
              } else {

                // Relation field should appear alone, as main entity field
                autocompleteValue =
                    StringUtils.substringBeforeLast(currentFieldValue, ",").concat(",")
                        .concat(mainEntityField.getFieldName().getSymbolName());
                fieldValuesToReturn.add(autocompleteValue);
              }
            } else {

              // Not relation field
              autocompleteValue =
                  completedValue.concat(",").concat(mainEntityField.getFieldName().getSymbolName());
              fieldValuesToReturn.add(autocompleteValue);
            }
          }
        }
      }
    }

    return fieldValuesToReturn;
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
