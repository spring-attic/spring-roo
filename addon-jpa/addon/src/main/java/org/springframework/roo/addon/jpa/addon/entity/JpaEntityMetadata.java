package org.springframework.roo.addon.jpa.addon.entity;

import static org.springframework.roo.model.JpaJavaType.DISCRIMINATOR_COLUMN;
import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.JpaJavaType.INHERITANCE;
import static org.springframework.roo.model.JpaJavaType.INHERITANCE_TYPE;
import static org.springframework.roo.model.JpaJavaType.MAPPED_SUPERCLASS;
import static org.springframework.roo.model.JpaJavaType.TABLE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.jpa.annotations.entity.RooJpaEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * The metadata for a JPA entity's *_Roo_Jpa_Entity.aj ITD.
 *
 * @author Andrew Swan
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @author Sergio Clares
 * @since 1.2.0
 */
public class JpaEntityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = JpaEntityMetadata.class.getName();


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



  private final JavaType annotatedEntity;
  private final JpaEntityAnnotationValues annotationValues;
  private final JpaEntityMetadata parent;
  private FieldMetadata identifierField;
  private MethodMetadata identifierAccessor;
  private FieldMetadata versionField;
  private MethodMetadata versionAccessor;
  private FieldMetadata iterableToAddCantBeNullConstant;
  private FieldMetadata iterableToRemoveCantBeNullConstant;

  private final Map<String, RelationInfo> relationInfos;

  private final Map<String, RelationInfo> relationInfosByMappedBy;

  private final Map<String, FieldMetadata> relationsAsChild;

  private final FieldMetadata compositionRelationField;

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  /**
   * Constructor
   *
   * @param metadataIdentificationString the JPA_ID of this
   *            {@link MetadataItem}
   * @param itdName the ITD's {@link JavaType} (required)
   * @param entityPhysicalType the entity's physical type (required)
   * @param parent can be <code>null</code> if none of the governor's
   *            ancestors provide {@link JpaEntityMetadata}
   * @param entityMemberDetails details of the entity's members (required)
   * @param identifierField
   * @param identifierAccessor
   * @param versionField
   * @param versionAccessor
   * @param annotationValues the effective annotation values taking into
   *            account the presence of a {@link RooJpaEntity} annotation (required)
   * @param entityDetails
   * @param fieldsRelationAsParent fields that declares a relation which current entity is the parent part
   * @param fieldsRelationAsChild fields that declares a relation which current entity is the child part
   * @param compositionRelationField field that declares a composition relation which current entity is the child part
   */
  public JpaEntityMetadata(final String metadataIdentificationString, final JavaType itdName,
      final PhysicalTypeMetadata entityPhysicalType, final JpaEntityMetadata parent,
      final MemberDetails entityMemberDetails, final FieldMetadata identifierField,
      final MethodMetadata identifierAccessor, final FieldMetadata versionField,
      final MethodMetadata versionAccessor, final JpaEntityAnnotationValues annotationValues,
      final ClassOrInterfaceTypeDetails entityDetails, List<FieldMetadata> fieldsRelationAsParent,
      Map<String, FieldMetadata> fieldsRelationAsChild, FieldMetadata compositionRelationField) {
    super(metadataIdentificationString, itdName, entityPhysicalType);
    Validate.notNull(annotationValues, "Annotation values are required");
    Validate.notNull(entityMemberDetails, "Entity MemberDetails are required");

    /*
     * Ideally we'd pass these parameters to the methods below rather than
     * storing them in fields, but this isn't an option due to various calls
     * to the parent entity.
     */
    this.annotatedEntity = entityPhysicalType.getType();
    this.annotationValues = annotationValues;
    this.parent = parent;
    this.identifierField = identifierField;
    this.identifierAccessor = identifierAccessor;
    this.versionField = versionField;
    this.versionAccessor = versionAccessor;

    // Add @Entity or @MappedSuperclass annotation
    builder
        .addAnnotation(annotationValues.isMappedSuperclass() ? getTypeAnnotation(MAPPED_SUPERCLASS)
            : getEntityAnnotation());

    // Add @Table annotation if required
    builder.addAnnotation(getTableAnnotation());

    // Add @Inheritance annotation if required
    builder.addAnnotation(getInheritanceAnnotation());

    // Add @DiscriminatorColumn if required
    builder.addAnnotation(getDiscriminatorColumnAnnotation());

    // Add @EntityFormat annotation
    builder.addAnnotation(getEntityFormatAnnotation());

    // Ensure there's a no-arg constructor (explicit or default)
    builder.addConstructor(getNoArgConstructor());

    // Include necessary static fields
    if (!isReadOnly()) {
      ensureGovernorHasField(new FieldMetadataBuilder(getIterableToAddCantBeNullConstant()));
      ensureGovernorHasField(new FieldMetadataBuilder(getIterableToRemoveCantBeNullConstant()));
    }

    // Manage relations

    MethodMetadata addMethod = null;
    MethodMetadata removeMethod = null;
    Cardinality cardinality;
    RelationInfo info;
    JavaType childType;
    JavaSymbolName addMethodName, removeMethodName;
    AnnotationMetadata jpaAnnotation;
    String fieldName;
    JpaRelationType relationType;
    AnnotationAttributeValue<?> relationTypeAttribute;

    Map<String, RelationInfo> fieldInfosTemporal = new TreeMap<String, RelationInfo>();
    Map<String, RelationInfo> fieldInfosMappedByTemporal = new TreeMap<String, RelationInfo>();
    ImportRegistrationResolver importResolver = builder.getImportRegistrationResolver();

    // process fields which this entity is parent part
    for (FieldMetadata field : fieldsRelationAsParent) {

      fieldName = field.getFieldName().getSymbolName();
      // Get cardinality
      jpaAnnotation = getJpaRelaationAnnotation(field);
      cardinality = getFieldCardinality(jpaAnnotation);
      Validate
          .notNull(
              cardinality,
              "Field '%s.%s' is annotated with @%s but this annotation only can be used in @OneToOne, @OneToMany or @ManyToMany field",
              governorPhysicalTypeMetadata.getType(), fieldName,
              RooJavaType.ROO_JPA_RELATION.getSimpleTypeName());

      // Get child type
      if (cardinality == Cardinality.ONE_TO_ONE) {
        childType = field.getFieldType();
      } else {
        childType = field.getFieldType().getBaseType();
      }

      // Get mappedBy annotation attribute
      String mappedBy = getMappedByValue(jpaAnnotation);
      Validate.notNull(mappedBy, "Missing 'mappedBy' attribute on @%s annotation of %s.%s field",
          jpaAnnotation.getAnnotationType(), governorPhysicalTypeMetadata.getType(),
          field.getFieldName());

      // "addTo" and "removeFrom" relation methods will be included
      // only if entity is not readOnly. Doesn't make sense to modify the relations
      // of a readOnly entity.
      if (!isReadOnly()) {

        // Prepare methods.
        addMethodName = getAddMethodName(field);
        removeMethodName = getRemoveMethodName(field);
        addMethod =
            getAddValueMethod(addMethodName, field, cardinality, childType, mappedBy,
                removeMethodName, importResolver);
        removeMethod =
            getRemoveMethod(removeMethodName, field, cardinality, childType, mappedBy,
                importResolver);
        // Add to ITD builder
        ensureGovernorHasMethod(new MethodMetadataBuilder(addMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(removeMethod));
      }

      relationTypeAttribute =
          field.getAnnotation(RooJavaType.ROO_JPA_RELATION).getAttribute("type");
      if (relationTypeAttribute == null) {
        relationType = JpaRelationType.AGGREGATION;
      } else {
        relationType =
            JpaRelationType.valueOf(((EnumDetails) relationTypeAttribute.getValue()).getField()
                .getSymbolName());
      }

      // Store info in temporal maps
      info =
          new RelationInfo(getDestination(), fieldName, addMethod, removeMethod, cardinality,
              childType, field, mappedBy, relationType);
      fieldInfosTemporal.put(fieldName, info);
      // Use ChildType+childField as keys to avoid overried values
      // (the same mappedBy on many relations. Ej. Order.customer and Invoice.customer)
      fieldInfosMappedByTemporal.put(
          getMappedByInfoKey(field.getFieldType().getBaseType(), mappedBy), info);

    }

    // store final info unmodifiable map
    this.relationsAsChild = Collections.unmodifiableMap(fieldsRelationAsChild);
    relationInfos = Collections.unmodifiableMap(fieldInfosTemporal);
    relationInfosByMappedBy = Collections.unmodifiableMap(fieldInfosMappedByTemporal);
    this.compositionRelationField = compositionRelationField;


    // Build the ITD based on what we added to the builder above
    itdTypeDetails = builder.build();
  }

  /**
   * Get key to use to locate a value on {@link #relationInfosByMappedBy}.
   *
   * This is due to _mappedBy_ value usually is the same between relations with
   * other entities (ej.: Order.customer, Invoice.customer, ContactNote.customer...)
   *
   * @param childEntity
   * @param mappedBy field value
   * @return
   */
  private String getMappedByInfoKey(JavaType childEntity, String mappedBy) {
    return childEntity.getFullyQualifiedTypeName() + "." + mappedBy;
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

  private AnnotationMetadata getDiscriminatorColumnAnnotation() {
    if (StringUtils.isNotBlank(annotationValues.getInheritanceType())
        && InheritanceType.SINGLE_TABLE.name().equals(annotationValues.getInheritanceType())) {
      // Theoretically not required based on @DiscriminatorColumn
      // JavaDocs, but Hibernate appears to fail if it's missing
      return getTypeAnnotation(DISCRIMINATOR_COLUMN);
    }
    return null;
  }

  /**
   * Generates the JPA @Entity annotation to be applied to the entity
   *
   * @return
   */
  private AnnotationMetadata getEntityAnnotation() {
    AnnotationMetadata entityAnnotation = getTypeAnnotation(ENTITY);
    if (entityAnnotation == null) {
      return null;
    }

    if (StringUtils.isNotBlank(annotationValues.getEntityName())) {
      final AnnotationMetadataBuilder entityBuilder =
          new AnnotationMetadataBuilder(entityAnnotation);
      entityBuilder.addStringAttribute("name", annotationValues.getEntityName());
      entityAnnotation = entityBuilder.build();
    }

    return entityAnnotation;
  }

  /**
   * Generates the Springlets `@EntityFormat` annotation to be applied to the entity
  *
  * @return AnnotationMetadata
  */
  private AnnotationMetadata getEntityFormatAnnotation() {
    AnnotationMetadata entityFormatAnnotation =
        getTypeAnnotation(SpringletsJavaType.SPRINGLETS_ENTITY_FORMAT);
    if (entityFormatAnnotation == null) {
      return null;
    }

    String expressionAttribute = this.annotationValues.getEntityFormatExpression();
    String messageAttribute = this.annotationValues.getEntityFormatMessage();

    final AnnotationMetadataBuilder entityFormatBuilder =
        new AnnotationMetadataBuilder(entityFormatAnnotation);

    // Check for each attribute individually
    if (StringUtils.isNotBlank(expressionAttribute)) {
      entityFormatBuilder.addStringAttribute("value", expressionAttribute);

    }

    if (StringUtils.isNotBlank(messageAttribute)) {
      entityFormatBuilder.addStringAttribute("message", messageAttribute);
    }

    entityFormatAnnotation = entityFormatBuilder.build();

    return entityFormatAnnotation;
  }


  /**
   * Returns the JPA @Inheritance annotation to be applied to the entity, if
   * applicable
   *
   * @param annotationValues the values of the {@link RooJpaEntity} annotation
   *            (required)
   * @return <code>null</code> if it's already present or not required
   */
  private AnnotationMetadata getInheritanceAnnotation() {
    if (governorTypeDetails.getAnnotation(INHERITANCE) != null) {
      return null;
    }
    if (StringUtils.isNotBlank(annotationValues.getInheritanceType())) {
      final AnnotationMetadataBuilder inheritanceBuilder =
          new AnnotationMetadataBuilder(INHERITANCE);
      inheritanceBuilder.addEnumAttribute("strategy", new EnumDetails(INHERITANCE_TYPE,
          new JavaSymbolName(annotationValues.getInheritanceType())));
      return inheritanceBuilder.build();
    }
    return null;
  }

  /**
   * Locates the no-arg constructor for this class, if available.
   * <p>
   * If a class defines a no-arg constructor, it is returned (irrespective of
   * access modifiers).
   * <p>
   * Otherwise, and if there is at least one other constructor declared in the
   * source file, this method creates one with public access.
   *
   * @return <code>null</code> if no constructor is to be produced
   */
  private ConstructorMetadataBuilder getNoArgConstructor() {
    // Search for an existing constructor
    final ConstructorMetadata existingExplicitConstructor =
        governorTypeDetails.getDeclaredConstructor(null);
    if (existingExplicitConstructor != null) {
      // Found an existing no-arg constructor on this class, so return it
      return new ConstructorMetadataBuilder(existingExplicitConstructor);
    }

    // To get this far, the user did not define a no-arg constructor
    if (governorTypeDetails.getDeclaredConstructors().isEmpty()) {
      // Java creates the default constructor => no need to add one
      return null;
    }

    // Create the constructor
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("super();");

    final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
    constructorBuilder.setBodyBuilder(bodyBuilder);
    constructorBuilder.setModifier(Modifier.PUBLIC);
    return constructorBuilder;
  }

  /**
   * Generates the JPA @Table annotation to be applied to the entity
   *
   * @param annotationValues
   * @return
   */
  private AnnotationMetadata getTableAnnotation() {
    final AnnotationMetadata tableAnnotation = getTypeAnnotation(TABLE);
    if (tableAnnotation == null) {
      return null;
    }
    final String catalog = annotationValues.getCatalog();
    final String schema = annotationValues.getSchema();
    final String table = annotationValues.getTable();
    if (StringUtils.isNotBlank(table) || StringUtils.isNotBlank(schema)
        || StringUtils.isNotBlank(catalog)) {
      final AnnotationMetadataBuilder tableBuilder = new AnnotationMetadataBuilder(tableAnnotation);
      if (StringUtils.isNotBlank(catalog)) {
        tableBuilder.addStringAttribute("catalog", catalog);
      }
      if (StringUtils.isNotBlank(schema)) {
        tableBuilder.addStringAttribute("schema", schema);
      }
      if (StringUtils.isNotBlank(table)) {
        tableBuilder.addStringAttribute("name", table);
      }
      return tableBuilder.build();
    }
    return null;
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
  private MethodMetadata getRemoveMethod(final JavaSymbolName removeMethodName,
      final FieldMetadata field, final Cardinality cardinality, final JavaType childType,
      final String mappedBy, ImportRegistrationResolver importResolver) {

    // Identify parameters types and names (if any)
    final List<JavaType> parameterTypes = new ArrayList<JavaType>(1);
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>(1);
    if (cardinality != Cardinality.ONE_TO_ONE) {
      parameterTypes.add(JavaType.iterableOf(childType));
      parameterNames.add(new JavaSymbolName(field.getFieldName().getSymbolName()
          + REMOVE_PARAMETER_SUFFIX));
    }

    // See if the type itself declared the method
    MethodMetadata existingMethod = getGovernorMethod(removeMethodName, parameterTypes);
    if (existingMethod != null) {
      return existingMethod;
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
        parameterNames, builder).build();
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
     *  Assert.notNull({param}, "The given Iterable of items to remove can't be null!");
     * for ({childType} item : {param}) {
     *   this.{field}.remove(item);
     *   item.set{mappedBy}(null);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notNull({param}, "The given Iterable of items to remove can't be null!");
    builder.appendFormalLine(String.format("Assert.notNull(%s, %s);", parameterName,
        getIterableToRemoveCantBeNullConstant().getFieldName()));

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
     * Assert.notNull({param}, "The given Iterable of items to remove can't be null!");
     * for ({childType} item : {param}) {
     *   this.{field}.remove(item);
     *   item.get{mappedBy}().remove(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notNull({param}, "The given Iterable of items to remove can't be null!");
    builder.appendFormalLine(String.format("Assert.notNull(%s, %s);", parameterName,
        getIterableToRemoveCantBeNullConstant().getFieldName()));

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
  private MethodMetadata getAddValueMethod(final JavaSymbolName addMethodName,
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
      parameterTypes.add(JavaType.iterableOf(childType));
      parameterNames.add(new JavaSymbolName(field.getFieldName().getSymbolName()
          + ADD_PARAMETER_SUFFIX));
    }

    // See if the type itself declared the method
    MethodMetadata existingMethod = getGovernorMethod(addMethodName, parameterTypes);
    if (existingMethod != null) {
      return existingMethod;
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
        parameterNames, builder).build();
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
     * Assert.notNull({param}, "The given Iterable of items to add can't be null!");
     * for ({childType} item : {param}) {
     *   this.{field}.add(item);
     *   item.set{mappedBy}(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notNull({param}, "The given Iterable of items to add can't be null!");
    builder.appendFormalLine(String.format("Assert.notNull(%s, %s);", parameterName,
        getIterableToAddCantBeNullConstant().getFieldName()));

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
   * This method obtains the constant that contains the message
   * "The given Iterable of items to add can't be null!".
   *
   * @return name of the generated constant
   */
  private FieldMetadata getIterableToAddCantBeNullConstant() {

    // Check if iterableCantBeNullConstant already exists
    if (iterableToAddCantBeNullConstant != null) {
      return iterableToAddCantBeNullConstant;
    }

    // If not exists, generate a new one and include into builder
    FieldMetadataBuilder constant =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL,
            new JavaSymbolName("ITERABLE_TO_ADD_CANT_BE_NULL_MESSAGE"), JavaType.STRING,
            "\"The given Iterable of items to add can't be null!\"");

    iterableToAddCantBeNullConstant = constant.build();

    return iterableToAddCantBeNullConstant;
  }

  /**
   * This method obtains the constant that contains the message
   * "The given Iterable of items to remove can't be null!".
   *
   * @return name of the generated constant
   */
  private FieldMetadata getIterableToRemoveCantBeNullConstant() {

    // Check if iterableCantBeNullConstant already exists
    if (iterableToRemoveCantBeNullConstant != null) {
      return iterableToRemoveCantBeNullConstant;
    }

    // If not exists, generate a new one and include into builder
    FieldMetadataBuilder constant =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL,
            new JavaSymbolName("ITERABLE_TO_REMOVE_CANT_BE_NULL_MESSAGE"), JavaType.STRING,
            "\"The given Iterable of items to add can't be null!\"");

    iterableToRemoveCantBeNullConstant = constant.build();

    return iterableToRemoveCantBeNullConstant;
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
     * Assert.notNull({param}, "The given Iterable of items to add can't be null!");
     * for ({childType} item : {param}) {
     *   this.{field}.add(item);
     *   item.get{mappedBy}().add(this);
     * }
     */

    importResolver.addImport(SpringJavaType.ASSERT);

    // Assert.notNull({param}, "The given Iterable of items to add can't be null!");
    builder.appendFormalLine(String.format("Assert.notNull(%s, %s);", parameterName,
        getIterableToAddCantBeNullConstant().getFieldName()));

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
   * @return information about relations which current entity is parent. Map key is current entity field name
   */
  public Map<String, RelationInfo> getRelationInfos() {
    return relationInfos;
  }

  /**
   * @return information about relations which current entity is parent. Map key is child entity plus related field name
   * @see #getRelationInfosByMappedBy(JavaType, String)
   */
  public Map<String, RelationInfo> getRelationInfosByMappedBy() {
    return relationInfosByMappedBy;
  }

  /**
   * @return information about relations which current entity is parent.
   */
  public RelationInfo getRelationInfosByMappedBy(JavaType childType, String childFieldName) {
    return relationInfosByMappedBy.get(getMappedByInfoKey(childType, childFieldName));
  }

  /**
   * @return fields declared on entity which entity is child part.
   */
  public Map<String, FieldMetadata> getRelationsAsChild() {
    return relationsAsChild;
  }

  /**
   * @return information about current identifier field
   */
  public FieldMetadata getCurrentIndentifierField() {
    return identifierField;
  }


  /**
   * @return information about current identifier accessor
   */
  public MethodMetadata getCurrentIdentifierAccessor() {
    return identifierAccessor;
  }

  /**
   * @return information about current version field
   */
  public FieldMetadata getCurrentVersionField() {
    return versionField;
  }

  /**
   * @return information about current version accessor
   */
  public MethodMetadata getCurrentVersionAccessor() {
    return versionAccessor;
  }


  public boolean isReadOnly() {
    return annotationValues.isReadOnly();
  }

  /**
   * @return true if this entity is the child part of a composition relation
   */
  public boolean isCompositionChild() {
    return compositionRelationField != null;
  }

  /**
   * @return field of current entity which defines the child part of a composition relation
   */
  public FieldMetadata getCompositionRelationField() {
    return compositionRelationField;
  }

  /**
   * @return metadata of the parent
   */
  public JpaEntityMetadata getParent() {
    return this.parent;
  }

  public JavaType getAnnotatedEntity() {
    return annotatedEntity;
  }

  /**
   * @return `@RooJpaEntity` `entityFormatMessage` value
   */
  public String getEntityFormatMessage() {
    return (String) this.annotationValues.getEntityFormatMessage();
  }

  /**
   * = _RelationInfo_
   *
   * *Immutable* information about a managed relation
   *
   * @author Jose Manuel Vivó
   * @since 2.0.0
   */
  public static class RelationInfo implements Comparable<RelationInfo> {

    /**
     * relation field name
     */
    public final String fieldName;


    /**
     * Method to use to add/set a item
     */
    public final MethodMetadata addMethod;

    /**
     * Method to use to remove/clean a item
     */
    public final MethodMetadata removeMethod;

    /**
     * Relationship carinality
     */
    public final Cardinality cardinality;

    /**
     * Parent (current) item type
     */
    public final JavaType entityType;

    /**
     * Child item type
     */
    public final JavaType childType;

    /**
     * parent field metadata
     */
    public final FieldMetadata fieldMetadata;

    /**
     * Child field name
     */
    public final String mappedBy;

    /**
     * Relation type (Aggregation/Composition)
     */
    public final JpaRelationType type;

    /**
     * Constructor
     *
     * @param fieldName
     * @param addMethod
     * @param removeMethod
     * @param cardinality
     * @param childType
     * @param fieldMetadata
     * @param mappedBy
     * @param type
     */
    protected RelationInfo(final JavaType entityType, final String fieldName,
        final MethodMetadata addMethod, final MethodMetadata removeMethod,
        final Cardinality cardinality, final JavaType childType, final FieldMetadata fieldMetadata,
        final String mappedBy, final JpaRelationType type) {
      super();
      this.entityType = entityType;
      this.fieldName = fieldName;
      this.addMethod = addMethod;
      this.removeMethod = removeMethod;
      this.cardinality = cardinality;
      this.childType = childType;
      this.fieldMetadata = fieldMetadata;
      this.mappedBy = mappedBy;
      this.type = type;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
      result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof RelationInfo)) {
        return false;
      }
      RelationInfo other = (RelationInfo) obj;
      if (entityType == null) {
        if (other.entityType != null) {
          return false;
        }
      } else if (!entityType.equals(other.entityType)) {
        return false;
      }
      if (fieldName == null) {
        if (other.fieldName != null) {
          return false;
        }
      } else if (!fieldName.equals(other.fieldName)) {
        return false;
      }
      return true;
    }

    @Override
    public int compareTo(RelationInfo o) {
      return fieldName.compareTo(o.fieldName);
    }

    public JpaRelationType getType() {
      return this.type;
    }
  }
}
