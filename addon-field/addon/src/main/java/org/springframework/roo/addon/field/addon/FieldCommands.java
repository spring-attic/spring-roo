package org.springframework.roo.addon.field.addon;

import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.SpringJavaType.PERSISTENT;
import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentFormatter;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Cascade;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.classpath.operations.jsr303.BooleanField;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.EmbeddedField;
import org.springframework.roo.classpath.operations.jsr303.EnumField;
import org.springframework.roo.classpath.operations.jsr303.ListField;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.ReferenceField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileContentType;
import org.springframework.roo.classpath.operations.jsr303.UploadedFileField;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.logging.HandlerUtils;

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

  // Project Settings 
  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";
  public static final String ROO_DEFAULT_JOIN_TABLE_NAME = "_ROO_JOIN_TABLE_";

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
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "assertFalse", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must assert false") final boolean assertFalse,
      @CliOption(key = "assertTrue", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must assert true") final boolean assertTrue,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use a primitive type") final boolean primitive,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final BooleanField fieldDetails =
        new BooleanField(physicalTypeIdentifier, primitive ? JavaType.BOOLEAN_PRIMITIVE
            : JavaType.BOOLEAN_OBJECT, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    fieldDetails.setAssertFalse(assertFalse);
    fieldDetails.setAssertTrue(assertTrue);
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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
      @CliOption(key = "type", mandatory = true, optionContext = "java-date",
          help = "The Java type of the entity") final JavaType fieldType,
      @CliOption(key = "persistenceType", mandatory = false,
          help = "The type of persistent storage to be used") final DateFieldPersistenceType persistenceType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "future", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be in the future") final boolean future,
      @CliOption(key = "past", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be in the past") final boolean past,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "dateFormat",
          mandatory = false,
          unspecifiedDefaultValue = "MEDIUM",
          specifiedDefaultValue = "MEDIUM",
          help = "Indicates the style of the date format (ignored if dateTimeFormatPattern is specified)") final DateTime dateFormat,
      @CliOption(
          key = "timeFormat",
          mandatory = false,
          unspecifiedDefaultValue = "NONE",
          specifiedDefaultValue = "NONE",
          help = "Indicates the style of the time format (ignored if dateTimeFormatPattern is specified)") final DateTime timeFormat,
      @CliOption(key = "dateTimeFormatPattern", mandatory = false,
          help = "Indicates a DateTime format pattern such as yyyy-MM-dd hh:mm:ss a") final String pattern,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final DateField fieldDetails = new DateField(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    fieldDetails.setFuture(future);
    fieldDetails.setPast(past);
    if (JdkJavaType.isDateField(fieldType)) {
      fieldDetails.setPersistenceType(persistenceType != null ? persistenceType
          : DateFieldPersistenceType.JPA_TIMESTAMP);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (dateFormat != null) {
      fieldDetails.setDateFormat(dateFormat);
    }
    if (timeFormat != null) {
      fieldDetails.setTimeFormat(timeFormat);
    }
    if (pattern != null) {
      fieldDetails.setPattern(pattern);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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

  @CliCommand(value = "field embedded",
      help = "Adds a private @Embedded field to an existing Java source file ")
  public void addFieldEmbeddedJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of the @Embeddable class") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the @Entity class to receive this field") final JavaType typeName,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    // Check if the field type is a JPA @Embeddable class
    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
    Validate
        .notNull(cid,
            "The specified target '--type' does not exist or can not be found. Please create this type first.");
    Validate.notNull(cid.getAnnotation(EMBEDDABLE),
        "The field embedded command is only applicable to JPA @Embeddable field types.");

    checkFieldExists(fieldName, shellContext, cid);

    // Check if the requested entity is a JPA @Entity
    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final PhysicalTypeMetadata targetTypeMetadata =
        (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
    Validate
        .notNull(targetTypeMetadata,
            "The specified target '--class' does not exist or can not be found. Please create this type first.");
    final PhysicalTypeDetails targetPtd = targetTypeMetadata.getMemberHoldingTypeDetails();
    Validate.isInstanceOf(MemberHoldingTypeDetails.class, targetPtd);

    final ClassOrInterfaceTypeDetails targetTypeCid = (ClassOrInterfaceTypeDetails) targetPtd;
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), targetTypeCid);
    Validate
        .isTrue(
            memberDetails.getAnnotation(ENTITY) != null
                || memberDetails.getAnnotation(PERSISTENT) != null,
            "The field embedded command is only applicable to JPA @Entity or Spring Data @Persistent target types.");

    final EmbeddedField fieldDetails =
        new EmbeddedField(physicalTypeIdentifier, fieldType, fieldName);

    insertField(fieldDetails, permitReservedWords, false);
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
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, help = "The enum type of this field") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "enumType", mandatory = false, help = "The fetch semantics at a JPA level") final EnumType enumType,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final EnumField fieldDetails = new EnumField(physicalTypeIdentifier, fieldType, fieldName);
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (enumType != null) {
      fieldDetails.setEnumType(enumType);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = "java-number",
          help = "The Java type of the entity") JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
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
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to use a primitive type if possible") final boolean primitive,
      @CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint") final boolean unique,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, javaTypeDetails);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    if (primitive && legalNumericPrimitives.contains(fieldType.getFullyQualifiedTypeName())) {
      fieldType =
          new JavaType(fieldType.getFullyQualifiedTypeName(), 0, DataType.PRIMITIVE, null, null);
    }
    final NumericField fieldDetails =
        new NumericField(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (decimalMin != null) {
      fieldDetails.setDecimalMin(decimalMin);
    }
    if (decimalMax != null) {
      fieldDetails.setDecimalMax(decimalMax);
    }
    if (digitsInteger != null) {
      fieldDetails.setDigitsInteger(digitsInteger);
    }
    if (digitsFraction != null) {
      fieldDetails.setDigitsFraction(digitsFraction);
    }
    if (min != null) {
      fieldDetails.setMin(min);
    }
    if (max != null) {
      fieldDetails.setMax(max);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (unique) {
      fieldDetails.setUnique(true);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    Validate.isTrue(fieldDetails.isDigitsSetCorrectly(),
        "Must specify both --digitsInteger and --digitsFractional for @Digits to be added");

    insertField(fieldDetails, permitReservedWords, transientModifier);
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

  @CliOptionVisibilityIndicator(command = "field reference", params = {"cardinality"},
      help = "Option 'cardinality' is not available for this type of class")
  public boolean isCardinalityVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isCardinalityVisibleForFieldReference(shellContext);
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

  @CliOptionVisibilityIndicator(command = "field reference", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldReference(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldReference(shellContext);
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
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, optionContext = PROJECT,
          help = "The Java type of the entity to reference") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "joinColumnName", mandatory = true, help = "The JPA @JoinColumn name") final String joinColumnName,
      @CliOption(key = "referencedColumnName", mandatory = false,
          help = "The JPA @JoinColumn referencedColumnName") final String referencedColumnName,
      @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "MANY_TO_ONE",
          specifiedDefaultValue = "MANY_TO_ONE",
          help = "The relationship cardinality at a JPA level") final Cardinality cardinality,
      @CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "cascadeType",
          mandatory = false,
          specifiedDefaultValue = "ALL",
          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH and REMOVE.") final Cascade cascadeType,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
    Validate
        .notNull(cid,
            "The specified target '--type' does not exist or can not be found. Please create this type first.");

    final ClassOrInterfaceTypeDetails selfCid = typeLocationService.getTypeDetails(typeName);
    checkFieldExists(fieldName, shellContext, selfCid);

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);
    Validate
        .isTrue(
            entityAnnotation != null || persistentAnnotation != null,
            "The field reference command is only applicable to JPA @Entity or Spring Data @Persistent target types.");

    Validate.isTrue(
        cardinality == Cardinality.MANY_TO_ONE || cardinality == Cardinality.ONE_TO_ONE,
        "Cardinality must be MANY_TO_ONE or ONE_TO_ONE for the field reference command");

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final ReferenceField fieldDetails =
        new ReferenceField(physicalTypeIdentifier, fieldType, fieldName, cardinality, cascadeType);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (joinColumnName != null) {
      fieldDetails.setJoinColumnName(joinColumnName);
    }
    if (referencedColumnName != null) {
      Validate.notNull(joinColumnName,
          "@JoinColumn name is required if specifying a referencedColumnName");
      fieldDetails.setReferencedColumnName(referencedColumnName);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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

  @CliOptionVisibilityIndicator(command = "field set", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldSet(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldSet(shellContext);
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

  @CliCommand(
      value = "field set",
      help = "Adds a private Set field to an existing Java source file (eg the 'one' side of a many-to-one)")
  public void addFieldSetJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity which will be contained within the Set") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "mappedBy", mandatory = false,
          help = "The field name on the referenced type which owns the relationship") final JavaSymbolName mappedBy,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "sizeMin", mandatory = false,
          help = "The minimum number of elements in the collection") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false,
          help = "The maximum number of elements in the collection") final Integer sizeMax,
      @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level") Cardinality cardinality,
      @CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(key = "joinTable", mandatory = true,
          specifiedDefaultValue = ROO_DEFAULT_JOIN_TABLE_NAME,
          help = "Join table name. Most usually used in @ManyToMany relations. If "
              + "name not specified it will take default @JoinTable name value.") final String joinTable,
      @CliOption(key = "joinColumns", mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity owning the relation") final String joinColumns,
      @CliOption(key = "referencedColumns", mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity owning the relation") final String referencedColumns,
      @CliOption(key = "inverseJoinColumns", mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity that does not own the relation") final String inverseJoinColumns,
      @CliOption(key = "inverseReferencedColumns", mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity that does not own the relation") final String inverseReferencedColumns,
      @CliOption(
          key = "cascadeType",
          mandatory = false,
          unspecifiedDefaultValue = "ALL",
          specifiedDefaultValue = "ALL",
          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH and REMOVE.") final Cascade cascadeType,
      ShellContext shellContext) {

    // Check if joinTable must have a specified value.
    checkJoinTableNameMandatory(joinTable);

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
    Validate
        .notNull(cid,
            "The specified target '--type' does not exist or can not be found. Please create this type first.");

    final ClassOrInterfaceTypeDetails selfCid = typeLocationService.getTypeDetails(typeName);
    checkFieldExists(fieldName, shellContext, selfCid);

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);

    if (entityAnnotation != null) {
      Validate.isTrue(cardinality == Cardinality.ONE_TO_MANY
          || cardinality == Cardinality.MANY_TO_MANY,
          "Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field set command");
    } else if (cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
      cardinality = null;
    } else if (persistentAnnotation != null) {
      // Yes, we can deal with that
    } else {
      throw new IllegalStateException(
          "The field set command is only applicable to enum, JPA @Entity or Spring Data @Persistence elements");
    }

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s', doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final SetField fieldDetails =
        new SetField(physicalTypeIdentifier, new JavaType(SET.getFullyQualifiedTypeName(), 0,
            DataType.TYPE, null, Arrays.asList(fieldType)), fieldName, fieldType, cardinality,
            cascadeType);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (mappedBy != null) {
      fieldDetails.setMappedBy(mappedBy);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (joinTable != null) {

      // Create strings arrays and set @JoinTable annotation
      String[] joinColumnsArray = null;
      String[] referencedColumnsArray = null;
      String[] inverseJoinColumnsArray = null;
      String[] inverseReferencedColumnsArray = null;
      if (joinColumns != null) {
        joinColumnsArray = joinColumns.replace(" ", "").split(",");
      }
      if (referencedColumns != null) {
        referencedColumnsArray = referencedColumns.replace(" ", "").split(",");
      }
      if (inverseJoinColumns != null) {
        inverseJoinColumnsArray = inverseJoinColumns.replace(" ", "").split(",");
      }
      if (inverseReferencedColumns != null) {
        inverseReferencedColumnsArray = inverseReferencedColumns.replace(" ", "").split(",");
      }

      // Validate same number of elements
      if (joinColumnsArray != null && referencedColumnsArray != null) {
        Validate.isTrue(joinColumnsArray.length == referencedColumnsArray.length,
            "--joinColumns and --referencedColumns must have same number of column values");
      }
      if (inverseJoinColumnsArray != null && inverseReferencedColumnsArray != null) {
        Validate
            .isTrue(inverseJoinColumnsArray.length == inverseReferencedColumnsArray.length,
                "--inverseJoinColumns and --inverseReferencedColumns must have same number of column values");
      }

      fieldDetails.setJoinTableAnnotation(joinTable, joinColumnsArray, referencedColumnsArray,
          inverseJoinColumnsArray, inverseReferencedColumnsArray);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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

  @CliOptionVisibilityIndicator(command = "field list", params = {"transient"},
      help = "Option 'transient' is not available for this type of class")
  public boolean isTransientVisibleForFieldList(ShellContext shellContext) {
    JavaType type = getTypeFromCommand(shellContext);
    if (type != null) {
      return getFieldCreatorProvider(type).isTransientVisibleForFieldList(shellContext);
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

  @CliCommand(
      value = "field list",
      help = "Adds a private List field to an existing Java source file (eg the 'one' side of a many-to-one)")
  public void addFieldListJpa(
      @CliOption(key = {"", "fieldName"}, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true,
          help = "The entity which will be contained within the Set") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "mappedBy", mandatory = false,
          help = "The field name on the referenced type which owns the relationship") final JavaSymbolName mappedBy,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "sizeMin", mandatory = false,
          help = "The minimum number of elements in the collection") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false,
          help = "The maximum number of elements in the collection") final Integer sizeMax,
      @CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "ONE_TO_MANY",
          specifiedDefaultValue = "ONE_TO_MANY",
          help = "The relationship cardinality at a JPA level") Cardinality cardinality,
      @CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") final Fetch fetch,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(key = "joinTable", mandatory = true,
          specifiedDefaultValue = ROO_DEFAULT_JOIN_TABLE_NAME,
          help = "Join table name. Most usually used in @ManyToMany relations. If name "
              + "not specified it will take default @JoinTable name value.") final String joinTable,
      @CliOption(key = "joinColumns", mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity owning the relation") final String joinColumns,
      @CliOption(key = "referencedColumns", mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity owning the relation") final String referencedColumns,
      @CliOption(key = "inverseJoinColumns", mandatory = true,
          help = "Comma separated list of join table's foreign key columns which "
              + "references the table of the entity that does not own the relation") final String inverseJoinColumns,
      @CliOption(key = "inverseReferencedColumns", mandatory = true,
          help = "Comma separated list of foreign key referenced columns in the table "
              + "of the entity that does not own the relation") final String inverseReferencedColumns,
      @CliOption(key = "cascadeType", mandatory = false, unspecifiedDefaultValue = "ALL",
          specifiedDefaultValue = "ALL",
          help = "CascadeType. Possible values are ALL, DETACH, MERGE, PERSIST, REFRESH "
              + "and REMOVE.") final Cascade cascadeType, ShellContext shellContext) {

    // Check if joinTable must have a specified value.
    checkJoinTableNameMandatory(joinTable);

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
    Validate.notNull(cid,
        "The specified target '--type' does not exist or can not be found. Please "
            + "create this type first.");

    final ClassOrInterfaceTypeDetails selfCid = typeLocationService.getTypeDetails(typeName);
    checkFieldExists(fieldName, shellContext, selfCid);

    // Check if the requested entity is a JPA @Entity
    final MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);
    final AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
    final AnnotationMetadata persistentAnnotation = memberDetails.getAnnotation(PERSISTENT);

    if (entityAnnotation != null) {
      Validate.isTrue(cardinality == Cardinality.ONE_TO_MANY
          || cardinality == Cardinality.MANY_TO_MANY,
          "Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field list command");
    } else if (cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
      cardinality = null;
    } else if (persistentAnnotation != null) {
      // Yes, we can deal with that
    } else {
      throw new IllegalStateException(
          "The field list command is only applicable to enum, JPA @Entity or Spring "
              + "Data @Persistence elements");
    }

    final ClassOrInterfaceTypeDetails javaTypeDetails =
        typeLocationService.getTypeDetails(typeName);
    Validate.notNull(javaTypeDetails, "The type specified, '%s' doesn't exist", typeName);

    final String physicalTypeIdentifier = javaTypeDetails.getDeclaredByMetadataId();
    final ListField fieldDetails =
        new ListField(physicalTypeIdentifier, new JavaType(LIST.getFullyQualifiedTypeName(), 0,
            DataType.TYPE, null, Arrays.asList(fieldType)), fieldName, fieldType, cardinality,
            cascadeType);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (mappedBy != null) {
      fieldDetails.setMappedBy(mappedBy);
    }
    if (fetch != null) {
      fieldDetails.setFetch(fetch);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (joinTable != null) {

      // Create strings arrays and set @JoinTable annotation
      String[] joinColumnsArray = null;
      String[] referencedColumnsArray = null;
      String[] inverseJoinColumnsArray = null;
      String[] inverseReferencedColumnsArray = null;
      if (joinColumns != null) {
        joinColumnsArray = joinColumns.replace(" ", "").split(",");
      }
      if (referencedColumns != null) {
        referencedColumnsArray = referencedColumns.replace(" ", "").split(",");
      }
      if (inverseJoinColumns != null) {
        inverseJoinColumnsArray = inverseJoinColumns.replace(" ", "").split(",");
      }
      if (inverseReferencedColumns != null) {
        inverseReferencedColumnsArray = inverseReferencedColumns.replace(" ", "").split(",");
      }

      // Validate same number of elements
      if (joinColumnsArray != null && referencedColumnsArray != null) {
        Validate.isTrue(joinColumnsArray.length == referencedColumnsArray.length,
            "--joinColumns and --referencedColumns must have same number of column values");
      }
      if (inverseJoinColumnsArray != null && inverseReferencedColumnsArray != null) {
        Validate.isTrue(inverseJoinColumnsArray.length == inverseReferencedColumnsArray.length,
            "--inverseJoinColumns and --inverseReferencedColumns must have same "
                + "number of column values");
      }

      fieldDetails.setJoinTableAnnotation(joinTable, joinColumnsArray, referencedColumnsArray,
          inverseJoinColumnsArray, inverseReferencedColumnsArray);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
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
          + "the field to add") final JavaSymbolName fieldName,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "decimalMin", mandatory = false,
          help = "The BigDecimal string-based representation of the minimum value") final String decimalMin,
      @CliOption(key = "decimalMax", mandatory = false,
          help = "The BigDecimal string based representation of the maximum value") final String decimalMax,
      @CliOption(key = "sizeMin", mandatory = false, help = "The minimum string length") final Integer sizeMin,
      @CliOption(key = "sizeMax", mandatory = false, help = "The maximum string length") final Integer sizeMax,
      @CliOption(key = "regexp", mandatory = false,
          help = "The required regular expression pattern") final String regexp,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether to mark the field with a unique constraint") final boolean unique,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(key = "lob", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates that this field is a Large Object") final boolean lob,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final StringField fieldDetails = new StringField(physicalTypeIdentifier, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (decimalMin != null) {
      fieldDetails.setDecimalMin(decimalMin);
    }
    if (decimalMax != null) {
      fieldDetails.setDecimalMax(decimalMax);
    }
    if (sizeMin != null) {
      fieldDetails.setSizeMin(sizeMin);
    }
    if (sizeMax != null) {
      fieldDetails.setSizeMax(sizeMax);
    }
    if (regexp != null) {
      fieldDetails.setRegexp(regexp.replace("\\", "\\\\"));
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (unique) {
      fieldDetails.setUnique(true);
    }
    if (value != null) {
      fieldDetails.setValue(value);
    }

    if (lob) {
      fieldDetails.getInitedAnnotations().add(
          new AnnotationMetadataBuilder("javax.persistence.Lob"));

      // ROO-3722: Add LAZY load in @Lob fields using @Basic
      AnnotationMetadataBuilder basicAnnotation =
          new AnnotationMetadataBuilder("javax.persistence.Basic");
      basicAnnotation.addEnumAttribute("fetch", new EnumDetails(new JavaType(
          "javax.persistence.FetchType"), new JavaSymbolName("LAZY")));
      fieldDetails.getInitedAnnotations().add(basicAnnotation);
    }
    insertField(fieldDetails, permitReservedWords, transientModifier);
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
          help = "The name of the file upload field to add") final JavaSymbolName fieldName,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "contentType", mandatory = true, help = "The content type of the file") final UploadedFileContentType contentType,
      @CliOption(key = "autoUpload", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the file is uploaded automatically when selected") final boolean autoUpload,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final UploadedFileField fieldDetails =
        new UploadedFileField(physicalTypeIdentifier, fieldName, contentType);
    fieldDetails.setAutoUpload(autoUpload);
    fieldDetails.setNotNull(notNull);
    if (column != null) {
      fieldDetails.setColumn(column);
    }

    insertField(fieldDetails, permitReservedWords, false);
  }


  private void insertField(final FieldDetails fieldDetails, final boolean permitReservedWords,
      final boolean transientModifier) {

    String module = null;
    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
      if (fieldDetails.getColumn() != null) {
        ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getColumn());
      }
    }

    final List<AnnotationMetadataBuilder> annotations = fieldDetails.getInitedAnnotations();
    fieldDetails.decorateAnnotationsList(annotations);
    fieldDetails.setAnnotations(annotations);

    if (fieldDetails.getFieldType() != null) {
      module = fieldDetails.getFieldType().getModule();
    }

    String initializer = null;
    if (fieldDetails instanceof CollectionField) {
      final CollectionField collectionField = (CollectionField) fieldDetails;
      module = collectionField.getGenericParameterTypeName().getModule();
      initializer = "new " + collectionField.getInitializer() + "()";
    } else if (fieldDetails instanceof DateField
        && fieldDetails.getFieldName().getSymbolName().equals("created")) {
      initializer = "new Date()";
    }
    int modifier = Modifier.PRIVATE;
    if (transientModifier) {
      modifier += Modifier.TRANSIENT;
    }
    fieldDetails.setModifiers(modifier);

    // Format the passed-in comment (if given)
    formatFieldComment(fieldDetails);

    final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    fieldBuilder.setFieldInitializer(initializer);
    typeManagementService.addField(fieldBuilder.build());

    if (module != null) {
      projectOperations.addModuleDependency(module);
    }
  }

  private void formatFieldComment(FieldDetails fieldDetails) {
    // If a comment was defined, we need to format it
    if (fieldDetails.getComment() != null) {

      // First replace all "" with the proper escape sequence \"
      String unescapedMultiLineComment = fieldDetails.getComment().replaceAll("\"\"", "\\\\\"");

      // Then unescape all characters
      unescapedMultiLineComment = StringEscapeUtils.unescapeJava(unescapedMultiLineComment);

      CommentFormatter commentFormatter = new CommentFormatter();
      String javadocComment = commentFormatter.formatStringAsJavadoc(unescapedMultiLineComment);

      fieldDetails.setComment(commentFormatter.format(javadocComment, 1));
    }
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
      @CliOption(key = "fieldName", mandatory = true, help = "The name of the field") final JavaSymbolName fieldName,
      @CliOption(key = "type", mandatory = true, help = "The Java type of this field") final JavaType fieldType,
      @CliOption(key = "class", mandatory = true, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT, help = "The name of the class to receive this field") final JavaType typeName,
      @CliOption(key = "notNull", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value cannot be null") final boolean notNull,
      @CliOption(key = "nullRequired", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Whether this value must be null") final boolean nullRequired,
      @CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
      @CliOption(key = "column", mandatory = true, help = "The JPA @Column name") final String column,
      @CliOption(key = "value", mandatory = false,
          help = "Inserts an optional Spring @Value annotation with the given content") final String value,
      @CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      ShellContext shellContext) {

    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(typeName);
    Validate.notNull(cid, "The type specified, '%s', doesn't exist", typeName);

    checkFieldExists(fieldName, shellContext, cid);

    final String physicalTypeIdentifier = cid.getDeclaredByMetadataId();
    final FieldDetails fieldDetails =
        new FieldDetails(physicalTypeIdentifier, fieldType, fieldName);
    fieldDetails.setNotNull(notNull);
    fieldDetails.setNullRequired(nullRequired);
    if (comment != null) {
      fieldDetails.setComment(comment);
    }
    if (column != null) {
      fieldDetails.setColumn(column);
    }

    insertField(fieldDetails, permitReservedWords, transientModifier);
  }

  @CliAvailabilityIndicator({"field other", "field number", "field string", "field date",
      "field boolean", "field enum", "field embedded", "field file", "field reference",
      "field set", "field list"})
  public boolean isFieldManagementAvailable() {
    return getFieldCreatorAvailable();
  }

  /**
   * Checks if entity has already a field with the same name and throws an exception 
   * in that case.
   * 
   * @param fieldName
   * @param shellContext
   * @param javaTypeDetails
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
   * Checks if exists some project setting that makes @JoinTable name mandatory. Throws 
   * an exception with a message if exists property {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} 
   * on project settings and its value is "true". Else, continue.
   * 
   * @param joinTable
   */
  private void checkJoinTableNameMandatory(String joinTable) {
    if (joinTable == null) {
      return;
    } else if (ROO_DEFAULT_JOIN_TABLE_NAME.equals(joinTable)) {

      // Check if property 'spring.roo.jpa.require.schema-object-name' is defined on project settings
      String requiredSchemaObjectName =
          projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

      if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
        throw new IllegalArgumentException(
            "It is mandatory to assign a specific table name for --joinTable. Please, "
                + "assign it a table name.");
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
}
