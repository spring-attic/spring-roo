package org.springframework.roo.addon.field.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.field.annotations.RooRelationManagement;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * = _RelationManagementMetadata_
 *
 * Metadata for {@link RooRelationManagement} annotation.
 *
 * This class generates in the aj file all method required to handle every relation fields
 * specified in {@link RooRelationManagement#relationFields()} attribute.
 *
 * This methods will be:
 * ._addTo..._: Sets all values from parent and child/children objects to set correctly relation between entities
 * ._removeFrom...: Cleans all values on parent an child/children objects to break relation between entities
 *
 * @author Jose Manuel Vivó
 * @since 2.0.
 *
 */
public class RelationManagementMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = RelationManagementMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);


  /**
   * Array of supported JPA-relationship annotations
   */
  private static final JavaType[] JPA_ANNOTATIONS_SUPPORTED = {JpaJavaType.ONE_TO_ONE,
      JpaJavaType.ONE_TO_MANY, JpaJavaType.MANY_TO_MANY};

  private static final JavaSymbolName MAPPEDBY_ATTRIBUTE = new JavaSymbolName("mappedBy");


  /**
   * prefix for add/remove method names
   */
  private static final String REMOVE_METHOD_PREFIX = "removeFrom";
  private static final String ADD_METHOD_PREFIX = "addTo";

  /**
   * Suffix for add/remove method parameter names
   */
  private static final String REMOVE_PARAMETER_SUFFIX = "ToRemove";
  private static final String ADD_PARAMETER_SUFFIX = "ToAdd";

  /**
   * Information about managed relations
   */
  private final Map<String, RelationManagementInfo> fieldInfos;

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
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

  /**
   * == Constructor
   *
   * Generates ITD with required add item and remove item methods
   *
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param fields
   */
  public RelationManagementMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ReleationManagementAnnotationValues annotationValues, Map<String, FieldMetadata> fields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);



    // Prepare build map for infos
    Map<String, RelationManagementInfo> fieldInfosTemporal =
        new HashMap<String, RelationManagementMetadata.RelationManagementInfo>(
            annotationValues.getRelationFields().length);

    MethodMetadataBuilder addMethod, removeMethod;
    Cardinality cardinality;
    RelationManagementInfo info;
    JavaType childType;
    JavaSymbolName addMethodName, removeMethodName;
    AnnotationMetadata jpaAnnotation;

    ImportRegistrationResolver importResolver = builder.getImportRegistrationResolver();

    // Iterate over declared relations on annotation
    for (String relationName : annotationValues.getRelationFields()) {

      // Get field definition
      FieldMetadata field = fields.get(relationName);
      Validate
          .notNull(
              field,
              "Missing field '%s' referenced on %s.%s.relationFields. Check if field is missing or annotation value should be updated/removed.",
              relationName, governorPhysicalTypeMetadata.getType(),
              RooRelationManagement.class.getName());

      // Get cardinality
      jpaAnnotation = getJpaRelaationAnnotation(field);
      cardinality = getFieldCardinality(jpaAnnotation);
      Validate
          .notNull(
              cardinality,
              "Field '%s' refered on %s.%s.relationFields is unsupported type (@OneToOne, @OneToMany or @ManyToMany). Check if field is erroneous or annotation value should be updated/removed.",
              relationName, governorPhysicalTypeMetadata.getType(),
              RooRelationManagement.class.getName());

      // Get child type
      if (cardinality == Cardinality.ONE_TO_ONE) {
        childType = field.getFieldType();
      } else {
        childType = field.getFieldType().getBaseType();
      }

      // Get mappedBy annotation attribute
      String mappedBy = getMappedByValue(jpaAnnotation);
      Validate.notNull(mappedBy, "Missing 'mappedBy' attribute on @%s annotation of %s.%s field",
          jpaAnnotation.getAnnotationType(), governorPhysicalTypeMetadata.getType(), relationName);

      // Prepare methods
      addMethodName = getAddMethodName(field);
      removeMethodName = getRemoveMethodName(field);
      addMethod =
          getAddValueMethod(addMethodName, field, cardinality, childType, mappedBy,
              removeMethodName, importResolver);
      removeMethod =
          getRemoveMethod(removeMethodName, field, cardinality, childType, mappedBy, importResolver);

      // Add to ITD builder
      builder.addMethod(addMethod);
      builder.addMethod(removeMethod);


      // Store info in temporal map
      info =
          new RelationManagementInfo(relationName, addMethodName, removeMethodName, cardinality,
              childType);
      fieldInfosTemporal.put(relationName, info);
    }

    // store final info unmodifiable map
    fieldInfos = Collections.unmodifiableMap(fieldInfosTemporal);



    // Build ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Gets `mappedBy` attribute value from JPA relationship annotation
   *
   * @param jpaAnnotation
   * @return
   */
  private String getMappedByValue(AnnotationMetadata jpaAnnotation) {
    AnnotationAttributeValue<?> mappedByValue = jpaAnnotation.getAttribute(MAPPEDBY_ATTRIBUTE);
    if (mappedByValue == null) {
      return null;
    }
    return ((StringAttributeValue) mappedByValue).getValue();
  }

  /**
   * Create remove method to handle referenced relation
   *
   * @param removeMethodName
   * @param field
   * @param cardinality
   * @param childType
   * @param mappedBy
   * @param importResolver
   * @return method metadata or null if method already in class
   */
  private MethodMetadataBuilder getRemoveMethod(final JavaSymbolName removeMethodName,
      final FieldMetadata field, final Cardinality cardinality, final JavaType childType,
      final String mappedBy, ImportRegistrationResolver importResolver) {

    // Identify parameters types and names (if any)
    final List<JavaType> parameterTypes = new ArrayList<JavaType>(1);
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>(1);
    if (cardinality != Cardinality.ONE_TO_ONE) {
      parameterTypes.add(new JavaType(Collection.class.getName(), 0, DataType.TYPE, null, Arrays
          .asList(childType)));
      parameterNames.add(new JavaSymbolName(field.getFieldName().getSymbolName()
          + REMOVE_PARAMETER_SUFFIX));
    }

    // See if the type itself declared the method
    if (governorHasMethod(removeMethodName, parameterTypes)) {
      return null;
    }


    final InvocableMemberBodyBuilder builder = new InvocableMemberBodyBuilder();

    if (cardinality == Cardinality.ONE_TO_ONE) {
      buildRemoveOneToOneBody(field, mappedBy, builder);
    } else if (cardinality == Cardinality.ONE_TO_MANY) {
      buildRemoveOneToManyBody(field, mappedBy, parameterNames.get(0), childType, builder,
          importResolver);
    } else {
      // ManyToMany
      buildRemoveManyToManyBody(field, mappedBy, parameterNames.get(0), childType, builder,
          importResolver);
    }

    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, removeMethodName,
        JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
        parameterNames, builder);
  }

  /**
   * Build remove method body for OneToOne relation
   *
   * @param field
   * @param mappedBy
   * @param builder
   */
  private void buildRemoveOneToOneBody(final FieldMetadata field, final String mappedBy,
      final InvocableMemberBodyBuilder builder) {
    final String fieldName = field.getFieldName().getSymbolName();
    // Build toString method body

    /*
     * if (this.{prop} != null) {
     *   {prop}.set{mappedBy}(null);
     * }
     * this.{prop} = null;
     */

    // if (this.{prop} != null) {
    builder.appendFormalLine(String.format("if (this.%s != null) {", fieldName));

    // {prop}.set{mappedBy}(null);
    builder.indent();
    builder.appendFormalLine(String.format("%s.set%s(null);", fieldName,
        StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");

    // this.{prop} = null;
    builder.appendFormalLine(String.format("this.%s = null;", fieldName));
  }

  /**
   * Build remove method body for OneToMany relation
   *
   * @param field
   * @param mappedBy
   * @param parameterName
   * @param childType
   * @param builder
   * @param importResolver
   */
  private void buildRemoveOneToManyBody(final FieldMetadata field, final String mappedBy,
      JavaSymbolName parameterName, final JavaType childType,
      final InvocableMemberBodyBuilder builder, ImportRegistrationResolver importResolver) {
    final String fieldName = field.getFieldName().getSymbolName();
    // Build method body

    /*
     * Assert.notEmpty({param}, "At least one item to remove is required");
     * for ({childType} item : {param}) {
     *   this.{field}.remove(item);
     *   item.set{mappedBy}(null);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notEmpty({param}, "At least one item to remove is required");
    builder.appendFormalLine(String.format(
        "Assert.notEmpty(%s, \"At least one item to remove is required\");", parameterName));

    // for ({childType} item : {param}) {
    builder.appendFormalLine(String.format("for (%s item : %s) {", childType.getSimpleTypeName(),
        parameterName));

    // this.{field}.remove(item);
    builder.indent();
    builder.appendFormalLine(String.format("this.%s.remove(item);", fieldName));

    // item.set{mappedBy}(null);
    builder.appendFormalLine(String.format("item.set%s(null);", StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");
  }

  /**
   * Build remove method body for ManyToMany relation
   *
   * @param field
   * @param mappedBy
   * @param parameterName
   * @param childType
   * @param builder
   * @param importResolver
   */
  private void buildRemoveManyToManyBody(final FieldMetadata field, final String mappedBy,
      JavaSymbolName parameterName, final JavaType childType,
      final InvocableMemberBodyBuilder builder, ImportRegistrationResolver importResolver) {
    final String fieldName = field.getFieldName().getSymbolName();
    // Build method body

    /*
     * Assert.notEmpty({param}, "At least one item to remove is required");
     * for ({childType} item : {param}) {
     *   this.{field}.remove(item);
     *   item.get{mappedBy}().remove(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);
    // Assert.notEmpty({param}, "At least one item to remove is required");
    builder.appendFormalLine(String.format(
        "Assert.notEmpty(%s, \"At least one item to remove is required\");", parameterName));

    // for ({childType} item : {param}) {
    builder.appendFormalLine(String.format("for (%s item : %s) {", childType.getSimpleTypeName(),
        parameterName));

    // this.{field}.remove(item);
    builder.indent();
    builder.appendFormalLine(String.format("this.%s.remove(item);", fieldName));

    // item.get{mappedBy}().remove(this);
    builder.appendFormalLine(String.format("item.get%s().remove(this);",
        StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");
  }

  /**
   * Generate method name to use for remove method of selected field
   *
   * @param field
   * @return
   */
  private JavaSymbolName getRemoveMethodName(final FieldMetadata field) {
    final JavaSymbolName methodName =
        new JavaSymbolName(REMOVE_METHOD_PREFIX
            + StringUtils.capitalize(field.getFieldName().getSymbolName()));
    return methodName;
  }

  /**
   * Create add method to handle referenced relation
   *
   * @param addMethodName
   * @param field
   * @param cardinality
   * @param childType
   * @param mappedBy
   * @param removeMethodName
   * @param importResolver
   * @return method metadata or null if method already in class
   */
  private MethodMetadataBuilder getAddValueMethod(final JavaSymbolName addMethodName,
      final FieldMetadata field, final Cardinality cardinality, final JavaType childType,
      final String mappedBy, final JavaSymbolName removeMethodName,
      ImportRegistrationResolver importResolver) {

    // Identify parameters type and name
    final List<JavaType> parameterTypes = new ArrayList<JavaType>(1);
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>(1);
    if (cardinality == Cardinality.ONE_TO_ONE) {
      parameterTypes.add(childType);
      parameterNames.add(field.getFieldName());
    } else {
      parameterTypes.add(new JavaType(Collection.class.getName(), 0, DataType.TYPE, null, Arrays
          .asList(childType)));
      parameterNames.add(new JavaSymbolName(field.getFieldName().getSymbolName()
          + ADD_PARAMETER_SUFFIX));
    }

    // See if the type itself declared the method
    if (governorHasMethod(addMethodName, parameterTypes)) {
      return null;
    }

    final InvocableMemberBodyBuilder builder = new InvocableMemberBodyBuilder();

    if (cardinality == Cardinality.ONE_TO_ONE) {
      buildAddOneToOneBody(field, mappedBy, parameterNames.get(0), childType, removeMethodName,
          builder);
    } else if (cardinality == Cardinality.ONE_TO_MANY) {
      buildAddOneToManyBody(field, mappedBy, parameterNames.get(0), childType, builder,
          importResolver);
    } else {
      buildAddManyToManyBody(field, mappedBy, parameterNames.get(0), childType, builder,
          importResolver);
    }

    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, addMethodName,
        JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
        parameterNames, builder);
  }

  /**
   * Build add method body for OneToOne relation
   *
   * @param field
   * @param mappedBy
   * @param parameter
   * @param childType
   * @param removeMethodName
   * @param builder
   */
  private void buildAddOneToOneBody(final FieldMetadata field, final String mappedBy,
      final JavaSymbolName parameter, final JavaType childType,
      final JavaSymbolName removeMethodName, InvocableMemberBodyBuilder builder) {

    final String fieldName = field.getFieldName().getSymbolName();
    // Build method body

    /*
     * if ({param} == null) {
     *   {removeMethod}();
     * } else {
     *   this.{field} = {param};
     *   {param}.set{mappedBy}(this);
     * }
     */

    // if ({param} == null) {
    builder.appendFormalLine(String.format("if (%s == null) {", parameter));

    // {removeMethod}();
    builder.indent();
    builder.appendFormalLine(String.format("%s();", removeMethodName));

    // } else {
    builder.indentRemove();
    builder.appendFormalLine("} else {");

    // this.{field} = {param};
    builder.indent();
    builder.appendFormalLine(String.format("this.%s = %s;", fieldName, parameter));

    // {param}.set{mappedBy}(this);
    builder.appendFormalLine(String.format("%s.set%s(this);", parameter,
        StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");

  }

  /**
   * Build add method body for OneToMany relation
   *
   * @param field
   * @param mappedBy
   * @param parameterName
   * @param childType
   * @param builder
   * @param importResolver
   */
  private void buildAddOneToManyBody(final FieldMetadata field, final String mappedBy,
      final JavaSymbolName parameterName, final JavaType childType,
      final InvocableMemberBodyBuilder builder, ImportRegistrationResolver importResolver) {
    final String fieldName = field.getFieldName().getSymbolName();
    // Build method body

    /*
     * Assert.notEmpty({param}, "At least one item to add is required");
     * for ({childType} item : {param}) {
     *   this.{field}.add(item);
     *   item.set{mappedBy}(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);
    // Assert.notEmpty({param}, "At least one item to add is required");
    builder.appendFormalLine(String.format(
        "Assert.notEmpty(%s, \"At least one item to add is required\");", parameterName));

    // for ({childType} item : {param}) {
    builder.appendFormalLine(String.format("for (%s item : %s) {", childType.getSimpleTypeName(),
        parameterName));

    // this.{field}.remove(item);
    builder.indent();
    builder.appendFormalLine(String.format("this.%s.add(item);", fieldName));

    // item.set{mappedBy}(null);
    builder.appendFormalLine(String.format("item.set%s(this);", StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");

  }

  /**
   * Build add method body for ManyToMany relation
   *
   * @param field
   * @param mappedBy
   * @param parameterName
   * @param childType
   * @param builder
   * @param importResolver
   */
  private void buildAddManyToManyBody(final FieldMetadata field, final String mappedBy,
      final JavaSymbolName parameterName, final JavaType childType,
      final InvocableMemberBodyBuilder builder, ImportRegistrationResolver importResolver) {
    final String fieldName = field.getFieldName().getSymbolName();
    // Build method body

    /*
     * Assert.notEmpty({param}, "At least one item to add is required");
     * for ({childType} item : {param}) {
     *   this.{field}.add(item);
     *   item.get{mappedBy}().add(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notEmpty({param}, "At least one item to add is required");
    builder.appendFormalLine(String.format(
        "Assert.notEmpty(%s, \"At least one item to add is required\");", parameterName));

    // for ({childType} item : {param}) {
    builder.appendFormalLine(String.format("for (%s item : %s) {", childType.getSimpleTypeName(),
        parameterName));

    // this.{field}.remove(item);
    builder.indent();
    builder.appendFormalLine(String.format("this.%s.add(item);", fieldName));

    // item.get{mappedBy}().add(this);
    builder.appendFormalLine(String.format("item.get%s().add(this);",
        StringUtils.capitalize(mappedBy)));

    // }
    builder.indentRemove();
    builder.appendFormalLine("}");

  }

  /**
   * Generate method name to use for add method of selected field
   *
   * @param field
   * @return
   */
  private JavaSymbolName getAddMethodName(final FieldMetadata field) {
    final JavaSymbolName methodName =
        new JavaSymbolName(ADD_METHOD_PREFIX
            + StringUtils.capitalize(field.getFieldName().getSymbolName()));
    return methodName;
  }

  /**
   * Returns JPA field annotations
   *
   * @param field
   * @return ONE_TO_MANY, MANY_TO_MANY, ONE_TO_ONE or null
   */
  private AnnotationMetadata getJpaRelaationAnnotation(final FieldMetadata field) {
    AnnotationMetadata annotation = null;
    for (JavaType type : JPA_ANNOTATIONS_SUPPORTED) {
      annotation = field.getAnnotation(type);
      if (annotation != null) {
        break;
      }
    }
    return annotation;
  }

  /**
   * Returns cardinality value based on JPA field annotations
   *
   * @param field
   * @return ONE_TO_MANY, MANY_TO_MANY, ONE_TO_ONE or null
   */
  private Cardinality getFieldCardinality(final AnnotationMetadata fieldJpaAnnotation) {
    Cardinality cardinality = null;
    if (JpaJavaType.ONE_TO_MANY.equals(fieldJpaAnnotation.getAnnotationType())) {
      cardinality = Cardinality.ONE_TO_MANY;
    } else if (JpaJavaType.MANY_TO_MANY.equals(fieldJpaAnnotation.getAnnotationType())) {
      cardinality = Cardinality.MANY_TO_MANY;
    } else if (JpaJavaType.ONE_TO_ONE.equals(fieldJpaAnnotation.getAnnotationType())) {
      cardinality = Cardinality.ONE_TO_ONE;
    }
    return cardinality;
  }

  /**
   * @return information about all relation managed
   */
  public Map<String, RelationManagementInfo> getRelationFieldInfos() {
    return fieldInfos;
  }

  /**
   * = _RelationManagementInfo_
   *
   * *Immutable* information about a managed relation
   *
   * @author Jose Manuel Vivó
   * @since 2.0.0
   */
  public static class RelationManagementInfo {

    /**
     * relation field name
     */
    public final String fieldName;

    /**
     * Method to use to add/set a item
     */
    public final JavaSymbolName addMethod;

    /**
     * Method to use to remove/clean a item
     */
    public final JavaSymbolName removeMethod;

    /**
     * Relationship carinality
     */
    public final Cardinality cardinality;

    /**
     * Child item type
     */
    public final JavaType childType;

    /**
     * Constructor
     *
     * @param fieldName
     * @param addMethod
     * @param removeMethod
     * @param cardinality
     * @param childType
     */
    public RelationManagementInfo(String fieldName, JavaSymbolName addMethod,
        JavaSymbolName removeMethod, Cardinality cardinality, JavaType childType) {
      super();
      this.fieldName = fieldName;
      this.addMethod = addMethod;
      this.removeMethod = removeMethod;
      this.cardinality = cardinality;
      this.childType = childType;
    }
  }
}
