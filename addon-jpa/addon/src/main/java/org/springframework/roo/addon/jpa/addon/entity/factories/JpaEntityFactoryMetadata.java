package org.springframework.roo.addon.jpa.addon.entity.factories;

import static org.springframework.roo.model.HibernateJavaType.VALIDATOR_CONSTRAINTS_EMAIL;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BIG_INTEGER;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.GREGORIAN_CALENDAR;
import static org.springframework.roo.model.JdkJavaType.TIMESTAMP;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMN;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.DIGITS;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.roo.addon.jpa.addon.dod.EmbeddedHolder;
import org.springframework.roo.addon.jpa.addon.dod.EmbeddedIdHolder;
import org.springframework.roo.addon.jpa.annotations.entity.factory.RooJpaEntityFactory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaEntityFactory}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public class JpaEntityFactoryMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String INDEX_VAR = "index";
  private static final JavaSymbolName INDEX_SYMBOL = new JavaSymbolName(INDEX_VAR);
  private static final JavaSymbolName MAX_SYMBOL = new JavaSymbolName("max");
  private static final JavaSymbolName MIN_SYMBOL = new JavaSymbolName("min");
  private static final String OBJ_VAR = "obj";
  private static final JavaSymbolName OBJ_SYMBOL = new JavaSymbolName(OBJ_VAR);
  private static final JavaSymbolName VALUE = new JavaSymbolName("value");
  private static final JavaSymbolName CREATE_FACTORY_METHOD_NAME = new JavaSymbolName("create");

  public static final JavaSymbolName SPECIFIC_METHOD_PREFIX = new JavaSymbolName("getSpecific");

  private static final String PROVIDES_TYPE_STRING = JpaEntityFactoryMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  private JavaType entity;
  private final List<JavaType> requiredDataOnDemandCollaborators = new ArrayList<JavaType>();
  private MethodMetadata randomPersistentEntityMethod;
  private final Map<FieldMetadata, Map<FieldMetadata, String>> embeddedFieldInitializers =
      new LinkedHashMap<FieldMetadata, Map<FieldMetadata, String>>();
  private final Map<FieldMetadata, String> fieldInitializers =
      new LinkedHashMap<FieldMetadata, String>();
  private EmbeddedIdHolder embeddedIdHolder;
  private List<EmbeddedHolder> embeddedHolders;
  private Map<FieldMetadata, JpaEntityFactoryMetadata> locatedFields;

  /**
   * Constructor
   *
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param entity
   * @param locatedFields 
   * @param embeddedHolder
   * @param entityFactoryClasses 
   * @param embeddedIdHolder 
   */
  public JpaEntityFactoryMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final Map<FieldMetadata, JpaEntityFactoryMetadata> locatedFields,
      final List<EmbeddedHolder> embeddedHolders,
      Set<ClassOrInterfaceTypeDetails> entityFactoryClasses, final EmbeddedIdHolder embeddedIdHolder) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(locatedFields, "Located fields map required");
    Validate.notNull(embeddedHolders, "Embedded holders list required");

    this.entity = entity;
    this.embeddedIdHolder = embeddedIdHolder;
    this.embeddedHolders = embeddedHolders;
    this.locatedFields = locatedFields;

    builder.addMethod(getCreateMethod());

    // Calculate and store field initializers
    for (final Map.Entry<FieldMetadata, JpaEntityFactoryMetadata> entry : locatedFields.entrySet()) {
      final FieldMetadata field = entry.getKey();
      final String initializer = getFieldInitializer(field, entry.getValue(), entityFactoryClasses);
      if (!StringUtils.isBlank(initializer)) {
        this.fieldInitializers.put(field, initializer);
      }
    }
    for (final EmbeddedHolder embeddedHolder : embeddedHolders) {
      final Map<FieldMetadata, String> initializers = new LinkedHashMap<FieldMetadata, String>();
      for (final FieldMetadata field : embeddedHolder.getFields()) {
        initializers.put(field, getFieldInitializer(field, null, entityFactoryClasses));
      }
      this.embeddedFieldInitializers.put(embeddedHolder.getEmbeddedField(), initializers);
    }

    for (final EmbeddedHolder embeddedHolder : embeddedHolders) {
      builder.addMethod(getEmbeddedClassMutatorMethod(embeddedHolder));
      addEmbeddedClassFieldMutatorMethodsToBuilder(embeddedHolder, entityFactoryClasses);
    }

    for (final MethodMetadataBuilder fieldInitializerMethod : getFieldMutatorMethods()) {
      builder.addMethod(fieldInitializerMethod);
    }

    addCollaboratingFieldsToBuilder(entityFactoryClasses);

    builder.addMethod(getEmbeddedIdMutatorMethod(entityFactoryClasses));

    itdTypeDetails = builder.build();
  }

  private void addCollaboratingFieldsToBuilder(
      final Set<ClassOrInterfaceTypeDetails> entityFactoryClasses) {
    final Set<JavaSymbolName> fields = new LinkedHashSet<JavaSymbolName>();
    for (final JavaType entityNeedingCollaborator : this.requiredDataOnDemandCollaborators) {
      final JavaType collaboratorType =
          getCollaboratingType(entityNeedingCollaborator, entityFactoryClasses);
      if (collaboratorType != null) {
        final String collaboratingFieldName =
            getCollaboratingFieldName(entityNeedingCollaborator, entityFactoryClasses)
                .getSymbolName();

        final JavaSymbolName fieldSymbolName = new JavaSymbolName(collaboratingFieldName);
        final FieldMetadata candidate = governorTypeDetails.getField(fieldSymbolName);
        if (candidate != null) {
          // We really expect the field to be correct if we're going to
          // rely on it
          Validate
              .isTrue(candidate.getFieldType().equals(collaboratorType),
                  "Field '%s' on '%s' must be of type '%s'", collaboratingFieldName,
                  destination.getFullyQualifiedTypeName(),
                  collaboratorType.getFullyQualifiedTypeName());
          Validate.isTrue(Modifier.isPrivate(candidate.getModifier()),
              "Field '%s' on '%s' must be private", collaboratingFieldName,
              destination.getFullyQualifiedTypeName());
          Validate.notNull(
              MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), AUTOWIRED),
              "Field '%s' on '%s' must be @Autowired", collaboratingFieldName,
              destination.getFullyQualifiedTypeName());
          // It's ok, so we can move onto the new field
          continue;
        }

        // Create field and add it to the ITD, if it hasn't already been
        if (!fields.contains(fieldSymbolName)) {
          // Must make the field
          final List<AnnotationMetadataBuilder> annotations =
              new ArrayList<AnnotationMetadataBuilder>();
          annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
          builder.addField(new FieldMetadataBuilder(getId(), 0, annotations, fieldSymbolName,
              collaboratorType));
          fields.add(fieldSymbolName);
        }
      }
    }
  }

  private void addEmbeddedClassFieldMutatorMethodsToBuilder(final EmbeddedHolder embeddedHolder,
      final Set<ClassOrInterfaceTypeDetails> dataOnDemandClasses) {
    final JavaType embeddedFieldType = embeddedHolder.getEmbeddedField().getFieldType();
    final JavaType[] parameterTypes = {embeddedFieldType, JavaType.INT_PRIMITIVE};
    final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL, INDEX_SYMBOL);

    for (final FieldMetadata field : embeddedHolder.getFields()) {
      final String initializer = getFieldInitializer(field, null, dataOnDemandClasses);
      final JavaSymbolName fieldMutatorMethodName =
          BeanInfoUtils.getMutatorMethodName(field.getFieldName());

      final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
      bodyBuilder.append(getFieldValidationBody(field, initializer, fieldMutatorMethodName, false));

      final JavaSymbolName embeddedClassMethodName =
          getEmbeddedFieldMutatorMethodName(embeddedHolder.getEmbeddedField().getFieldName(),
              field.getFieldName());
      if (governorHasMethod(embeddedClassMethodName, parameterTypes)) {
        // Method found in governor so do not create method in ITD
        continue;
      }

      builder.addMethod(new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
          embeddedClassMethodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType
              .convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder));
    }
  }

  private String getColumnPrecisionAndScaleBody(final FieldMetadata field,
      final Map<String, Object> values, final String suffix) {
    if (values == null || !values.containsKey("precision")) {
      return InvocableMemberBodyBuilder.getInstance().getOutput();
    }

    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();

    Integer precision = (Integer) values.get("precision");
    Integer scale = (Integer) values.get("scale");
    if (precision != null && scale != null && precision < scale) {
      scale = 0;
    }

    final BigDecimal maxValue;
    if (scale == null || scale == 0) {
      maxValue = new BigDecimal(StringUtils.rightPad("9", precision, '9'));
    } else {
      maxValue =
          new BigDecimal(StringUtils.rightPad("9", precision - scale, '9') + "."
              + StringUtils.rightPad("9", scale, '9'));
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    if (fieldType.equals(BIG_DECIMAL)) {
      bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
          + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\""
          + maxValue + "\");");
    } else {
      bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix
          + ") {");
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
    }

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    return bodyBuilder.getOutput();
  }

  private JavaSymbolName getCollaboratingFieldName(final JavaType entity,
      final Set<ClassOrInterfaceTypeDetails> dataOnDemandClasses) {
    JavaSymbolName symbolName = null;
    JavaType collaboratingType = getCollaboratingType(entity, dataOnDemandClasses);
    if (collaboratingType != null) {
      symbolName =
          new JavaSymbolName(StringUtils.uncapitalize(collaboratingType.getSimpleTypeName()));
    }
    return symbolName;
  }

  private JavaType getCollaboratingType(final JavaType entity,
      final Set<ClassOrInterfaceTypeDetails> entityFactoryClasses) {
    JavaType dataOnDemand = null;
    for (ClassOrInterfaceTypeDetails dataOnDemandClass : entityFactoryClasses) {
      String searchDataOnDemand = entity.getSimpleTypeName().concat("DataOnDemand");
      if (dataOnDemandClass.getType().getSimpleTypeName().equals(searchDataOnDemand)
          && governorTypeDetails.getType().getModule()
              .equals(dataOnDemandClass.getType().getModule())) {
        dataOnDemand = dataOnDemandClass.getType();
      }
    }
    return dataOnDemand;
  }

  private MethodMetadata getCreateMethod() {

    // Define methodName
    final JavaSymbolName methodName = CREATE_FACTORY_METHOD_NAME;

    List<JavaType> parameterTypes = new ArrayList<JavaType>();
    parameterTypes.add(JavaType.INT_PRIMITIVE);

    // Check if method exists
    MethodMetadata existingMethod = getGovernorMethod(methodName, parameterTypes);
    if (existingMethod != null) {
      return existingMethod;
    }

    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(INDEX_SYMBOL);

    // Add body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Entity obj = new Entity();
    bodyBuilder.appendFormalLine("%1$s %2$s = new %1$s();", getNameOfJavaType(this.entity),
        OBJ_SYMBOL);

    // Set values for transient object
    for (final Map.Entry<FieldMetadata, JpaEntityFactoryMetadata> entry : locatedFields.entrySet()) {
      bodyBuilder.appendFormalLine("%s(%s, %s);",
          BeanInfoUtils.getMutatorMethodName(entry.getKey()), OBJ_SYMBOL, INDEX_SYMBOL);
    }

    // return obj;
    bodyBuilder.appendFormalLine("return %s;", OBJ_SYMBOL);

    // Create method
    MethodMetadataBuilder method =
        new MethodMetadataBuilder(this.getId(), Modifier.PUBLIC, methodName, this.entity,
            AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);

    CommentStructure commentStructure = new CommentStructure();
    List<String> paramsInfo = new ArrayList<String>();
    paramsInfo.add(String.format("%s position of the %s", INDEX_VAR,
        this.entity.getSimpleTypeName()));
    JavadocComment comment =
        new JavadocComment(String.format("Creates a new {@link %s} with the given %s.",
            this.entity.getSimpleTypeName(), INDEX_VAR), paramsInfo, String.format(
            "a new transient %s", this.entity.getSimpleTypeName()), null);
    commentStructure.addComment(comment, CommentLocation.BEGINNING);
    method.setCommentStructure(commentStructure);

    return method.build();
  }

  private String getDecimalMinAndDecimalMaxBody(final FieldMetadata field,
      final AnnotationMetadata decimalMinAnnotation, final AnnotationMetadata decimalMaxAnnotation,
      final String suffix) {
    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    if (decimalMinAnnotation != null && decimalMaxAnnotation == null) {
      final String minValue = (String) decimalMinAnnotation.getAttribute(VALUE).getValue();

      if (fieldType.equals(BIG_DECIMAL)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue + "\")) == -1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName()
            + "(\"" + minValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    } else if (decimalMinAnnotation == null && decimalMaxAnnotation != null) {
      final String maxValue = (String) decimalMaxAnnotation.getAttribute(VALUE).getValue();

      if (fieldType.equals(BIG_DECIMAL)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName()
            + "(\"" + maxValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    } else if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
      final String minValue = (String) decimalMinAnnotation.getAttribute(VALUE).getValue();
      final String maxValue = (String) decimalMaxAnnotation.getAttribute(VALUE).getValue();
      Validate
          .isTrue(
              Double.parseDouble(maxValue) >= Double.parseDouble(minValue),
              "The value of @DecimalMax must be greater or equal to the value of @DecimalMin for field %s",
              fieldName);

      if (fieldType.equals(BIG_DECIMAL)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName
            + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
            + "\")) == 1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName()
            + "(\"" + maxValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || "
            + fieldName + " > " + maxValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    }

    return bodyBuilder.getOutput();
  }

  private String getDigitsBody(final FieldMetadata field,
      final AnnotationMetadata digitsAnnotation, final String suffix) {
    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();

    final Integer integerValue =
        (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("integer")).getValue();
    final Integer fractionValue =
        (Integer) digitsAnnotation.getAttribute(new JavaSymbolName("fraction")).getValue();
    final BigDecimal maxValue =
        new BigDecimal(StringUtils.rightPad("9", integerValue, '9') + "."
            + StringUtils.rightPad("9", fractionValue, '9'));

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    if (fieldType.equals(BIG_DECIMAL)) {
      bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
          + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_DECIMAL.getSimpleTypeName() + "(\""
          + maxValue + "\");");
    } else {
      bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue.doubleValue() + suffix
          + ") {");
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine(fieldName + " = " + maxValue.doubleValue() + suffix + ";");
    }

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    return bodyBuilder.getOutput();
  }

  private String getFieldInitializer(final FieldMetadata field,
      final JpaEntityFactoryMetadata collaboratingMetadata,
      final Set<ClassOrInterfaceTypeDetails> dataOnDemandClasses) {
    final JavaType fieldType = field.getFieldType();
    final String fieldName = field.getFieldName().getSymbolName();
    String initializer = "null";
    final String fieldInitializer = field.getFieldInitializer();
    final Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

    // Date fields included for DataNucleus (
    if (fieldType.equals(DATE)) {
      if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PAST) != null) {
        builder.getImportRegistrationResolver().addImport(DATE);
        initializer = "new Date(new Date().getTime() - 10000000L)";
      } else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), FUTURE) != null) {
        builder.getImportRegistrationResolver().addImport(DATE);
        initializer = "new Date(new Date().getTime() + 10000000L)";
      } else {
        builder.getImportRegistrationResolver().addImports(CALENDAR, GREGORIAN_CALENDAR);
        initializer =
            "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), \n\t\t\tCalendar.getInstance().get(Calendar.MONTH), \n\t\t\tCalendar.getInstance().get(Calendar.DAY_OF_MONTH), \n\t\t\tCalendar.getInstance().get(Calendar.HOUR_OF_DAY), \n\t\t\tCalendar.getInstance().get(Calendar.MINUTE), \n\t\t\tCalendar.getInstance().get(Calendar.SECOND) + \n\t\t\tnew Double(Math.random() * 1000).intValue()).getTime()";
      }
    } else if (fieldType.equals(CALENDAR)) {
      builder.getImportRegistrationResolver().addImports(CALENDAR, GREGORIAN_CALENDAR);

      final String calendarString =
          "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), \n\t\t\tCalendar.getInstance().get(Calendar.MONTH), \n\t\t\tCalendar.getInstance().get(Calendar.DAY_OF_MONTH)";
      if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PAST) != null) {
        initializer = calendarString + " - 1)";
      } else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), FUTURE) != null) {
        initializer = calendarString + " + 1)";
      } else {
        initializer = "Calendar.getInstance()";
      }
    } else if (fieldType.equals(TIMESTAMP)) {
      builder.getImportRegistrationResolver().addImports(CALENDAR, GREGORIAN_CALENDAR, TIMESTAMP);
      initializer =
          "new Timestamp(new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), \n\t\t\tCalendar.getInstance().get(Calendar.MONTH), \n\t\t\tCalendar.getInstance().get(Calendar.DAY_OF_MONTH), \n\t\t\tCalendar.getInstance().get(Calendar.HOUR_OF_DAY), \n\t\t\tCalendar.getInstance().get(Calendar.MINUTE), \n\t\t\tCalendar.getInstance().get(Calendar.SECOND) + \n\t\t\tnew Double(Math.random() * 1000).intValue()).getTime().getTime())";
    } else if (fieldType.equals(STRING)) {
      if (fieldInitializer != null && fieldInitializer.contains("\"")) {
        final int offset = fieldInitializer.indexOf("\"");
        initializer = fieldInitializer.substring(offset + 1, fieldInitializer.lastIndexOf("\""));
      } else {
        initializer = fieldName;
      }

      if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
          VALIDATOR_CONSTRAINTS_EMAIL) != null || fieldName.toLowerCase().contains("email")) {
        initializer = "\"foo\" + " + INDEX_VAR + " + \"@bar.com\"";
      } else {
        int maxLength = Integer.MAX_VALUE;

        // Check for @Size
        final AnnotationMetadata sizeAnnotation =
            MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
        if (sizeAnnotation != null) {
          final AnnotationAttributeValue<?> maxValue = sizeAnnotation.getAttribute(MAX_SYMBOL);
          if (maxValue != null) {
            validateNumericAnnotationAttribute(fieldName, "@Size", "max", maxValue.getValue());
            maxLength = ((Integer) maxValue.getValue()).intValue();
          }
          final AnnotationAttributeValue<?> minValue = sizeAnnotation.getAttribute(MIN_SYMBOL);
          if (minValue != null) {
            validateNumericAnnotationAttribute(fieldName, "@Size", "min", minValue.getValue());
            final int minLength = ((Integer) minValue.getValue()).intValue();
            Validate.isTrue(maxLength >= minLength,
                "@Size attribute 'max' must be greater than 'min' for field '%s' in %s", fieldName,
                entity.getFullyQualifiedTypeName());
            if (initializer.length() + 2 < minLength) {
              initializer =
                  String.format("%1$-" + (minLength - 2) + "s", initializer).replace(' ', 'x');
            }
          }
        } else {
          if (field.getCustomData().keySet().contains(CustomDataKeys.COLUMN_FIELD)) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> columnValues =
                (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
            if (columnValues.keySet().contains("length")) {
              final Object lengthValue = columnValues.get("length");
              validateNumericAnnotationAttribute(fieldName, "@Column", "length", lengthValue);
              maxLength = ((Integer) lengthValue).intValue();
            }
          }
        }

        switch (maxLength) {
          case 0:
            initializer = "\"\"";
            break;
          case 1:
            initializer = "String.valueOf(" + INDEX_VAR + ")";
            break;
          case 2:
            initializer = "\"" + initializer.charAt(0) + "\" + " + INDEX_VAR;
            break;
          default:
            if (initializer.length() + 2 > maxLength) {
              initializer = "\"" + initializer.substring(0, maxLength - 2) + "_\" + " + INDEX_VAR;
            } else {
              initializer = "\"" + initializer + "_\" + " + INDEX_VAR;
            }
        }
      }
    } else if (fieldType.equals(new JavaType(STRING.getFullyQualifiedTypeName(), 1, DataType.TYPE,
        null, null))) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ \"Y\", \"N\" }");
    } else if (fieldType.equals(JavaType.BOOLEAN_OBJECT)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "Boolean.TRUE");
    } else if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "true");
    } else if (fieldType.equals(JavaType.INT_OBJECT)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR + ")");
    } else if (fieldType.equals(JavaType.INT_PRIMITIVE)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, INDEX_VAR);
    } else if (fieldType.equals(new JavaType(JavaType.INT_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "{ " + INDEX_VAR + ", " + INDEX_VAR + " }");
    } else if (fieldType.equals(JavaType.DOUBLE_OBJECT)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").doubleValue()"); // Auto-boxed
    } else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").doubleValue()");
    } else if (fieldType.equals(new JavaType(JavaType.DOUBLE_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(" + INDEX_VAR
              + ").doubleValue(), new Integer(" + INDEX_VAR + ").doubleValue() }");
    } else if (fieldType.equals(JavaType.FLOAT_OBJECT)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").floatValue()"); // Auto-boxed
    } else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").floatValue()");
    } else if (fieldType.equals(new JavaType(JavaType.FLOAT_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(" + INDEX_VAR
              + ").floatValue(), new Integer(" + INDEX_VAR + ").floatValue() }");
    } else if (fieldType.equals(JavaType.LONG_OBJECT)) {
      initializer =
          StringUtils
              .defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR + ").longValue()"); // Auto-boxed
    } else if (fieldType.equals(JavaType.LONG_PRIMITIVE)) {
      initializer =
          StringUtils
              .defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR + ").longValue()");
    } else if (fieldType.equals(new JavaType(JavaType.LONG_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(" + INDEX_VAR
              + ").longValue(), new Integer(" + INDEX_VAR + ").longValue() }");
    } else if (fieldType.equals(JavaType.SHORT_OBJECT)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").shortValue()"); // Auto-boxed
    } else if (fieldType.equals(JavaType.SHORT_PRIMITIVE)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "new Integer(" + INDEX_VAR
              + ").shortValue()");
    } else if (fieldType.equals(new JavaType(JavaType.SHORT_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "{ new Integer(" + INDEX_VAR
              + ").shortValue(), new Integer(" + INDEX_VAR + ").shortValue() }");
    } else if (fieldType.equals(JavaType.CHAR_OBJECT)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "new Character('N')");
    } else if (fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "'N'");
    } else if (fieldType.equals(new JavaType(JavaType.CHAR_OBJECT.getFullyQualifiedTypeName(), 1,
        DataType.PRIMITIVE, null, null))) {
      initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ 'Y', 'N' }");
    } else if (fieldType.equals(BIG_DECIMAL)) {
      builder.getImportRegistrationResolver().addImport(BIG_DECIMAL);
      initializer = BIG_DECIMAL.getSimpleTypeName() + ".valueOf(" + INDEX_VAR + ")";
    } else if (fieldType.equals(BIG_INTEGER)) {
      builder.getImportRegistrationResolver().addImport(BIG_INTEGER);
      initializer = BIG_INTEGER.getSimpleTypeName() + ".valueOf(" + INDEX_VAR + ")";
    } else if (fieldType.equals(JavaType.BYTE_OBJECT)) {
      initializer = "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ")";
    } else if (fieldType.equals(JavaType.BYTE_PRIMITIVE)) {
      initializer =
          "new Byte(" + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"") + ").byteValue()";
    } else if (fieldType.equals(JavaType.BYTE_ARRAY_PRIMITIVE)) {
      initializer =
          StringUtils.defaultIfEmpty(fieldInitializer, "String.valueOf(" + INDEX_VAR
              + ").getBytes()");
    } else if (fieldType.equals(entity)) {
      // Avoid circular references (ROO-562)
      initializer = OBJ_VAR;
    } else if (fieldCustomDataKeys.contains(CustomDataKeys.ENUMERATED_FIELD)) {
      builder.getImportRegistrationResolver().addImport(fieldType);
      initializer = fieldType.getSimpleTypeName() + ".class.getEnumConstants()[0]";
    } else if (collaboratingMetadata != null && collaboratingMetadata.getEntityType() != null) {
      requiredDataOnDemandCollaborators.add(fieldType);
      initializer =
          getFieldInitializerForRelatedEntity(field, collaboratingMetadata, fieldCustomDataKeys,
              dataOnDemandClasses);
    }

    return initializer;
  }

  private String getFieldInitializerForRelatedEntity(final FieldMetadata field,
      final JpaEntityFactoryMetadata collaboratingMetadata, final Set<?> fieldCustomDataKeys,
      final Set<ClassOrInterfaceTypeDetails> dataOnDemandClasses) {
    // To avoid circular references, we don't try to set nullable fields
    final boolean nullableField =
        field.getAnnotation(NOT_NULL) == null && isNullableJoinColumn(field);
    if (nullableField) {
      return null;
    }
    JavaSymbolName collaboratingFieldName =
        getCollaboratingFieldName(field.getFieldType(), dataOnDemandClasses);
    if (collaboratingFieldName != null) {
      final String collaboratingName = collaboratingFieldName.getSymbolName();
      if (fieldCustomDataKeys.contains(CustomDataKeys.ONE_TO_ONE_FIELD)) {
        // We try to keep the same ID (ROO-568)
        return collaboratingName + "." + SPECIFIC_METHOD_PREFIX.getSymbolName() + "(" + INDEX_VAR
            + ")";
      }
      return collaboratingName + "."
          + collaboratingMetadata.getRandomPersistentEntityMethod().getMethodName().getSymbolName()
          + "()";
    } else {
      return null;
    }

  }

  private List<MethodMetadataBuilder> getFieldMutatorMethods() {
    final List<MethodMetadataBuilder> fieldMutatorMethods = new ArrayList<MethodMetadataBuilder>();
    final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL, INDEX_SYMBOL);
    final JavaType[] parameterTypes = {entity, JavaType.INT_PRIMITIVE};

    for (final Map.Entry<FieldMetadata, String> entry : fieldInitializers.entrySet()) {
      final FieldMetadata field = entry.getKey();
      final JavaSymbolName mutatorName = BeanInfoUtils.getMutatorMethodName(field.getFieldName());

      // Locate user-defined method
      if (governorHasMethod(mutatorName, parameterTypes)) {
        // Method found in governor so do not create method in ITD
        continue;
      }

      // Method not on governor so need to create it
      final String initializer = entry.getValue();
      if (!StringUtils.isBlank(initializer)) {
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.append(getFieldValidationBody(field, initializer, mutatorName, false));

        fieldMutatorMethods.add(new MethodMetadataBuilder(getId(), Modifier.PUBLIC, mutatorName,
            JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
            parameterNames, bodyBuilder));
      }
    }

    return fieldMutatorMethods;
  }

  private String getFieldValidationBody(final FieldMetadata field, final String initializer,
      final JavaSymbolName mutatorName, final boolean isFieldOfEmbeddableType) {
    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();

    String suffix = "";
    if (fieldType.equals(JavaType.LONG_OBJECT) || fieldType.equals(JavaType.LONG_PRIMITIVE)) {
      suffix = "L";
    } else if (fieldType.equals(JavaType.FLOAT_OBJECT)
        || fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
      suffix = "F";
    } else if (fieldType.equals(JavaType.DOUBLE_OBJECT)
        || fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
      suffix = "D";
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(getTypeStr(fieldType) + " " + fieldName + " = " + initializer
        + ";");

    if (fieldType.equals(JavaType.STRING)) {
      boolean isUnique = isFieldOfEmbeddableType;
      @SuppressWarnings("unchecked")
      final Map<String, Object> values =
          (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
      if (!isUnique && values != null && values.containsKey("unique")) {
        isUnique = (Boolean) values.get("unique");
      }

      // Check for @Size or @Column with length attribute
      final AnnotationMetadata sizeAnnotation =
          MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), SIZE);
      if (sizeAnnotation != null && sizeAnnotation.getAttribute(MAX_SYMBOL) != null) {
        final Integer maxValue = (Integer) sizeAnnotation.getAttribute(MAX_SYMBOL).getValue();
        bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + maxValue + ") {");
        bodyBuilder.indent();
        if (isUnique) {
          bodyBuilder.appendFormalLine(fieldName + " = new Random().nextInt(10) + " + fieldName
              + ".substring(1, " + maxValue + ");");
        } else {
          bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, " + maxValue
              + ");");
        }
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
      } else if (sizeAnnotation == null && values != null) {
        if (values.containsKey("length")) {
          final Integer lengthValue = (Integer) values.get("length");
          bodyBuilder.appendFormalLine("if (" + fieldName + ".length() > " + lengthValue + ") {");
          bodyBuilder.indent();
          if (isUnique) {
            bodyBuilder.appendFormalLine(fieldName + " = new Random().nextInt(10) + " + fieldName
                + ".substring(1, " + lengthValue + ");");
          } else {
            bodyBuilder.appendFormalLine(fieldName + " = " + fieldName + ".substring(0, "
                + lengthValue + ");");
          }
          bodyBuilder.indentRemove();
          bodyBuilder.appendFormalLine("}");
        }
      }
    } else if (JdkJavaType.isDecimalType(fieldType)) {
      // Check for @Digits, @DecimalMax, @DecimalMin
      final AnnotationMetadata digitsAnnotation =
          MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DIGITS);
      final AnnotationMetadata decimalMinAnnotation =
          MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DECIMAL_MIN);
      final AnnotationMetadata decimalMaxAnnotation =
          MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), DECIMAL_MAX);

      if (digitsAnnotation != null) {
        bodyBuilder.append(getDigitsBody(field, digitsAnnotation, suffix));
      } else if (decimalMinAnnotation != null || decimalMaxAnnotation != null) {
        bodyBuilder.append(getDecimalMinAndDecimalMaxBody(field, decimalMinAnnotation,
            decimalMaxAnnotation, suffix));
      } else if (field.getCustomData().keySet().contains(CustomDataKeys.COLUMN_FIELD)) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> values =
            (Map<String, Object>) field.getCustomData().get(CustomDataKeys.COLUMN_FIELD);
        bodyBuilder.append(getColumnPrecisionAndScaleBody(field, values, suffix));
      }
    } else if (JdkJavaType.isIntegerType(fieldType)) {
      // Check for @Min and @Max
      bodyBuilder.append(getMinAndMaxBody(field, suffix));
    }

    if (mutatorName != null) {
      bodyBuilder.appendFormalLine(OBJ_VAR + "." + mutatorName.getSymbolName() + "(" + fieldName
          + ");");
    }

    return bodyBuilder.getOutput();
  }

  private MethodMetadataBuilder getEmbeddedClassMutatorMethod(final EmbeddedHolder embeddedHolder) {
    final JavaSymbolName methodName =
        getEmbeddedFieldMutatorMethodName(embeddedHolder.getEmbeddedField().getFieldName());
    final JavaType[] parameterTypes = {entity, JavaType.INT_PRIMITIVE};

    // Locate user-defined method
    if (governorHasMethod(methodName, parameterTypes)) {
      // Method found in governor so do not create method in ITD
      return null;
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Create constructor for embedded class
    final JavaType embeddedFieldType = embeddedHolder.getEmbeddedField().getFieldType();
    builder.getImportRegistrationResolver().addImport(embeddedFieldType);
    bodyBuilder.appendFormalLine(embeddedFieldType.getSimpleTypeName() + " embeddedClass = new "
        + embeddedFieldType.getSimpleTypeName() + "();");
    for (final FieldMetadata field : embeddedHolder.getFields()) {
      bodyBuilder.appendFormalLine(getEmbeddedFieldMutatorMethodName(
          embeddedHolder.getEmbeddedField().getFieldName(), field.getFieldName()).getSymbolName()
          + "(embeddedClass, " + INDEX_VAR + ");");
    }
    bodyBuilder.appendFormalLine(OBJ_VAR + "." + embeddedHolder.getEmbeddedMutatorMethodName()
        + "(embeddedClass);");

    final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL, INDEX_SYMBOL);

    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
        AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
  }

  private MethodMetadataBuilder getEmbeddedIdMutatorMethod(
      final Set<ClassOrInterfaceTypeDetails> dataOnDemandClasses) {
    if (!hasEmbeddedIdentifier()) {
      return null;
    }

    final JavaSymbolName embeddedIdMutator = embeddedIdHolder.getEmbeddedIdMutator();
    final JavaSymbolName methodName = getEmbeddedIdMutatorMethodName();
    final JavaType[] parameterTypes = {entity, JavaType.INT_PRIMITIVE};

    // Locate user-defined method
    if (governorHasMethod(methodName, parameterTypes)) {
      // Method found in governor so do not create method in ITD
      return null;
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Create constructor for embedded id class
    final JavaType embeddedIdFieldType = embeddedIdHolder.getEmbeddedIdField().getFieldType();
    builder.getImportRegistrationResolver().addImport(embeddedIdFieldType);

    final StringBuilder sb = new StringBuilder();
    final List<FieldMetadata> identifierFields = embeddedIdHolder.getIdFields();
    for (int i = 0, n = identifierFields.size(); i < n; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      final FieldMetadata field = identifierFields.get(i);
      final String fieldName = field.getFieldName().getSymbolName();
      final JavaType fieldType = field.getFieldType();
      builder.getImportRegistrationResolver().addImport(fieldType);
      final String initializer = getFieldInitializer(field, null, dataOnDemandClasses);
      bodyBuilder.append(getFieldValidationBody(field, initializer, null, true));
      sb.append(fieldName);
    }
    bodyBuilder.appendFormalLine("");
    bodyBuilder.appendFormalLine(embeddedIdFieldType.getSimpleTypeName()
        + " embeddedIdClass = new " + embeddedIdFieldType.getSimpleTypeName() + "(" + sb.toString()
        + ");");
    bodyBuilder.appendFormalLine(OBJ_VAR + "." + embeddedIdMutator + "(embeddedIdClass);");

    final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL, INDEX_SYMBOL);

    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
        AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
  }

  private String getMinAndMaxBody(final FieldMetadata field, final String suffix) {
    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final AnnotationMetadata minAnnotation =
        MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MIN);
    final AnnotationMetadata maxAnnotation =
        MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), MAX);
    if (minAnnotation != null && maxAnnotation == null) {
      final Number minValue = (Number) minAnnotation.getAttribute(VALUE).getValue();

      if (fieldType.equals(BIG_INTEGER)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue + "\")) == -1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName()
            + "(\"" + minValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + minValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    } else if (minAnnotation == null && maxAnnotation != null) {
      final Number maxValue = (Number) maxAnnotation.getAttribute(VALUE).getValue();

      if (fieldType.equals(BIG_INTEGER)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue + "\")) == 1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName()
            + "(\"" + maxValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " > " + maxValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    } else if (minAnnotation != null && maxAnnotation != null) {
      final Number minValue = (Number) minAnnotation.getAttribute(VALUE).getValue();
      final Number maxValue = (Number) maxAnnotation.getAttribute(VALUE).getValue();
      Validate
          .isTrue(maxValue.longValue() >= minValue.longValue(),
              "The value of @Max must be greater or equal to the value of @Min for field %s",
              fieldName);

      if (fieldType.equals(BIG_INTEGER)) {
        bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
            + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue + "\")) == -1 || " + fieldName
            + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue
            + "\")) == 1) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = new " + BIG_INTEGER.getSimpleTypeName()
            + "(\"" + maxValue + "\");");
      } else {
        bodyBuilder.appendFormalLine("if (" + fieldName + " < " + minValue + suffix + " || "
            + fieldName + " > " + maxValue + suffix + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(fieldName + " = " + maxValue + suffix + ";");
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    }

    return bodyBuilder.getOutput();
  }

  private String getTypeStr(final JavaType fieldType) {
    builder.getImportRegistrationResolver().addImport(fieldType);

    final String arrayStr = fieldType.isArray() ? "[]" : "";
    String typeStr = fieldType.getSimpleTypeName();

    if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.FLOAT_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "float" + arrayStr;
    } else if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.DOUBLE_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "double" + arrayStr;
    } else if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.INT_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "int" + arrayStr;
    } else if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.SHORT_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "short" + arrayStr;
    } else if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.BYTE_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "byte" + arrayStr;
    } else if (fieldType.getFullyQualifiedTypeName().equals(
        JavaType.CHAR_PRIMITIVE.getFullyQualifiedTypeName())
        && fieldType.isPrimitive()) {
      typeStr = "char" + arrayStr;
    } else if (fieldType.equals(new JavaType(STRING.getFullyQualifiedTypeName(), 1, DataType.TYPE,
        null, null))) {
      typeStr = "String[]";
    }
    return typeStr;
  }

  private boolean isNullableJoinColumn(final FieldMetadata field) {
    final AnnotationMetadata joinColumnAnnotation = field.getAnnotation(JOIN_COLUMN);
    if (joinColumnAnnotation == null) {
      return true;
    }
    final AnnotationAttributeValue<?> nullableAttr =
        joinColumnAnnotation.getAttribute(new JavaSymbolName("nullable"));
    return nullableAttr == null || (Boolean) nullableAttr.getValue();
  }

  private void validateNumericAnnotationAttribute(final String fieldName,
      final String annotationName, final String attributeName, final Object object) {
    Validate.isTrue(NumberUtils.isNumber(object.toString()),
        "%s '%s' attribute for field '%s' in backing type %s must be numeric", annotationName,
        attributeName, fieldName, entity.getFullyQualifiedTypeName());
  }

  public JavaSymbolName getCreateFactoryMethodName() {
    return JpaEntityFactoryMetadata.CREATE_FACTORY_METHOD_NAME;
  }

  public JavaSymbolName getEmbeddedFieldMutatorMethodName(final JavaSymbolName fieldName) {
    return BeanInfoUtils.getMutatorMethodName(fieldName);
  }

  public JavaSymbolName getEmbeddedFieldMutatorMethodName(final JavaSymbolName embeddedFieldName,
      final JavaSymbolName fieldName) {
    return getEmbeddedFieldMutatorMethodName(new JavaSymbolName(embeddedFieldName.getSymbolName()
        + StringUtils.capitalize(fieldName.getSymbolName())));
  }

  public List<EmbeddedHolder> getEmbeddedHolders() {
    return this.embeddedHolders;
  }

  public JavaSymbolName getEmbeddedIdMutatorMethodName() {
    final List<JavaSymbolName> fieldNames = new ArrayList<JavaSymbolName>();
    for (final FieldMetadata field : fieldInitializers.keySet()) {
      fieldNames.add(field.getFieldName());
    }

    int index = -1;
    JavaSymbolName embeddedIdField;
    while (true) {
      // Compute the required field name
      index++;
      embeddedIdField = new JavaSymbolName("embeddedIdClass" + StringUtils.repeat("_", index));
      if (!fieldNames.contains(embeddedIdField)) {
        // Found a usable name
        break;
      }
    }
    return BeanInfoUtils.getMutatorMethodName(embeddedIdField);
  }

  public JavaType getEntityType() {
    return this.entity;
  }

  /**
   * Returns the {@link JavaType} representing the physical type of this ITD governor.
   * 
   * @return the {@link JavaType} for the governor physical type.
   */
  public JavaType getGovernorType() {
    return this.governorPhysicalTypeMetadata.getType();
  }

  /**
   * @return the "getRandomEntity():Entity" method (never returns null)
   */
  public MethodMetadata getRandomPersistentEntityMethod() {
    return randomPersistentEntityMethod;
  }

  public boolean hasEmbeddedIdentifier() {
    return this.embeddedIdHolder != null;
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }

}
