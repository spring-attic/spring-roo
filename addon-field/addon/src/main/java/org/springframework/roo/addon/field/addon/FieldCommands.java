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
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class FieldCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FieldCommands.class);

  //------------ OSGi component attributes ----------------//
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

  @CliOptionVisibilityIndicator(command = "field boolean", params = {"assertFalse"},
      help = "Option `--assertFalse` is not available if option `--assertTrue` "
          + "has already been specified.")
  public boolean isAssertFalseVisibleForFieldBoolean(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isAssertFalseVisibleForFieldBoolean(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field boolean", params = {"assertTrue"},
      help = "Option `--assertTrue` is not available if option `--assertFalse` "
          + "has already been specified.")
  public boolean isAssertTrueVisibleForFieldBoolean(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isAssertTrueVisibleForFieldBoolean(shellContext);
    }
    return false;
  }

  @CliCommand(value = "field boolean",
      help = "Adds a private boolean field to an existing Java source file.")
  public void addFieldBoolean(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be included."
              + " If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working with"
              + " multiple modules, you should specify the name of the class and the module where "
              + "it is. Ex.: `--class model:~.domain.MyClass`. If the module is not specified, it "
              + "is assumed that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo shell.") final JavaType typeName,
      @CliOption(key = "column", mandatory = true,
          help = "The JPA @Column name. This option is only available for JPA entities and "
              + "embeddable classes. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` "
              + "configuration setting exists and it's `true`.") final String column,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean transientModifier,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` "
              + "annotation to the field. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(key = "assertFalse", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the value of this field must be false. Adds "
              + "`javax.validation.constraints.AssertFalse` annotation to the field. "
              + "This option is not available if `--asssertTrue` has already " + "been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean assertFalse,
      @CliOption(key = "assertTrue", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the value of this field must be true. Adds "
              + "`javax.validation.constraints.AssertTrue` annotation to the field. "
              + "This option is not available if `--asssertFalse` has already "
              + "been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean assertTrue,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` annotation with the given content, typically used for expression-driven dependency injection.") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use the primitive type. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean primitive,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    getFieldCreatorProvider(typeName).createBooleanField(javaTypeDetails, primitive, fieldName,
        notNull, assertFalse, assertTrue, column, comment, value, permitReservedWords,
        transientModifier);
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

  @CliOptionVisibilityIndicator(command = "field date", params = {"future"},
      help = "Option '--future' is not available when option '--past' has already "
          + "been specified.")
  public boolean isFutureVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isFutureVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"past"},
      help = "Option '--past' is not available when option '--future' has already "
          + "been specified.")
  public boolean isPastVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isPastVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"dateFormat", "timeFormat"},
      help = "Options `--dateFormat` and `--timeFormat` are not available when option "
          + "`--dateTimeFormatPattern` has already been specified.")
  public boolean areDateAndTimeFormatVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areDateAndTimeFormatVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"dateTimeFormatPattern"},
      help = "Option `--dateTimeFormatPattern` is not available when options "
          + "`--dateFormat` or `--timeFormat` have already been specified.")
  public boolean isDateTimeFormatPatternVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isDateTimeFormatPatternVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field date", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available when option `--notNull` "
          + "has already been specified.")
  public boolean isNullRequiredVisibleForFieldDate(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldDate(shellContext);
    }
    return false;
  }

  @CliCommand(value = "field date",
      help = "Adds a private date field to an existing Java source file.")
  public void addFieldDateJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
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
              + "the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo shell.") final JavaType typeName,
      @CliOption(
          key = "persistenceType",
          mandatory = false,
          help = "The type of persistent storage to be used. It adds a `javax.persistence.TemporalType` "
              + "to a `javax.persistence.Temporal` annotation into the field. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option not present: `TemporalType.TIMESTAMP`") final DateFieldPersistenceType persistenceType,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA @Column name. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`. "
              + "This option is only visible for JPA entities and embeddable classes.") final String column,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present:`true`. Default if option not present: `false`.") final boolean transientModifier,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(
          key = "future",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be in the future. Adds `field.javax.validation.constraints.Future` "
              + "annotation to the field. "
              + "This option is not available if `--past` option has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean future,
      @CliOption(
          key = "past",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be in the past. Adds `field.javax.validation.constraints.Past` "
              + "annotation to the field. "
              + "This option is not available if `--future` option has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean past,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` "
              + "annotation with the given content, typically used for expression-driven dependency "
              + "injection.") final String value,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(key = "dateFormat", mandatory = false, unspecifiedDefaultValue = "MEDIUM",
          specifiedDefaultValue = "MEDIUM",
          help = "Indicates the style of the date format, adding `style` attribute to "
              + "`org.springframework.format.annotation.DateTimeFormat` annotation "
              + "into the field, with date style (first character of the code). "
              + "Possible values are: `MEDIUM` (style='M-'), `NONE` (style='--') and "
              + "`SHORT` (style='S-'). "
              + "This option is not available if `--dateTimeFormatPattern` has already "
              + "been specified." + "Default: `MEDIUM`.") final DateTime dateFormat,
      @CliOption(key = "timeFormat", mandatory = false, unspecifiedDefaultValue = "NONE",
          specifiedDefaultValue = "NONE",
          help = "Indicates the style of the time format, adding `style` attribute to "
              + "`org.springframework.format.annotation.DateTimeFormat` annotation "
              + "into the field, with time style (second character of the code). "
              + "Possible values are: `MEDIUM` (style='-M'), `NONE` (style='--') and "
              + "`SHORT` (style='-S'). "
              + "This option is not available if `--dateTimeFormatPattern` has already "
              + "been specified. " + "Default: `NONE`.") final DateTime timeFormat,
      @CliOption(
          key = "dateTimeFormatPattern",
          mandatory = false,
          help = "Indicates a 'custom' DateTime format pattern such as yyyy-MM-dd hh:mm:ss, adding "
              + "`pattern` attribute to `org.springframework.format.annotation.DateTimeFormat` "
              + "annotation into the field, with the provided value. "
              + "This option is not available if `--timeFormat` or `--dateFormat` have already "
              + "been specified.") final String pattern, ShellContext shellContext) {

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

  @CliCommand(value = "field embedded",
      help = "Adds a private `@Embedded` field to an existing Java source file. "
          + "This command is only available for entities annotated with `@RooJpaEntity` (Roo JPA "
          + "entities). Therefore, you should focus the desired entity in the Roo Shell to make "
          + "this command available.")
  public void addFieldEmbeddedJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of an embeddable class, annotated with `@Embeddable`. "
              + "Possible values are: any class in the project annotated with `@Embeddable`.") final JavaType fieldType,
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
              + "that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
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

  @CliOptionVisibilityIndicator(command = "field enum", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldEnum(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field enum", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available when option `--notNull` "
          + "has already been specified.")
  public boolean isNullRequiredVisibleForFieldEnum(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldEnum(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field enum",
      help = "Adds a private enum field to an existing Java source file. The field type must be a Java enum type.")
  public void addFieldEnum(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = ENUMERATION,
          help = "The Java type of the field. It must be a Java enum type. "
              + "Possible values are: any enumerated class in the user's project.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module project, "
              + "simply specify the name of the class in which the field will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyClass`"
              + " (where `~` is the base package). When working with multiple modules, you should specify"
              + " the name of the class and the module where it is. Ex.: `--class model:~.domain.MyClass`."
              + " If the module is not specified, it is assumed that the class is in the module which has"
              + " the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA `@Column` name. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration "
              + "setting exists and it's `true`. "
              + "This option is only available for JPA entities and embeddable classes.") final String column,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present:`true`; default if option not present: `false`.") final boolean transientModifier,
      @CliOption(
          key = "enumType",
          mandatory = false,
          help = "Defines how the enumerated field should be persisted at a JPA level. Adds the "
              + "`javax.persistence.Enumerated` annotation to the field, with `javax.persistence.EnumType`"
              + " attribute. "
              + "Possible values are: `ORDINAL` (persists as an integer) and `STRING` "
              + "(persists as a String). "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option not present: `ORDINAL` (no `@Enumerated` - default by JPA).") final EnumType enumType,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails enumTypeCid = typeLocationService.getTypeDetails(fieldType);
    Validate.notNull(enumTypeCid,
        "The enum type specified, '%s', doesn't exist. Please, specify an existent enum type.",
        fieldType.getSimpleTypeName());

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid,
        "The class specified, '%s', doesn't exist. Please, specify an existent class.",
        typeName.getSimpleTypeName());

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

  @CliOptionVisibilityIndicator(command = "field number", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldNumber(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field number", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldNumber(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldNumber(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field number",
      help = "Adds a private numeric field to an existing Java source file. User can choose the field type between a wide range of numeric types.")
  public void addFieldNumber(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(
          key = "type",
          mandatory = true,
          optionContext = "java-number",
          help = "The Java type of the field. Only numeric types allowed. "
              + "Possible values are: `java.math.BigDecimal`, `java.math.BigInteger`, `byte`, "
              + "`java.lang.Byte`, `double`, `java.lang.Double`, `float`, `java.lang.Float`, `int`, "
              + "`java.lang.Integer`, `long`, `java.lang.Long`, `java.lang.Number`, `short` and "
              + "`java.lang.Short`.") JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be included. "
              + "If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working with "
              + "multiple modules, you should specify the name of the class and the module where it "
              + "is. Ex.: `--class model:~.domain.MyClass`. If the module is not specified, it is "
              + "assumed that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(key = "column", mandatory = true, help = "The JPA `@Column` name."
          + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` "
          + "configuration setting exists and it's `true`. "
          + "This option is only available for JPA entities and embeddable classes.") final String column,
      @CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean unique,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present:`true`. Default if option not present: `false`.") final boolean transientModifier,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` or `--primitive` option have already been specified "
              + "with value `true` or without value. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(
          key = "decimalMin",
          mandatory = false,
          help = "The BigDecimal string-based representation of the minimum value. It adds to the field "
              + "`javax.validation.constraints.DecimalMin` annotation with provided value.") final String decimalMin,
      @CliOption(
          key = "decimalMax",
          mandatory = false,
          help = "The BigDecimal string based representation of the maximum value. It adds to the field "
              + "`javax.validation.constraints.DecimalMax` annotation with provided value.") final String decimalMax,
      @CliOption(
          key = "digitsInteger",
          mandatory = false,
          help = "Maximum number of integral digits accepted for this number. It creates or updates field "
              + "`javax.validation.constraints.Digits` annotation, adding `integer` attribute with the "
              + "provided value.") final Integer digitsInteger,
      @CliOption(
          key = "digitsFraction",
          mandatory = false,
          help = "Maximum number of fractional digits accepted for this number. It creates or updates field "
              + "`javax.validation.constraints.Digits` annotation, adding `fraction` attribute with the "
              + "provided value.") final Integer digitsFraction,
      @CliOption(key = "min", mandatory = false,
          help = "The minimum value of the numeric field. It adds "
              + "`javax.validation.constraints.Min` with provided value to the field.") final Long min,
      @CliOption(key = "max", mandatory = false,
          help = "The maximum value of the numeric field. It adds "
              + "`javax.validation.constraints.Max` with provided value to the field.") final Long max,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` "
              + "annotation with the given content, typically used for expression-driven dependency "
              + "injection. ") final String value,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use a primitive type if possible. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean primitive,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
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

  @CliOptionVisibilityIndicator(command = "field reference", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldReference(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field reference", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldReference(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field reference",
      help = "Adds a private reference field, representing (always) a bidirectional 'one-to-one' "
          + "relation, to an existing Java source file. Therefore, this command will add as well a "
          + "'one-to-one' field on the other side of the relation. "
          + "This command is only available for entities annotated with `@RooJpaEntity`, so you "
          + "should focus the desired entity in the Roo Shell to make this command available.")
  public void addFieldReferenceJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of the entity to reference.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be "
              + "included. If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working "
              + "with multiple modules, you should specify the name of the class and the module "
              + "where it is. Ex.: `--class model:~.domain.MyClass`. If the module is not "
              + "specified, it is assumed that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(key = "joinColumnName", mandatory = true,
          help = "The JPA `@JoinColumn` `name` attribute. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` "
              + "configuration setting exists and it's `true`. "
              + "This option is only available for JPA entities.") final String joinColumnName,
      @CliOption(key = "referencedColumnName", mandatory = false,
          help = "The JPA `@JoinColumn` `referencedColumnName` attribute."
              + "This option is only available for JPA entities.") final String referencedColumnName,
      @CliOption(
          key = "fetch",
          mandatory = false,
          help = "The fetch semantics at a JPA level. It adds the provided value to `fetch` attribute "
              + "of JPA `@OneToOne`. If this option is not provided, default fetch type will be `LAZY`. "
              + "Possible values are `LAZY`and `EAGER`. "
              + "This option is only available for JPA entities.") final Fetch fetch,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship, which will be also "
              + "created due to bidirectional relation. If not specified, it will take the lower camel "
              + "case of the current entity (focused entity or specified in `--class` option). If the "
              + "field already exists in the related entity, command won't be executed. "
              + "This option is only available for JPA entities. "
              + "Default if not present: current entity name in lower camel case.") final JavaSymbolName mappedBy,
      @CliOption(key = "aggregation", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Whether the relationship type is 'aggregation' or 'composition'. An aggregation "
              + "relation means that children entities aren't dependent from parent entity "
              + "(current entity) and they can exist without parent entity. In the other hand, in "
              + "a composition relation the parent entity of the relationship also owns the life "
              + "cycle of related entities. The parent entity is responsible for the creation and "
              + "destruction of children entities, these being linked to a single parent entity. "
              + "A child entity cannot be in two different composition relationships. "
              + "Default: `true`.") final boolean aggregation,
      @CliOption(key = "orphanRemoval", mandatory = false, specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been "
              + "removed from the relationship and to cascade the remove operation to those "
              + "entities. If this relation represents a 'composition' relation and this option "
              + "is not present, `--orphanRemoval` value will be `true`. "
              + "Default if option present: `true`.") Boolean orphanRemoval,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(
          key = "entityFormatExpression",
          mandatory = false,
          help = "The SpEL expression used to format the related entity when showing it in presentation layer e.g. "
              + "`{#fieldA} {#fieldB}`. It adds the `value` attribute to `io.springlets.format.EntityFormat` "
              + "annotation.") String formatExpression,
      @CliOption(
          key = "entityFormatMessage",
          mandatory = false,
          help = "The message key used to obtain a localized SpEL expression to format the related entity when "
              + "showing it in presentation layer. It adds the `message` attribute to "
              + "`io.springlets.format.EntityFormat` annotation and creates a message in all message bundles "
              + "with the provided key. Message value should be modified by developer.") String formatMessage,
      //      @CliOption(
      //          key = "cascadeType",
      //          mandatory = false,
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH and REMOVE.") final Cascade cascadeType,
      ShellContext shellContext) {

    // TODO support multiple cascade type
    getFieldCreatorProvider(typeName)
        .createReferenceField(typeName, fieldType, fieldName, aggregation, mappedBy, null, notNull,
            joinColumnName, referencedColumnName, fetch, comment, permitReservedWords,
            orphanRemoval, shellContext.isForce(), formatExpression, formatMessage);
  }

  @CliOptionMandatoryIndicator(command = "field set", params = {"joinColumnName"})
  public boolean isColumnMandatoryForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinColumnNameMandatoryForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"joinColumnName"},
      help = "Option 'joinColumnName' is not available for this type of class")
  public boolean isJoinColumnNameVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinColumnNameVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"referencedColumnName"},
      help = "Option 'referencedColumnName' is not available for this type of class")
  public boolean isReferencedColumnNameVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isReferencedColumnNameVisibleForFieldSet(shellContext);
    }
    return false;
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

  @CliOptionVisibilityIndicator(command = "field set", params = {"aggregation", "comment", "fetch",
      "force", "mappedBy", "notNull", "nullRequired", "orphanRemoval", "permitReservedWords",
      "profile", "sizeMax", "sizeMin"}, help = "--joinTable or --joinColumnName must be defined")
  public boolean areOptionalParametersVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areOptionalParametersVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field set", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldSet(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field set",
      help = "Adds a private `Set` field to an existing Java source file, representing (always) a "
          + "bidirectional relation with other entity. Therefore, this command will also add a field "
          + "on the other side of the relation (the owner side, with `mappedBy` attribute), which will"
          + " be a `Set` field for 'many-to-many' relations, or a *not* `Collection` field for a "
          + "'one-to-many' relation. All added fields will have the needed JPA annotations to properly "
          + "manage bidirectional relations. "
          + "This command is only available for entities annotated with `@RooJpaEntity` (Roo JPA "
          + "entities). Therefore, you should focus the desired entity in the Roo Shell to make "
          + "this command available.")
  public void addFieldSetJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity related to this one, which will be contained within the `List`."
              + "Possible values are: any of the entities in the project.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be included."
              + " If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working with"
              + " multiple modules, you should specify the name of the class and the module where "
              + "it is. Ex.: `--class model:~.domain.MyClass`. If the module is not specified, it "
              + "is assumed that the class is in the module which has the focus."
              + "This option is mandatory for this command when the focus is not set to one class."
              + "Default if option not present: the class focused by Roo shell.") final JavaType typeName,
      @CliOption(
          key = "joinColumnName",
          mandatory = true,
          help = "The JPA `@JoinColumn` `name` attribute. When this option is set, cardinality will be set as `ONE_TO_MANY`."
              + "This option is mandatory for 'ONE_TO_MANY' relationships without join table, "
              + "if `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`.") final String joinColumnName,
      @CliOption(
          key = "referencedColumnName",
          mandatory = false,
          help = "The JPA `@JoinColumn` `referencedColumnName` attribute. This option is only available"
              + " when `--joinColumnName` option is set.") final String referencedColumnName,
      @CliOption(
          key = "joinTable",
          mandatory = true,
          help = "Join table name. Most usually used in @ManyToMany relations."
              + "This option is mandatory for this command if `--cardinality` is set to `MANY_TO_MANY` "
              + "and `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`.") final String joinTable,
      @CliOption(
          key = "joinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which references the table "
              + "of the related entity (the owner entity in bidirectional relations)."
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`."
              + "This option is only available when `--joinTable` " + "option is set.") final String joinColumns,
      @CliOption(
          key = "referencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the primary table of the "
              + "related entity (the owner entity in bidirectional relations)."
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`."
              + "This option is only available when `--joinTable` " + "option is set.") final String referencedColumns,
      @CliOption(
          key = "inverseJoinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which references the table of "
              + "the entity that does not own the relation (current entity)."
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`."
              + "This option is only available when `--joinTable` " + "option is set.") final String inverseJoinColumns,
      @CliOption(
          key = "inverseReferencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the primary table of the "
              + "entity that does not own the relation (current entity)."
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`."
              + "This option is only available when `--joinTable` " + "option is set.") final String inverseReferencedColumns,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship, which will be also "
              + "created due to bidirectional relation. If not specified, it will take the lower camel "
              + "case of the current entity (focused entity or specified in `--class` option). If the "
              + "field already exists in the related entity, command won't be executed."
              + "This option is only available for JPA entities."
              + "Default if not present: current entity name in lower camel case.") final JavaSymbolName mappedBy,
      @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level. "
              + "This option is only available for JPA entities and embeddable classes."
              + "Default: `ONE_TO_MANY`.") CardinalitySupported cardinality,
      @CliOption(
          key = "fetch",
          mandatory = false,
          help = "The fetch semantics at a JPA level. It adds the provided value to `fetch` attribute of "
              + "JPA `@OneToMany`, `@ManyToMany` and `@ManyToOne`. "
              + "Possible values are: `LAZY`and `EAGER`."
              + "Default if option not present: `LAZY`.") final Fetch fetch,
      @CliOption(
          key = "aggregation",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Whether the relationship type is 'aggregation' or 'composition'. An aggregation "
              + "relation means that children entities aren't dependent from parent entity (current "
              + "entity) and they can exist without parent entity. In the other hand, in a composition "
              + "relation the parent entity of the relationship also owns the life cycle of related "
              + "entities. The parent entity is responsible for the creation and destruction of children"
              + " entities, these being linked to a single parent entity. A child entity cannot be in two"
              + " different composition relationships." + "Default: `true`.") final boolean aggregation,
      @CliOption(
          key = "orphanRemoval",
          mandatory = false,
          specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been removed from"
              + " the relationship and to cascade the remove operation to those entities. If this "
              + "relation represents a 'composition' relation and this option is not present, "
              + "`--orphanRemoval` value will be `true`." + "Default if option present: `true`.") Boolean orphanRemoval,
      @CliOption(
          key = "sizeMin",
          mandatory = false,
          help = "The minimum number of elements in the collection. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `min` attribute value.") final Integer sizeMin,
      @CliOption(
          key = "sizeMax",
          mandatory = false,
          help = "The maximum number of elements in the collection. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `max` attribute value.") final Integer sizeMax,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(
          key = "entityFormatExpression",
          mandatory = false,
          help = "The SpEL expression used to format the related entity when showing it in presentation layer e.g. "
              + "{#fieldA} {#fieldB}. It adds the `value` attribute to `io.springlets.format.EntityFormat` "
              + "annotation."
              + "This option is available only if `--entityFormatMessage` has not been specified and "
              + "`--cardinality` is `MANY_TO_ONE`.") String formatExpression,
      @CliOption(
          key = "entityFormatMessage",
          mandatory = false,
          help = "The message key used to obtain a localized SpEL expression to format the related entity when "
              + "showing it in presentation layer. It adds the `message` attribute to "
              + "`io.springlets.format.EntityFormat` annotation and creates a message in all message bundles "
              + "with the provided key. Message value should be modified by developer."
              + "This option is available only if `--entityFormatExpression` has not been specified and "
              + "`--cardinality` is `MANY_TO_ONE`.") String formatMessage,
      //      @CliOption(key = "cascadeType", mandatory = false, unspecifiedDefaultValue = "ALL",
      //          specifiedDefaultValue = "ALL",
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH "
      //              + "and REMOVE.") final Cascade cascadeType,
      ShellContext shellContext) {

    // TODO support multiple cascade type

    getFieldCreatorProvider(typeName).createSetField(typeName, fieldType, fieldName,
        cardinality.getCardinality(), null, notNull, sizeMin, sizeMax, mappedBy, fetch, comment,
        joinColumnName, referencedColumnName, joinTable, joinColumns, referencedColumns,
        inverseJoinColumns, inverseReferencedColumns, permitReservedWords, aggregation,
        orphanRemoval, shellContext.isForce(), formatExpression, formatMessage);

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

  @CliOptionMandatoryIndicator(command = "field list", params = {"joinColumnName"})
  public boolean isColumnMandatoryForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinColumnNameMandatoryForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"joinColumnName"},
      help = "Option 'joinColumnName' is not available for this type of class")
  public boolean isJoinColumnNameVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isJoinColumnNameVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"referencedColumnName"},
      help = "Option 'referencedColumnName' is not available for this type of class")
  public boolean isReferencedColumnNameVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isReferencedColumnNameVisibleForFieldList(shellContext);
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

  @CliOptionVisibilityIndicator(command = "field list", params = {"aggregation", "comment",
      "fetch", "force", "mappedBy", "notNull", "orphanRemoval", "permitReservedWords", "profile",
      "sizeMax", "sizeMin"}, help = "--joinTable or --joinColumnName must be defined first.")
  public boolean areOptionalParametersVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).areOptionalParametersVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field list", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldList(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field list",
      help = "Adds a private `List` field to an existing Java source file, representing (always) a "
          + "bidirectional relation with other entity. Therefore, this command will also add a field "
          + "on the other side of the relation (the owner side, with `mappedBy` attribute), which will"
          + " be a `List` field for 'many-to-many' relations, or a *not* `Collection` field for a "
          + "'one-to-many' relation. All added fields will have the needed JPA annotations to properly "
          + "manage bidirectional relations."
          + "This command is only available for entities annotated with `@RooJpaEntity` (Roo JPA "
          + "entities). Therefore, you should focus the desired entity in the Roo Shell to make "
          + "this command available.")
  public void addFieldListJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity related to this one, which will be contained within the `List`. "
              + "Possible values are: any of the entities in the project.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be included."
              + " If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working with"
              + " multiple modules, you should specify the name of the class and the module where "
              + "it is. Ex.: `--class model:~.domain.MyClass`. If the module is not specified, it "
              + "is assumed that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo shell.") final JavaType typeName,
      @CliOption(
          key = "joinColumnName",
          mandatory = true,
          help = "The JPA `@JoinColumn` `name` attribute. When this option is set, cardinality will be set as `ONE_TO_MANY`."
              + "This option is mandatory for 'ONE_TO_MANY' relationships without join table, "
              + "if `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`.") final String joinColumnName,
      @CliOption(
          key = "referencedColumnName",
          mandatory = false,
          help = "The JPA `@JoinColumn` `referencedColumnName` attribute. This option is only available"
              + " when `--joinColumnName` option is set.") final String referencedColumnName,
      @CliOption(
          key = "joinTable",
          mandatory = true,
          help = "Join table name. Most usually used in `@ManyToMany` relations. "
              + "This option is mandatory for this command if `--cardinality` is set to `MANY_TO_MANY` "
              + "and `spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`.") final String joinTable,
      @CliOption(
          key = "joinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which references the table "
              + "of the related entity (the owner entity in bidirectional relations). "
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`. "
              + "This option is only available when `--joinTable` " + "option is set.") final String joinColumns,
      @CliOption(
          key = "referencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the primary table of the "
              + "related entity (the owner entity in bidirectional relations). "
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`. "
              + "This option is only available when `--joinTable` " + "option is set.") final String referencedColumns,
      @CliOption(
          key = "inverseJoinColumns",
          mandatory = true,
          help = "Comma separated list of join table's foreign key columns which references the table of "
              + "the entity that does not own the relation (current entity). "
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`. "
              + "This option is only available when `--joinTable` " + "option is set.") final String inverseJoinColumns,
      @CliOption(
          key = "inverseReferencedColumns",
          mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the primary table of the "
              + "entity that does not own the relation (current entity). "
              + "This option is mandatory if `--joinTable` option has been specified and if "
              + "`spring.roo.jpa.require.schema-object-name` configuration setting exists and it's `true`. "
              + "This option is only available when `--joinTable` " + "option is set.") final String inverseReferencedColumns,
      @CliOption(
          key = "mappedBy",
          mandatory = false,
          help = "The field name on the referenced type which owns the relationship, which will be also "
              + "created due to bidirectional relation. If not specified, it will take the lower camel "
              + "case of the current entity (focused entity or specified in `--class` option). If the "
              + "field already exists in the related entity, command won't be executed. "
              + "This option is only available for JPA entities. "
              + "Default if not present: current entity name in lower camel case.") final JavaSymbolName mappedBy,
      @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level. " + "Default: `ONE_TO_MANY`.") CardinalitySupported cardinality,
      @CliOption(
          key = "fetch",
          mandatory = false,
          help = "The fetch semantics at a JPA level. It adds the provided value to `fetch` attribute of "
              + "JPA `@OneToMany`, `@ManyToMany` and `@ManyToOne`. "
              + "Possible values are: `LAZY`and `EAGER`. "
              + "Default if option not present: `LAZY`.") final Fetch fetch,
      @CliOption(
          key = "aggregation",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "true",
          help = "Whether the relationship type is 'aggregation' or 'composition'. An aggregation "
              + "relation means that children entities aren't dependent from parent entity (current "
              + "entity) and they can exist without parent entity. In the other hand, in a composition "
              + "relation the parent entity of the relationship also owns the life cycle of related "
              + "entities. The parent entity is responsible for the creation and destruction of children"
              + " entities, these being linked to a single parent entity. A child entity cannot be in two"
              + " different composition relationships. " + "Default: `true`.") final boolean aggregation,
      @CliOption(
          key = "orphanRemoval",
          mandatory = false,
          specifiedDefaultValue = "true",
          help = "Indicates whether to apply the remove operation to entities that have been removed from"
              + " the relationship and to cascade the remove operation to those entities. If this "
              + "relation represents a 'composition' relation and this option is not present, "
              + "`--orphanRemoval` value will be `true`. " + "Default if option present: `true`.") Boolean orphanRemoval,
      @CliOption(
          key = "sizeMin",
          mandatory = false,
          help = "The minimum number of elements in the collection. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `min` attribute value.") final Integer sizeMin,
      @CliOption(
          key = "sizeMax",
          mandatory = false,
          help = "The maximum number of elements in the collection. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `max` attribute value.") final Integer sizeMax,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(
          key = "entityFormatExpression",
          mandatory = false,
          help = "The SpEL expression used to format the related entity when showing it in presentation layer e.g. "
              + "`{#fieldA} {#fieldB}`. It adds the `value` attribute to `io.springlets.format.EntityFormat` "
              + "annotation.") String formatExpression,
      @CliOption(
          key = "entityFormatMessage",
          mandatory = false,
          help = "The message key used to obtain a localized SpEL expression to format the related entity when "
              + "showing it in presentation layer. It adds the `message` attribute to "
              + "`io.springlets.format.EntityFormat` annotation and creates a message in all message bundles "
              + "with the provided key. Message value should be modified by developer.") String formatMessage,
      //      @CliOption(key = "cascadeType", mandatory = false, unspecifiedDefaultValue = "ALL",
      //          specifiedDefaultValue = "ALL",
      //          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH "
      //              + "and REMOVE.") final Cascade cascadeType,
      ShellContext shellContext) {

    // TODO Support multiple cascade type

    getFieldCreatorProvider(typeName).createListField(typeName, fieldType, fieldName,
        cardinality.getCardinality(), null, notNull, sizeMin, sizeMax, mappedBy, fetch, comment,
        joinColumnName, referencedColumnName, joinTable, joinColumns, referencedColumns,
        inverseJoinColumns, inverseReferencedColumns, permitReservedWords, aggregation,
        orphanRemoval, shellContext.isForce(), formatExpression, formatMessage);

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

  @CliOptionVisibilityIndicator(command = "field string", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field string", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldString(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldString(shellContext);
    }
    return false;
  }

  @CliCommand(value = "field string",
      help = "Adds a private String field to an existing Java source file.")
  public void addFieldString(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add.") final JavaSymbolName fieldName,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module project, "
              + "simply specify the name of the class in which the field will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyClass`"
              + " (where `~` is the base package). When working with multiple modules, you should specify"
              + " the name of the class and the module where it is. Ex.: `--class model:~.domain.MyClass`."
              + " If the module is not specified, it is assumed that the class is in the module which has"
              + " the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA `@Column` name. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration "
              + "setting exists and it's `true`. "
              + "This option is only available for JPA entities and embeddable classes.") final String column,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present:`true`. Default if option not present: `false`") final boolean transientModifier,
      @CliOption(
          key = "lob",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates that this field is a Large Object. This option adds `javax.persistence.Lob` "
              + "annotation to the field. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean lob,
      @CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean unique,
      @CliOption(
          key = "regexp",
          mandatory = false,
          help = "The required regular expression pattern. This option adds "
              + "`javax.validation.constraints.Pattern` with the provided value as `regexp` attribute.") final String regexp,
      @CliOption(
          key = "sizeMin",
          mandatory = false,
          help = "The minimum string length. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `min` attribute value.") final Integer sizeMin,
      @CliOption(
          key = "sizeMax",
          mandatory = false,
          help = "The maximum string length. This option adds or updates "
              + "`javax.validation.constraints.Size` with the provided value as `max` attribute value.") final Integer sizeMax,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(
          key = "value",
          mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` "
              + "annotation with the given content, typically used for expression-driven dependency "
              + "injection.") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

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
      help = "Adds a byte array field for storing uploaded file contents.")
  public void addFileUploadField(
      @CliOption(key = {"", "fieldName"}, mandatory = true,
          help = "The name of the file upload field to add.") final JavaSymbolName fieldName,
      @CliOption(key = "contentType", mandatory = true, help = "The content type of the file. "
          + "Possible values are: `CSS`, `CSV`, `DOC`, `GIF`, `HTML`, `JAVASCRIPT`, `JPG`, "
          + "`JSON`, `MP3`, `MP4`, `MPEG`, `PDF`, `PNG`, `TXT`, `XLS`, `XML` and `ZIP`.") final UploadedFileContentType contentType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module project, "
              + "simply specify the name of the class in which the field will be included. If you "
              + "consider it necessary, you can also specify the package. Ex.: `--class ~.domain.MyClass`"
              + " (where `~` is the base package). When working with multiple modules, you should specify"
              + " the name of the class and the module where it is. Ex.: `--class model:~.domain.MyClass`."
              + " If the module is not specified, it is assumed that the class is in the module which has"
              + " the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(
          key = "column",
          mandatory = true,
          help = "The JPA `@Column` name. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration "
              + "setting exists and it's `true`. "
              + "This option is only available for JPA entities and embeddable classes.") final String column,
      @CliOption(key = "autoUpload", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the file is uploaded automatically when selected. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean autoUpload,
      @CliOption(
          key = "notNull",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` annotation to the field."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
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

  @CliOptionVisibilityIndicator(command = "field other", params = {"notNull"},
      help = "Option `--notNull` is not available when option `--nullRequired` "
          + "has already been specified.")
  public boolean isNotNullVisibleForFieldOther(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNotNullVisibleForFieldOther(shellContext);
    }
    return false;
  }

  @CliOptionVisibilityIndicator(command = "field other", params = {"nullRequired"},
      help = "Option `--nullRequired` is not available if numeric type is primitive "
          + "or `--notNull` option has been specified.")
  public boolean isNullRequiredVisibleForFieldOther(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isNullRequiredVisibleForFieldOther(shellContext);
    }
    return false;
  }

  @CliCommand(
      value = "field other",
      help = "Inserts a private field into the specified file. User can choose a custom type for the field by specifying its fully qualified name.")
  public void insertField(
      @CliOption(key = "fieldName", mandatory = true, help = "The name of the field.") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, help = "The Java type of this field.") final JavaType fieldType,
      @CliOption(
          key = "class",
          mandatory = true,
          unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the class to generate the field. When working on a mono module "
              + "project, simply specify the name of the class in which the field will be included."
              + " If you consider it necessary, you can also specify the package. "
              + "Ex.: `--class ~.domain.MyClass` (where `~` is the base package). When working "
              + "with multiple modules, you should specify the name of the class and the module "
              + "where it is. Ex.: `--class model:~.domain.MyClass`. If the module is not "
              + "specified, it is assumed that the class is in the module which has the focus. "
              + "This option is mandatory for this command when the focus is not set to one class. "
              + "Default if option not present: the class focused by Roo Shell.") final JavaType typeName,
      @CliOption(key = "column", mandatory = true, help = "The JPA `@Column` name."
          + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration"
          + " setting exists and it's `true`. "
          + "This option is only available for JPA entities and embeddable classes.") final String column,
      @CliOption(
          key = "transient",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates to mark the field as transient, adding JPA `javax.persistence.Transient` "
              + "annotation. This marks the field as not persistent. "
              + "This option is only available for JPA entities and embeddable classes. "
              + "Default if option present:`true`. Default if option not present: `false`") final boolean transientModifier,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value cannot be null. Adds `javax.validation.constraints.NotNull` "
              + "annotation to the field. "
              + "This option is not available if `--nullRequired` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean notNull,
      @CliOption(
          key = "nullRequired",
          mandatory = false,
          unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether this value must be null. Adds `javax.validation.constraints.Null` annotation to the field. "
              + "This option is not available if `--notNull` has already been specified. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs.") final String comment,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring `org.springframework.beans.factory.annotation.Value` "
              + "annotation with the given content, typically used for expression-driven "
              + "dependency injection.") final String value,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    getFieldCreatorProvider(typeName).createOtherField(cid, fieldType, fieldName, notNull,
        nullRequired, comment, column, permitReservedWords, transientModifier);
  }

  // Availability indicators //

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
