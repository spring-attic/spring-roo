package org.springframework.roo.addon.field.addon;

import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.shell.OptionContexts.ENUMERATION;
import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Additional shell commands for the purpose of creating fields.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class FieldCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FieldCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private MemberDetailsScanner memberDetailsScanner;
  @Reference
  private MetadataService metadataService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private StaticFieldConverter staticFieldConverter;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private ProjectSettingsService projectSettings;
  @Reference
  private LastUsed lastUsed;
  private Converter<JavaType> javaTypeConverter;

  // FieldCreatorProvider implementations
  private List<FieldCreatorProvider> fieldCreatorProviders = new ArrayList<FieldCreatorProvider>();

  private final Set<String> legalNumericPrimitives = new HashSet<String>();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    legalNumericPrimitives.add(Short.class.getName());
    legalNumericPrimitives.add(Byte.class.getName());
    legalNumericPrimitives.add(Integer.class.getName());
    legalNumericPrimitives.add(Long.class.getName());
    legalNumericPrimitives.add(Float.class.getName());
    legalNumericPrimitives.add(Double.class.getName());
    staticFieldConverter.add(Cardinality.class);
    staticFieldConverter.add(Fetch.class);
    staticFieldConverter.add(EnumType.class);
    staticFieldConverter.add(DateTime.class);
  }

  protected void deactivate(final ComponentContext context) {
    staticFieldConverter.remove(Cardinality.class);
    staticFieldConverter.remove(Fetch.class);
    staticFieldConverter.remove(EnumType.class);
    staticFieldConverter.remove(DateTime.class);
    legalNumericPrimitives.add(Short.class.getName());
    legalNumericPrimitives.add(Byte.class.getName());
    legalNumericPrimitives.add(Integer.class.getName());
    legalNumericPrimitives.add(Long.class.getName());
    legalNumericPrimitives.add(Float.class.getName());
    legalNumericPrimitives.add(Double.class.getName());
    this.context = null;
  }

  @CliOptionMandatoryIndicator(command = "field boolean", params = {"column"})
  public boolean isColumnMandatoryForFieldBoolean(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldBoolean(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field boolean", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldBoolean(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldBoolean(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field boolean", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldBoolean(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldBoolean(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field boolean", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldBoolean(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field boolean", params = {"class"})
  public boolean isClassMandatoryForFieldBoolean() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field boolean",
      help = "Adds a private boolean field to an existing Java source file")
  public void addFieldBoolean(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available and mandatory for"
              + " this command when the focus is not set to one class. When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(
          key = "assertFalse",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the value of this field must be false. Adds `javax.validation.constraints.AssertFalse` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean assertFalse,
      @CliOption(
          key = "assertTrue",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the value of this field must be true. Adds `javax.validation.constraints.AssertTrue` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean assertTrue,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes."
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`.") final String column,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` annotation with the given content, typically used for expression-driven dependency injection.") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use the primitive type."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean primitive,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` annotation. This marks the field as not persistent."
              + "This option is only available for JPA entities and embeddable classes."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    getFieldCreatorProvider(typeName).createBooleanField(javaTypeDetails, primitive, fieldName,
        notNull, nullRequired, assertFalse, assertTrue, column, comment, value,
        permitReservedWords, transientModifier);
  }

  @CliOptionMandatoryIndicator(command = "field date", params = {"column"})
  public boolean isColumnMandatoryForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"persistenceType"},
      help = "Option 'persistenceType' is not available for this type of class")
  public boolean isPersistenceTypeVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isPersistenceTypeVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldDate(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field date", params = {"class"})
  public boolean isClassMandatoryForFieldDate() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field date",
      help = "Adds a private date field to an existing Java source file")
  public void addFieldDateJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(
          key = "type",
          mandatory = true,
          optionContext = "java-date",
          help = "The Java date type of the field. Its value can be `java.util.Date` or `java.util.Calendar`.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module project, "
              + "simply specify the name of the class in which the field will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyClass` "
              + "(where `~` is the base package). When working with multiple modules, you should specify "
              + "the name of the class and the module where it is. Ex.: `--class model:~.domain.MyClass`. "
              + "If the module is not specified, it is assumed that the class is in the module which has "
              + "the focus."
              + "This option is mandatory for this command when the focus is not set to one class."
              + "Default if option not present: the class focused by Roo shell.") final JavaType typeName,
      @CliOption(
          key = "persistenceType",
          mandatory = false,
          help = "The type of persistent storage to be used. It adds a `javax.persistence.TemporalType` to a `javax.persistence.Temporal` annotation into the field"
              + "This option is only available for JPA entities and embeddable classes."
              + "Default if option not present: `TemporalType.TIMESTAMP`") final DateFieldPersistenceType persistenceType,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(
          key = "future",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be in the future. Adds `field.javax.validation.constraints.Future` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`") final boolean future,
      @CliOption(
          key = "past",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be in the past. Adds `field.javax.validation.constraints.Past` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`") final boolean past,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`."
              + "This option is only visible for JPA entities and embeddable classes.") final String column,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` annotation with the given content, typically used for expression-driven dependency injection. ") final String value,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` annotation. This marks the field as not persistent."
              + "This option is only available for JPA entities and embeddable classes."
              + "Default if option present:`true`. Default if option not present: `false`.") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(
          key = "dateFormat",
          mandatory = false,
          unspecifiedDefaultValue = "MEDIUM",
          specifiedDefaultValue = "MEDIUM",
          help = "Indicates the style of the time format (ignored if dateTimeFormatPattern is specified), adding `style` attribute to `org.springframework.format.annotation.DateTimeFormat` annotation into the field. Possible values are: MEDIUM (style='MS'), NONE (style='-S') and SHORT (style='SS')."
              + "Default: `MEDIUM`.") final DateTime dateFormat,
      @CliOption(
          key = "timeFormat",
          mandatory = false,
          unspecifiedDefaultValue = "NONE",
          specifiedDefaultValue = "NONE",
          help = "Indicates the style of the time format (ignored if dateTimeFormatPattern is specified), adding `style` attribute to `org.springframework.format.annotation.DateTimeFormat` annotation into the field. Possible values are: MEDIUM (style='MS'), NONE (style='-S') and SHORT (style='SS')."
              + "Default: `NONE`.") final DateTime timeFormat,
      @CliOption(
          key = "dateTimeFormatPattern",
          mandatory = false,
          help = "Indicates a 'custom' DateTime format pattern such as yyyy-MM-dd hh:mm:ss, adding `pattern` attribute to `org.springframework.format.annotation.DateTimeFormat` annotation into the field, with the provided value.") final String pattern,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    getFieldCreatorProvider(typeName).createDateField(javaTypeDetails, fieldType, fieldName,
        notNull, nullRequired, future, past, persistenceType, column, comment, dateFormat,
        timeFormat, pattern, value, permitReservedWords, transientModifier);
  }

  @CliOptionVisibilityIndicator(command = "field embedded", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldEmbedded(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field embedded", params = {"class"})
  public boolean isClassMandatoryForFieldEmbedded() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(command = "field embedded", param = "type",
      help = "--type option should be an entity.")
  public List<String> getFieldEmbeddedAllPossibleValues(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).getFieldEmbeddedAllPossibleValues(shellContext);
    }
    return new ArrayList<String>();
  }

  @CliCommand(
      value = "field embedded",
      help = "Adds a private @Embedded field to an existing Java source file. This command is only available "
          + "for entities annotated with `@RooJpaEntity`. Therefore, you should focus the desired entity "
          + "in the Roo Shell to make this command available.")
  public void addFieldEmbeddedJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of the an embeddable class, annotated with `@Embeddable`.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module project, "
              + "simply specify the name of the class in which the field will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyClass`"
              + " (where `~` is the base package). When working with multiple modules, you should "
              + "specify the name of the class and the module where it is. "
              + "Ex.: `--class model:~.domain.MyClass`. If the module is not specified, it is assumed "
              + "that the class is in the module which has the focus."
              + "This option is mandatory for this command when the focus is not set to one class."
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    // Check if the field type is a JPA @Embeddable class
    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
    Validate
        .notNull(cid,
            "The specified target '--type' does not exist or can not be found. Please create this type first.");
    Validate.notNull(cid.getAnnotation(EMBEDDABLE),
        "The field embedded command is only applicable to JPA @Embeddable field types.");

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createEmbeddedField(typeName, fieldType, fieldName,
        permitReservedWords);
  }

  @CliOptionMandatoryIndicator(command = "field enum", params = {"column"})
  public boolean isColumnMandatoryForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldEnum(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field enum", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldEnum(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field enum", params = {"enumType"},
      help = "Option 'enumType' is not available for this type of class")
  public boolean isEnumTypeVisibleForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isEnumTypeVisibleForFieldEnum(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field enum", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldEnum(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field enum", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldEnum(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field enum", params = {"class"})
  public boolean isClassMandatoryForFieldEnum() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field enum",
      help = "Adds a private enum field to an existing Java source file")
  public void addFieldEnum(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = ENUMERATION,
          help = "The enum type of this field (mandatory)") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's "
              + "'true'. See 'settings list' command)") final String column,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "enumType", mandatory = false, help = "The fetch semantics at a JPA level") final EnumType enumType,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient. This option is only available for JPA entities "
              + "and embeddable classes") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createEnumField(cid, fieldType, fieldName, column, notNull,
        nullRequired, enumType, comment, permitReservedWords, transientModifier);
  }

  @CliOptionMandatoryIndicator(command = "field number", params = {"column"})
  public boolean isColumnMandatoryForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldNumber(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field number", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldNumber(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field number", params = {"unique"},
      help = "Option 'unique' is not available for this type of class")
  public boolean isUniqueVisibleForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isUniqueVisibleForFieldNumber(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field number", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldNumber(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field number", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldNumber(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field number", params = {"class"})
  public boolean isClassMandatoryForFieldNumber() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field number",
      help = "Adds a private numeric field to an existing Java source file")
  public void addFieldNumber(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = "java-number",
          help = "The Java type of the field (mandatory)") JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "decimalMin", mandatory = false,
          help = "The BigDecimal string-based representation of the minimum value") final String decimalMin,
      @CliOption(key = "decimalMax", mandatory = false,
          help = "The BigDecimal string based representation of the maximum value") final String decimalMax,
      @CliOption(key = "digitsInteger", mandatory = false,
          help = "Maximum number of integral digits accepted for this number") final Integer digitsInteger,
      @CliOption(key = "digitsFraction", mandatory = false,
          help = "Maximum number of fractional digits accepted for this number") final Integer digitsFraction,
      @CliOption(key = "min", mandatory = false, help = "The minimum value") final Long min,
      @CliOption(key = "max", mandatory = false, help = "The maximum value") final Long max,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's "
              + "'true'. See 'settings list' command)") final String column,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient. This option is only available for JPA entities"
              + " and embeddable classes") final boolean transientModifier,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use a primitive type if possible") final boolean primitive,
      @CliOption(
          key = "unique",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint. This option is only available for JPA entities"
              + " and embeddable classes") final boolean unique,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    getFieldCreatorProvider(typeName).createNumericField(javaTypeDetails, fieldType, primitive,
        legalNumericPrimitives, fieldName, notNull, nullRequired, decimalMin, decimalMax,
        digitsInteger, digitsFraction, min, max, column, comment, unique, value,
        permitReservedWords, transientModifier);
  }

  @CliOptionAutocompleteIndicator(command = "field reference", param = "type",
      help = "--type option should be an entity.")
  public List<String> getReferenceTypePossibleValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("type");

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> entitiesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entitiesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionMandatoryIndicator(command = "field reference", params = {"joinColumnName"})
  public boolean isColumnMandatoryForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldReference(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"joinColumnName"},
      help = "Option 'joinColumnName' is not available for this type of class")
  public boolean isJoinColumnNameVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinColumnNameVisibleForFieldReference(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"referencedColumnName"},
      help = "Option 'referencedColumnName' is not available for this type of class")
  public boolean isReferencedColumnNameVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isReferencedColumnNameVisibleForFieldReference(
          shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"fetch"},
      help = "Option 'fetch' is not available for this type of class")
  public boolean isFetchVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isFetchVisibleForFieldReference(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"cascadeType"},
      help = "Option 'cascadeType' is not available for this type of class")
  public boolean isCascadeTypeVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isCascadeTypeVisibleForFieldReference(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldReference(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field reference", params = {"class"})
  public boolean isClassMandatoryForFieldReference() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(
      value = "field reference",
      help = "Adds a private reference field to an existing Java source file (eg the 'many' side of a many-to-one)")
  public void addFieldReferenceJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of the entity to reference (mandatory)") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(
          key = "joinColumnName",
          mandatory = true,
          help = "The JPA @JoinColumn name. This option is only available for JPA "
              + "entities and embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting "
              + "exists and it’s 'true', See 'settings list' command)") final String joinColumnName,
      @CliOption(key = "referencedColumnName", mandatory = false,
          help = "The JPA @JoinColumn referencedColumnName. This option is only available for JPA "
              + "entities and embeddable classes") final String referencedColumnName,
      @CliOption(key = "fetch", mandatory = false,
          help = "The fetch semantics at a JPA level. This option is only available for JPA "
              + "entities and embeddable classes") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship. This option is only available for JPA entities") final JavaSymbolName mappedBy,
      //      @CliOption(
      //          key = "cascadeType",
      //          mandatory = false,
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH and REMOVE.") final Cascade cascadeType,
      @CliOption(
          key = "aggregation",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Aggregation. Identify relationship as 'aggregation' (if is 'true', default) or a 'composition' (if is 'false')") final boolean aggregation,
      @CliOption(
          key = "orphanRemoval",
          mandatory = false,
          specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been removed from the relationship and to cascade the "
              + "remove operation to those entities") Boolean orphanRemoval,
      ShellContext shellContext) {

    // TODO support multiple cascade type
    getFieldCreatorProvider(typeName).createReferenceField(typeName, fieldType, fieldName,
        aggregation, mappedBy, null, notNull, joinColumnName, referencedColumnName, fetch, comment,
        permitReservedWords, orphanRemoval, shellContext.isForce());
  }

  @CliOptionMandatoryIndicator(command = "field set", params = {"joinColumns", "referencedColumns",
      "inverseJoinColumns", "inverseReferencedColumns"})
  public boolean areJoinTableParamsMandatoryForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areJoinTableParamsMandatoryForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field set", params = {"joinTable"})
  public boolean isJoinTableMandatoryForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinTableMandatoryForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"joinColumns",
      "referencedColumns", "inverseJoinColumns", "inverseReferencedColumns"},
      help = "Options --joinColumns, --referencedColumns, --inverseJoinColumns and "
          + "--inverseReferencedColumns must be used with a specific --joinTable option.")
  public boolean areJoinTableParamsVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areJoinTableParamsVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"mappedBy"},
      help = "Option 'mappedBy' is not available for this type of class")
  public boolean isMappedByVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isMappedByVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"cardinality"},
      help = "Option 'cardinality' is not available for this type of class")
  public boolean isCardinalityVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isCardinalityVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"fetch"},
      help = "Option 'fetch' is not available for this type of class")
  public boolean isFetchVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isFetchVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"joinTable"},
      help = "Option 'joinTable' is not available for this type of class")
  public boolean isJoinTableVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinTableVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldSet(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field set", params = {"class"})
  public boolean isClassMandatoryForFieldSet() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(command = "field set", param = "type",
      help = "--type option should be an entity.")
  public List<String> getFieldSetTypeAllPossibleValues(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).getFieldSetTypeAllPossibleValues(shellContext);
    }
    return new ArrayList<String>();
  }

  @CliCommand(
      value = "field set",
      help = "Adds a private Set field to an existing Java source file (eg the 'one' side of a many-to-one)")
  public void addFieldSetJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity which will be contained within the Set (mandatory)") final JavaType fieldType,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship. This option is only available for"
              + " JPA entities") final JavaSymbolName mappedBy,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "sizeMin", mandatory = false,
          help = "The minimum number of elements in the collection") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false,
          help = "The maximum number of elements in the collection") final Integer sizeMax,
      @CliOption(
          key = "cardinality",
          mandatory = false,
          unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level. This option is only available for JPA entities and embeddable classes") CardinalitySupported cardinality,
      @CliOption(key = "fetch", mandatory = false,
          help = "The fetch semantics at a JPA level. This option is only available for JPA "
              + "entities and embeddable classes") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "joinTable",
          mandatory = true,
          help = "Join table name. Most usually used in @ManyToMany relations. This option is only available for JPA entities "
              + "and embeddable classes (mandatory if 'cardinality' option is 'MANY_TO_MANY')") final String joinTable,
      @CliOption(
          key = "joinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity owning the relation. This option is only available for JPA entities and "
              + "embeddable classes when 'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' "
              + "configuration setting it's 'true', See 'settings list' command)") final String joinColumns,
      @CliOption(
          key = "referencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity owning the relation. This option is only available for JPA entities and embeddable classes when "
              + "'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting"
              + " it's 'true', See 'settings list' command)") final String referencedColumns,
      @CliOption(
          key = "inverseJoinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity that does not own the relation. This option is only available for JPA entities "
              + "and embeddable classes when 'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' "
              + "configuration setting it's 'true', See 'settings list' command)") final String inverseJoinColumns,
      @CliOption(
          key = "inverseReferencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity that does not own the relation. This option is only available for JPA entities and embeddable classes"
              + " when 'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting"
              + " it's 'true', See 'settings list' command)") final String inverseReferencedColumns,
      //      @CliOption(
      //          key = "cascadeType",
      //          mandatory = false,
      //          unspecifiedDefaultValue = "ALL",
      //          specifiedDefaultValue = "ALL",
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH and REMOVE.") final Cascade cascadeType,
      @CliOption(
          key = "aggregation",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Aggregation. Identify relationship as 'aggregation' (if is 'true', default) or a 'composition' (if is 'false')") final boolean aggregation,
      @CliOption(
          key = "orphanRemoval",
          mandatory = false,
          specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been removed from the relationship and to cascade the remove "
              + "operation to those entities") Boolean orphanRemoval,

      ShellContext shellContext) {

    // TODO support multiple cascade type

    getFieldCreatorProvider(typeName).createSetField(typeName, fieldType, fieldName,
        cardinality.getCardinality(), null, notNull, sizeMin, sizeMax, mappedBy, fetch, comment,
        joinTable, joinColumns, referencedColumns, inverseJoinColumns, inverseReferencedColumns,
        permitReservedWords, aggregation, orphanRemoval, shellContext.isForce());

  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"joinColumns",
      "referencedColumns", "inverseJoinColumns", "inverseReferencedColumns"},
      help = "Options --joinColumns, --referencedColumns, --inverseJoinColumns and "
          + "--inverseReferencedColumns must be used with a specific --joinTable option.")
  public boolean areJoinTableParamsVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areJoinTableParamsVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field list", params = {"joinTable"})
  public boolean isJoinTableMandatoryForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinTableMandatoryForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field list", params = {"joinColumns",
      "referencedColumns", "inverseJoinColumns", "inverseReferencedColumns"})
  public boolean areJoinTableParamsMandatoryForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areJoinTableParamsMandatoryForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"mappedBy"},
      help = "Option 'mappedBy' is not available for this type of class")
  public boolean isMappedByVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isMappedByVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"cardinality"},
      help = "Option 'cardinality' is not available for this type of class")
  public boolean isCardinalityVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isCardinalityVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"fetch"},
      help = "Option 'fetch' is not available for this type of class")
  public boolean isFetchVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isFetchVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"joinTable"},
      help = "Option 'joinTable' is not available for this type of class")
  public boolean isJoinTableVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinTableVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldList(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field list", params = {"class"})
  public boolean isClassMandatoryForFieldList() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(command = "field list", param = "type",
      help = "--type option should be an entity.")
  public List<String> getFieldListTypePossibleValues(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).getFieldListTypeAllPossibleValues(shellContext);
    }
    return new ArrayList<String>();
  }

  @CliCommand(
      value = "field list",
      help = "Adds a private List field to an existing Java source file (eg the 'one' side of a many-to-one)")
  public void addFieldListJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = "update,project",
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity which will be contained within the List (mandatory)") final JavaType fieldType,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship. This option is only available for JPA entities") final JavaSymbolName mappedBy,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "sizeMin", mandatory = false,
          help = "The minimum number of elements in the collection") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false,
          help = "The maximum number of elements in the collection") final Integer sizeMax,
      @CliOption(
          key = "cardinality",
          mandatory = false,
          unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level. This option is only available for JPA entities and embeddable "
              + "classes") CardinalitySupported cardinality,
      @CliOption(
          key = "fetch",
          mandatory = false,
          help = "The fetch semantics at a JPA level. This option is only available for JPA entities "
              + "and embeddable classes") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "joinTable",
          mandatory = true,
          help = "Join table name. Most usually used in @ManyToMany relations. This option is only available for JPA entities "
              + "and embeddable classes (mandatory if 'cardinality' option is 'MANY_TO_MANY')") final String joinTable,
      @CliOption(
          key = "joinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity owning the relation. This option is only available for JPA entities and embeddable"
              + " classes when 'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration "
              + "setting it's 'true', See 'settings list' command)") final String joinColumns,
      @CliOption(
          key = "referencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity owning the relation. This option is only available for JPA entities and embeddable classes when 'joinTable' "
              + "option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's 'true', "
              + "See 'settings list' command)") final String referencedColumns,
      @CliOption(
          key = "inverseJoinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity that does not own the relation. This option is only available for JPA entities and embeddable"
              + " classes when 'joinTable' option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting"
              + " it's 'true', See 'settings list' command)") final String inverseJoinColumns,
      @CliOption(
          key = "inverseReferencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity that does not own the relation. This option is only available for JPA entities and embeddable classes when 'joinTable'"
              + " option is set (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's 'true', See 'settings "
              + "list' command)") final String inverseReferencedColumns,
      //      @CliOption(key = "cascadeType", mandatory = false, unspecifiedDefaultValue = "ALL",
      //          specifiedDefaultValue = "ALL",
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH "
      //              + "and REMOVE.") final Cascade cascadeType,
      @CliOption(
          key = "aggregation",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Aggregation. Identify relationship as 'aggregation' (if is 'true', default) or a 'composition' (if is 'false')") final boolean aggregation,
      @CliOption(
          key = "orphanRemoval",
          mandatory = false,
          specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been removed from the relationship and to cascade the remove operation "
              + "to those entities") Boolean orphanRemoval, ShellContext shellContext) {

    // TODO Support multiple cascade type

    getFieldCreatorProvider(typeName).createListField(typeName, fieldType, fieldName,
        cardinality.getCardinality(), null, notNull, sizeMin, sizeMax, mappedBy, fetch, comment,
        joinTable, joinColumns, referencedColumns, inverseJoinColumns, inverseReferencedColumns,
        permitReservedWords, aggregation, orphanRemoval, shellContext.isForce());

  }

  @CliOptionMandatoryIndicator(command = "field string", params = {"column"})
  public boolean isColumnMandatoryForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"unique"},
      help = "Option 'unique' is not available for this type of class")
  public boolean isUniqueVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isUniqueVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"lob"},
      help = "Option 'lob' is not available for this type of class")
  public boolean isLobVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isLobVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldString(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field string", params = {"class"})
  public boolean isClassMandatoryForFieldString() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field string",
      help = "Adds a private string field to an existing Java source file")
  public void addFieldString(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of "
          + "the field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "sizeMin", mandatory = false, help = "The minimum string length") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false, help = "The maximum string length") final Integer sizeMax,
      @CliOption(key = "regexp", mandatory = false,
          help = "The required regular expression pattern") final String regexp,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's "
              + "'true'. See 'settings list' command)") final String column,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient. This option is only available for JPA entities "
              + "and embeddable classes") final boolean transientModifier,
      @CliOption(
          key = "unique",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint. This option is only available for JPA entities"
              + " and embeddable classes") final boolean unique,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "lob",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates that this field is a Large Object. This option is only available for JPA entities "
              + "and embeddable classes") final boolean lob, ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createStringField(cid, fieldName, notNull, nullRequired,
        null, null, sizeMin, sizeMax, regexp, column, comment, unique, value, lob,
        permitReservedWords, transientModifier);
  }

  @CliOptionMandatoryIndicator(command = "field file", params = {"column"})
  public boolean isColumnMandatoryForFieldFile(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldFile(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field file", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldFile(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldFile(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field file", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldFile(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field file", params = {"class"})
  public boolean isClassMandatoryForFieldFile() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field file",
      help = "Adds a byte array field for storing uploaded file contents (JSF-scaffolded UIs only)")
  public void addFileUploadField(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the file upload field to add (mandatory)") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "contentType", mandatory = true,
          help = "The content type of the file (mandatory)") final UploadedFileContentType contentType,
      @CliOption(key = "autoUpload", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the file is uploaded automatically when selected") final boolean autoUpload,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's "
              + "'true'. See 'settings list' command)") final String column,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createFileField(cid, fieldName, contentType, autoUpload,
        notNull, column, permitReservedWords);
  }

  @CliOptionMandatoryIndicator(command = "field other", params = {"column"})
  public boolean isColumnMandatoryForFieldOther(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnMandatoryForFieldOther(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field other", params = {"column"},
      help = "Option 'column' is not available for this type of class")
  public boolean isColumnVisibleForFieldOther(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isColumnVisibleForFieldOther(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field other", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldOther(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldOther(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field other", params = {"class"},
      help = "Option 'class' is not available "
          + "for this command when the focus is set to one class.")
  public boolean isClassVisibleForFieldOther(ShellContext shellContext) {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "field other", params = {"class"})
  public boolean isClassMandatoryForFieldOther() {
    if (lastUsed.getJavaType() == null) {
      return true;
    }
    return false;
  }

  @CliCommand(value = "field other", help = "Inserts a private field into the specified file")
  public void insertField(
      @CliOption(key = "fieldName", mandatory = true, help = "The name of the field (mandatory)") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, help = "The Java type of this field (mandatory)") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to receive this field. This option is available for"
              + " this command when the focus is not set to one class (mandatory). When working on a mono module project, "
              + "simply specify the name of the entity in which the new field will be included. If you considers it necessary,"
              + " you can also specify the package. Ex.: '--class ~ .domain.MyEntity'. When working with multiples modules, "
              + "you should specify the module of the the entity and the name of the entity that will create the new field. If "
              + "the module is not specified, it is assumed that the entity is in the module that has set the focus. If "
              + "you considers it necessary, you can also specify the package of the entity with the module name. "
              + "Ex.: '--class model: ~ .domain.MyEntity'") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes (mandatory if 'spring.roo.jpa.require.schema-object-name' configuration setting it's "
              + "'true'. See 'settings list' command)") final String column,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient. This option is only available for JPA entities "
              + "and embeddable classes") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createOtherField(cid, fieldType, fieldName, notNull,
        nullRequired, comment, column, permitReservedWords, transientModifier);
  }

  @CliAvailabilityIndicator({"field other", "field number", "field string", "field date",
      "field boolean", "field enum", "field file"})
  public boolean isFieldManagementAvailable() {
    return getFieldCreatorAvailable();
  }

  @CliAvailabilityIndicator({"field embedded"})
  public boolean isFieldEmbeddedAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      return getFieldCreatorProvider(type).isFieldEmbeddedAvailable();
    }
    return getFieldCreatorAvailable();
  }

  @CliAvailabilityIndicator({"field reference"})
  public boolean isFieldReferenceAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      return getFieldCreatorProvider(type).isFieldReferenceAvailable();
    }
    return getFieldCreatorAvailable();
  }

  @CliAvailabilityIndicator({"field set", "field list"})
  public boolean isFieldCollectionAvailable() {
    JavaType type = lastUsed.getJavaType();
    if (type != null) {
      return getFieldCreatorProvider(type).isFieldCollectionAvailable();
    }
    return getFieldCreatorAvailable();
  }


  /**
   * Checks if entity has already a field with the same name and throws an exception
   * in that case.
   *
   * @param fieldName
   * @param shellContext
   * @param javaTypeDetails
   *
   * @deprecated this should be done by operation class (see JpaFieldCreatorProvider.checkFieldExists)
   */
  private void checkFieldExists(final JavaSymbolName fieldName, ShellContext shellContext,
      final ClassOrInterfaceTypeDetails javaTypeDetails) {
    MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), javaTypeDetails);
    List<FieldMetadata> fields = memberDetails.getFields();
    for (FieldMetadata field : fields) {
      if (field.getFieldName().equals(fieldName) && !shellContext.isForce()) {
        throw new IllegalArgumentException(
            String
                .format(
                    "Field '%s' already exists and cannot be created. Try to use a "
                        + "different field name on --fieldName parameter or use --force parameter to overwrite it.",
                    fieldName));
      }
    }
  }

  /**
   * Tries to obtain JavaType indicated in command or which has the focus
   * in the Shell
   *
   * @param shellContext the Roo Shell context
   * @return JavaType or null if no class has the focus or no class is
   * specified in the command
   */
  private JavaType getTypeFromCommand(ShellContext shellContext) {
    // Try to get 'class' from ShellContext
    String typeString = shellContext.getParameters().get("class");
    JavaType type = null;
    if (typeString != null) {
      type = getJavaTypeConverter().convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }
    return type;
  }

  /**
   * Gets the right implementation of FieldCreatorProvider for a JavaType
   *
   * @param type the JavaType to get the implementation
   * @return FieldCreatorProvider implementation
   */
  public FieldCreatorProvider getFieldCreatorProvider(JavaType type) {

    // Get all Services implement FieldCreatorProvider interface
    if (fieldCreatorProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FieldCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          FieldCreatorProvider fieldCreatorProvider =
              (FieldCreatorProvider) this.context.getService(ref);
          fieldCreatorProviders.add(fieldCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FieldCreatorProvider on FieldCommands.");
        return null;
      }
    }

    for (FieldCreatorProvider provider : fieldCreatorProviders) {
      if (provider.isValid(type)) {
        return provider;
      }
    }

    return null;

  }

  /**
   * Checks all FieldCreator implementations looking for any available
   *
   * @return <code>true</code> if any of the implementations is available or
   * <code>false</code> if none of the implementations are available.
   */
  private boolean getFieldCreatorAvailable() {
    // Get all Services implement FieldCreatorProvider interface
    if (fieldCreatorProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FieldCreatorProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          FieldCreatorProvider fieldCreatorProvider =
              (FieldCreatorProvider) this.context.getService(ref);
          fieldCreatorProviders.add(fieldCreatorProvider);
        }

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FieldCreatorProvider on FieldCommands.");
        return false;
      }
    }

    for (FieldCreatorProvider provider : fieldCreatorProviders) {
      if (provider.isFieldManagementAvailable()) {
        return true;
      }
    }

    return false;
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
}
