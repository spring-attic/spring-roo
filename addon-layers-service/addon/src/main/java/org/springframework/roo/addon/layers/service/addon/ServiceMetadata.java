package org.springframework.roo.addon.layers.service.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.service.annotations.RooService;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooService}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ServiceMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final Comparator<FieldMetadata> FIELD_METADATA_COMPARATOR =
      new Comparator<FieldMetadata>() {
        @Override
        public int compare(FieldMetadata field1, FieldMetadata field2) {
          return field1.getFieldName().compareTo(field2.getFieldName());
        }
      };



  private final JavaType entity;
  private final JavaType identifierType;
  private final List<MethodMetadata> finders;
  private final MethodMetadata findAllGlobalSearchMethod;
  private final List<MethodMetadata> transactionalDefinedMethod;
  private final List<MethodMetadata> notTransactionalDefinedMethod;
  private final Map<FieldMetadata, MethodMetadata> countByReferenceFieldDefinedMethod;
  private final Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllDefinedMethods;
  private final List<MethodMetadata> customCountMethods;
  private final JpaEntityMetadata entityMetadata;
  private final RepositoryJpaMetadata repositoryMetadata;
  private final MethodMetadata saveMethod;
  private final MethodMetadata deleteMethod;
  private final MethodMetadata saveBatchMethod;
  private final MethodMetadata deleteBatchMethod;
  private final MethodMetadata findOneMethod;
  private final MethodMetadata findAllMethod;
  private final MethodMetadata findAllIterableMethod;
  private final MethodMetadata countMethod;
  private final MethodMetadata findAllWithGlobalSearchMethod;
  private final Map<JavaType, JpaEntityMetadata> relatedEntitiesMetadata;
  private final MethodMetadata deleteIdMethod;


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
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param entity entity referenced on interface
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param entityMetadata
   * @param repositoryMetadata
   * @param finders list of finders added to current entity
   * @param findAllGlobalSearchMethod MethodMetadata with findAllGlobalSearch method
   * @param referencedFieldsFindAllMethods
   * @param countByReferencedFieldsMethods
   * @param customCountMethods
   * @param relatedEntities
   *
   */
  public ServiceMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JavaType identifierType, final JpaEntityMetadata entityMetadata,
      RepositoryJpaMetadata repositoryMetadata, final List<MethodMetadata> finders,
      final MethodMetadata findAllGlobalSearchMethod,
      final Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethods,
      final Map<FieldMetadata, MethodMetadata> countByReferencedFieldsMethods,
      final List<MethodMetadata> customCountMethods,
      Map<JavaType, JpaEntityMetadata> relatedEntities) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    Validate.notNull(entity, "ERROR: Entity required to generate service interface");
    Validate.notNull(identifierType,
        "ERROR: Entity identifier type required to generate service interface");

    this.entity = entity;
    this.identifierType = identifierType;
    this.entityMetadata = entityMetadata;
    this.relatedEntitiesMetadata = relatedEntities;
    this.repositoryMetadata = repositoryMetadata;
    this.finders = finders;
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllDefinedMethods =
        new HashMap<FieldMetadata, MethodMetadata>();
    List<MethodMetadata> transactionalDefinedMethod = new ArrayList<MethodMetadata>();
    List<MethodMetadata> notTransactionalDefinedMethod = new ArrayList<MethodMetadata>();
    Map<FieldMetadata, MethodMetadata> countByReferenceFieldDefinedMethod =
        new HashMap<FieldMetadata, MethodMetadata>();
    this.customCountMethods = Collections.unmodifiableList(customCountMethods);

    this.findOneMethod = getFindOneMethod();
    notTransactionalDefinedMethod.add(findOneMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(findOneMethod));

    // Generating persistent methods for modifiable entities
    // (not reandOnly an no composition child)
    if (entityMetadata.isReadOnly() || entityMetadata.isCompositionChild()) {
      this.saveMethod = null;
      this.deleteMethod = null;
      this.saveBatchMethod = null;
      this.deleteBatchMethod = null;
      this.deleteIdMethod = null;
    } else {
      // Add modification methods
      this.saveMethod = getSaveMethod();
      transactionalDefinedMethod.add(saveMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(saveMethod));

      this.deleteMethod = getDeleteMethod();
      transactionalDefinedMethod.add(deleteMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteMethod));

      this.deleteIdMethod = getDeleteIdMethod();
      transactionalDefinedMethod.add(deleteIdMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteIdMethod));


      this.saveBatchMethod = getSaveBatchMethod();
      transactionalDefinedMethod.add(saveBatchMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(saveBatchMethod));

      this.deleteBatchMethod = getDeleteBatchMethod();
      transactionalDefinedMethod.add(deleteBatchMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteBatchMethod));
    }

    // Add standard finders methods (if not composition child)
    if (entityMetadata.isCompositionChild()) {
      // No standard finder methods
      this.findAllMethod = null;
      this.findAllIterableMethod = null;
      this.countMethod = null;
      this.findAllWithGlobalSearchMethod = null;

    } else {
      // Add standard finders methods
      this.findAllMethod = getFindAllMethod();
      notTransactionalDefinedMethod.add(findAllMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(findAllMethod));

      this.findAllIterableMethod = getFindAllIterableMethod();
      notTransactionalDefinedMethod.add(findAllIterableMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(findAllIterableMethod));

      this.countMethod = getCountMethod();
      notTransactionalDefinedMethod.add(countMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));

      // Generating findAll method that includes GlobalSearch parameter
      this.findAllWithGlobalSearchMethod = getFindAllGlobalSearchMethod();
      notTransactionalDefinedMethod.add(findAllWithGlobalSearchMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(findAllWithGlobalSearchMethod));
    }

    // Add relation management methods
    Map<RelationInfo, MethodMetadata> addToRelationMethods =
        new TreeMap<RelationInfo, MethodMetadata>();
    Map<RelationInfo, MethodMetadata> removeFromRelationMethods =
        new TreeMap<RelationInfo, MethodMetadata>();
    MethodMetadata tmpMethod;
    for (RelationInfo relationInfo : entityMetadata.getRelationInfos().values()) {

      if (!(relationInfo.type == JpaRelationType.COMPOSITION && relationInfo.cardinality == Cardinality.ONE_TO_ONE)) {
        // addToRELATION
        tmpMethod = getAddToRelationMethod(relationInfo);
        addToRelationMethods.put(relationInfo, tmpMethod);
        ensureGovernorHasMethod(new MethodMetadataBuilder(tmpMethod));

        // removeFromRELATION
        tmpMethod = getRemoveFromRelationMethod(relationInfo);
        removeFromRelationMethods.put(relationInfo, tmpMethod);
        ensureGovernorHasMethod(new MethodMetadataBuilder(tmpMethod));
      }
    }

    // Generating finders
    for (MethodMetadata finder : finders) {
      MethodMetadata finderMethod = getFinderMethod(finder);
      notTransactionalDefinedMethod.add(finderMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(finderMethod));
    }

    // Generating count finder methods
    for (MethodMetadata customCountMethod : customCountMethods) {
      MethodMetadata customCountServiceMethod = getCustomCountMethod(customCountMethod);
      notTransactionalDefinedMethod.add(customCountServiceMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(customCountServiceMethod));
    }

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(FIELD_METADATA_COMPARATOR);
    referencedFieldsFindAllMethodsOrderedByFieldName.putAll(referencedFieldsFindAllMethods);

    // Generating all findAll method for every referenced fields
    for (Entry<FieldMetadata, MethodMetadata> findAllReferencedFieldMethod : referencedFieldsFindAllMethodsOrderedByFieldName
        .entrySet()) {
      MethodMetadata method =
          getFindAllReferencedFieldMethod(findAllReferencedFieldMethod.getValue());
      referencedFieldsFindAllDefinedMethods.put(findAllReferencedFieldMethod.getKey(), method);
      ensureGovernorHasMethod(new MethodMetadataBuilder(method));
    }

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> countByReferencedFieldsMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(FIELD_METADATA_COMPARATOR);
    countByReferencedFieldsMethodsOrderedByFieldName.putAll(countByReferencedFieldsMethods);

    // Generating all countByReferencedField methods
    if (countByReferencedFieldsMethods != null) {
      for (Entry<FieldMetadata, MethodMetadata> countByReferencedFieldMethod : countByReferencedFieldsMethodsOrderedByFieldName
          .entrySet()) {
        MethodMetadata method =
            getCountByReferencedFieldMethod(countByReferencedFieldMethod.getValue());
        countByReferenceFieldDefinedMethod.put(countByReferencedFieldMethod.getKey(), method);
        ensureGovernorHasMethod(new MethodMetadataBuilder(method));
      }
    }

    this.referencedFieldsFindAllDefinedMethods =
        Collections.unmodifiableMap(referencedFieldsFindAllDefinedMethods);
    this.transactionalDefinedMethod = Collections.unmodifiableList(transactionalDefinedMethod);
    this.notTransactionalDefinedMethod =
        Collections.unmodifiableList(notTransactionalDefinedMethod);
    this.countByReferenceFieldDefinedMethod =
        Collections.unmodifiableMap(countByReferencedFieldsMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private MethodMetadata getRemoveFromRelationMethod(RelationInfo relationInfo) {
    final MethodMetadata entityRemoveMethod = relationInfo.removeMethod;
    // Define method name
    JavaSymbolName methodName = entityRemoveMethod.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // add parent entity parameter
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(entity.getSimpleTypeName())));

    // Get related entity metadata
    JpaEntityMetadata childEntityMetadata = relatedEntitiesMetadata.get(relationInfo.childType);
    Validate.notNull(childEntityMetadata, "Can't get entity metadata for %s entity generating %s",
        relationInfo.childType, aspectName);

    if (relationInfo.cardinality != Cardinality.ONE_TO_ONE) {
      // add child entity parameter
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType
          .iterableOf(childEntityMetadata.getCurrentIndentifierField().getFieldType())));
      parameterNames.add(entityRemoveMethod.getParameterNames().get(0));
    }

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName, entity,
            parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  private MethodMetadata getAddToRelationMethod(RelationInfo relationInfo) {
    final MethodMetadata entityAddMethod = relationInfo.addMethod;
    // Define method name
    JavaSymbolName methodName = entityAddMethod.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // add parent entity parameter
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(entity.getSimpleTypeName())));



    // add child entity parameter
    parameterNames.add(entityAddMethod.getParameterNames().get(0));
    if (relationInfo.cardinality == Cardinality.ONE_TO_ONE) {
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(relationInfo.childType));
    } else {
      // Get related entity metadata
      JpaEntityMetadata childEntityMetadata = relatedEntitiesMetadata.get(relationInfo.childType);
      Validate.notNull(childEntityMetadata,
          "Can't get entity metadata for %s entity generating %s", relationInfo.childType,
          aspectName);
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType
          .iterableOf(childEntityMetadata.getCurrentIndentifierField().getFieldType())));
    }


    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName, entity,
            parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" method. This method includes
   * GlobalSearch parameters to be able to filter results.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getFindAllGlobalSearchMethod() {
    // Define method name
    JavaSymbolName methodName = this.findAllGlobalSearchMethod.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = this.findAllGlobalSearchMethod.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = this.findAllGlobalSearchMethod.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            this.findAllGlobalSearchMethod.getReturnType(), parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" method.
   *
   * @return MethodMetadataBuilder with public List <Entity> findAll();
   *         structure
   */
  private MethodMetadata getFindAllMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findAll");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    JavaType listEntityJavaType = JavaType.listOf(entity);

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" with iterable parameter.
   *
   * @return MethodMetadataBuilder with public List <Entity> findAll(Iterable
   *         <Long> ids) structure
   */
  private MethodMetadata getFindAllIterableMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findAll");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.iterableOf(identifierType)));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }


    JavaType listEntityJavaType = JavaType.listOf(entity);

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }


  /**
   * Method that generates method "findOne".
   *
   * @return MethodMetadataBuilder with public Entity findOne(Long id);
   *         structure
   */
  private MethodMetadata getFindOneMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findOne");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(identifierType));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(entityMetadata.getCurrentIndentifierField().getFieldName());

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName, entity,
            parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "count".
   *
   * @return MethodMetadataBuilder with public long count();
   *         structure
   */
  private MethodMetadata getCountMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("count");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.LONG_PRIMITIVE, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "save" method.
   *
   * @return MethodMetadataBuilder with public Entity save(Entity entity);
   *         structure
   */
  private MethodMetadata getSaveMethod() {
    // Define save method
    JavaSymbolName methodName = new JavaSymbolName("save");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.entity));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entity"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            this.entity, parameterTypes, parameterNames, null);

    // Build a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Method that generates "delete" method.
   *
   * @return MethodMetadataBuilder with public void delete(Entity entity); structure
   */
  private MethodMetadata getDeleteMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.entity));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames
        .add(new JavaSymbolName(StringUtils.uncapitalize(this.entity.getSimpleTypeName())));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    // Build a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Method that generates "delete" (by id) method.
   *
   * @return MethodMetadataBuilder with public void delete(Entity entity); structure
   */
  private MethodMetadata getDeleteIdMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.identifierType));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(entityMetadata.getCurrentIndentifierField().getFieldName());

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    // Build a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Method that generates "save" batch method.
   *
   * @return MethodMetadataBuilder with public List<Entity> save(Iterable
   *         <Entity> entities); structure
   */
  private MethodMetadata getSaveBatchMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("save");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.iterableOf(entity)));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entities"));

    JavaType listEntityJavaType = JavaType.listOf(entity);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    // Build a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Method that generates "delete" batch method
   *
   * @return MethodMetadataBuilder with public void delete(Iterable
   *         <Long> ids); structure
   */
  private MethodMetadata getDeleteBatchMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.iterableOf(identifierType)));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    // Build a MethodMetadata instance
    MethodMetadata methodMetadata = methodBuilder.build();

    // delete method must be defined with @Transactional
    //methodMetadata.setTransactional(true);

    return methodMetadata;
  }

  /**
   * Method that generates countByReferencedField method on current interface
   *
   * @param countMethod
   * @return
   */
  private MethodMetadata getCountByReferencedFieldMethod(MethodMetadata countMethod) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    List<AnnotatedJavaType> methodParamTypes = countMethod.getParameterTypes();
    List<JavaSymbolName> methodParamNames = countMethod.getParameterNames();
    for (int i = 0; i < countMethod.getParameterTypes().size(); i++) {
      parameterTypes.add(methodParamTypes.get(i));
      parameterNames.add(methodParamNames.get(i));
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            countMethod.getMethodName(), countMethod.getReturnType(), parameterTypes,
            parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates findAll method for provided referenced fields on current interface
   *
   * @param countMethod
   * @return
   */
  private MethodMetadata getFindAllReferencedFieldMethod(MethodMetadata method) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    List<AnnotatedJavaType> methodParamTypes = method.getParameterTypes();
    List<JavaSymbolName> methodParamNames = method.getParameterNames();
    for (int i = 0; i < method.getParameterTypes().size(); i++) {
      parameterTypes.add(methodParamTypes.get(i));
      parameterNames.add(methodParamNames.get(i));
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            method.getMethodName(), method.getReturnType(), parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates finder method on current interface
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderMethod(MethodMetadata finderMethod) {

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            finderMethod.getMethodName(), finderMethod.getReturnType(),
            finderMethod.getParameterTypes(), finderMethod.getParameterNames(), null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates custom count method.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCustomCountMethod(MethodMetadata customCountMethod) {

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            customCountMethod.getMethodName(), customCountMethod.getReturnType(),
            customCountMethod.getParameterTypes(), customCountMethod.getParameterNames(), null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns all defined methods in service interface
   * that aren transactional
   *
   * @return
   */
  public List<MethodMetadata> getTransactionalDefinedMethods() {
    return this.transactionalDefinedMethod;
  }

  /**
   * This method returns all defined methods in service interface
   * that aren't transactional
   *
   * @return
   */
  public List<MethodMetadata> getNotTransactionalDefinedMethods() {
    return this.notTransactionalDefinedMethod;
  }

  /**
   * This method returns all defined count methods in
   * service interface
   *
   * @return
   */
  public Map<FieldMetadata, MethodMetadata> getCountByReferenceFieldDefinedMethod() {
    return this.countByReferenceFieldDefinedMethod;
  }

  /**
   * This method returns all defined findAll methods for referenced fields in
   * service interface
   *
   * @return
   */
  public Map<FieldMetadata, MethodMetadata> getReferencedFieldsFindAllDefinedMethods() {
    return this.referencedFieldsFindAllDefinedMethods;
  }

  /**
   * Method that returns the finder methos.
   *
   * @return a list of finder methods
   */
  public List<MethodMetadata> getFinders() {
    return finders;
  }

  /**
   * Method that returns the count methods.
   *
   * @return a list of count methods
   */
  public List<MethodMetadata> getCountMethods() {
    return this.customCountMethods;
  }

  public MethodMetadata getCurrentSaveMethod() {
    return this.saveMethod;
  }

  public MethodMetadata getCurrentSaveBatchMethod() {
    return this.saveBatchMethod;
  }

  public MethodMetadata getCurrentDeleteBatchMethod() {
    return this.deleteBatchMethod;
  }

  public MethodMetadata getCurrentDeleteMethod() {
    return this.deleteMethod;
  }

  public MethodMetadata getCurrentFindOneMethod() {
    return this.findOneMethod;
  }

  public MethodMetadata getCurrentFindAllMethod() {
    return this.findAllMethod;
  }

  public MethodMetadata getCurrentFindAllIterableMethod() {
    return this.findAllIterableMethod;
  }

  public MethodMetadata getCurrentCountMethod() {
    return this.countMethod;
  }

  public MethodMetadata getCurrentFindAllWithGlobalSearchMethod() {
    return this.findAllWithGlobalSearchMethod;
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
