package org.springframework.roo.addon.dto.addon;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;

/**
 * Commands for the DTO add-on to be used by the ROO shell.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoCommands implements CommandMarker {

  @Reference
  private DtoOperations dtoOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private ProjectOperations projectOperations;

  @CliAvailabilityIndicator({"dto"})
  public boolean isDtoCreationAvailable() {
    return dtoOperations.isDtoCreationPossible();
  }

  /**
   * Makes 'all' option visible only if 'entity' or 'class' options are not already specified.
   * 
   * @param shellContext
   * @return true if 'entity' or 'class' are not specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "dto", params = {"all"},
      help = "Option 'all' can't be used with 'entity' or 'class' options in the same command.")
  public boolean isAllVisible(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class") || params.containsKey("entity")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'class' or 'entity' options visible only if 'all' option is not already specified.
   * 
   * @param shellContext
   * @return true if 'all' is not specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "dto", params = {"class", "entity"},
      help = "Options 'class' and 'entity' can't be used with 'all' option in the same command.")
  public boolean areClassAndEntityVisible(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("all")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'fields' option visible only if 'entity' option is specified and 'excludeFields' not specified. 
   * 
   * @param shellContext
   * @return true if 'entity' is specified and 'excludeFields' is not specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(
      command = "dto",
      params = {"fields"},
      help = "Option 'fields' only can be used with 'entity' option and if 'excludeFields' is not specified.")
  public boolean isFieldsVisible(ShellContext shellContext) {

    // Check already specified parameters
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity") && !params.containsKey("excludeFields")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'excludeFields' option visible only if 'entity' option is specified and 'fields' not specified. 
   * 
   * @param shellContext
   * @return true if 'entity' is specified and 'fields' is not specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(
      command = "dto",
      params = {"excludeFields"},
      help = "Option 'excludeFields' only can be used with 'entity' option and if 'fields' is not specified.")
  public boolean isExcludeFieldsVisible(ShellContext shellContext) {

    // Check already specified parameters
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity") && !params.containsKey("fields")) {
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
  @CliOptionAutocompleteIndicator(command = "dto", param = "entity",
      help = "Option entity must have an existing entity value. Please, assign it a right value.",
      validate = false)
  public List<String> returnEntityValues(ShellContext shellContext) {

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity full qualilfied names
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            JpaJavaType.ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  @CliCommand(value = "dto", help = "Creates a new DTO class in SRC_MAIN_JAVA")
  public void newDtoClass(
      @CliOption(
          key = "class",
          mandatory = false,
          optionContext = UPDATE_PROJECT,
          help = "Name of the DTO class to create, including package and module (if multimodule project)") final JavaType name,
      @CliOption(key = "entity", mandatory = false, optionContext = UPDATE_PROJECT,
          help = "Name of the entity which can be used to create DTO from") final JavaType entity,
      @CliOption(key = "all", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether should create one DTO per each entity in the project") final boolean all,
      @CliOption(key = "fields", mandatory = false,
          help = "Comma separated list of entity fields to be included into the DTO") final String fields,
      @CliOption(key = "excludeFields", mandatory = false,
          help = "Comma separated list of entity fields to be excluded into the DTO") final String excludeFields,
      @CliOption(key = "immutable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Whether the DTO should be inmutable") final boolean immutable,
      @CliOption(key = "utilityMethods", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the DTO should implement 'toString', 'hashCode' and 'equals' methods") final boolean utilityMethods,
      @CliOption(key = "serializable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Whether the DTO should implement Serializable") final boolean serializable,
      ShellContext shellContext) {

    // Check if exists other DTO with the same name
    if (name != null) {
      Set<ClassOrInterfaceTypeDetails> currentDtos =
          typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
      for (ClassOrInterfaceTypeDetails dto : currentDtos) {
        // If exists and developer doesn't use --force global parameter,
        // we can't create a duplicate dto
        if (name.equals(dto.getName()) && !shellContext.isForce()) {
          throw new IllegalArgumentException(
              String
                  .format(
                      "DTO '%s' already exists and cannot be created. Try to use a "
                          + "different DTO name on --class parameter or use --force parameter to overwrite it.",
                      name));
        }
      }
    }

    if (entity != null) {
      Set<ClassOrInterfaceTypeDetails> currentEntities =
          typeLocationService
              .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
      boolean entityFound = false;
      for (ClassOrInterfaceTypeDetails cid : currentEntities) {
        // If exists and developer doesn't use --force global parameter,
        // we can't create a duplicate dto
        if (entity.equals(cid.getName())) {
          entityFound = true;
          break;
        }
      }
      if (!entityFound) {
        throw new IllegalArgumentException(String.format(
            "Entity '%s' doesn't exists or is not a @RooJpaEntity", entity));
      }
    }

    if (all) {
      dtoOperations.createDtoFromAll(immutable, utilityMethods, serializable);
    } else if (entity != null) {
      dtoOperations.createDtoFromEntity(name, entity, fields, excludeFields, immutable,
          utilityMethods, serializable);
    } else {
      boolean fromEntity = false;
      dtoOperations.createDto(name, immutable, utilityMethods, serializable, fromEntity);
    }
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   * 
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";
    if (StringUtils.isNotBlank(cid.getType().getModule())) {
      javaTypeString = cid.getType().getModule().concat(":");
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Replace String if necessary
    if (StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {
      javaTypeString =
          javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
              topLevelPackageString, "~"));
    }

    return javaTypeString;
  }

}
