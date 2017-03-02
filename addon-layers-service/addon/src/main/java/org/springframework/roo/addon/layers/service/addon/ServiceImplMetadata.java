package org.springframework.roo.addon.layers.service.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooServiceImpl}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @author Sergio Clares
 * @since 2.0
 */
public class ServiceImplMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ServiceImplMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaSymbolName GET_ENTITY_TYPE_METHOD_NAME = new JavaSymbolName(
      "getEntityType");
  private static final JavaSymbolName GET_ID_TYPE_METHOD_NAME = new JavaSymbolName("getIdType");

  private static final AnnotationMetadata LAZY_ANNOTATION = new AnnotationMetadataBuilder(
      SpringJavaType.LAZY).build();
  private static final JavaSymbolName FIND_ONE_DETACHED = new JavaSymbolName("findOneDetached");

  private ImportRegistrationResolver importResolver;

  private final JavaType repository;
  private final JavaType entity;
  private final Map<FieldMetadata, MethodMetadata> allCountByReferencedFieldMethods;
  private final Map<FieldMetadata, MethodMetadata> allFindAllByReferencedFieldMethods;
  private final MethodMetadata findAllIterableMethod;
  private final FieldMetadata repositoryFieldMetadata;
  private final Map<JavaType, FieldMetadata> requiredServiceFieldByEntity;
  private final ServiceMetadata serviceMetadata;
  private final JpaEntityMetadata entityMetadata;
  private final List<Pair<FieldMetadata, RelationInfo>> childRelationsInfo;
  private final JavaType entityIdentifierType;

  // Temporal arrays don't share
  private ArrayList<MethodMetadata> pendingTransactionalMethodToAdd;
  private ArrayList<MethodMetadata> pendingNonTransactionalMethodToAdd;

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
   * @param identifier
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param serviceInterface
   *            JavaType with interface that this service will implement
   * @param repositoryMetadata
   * @param entityMetadata
   * @param serviceMetadata
   * @param requiredServicesByEntity
   * @param childRelationsInfo
   */
  public ServiceImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType serviceInterface,
      final JavaType repository, RepositoryJpaMetadata repositoryMetadata, final JavaType entity,
      JpaEntityMetadata entityMetadata, ServiceMetadata serviceMetadata,
      Map<JavaType, ServiceMetadata> requiredServicesByEntity,
      List<Pair<FieldMetadata, RelationInfo>> childRelationsInfo) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.entity = entity;
    this.repository = repository;
    this.findAllIterableMethod = serviceMetadata.getCurrentFindAllIterableMethod();
    this.allCountByReferencedFieldMethods =
        Collections.unmodifiableMap(serviceMetadata.getCountByReferenceFieldDefinedMethod());
    this.allFindAllByReferencedFieldMethods =
        Collections.unmodifiableMap(serviceMetadata.getReferencedFieldsFindAllDefinedMethods());
    this.serviceMetadata = serviceMetadata;
    this.entityMetadata = entityMetadata;
    this.childRelationsInfo = childRelationsInfo;
    this.entityIdentifierType = serviceMetadata.getIdType();

    // All services should include @Service annotation
    AnnotationMetadataBuilder serviceAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.SERVICE);
    ensureGovernorIsAnnotated(serviceAnnotation);

    // All service related with repository should be generated with
    // @Transactional(readOnly = true) annotation
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Services should include repository field if there's
    // a repository related with managed entity
    this.repositoryFieldMetadata = getFieldFor(repository);
    ensureGovernorHasField(new FieldMetadataBuilder(repositoryFieldMetadata));

    // Add fields for required services to
    Map<JavaType, FieldMetadata> requiredServiceFieldByEntityTemp =
        new TreeMap<JavaType, FieldMetadata>();
    for (Entry<JavaType, ServiceMetadata> service : requiredServicesByEntity.entrySet()) {
      FieldMetadata field = getFieldFor(service.getValue().getDestination());
      requiredServiceFieldByEntityTemp.put(service.getKey(), field);
    }
    for (FieldMetadata field : requiredServiceFieldByEntityTemp.values()) {
      ensureGovernorHasField(new FieldMetadataBuilder(field));
    }
    this.requiredServiceFieldByEntity =
        Collections.unmodifiableMap(requiredServiceFieldByEntityTemp);

    // Add constructor
    ensureGovernorHasConstructor(getConstructor());

    pendingTransactionalMethodToAdd =
        new ArrayList<MethodMetadata>(serviceMetadata.getTransactionalDefinedMethods());
    pendingNonTransactionalMethodToAdd =
        new ArrayList<MethodMetadata>(serviceMetadata.getNotTransactionalDefinedMethods());

    // Generating all addTo methods that should be implemented
    for (Entry<RelationInfo, MethodMetadata> entry : serviceMetadata.getAddToRelationMethods()
        .entrySet()) {
      ensureGovernorHasMethod(
          new MethodMetadataBuilder(getMethodAddTo(entry.getValue(), entry.getKey())),
          entry.getValue());
    }

    // Generating all removeFrom methods that should be implemented
    for (Entry<RelationInfo, MethodMetadata> entry : serviceMetadata.getRemoveFromRelationMethods()
        .entrySet()) {
      ensureGovernorHasMethod(
          new MethodMetadataBuilder(getMethodRemoveFrom(entry.getValue(), entry.getKey())),
          entry.getValue());
    }

    // Generating all setRelation methods that should be implemented
    for (Entry<RelationInfo, MethodMetadata> entry : serviceMetadata.getSetRelationMethods()
        .entrySet()) {
      ensureGovernorHasMethod(
          new MethodMetadataBuilder(getSetRelation(entry.getValue(), entry.getKey())),
          entry.getValue());
    }

    // Generating transactional methods that should be implemented
    for (MethodMetadata method : pendingTransactionalMethodToAdd) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getMethod(method, true)));
    }

    // Generating not transactional methods that should be implemented
    for (MethodMetadata method : pendingNonTransactionalMethodToAdd) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getMethod(method)));
    }

    // ROO-3868: New entity visualization support
    ensureGovernorHasMethod(new MethodMetadataBuilder(getEntityTypeGetterMethod()));
    ensureGovernorHasMethod(new MethodMetadataBuilder(getIdentifierTypeGetterMethod()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private MethodMetadata getMethodRemoveFrom(MethodMetadata methodToBeImplemented,
      RelationInfo relationInfo) {
    return getMethodAddRemoveRel(methodToBeImplemented, relationInfo, relationInfo.removeMethod);
  }

  private MethodMetadata getMethodAddRemoveRel(MethodMetadata methodToBeImplemented,
      RelationInfo relationInfo, MethodMetadata operationMethod) {

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Prepare constants
    final String operation = operationMethod.getMethodName().getSymbolName();
    final JavaSymbolName param0 = methodToBeImplemented.getParameterNames().get(0);
    final JavaType childType = relationInfo.childType;
    final FieldMetadata childServideField = requiredServiceFieldByEntity.get(childType);
    final String parentFieldName = relationInfo.fieldName;
    final JavaSymbolName param1 = methodToBeImplemented.getParameterNames().get(1);
    final JavaType param1TypeWrapped =
        methodToBeImplemented.getParameterTypes().get(1).getJavaType().getParameters().get(0);
    final String saveMethod =
        serviceMetadata.getCurrentSaveMethod().getMethodName().getSymbolName();

    String childListVariable;

    if (childType.equals(param1TypeWrapped)) {
      childListVariable = param1.getSymbolName();
    } else {
      // List<{childType}> {parentFieldName} =
      // {childService}.findAll({param1});
      bodyBuilder.appendFormalLine("%s<%s> %s = %s().findAll(%s);",
          getNameOfJavaType(JavaType.LIST), getNameOfJavaType(childType), parentFieldName,
          getAccessorMethod(childServideField).getMethodName(), param1);
      childListVariable = parentFieldName;
    }
    // {param0}.{operation}({childListVariable});
    bodyBuilder.appendFormalLine("%s.%s(%s);", param0, operation, childListVariable);

    // return {repoField}.{saveMethod}({param0});
    bodyBuilder.appendFormalLine("return %s().%s(%s);", getAccessorMethod(repositoryFieldMetadata)
        .getMethodName(), saveMethod, param0);
    return getMethod(methodToBeImplemented, true, bodyBuilder);
  }

  /**
   * Builds a method which returns the class of entity JavaType.
   * 
   * @return MethodMetadataBuilder
   */
  private MethodMetadata getEntityTypeGetterMethod() {
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    MethodMetadata existingMethod =
        getGovernorMethod(GET_ENTITY_TYPE_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return ENTITY_TYPE.class;
    bodyBuilder.appendFormalLine("return %s.class;",
        this.entity.getNameIncludingTypeParameters(false, importResolver));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, GET_ENTITY_TYPE_METHOD_NAME,
            JavaType.wrapperOf(JavaType.CLASS, this.entity), parameterTypes, parameterNames,
            bodyBuilder);

    // Build and return a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Builds a method which returns the class of the entity identifier JavaType.
   * 
   * @return MethodMetadataBuilder
   */
  private MethodMetadata getIdentifierTypeGetterMethod() {
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    MethodMetadata existingMethod =
        getGovernorMethod(GET_ID_TYPE_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return IDENTIFIER_TYPE.class;
    bodyBuilder.appendFormalLine("return %s.class;", getNameOfJavaType(this.entityIdentifierType));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, GET_ID_TYPE_METHOD_NAME,
            JavaType.wrapperOf(JavaType.CLASS, this.serviceMetadata.getIdType()), parameterTypes,
            parameterNames, bodyBuilder);

    // Build and return a MethodMetadata instance
    return methodBuilder.build();
  }

  private MethodMetadata getSetRelation(MethodMetadata methodToBeImplemented,
      RelationInfo relationInfo) {

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Prepare constants
    final JavaSymbolName param0 = methodToBeImplemented.getParameterNames().get(0);
    final JavaType childType = relationInfo.childType;
    final FieldMetadata childServideField = requiredServiceFieldByEntity.get(childType);
    final String childTypeNameJavaType = getNameOfJavaType(childType);
    final JavaSymbolName param1 = methodToBeImplemented.getParameterNames().get(1);
    final String saveMethod =
        serviceMetadata.getCurrentSaveMethod().getMethodName().getSymbolName();

    String childListVariable = "items";
    // List<{childType}> {parentFieldName} =
    // {childService}.findAll({param1});
    bodyBuilder.appendFormalLine("%s<%s> %s = %s().findAll(%s);", getNameOfJavaType(JavaType.LIST),
        childTypeNameJavaType, childListVariable, getAccessorMethod(childServideField)
            .getMethodName(), param1);

    // Set<{childType}> currents = {param0}.get{rel.property}();
    bodyBuilder.appendFormalLine("%s currents = %s.get%s();",
        getNameOfJavaType(relationInfo.fieldMetadata.getFieldType()), param0,
        relationInfo.fieldMetadata.getFieldName().getSymbolNameCapitalisedFirstLetter());

    // Set<{childType}> toRemove = new
    // HashSet<{childType}>({parentFieldName});
    bodyBuilder.appendFormalLine("%s<%s> toRemove = new %s<%s>();",
        getNameOfJavaType(JavaType.SET), childTypeNameJavaType,
        getNameOfJavaType(JdkJavaType.HASH_SET), childTypeNameJavaType);

    // for (Iterator<{childType}> iterator = current.iterator();
    // iterator.hasNext();) {
    bodyBuilder.appendFormalLine(
        "for (%s<%s> iterator = currents.iterator(); iterator.hasNext();) {",
        getNameOfJavaType(JavaType.ITERATOR), childTypeNameJavaType);

    bodyBuilder.indent();
    final String itemName = "next".concat(StringUtils.capitalize(childType.getSimpleTypeName()));
    // {childType} {itemName} = iterator.next();
    bodyBuilder.appendFormalLine("%s %s = iterator.next();", childTypeNameJavaType, itemName);

    // if ({childListVariable}.contains({itemName})) {
    bodyBuilder.appendFormalLine("if (%s.contains(%s)) {", childListVariable, itemName);

    bodyBuilder.indent();
    // {childListVariable}.remove({itemName});
    bodyBuilder.appendFormalLine("%s.remove(%s);", childListVariable, itemName);

    bodyBuilder.indentRemove();
    // } else {
    bodyBuilder.appendFormalLine("} else {");

    bodyBuilder.indent();

    // toRemove.add(product);
    bodyBuilder.appendFormalLine("toRemove.add(%s);", itemName);

    bodyBuilder.indentRemove();
    // }
    bodyBuilder.appendFormalLine("}");

    // }
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // {param0}.{removeFromMethod}(toRemove);
    bodyBuilder.appendFormalLine("%s.%s(toRemove);", param0,
        relationInfo.removeMethod.getMethodName());

    // {param0}.{addToMethod}({childListVariable);
    bodyBuilder.appendFormalLine("%s.%s(%s);", param0, relationInfo.addMethod.getMethodName(),
        childListVariable);

    // // Force the version update of the parent side to know that the parent has changed
    bodyBuilder
        .appendFormalLine("// Force the version update of the parent side to know that the parent has changed");

    // // because it has new books assigned
    bodyBuilder.appendFormalLine("// because it has new books assigned");

    // {param0}.setVersion({param0}.getVersion() + 1);
    bodyBuilder.appendFormalLine("%s.setVersion(%s.getVersion() + 1);", param0, param0);

    // return {repoField}.{saveMethod}({param0});
    bodyBuilder.appendFormalLine("return %s().%s(%s);", getAccessorMethod(repositoryFieldMetadata)
        .getMethodName(), saveMethod, param0);
    return getMethod(methodToBeImplemented, true, bodyBuilder);
  }

  private MethodMetadata getMethodAddTo(MethodMetadata methodToBeImplemented,
      RelationInfo relationInfo) {
    return getMethodAddRemoveRel(methodToBeImplemented, relationInfo, relationInfo.addMethod);
  }

  /**
   * Delegates on supper and remove original from
   * {@link #pendingNonTransactionalMethodToAdd} and
   * {@link #pendingNonTransactionalMethodToAdd} to avoid generate two times
   * the same method
   *
   * @param methodMetadata
   * @param original
   */
  private void ensureGovernorHasMethod(final MethodMetadataBuilder methodMetadata,
      final MethodMetadata original) {
    ensureGovernorHasMethod(methodMetadata);

    removeMethodFromPendingList(pendingNonTransactionalMethodToAdd, original);
    removeMethodFromPendingList(pendingTransactionalMethodToAdd, original);
  }

  /**
   * Removes metadataToRemove from list
   *
   * Compares, method name, number params and params types to decide it method
   * match and should be removed from list.
   *
   * @param list
   * @param metadataToRemove
   */
  private void removeMethodFromPendingList(List<MethodMetadata> list,
      MethodMetadata metadataToRemove) {
    final Iterator<MethodMetadata> iter = list.iterator();
    MethodMetadata current;
    boolean matchTypes;
    while (iter.hasNext()) {
      current = iter.next();
      matchTypes = true;
      // Check name
      if (!current.getMethodName().equals(metadataToRemove.getMethodName())) {
        continue;
      }

      // Check number of params
      if (current.getParameterTypes().size() != metadataToRemove.getParameterTypes().size()) {
        continue;
      }

      // Check params types
      for (int i = 0; i < current.getParameterTypes().size(); i++) {
        if (!current.getParameterTypes().get(i).equals(metadataToRemove.getParameterTypes().get(i))) {
          matchTypes = false;
          break;
        }
      }

      // all matches
      if (matchTypes) {
        // remove item
        iter.remove();
      }
    }
  }

  /**
   * Method that generates Service implementation constructor. If exists a
   * repository, it will be included as constructor parameter
   *
   * @return
   */
  private ConstructorMetadataBuilder getConstructor() {

    ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Append repository parameter if needed
    if (repositoryFieldMetadata != null) {
      addFieldToConstructor(bodyBuilder, constructorBuilder, repositoryFieldMetadata, false);
    }

    for (FieldMetadata requiredService : requiredServiceFieldByEntity.values()) {
      addFieldToConstructor(bodyBuilder, constructorBuilder, requiredService, true);
    }

    constructorBuilder.setBodyBuilder(bodyBuilder);
    constructorBuilder.setModifier(Modifier.PUBLIC);

    // Adding @Autowired annotation
    constructorBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return constructorBuilder;
  }

  /**
   * Adds a field to constructor parameters and body
   *
   * @param bodyBuilder
   * @param constructorBuilder
   * @param field
   * @param lazy
   */
  private void addFieldToConstructor(final InvocableMemberBodyBuilder bodyBuilder,
      final ConstructorMetadataBuilder constructorBuilder, final FieldMetadata field,
      final boolean lazy) {
    final String fieldName = field.getFieldName().getSymbolName();
    final JavaType fieldType = field.getFieldType();
    if (lazy) {
      importResolver.addImport(SpringJavaType.LAZY);
      constructorBuilder.addParameterName(field.getFieldName());
      constructorBuilder.addParameterType(new AnnotatedJavaType(fieldType, LAZY_ANNOTATION));
    } else {
      constructorBuilder.addParameter(fieldName, fieldType);
    }
    bodyBuilder.appendFormalLine(String.format("%s(" + fieldName + ");", getMutatorMethod(field)
        .getMethodName()));
  }

  /**
   * Method that generates implementation of provided method
   *
   * @return MethodMetadataBuilder
   */
  private MethodMetadata getMethod(final MethodMetadata methodToBeImplemented) {
    return getMethod(methodToBeImplemented, false);
  }

  /**
   * Method that generates implementation of provided method
   *
   * @return MethodMetadataBuilder
   */
  private MethodMetadata getMethod(final MethodMetadata methodToBeImplemented,
      boolean isTransactional) {

    // Check if is batch method
    boolean isBatch = false;
    for (AnnotatedJavaType parameters : methodToBeImplemented.getParameterTypes()) {
      JavaType type = parameters.getJavaType();
      isBatch = !type.getParameters().isEmpty();
      if (isBatch) {
        break;
      }
    }

    // Check what is the particular method
    boolean isDelete = methodToBeImplemented.getMethodName().getSymbolName().equals("delete");
    boolean isSaveMethod =
        methodToBeImplemented.equals(this.serviceMetadata.getCurrentSaveMethod());
    boolean isFindOneForUpdate =
        methodToBeImplemented.getMethodName().getSymbolName().equals("findOneForUpdate");

    InvocableMemberBodyBuilder bodyBuilder;
    if (isDelete) {
      bodyBuilder = builDeleteMethodBody(methodToBeImplemented, isBatch);
    } else if (isSaveMethod) {
      bodyBuilder = builSaveMethodBody(methodToBeImplemented);
    } else if (isFindOneForUpdate) {
      bodyBuilder = buildFindOneForUpdateBody(methodToBeImplemented);
    } else {
      bodyBuilder = builMethodBody(methodToBeImplemented);
    }

    return getMethod(methodToBeImplemented, isTransactional, bodyBuilder);
  }

  /**
   * Method that generates implementation of provided method with specified
   * body
   *
   * @param methodToBeImplemented
   * @param isTransactional
   * @param bodyBuilder
   * @return
   */
  private MethodMetadata getMethod(final MethodMetadata methodToBeImplemented,
      boolean isTransactional, InvocableMemberBodyBuilder bodyBuilder) {
    List<JavaSymbolName> parameterNames =
        new ArrayList<JavaSymbolName>(methodToBeImplemented.getParameterNames());

    List<AnnotatedJavaType> parameterTypes =
        new ArrayList<AnnotatedJavaType>(methodToBeImplemented.getParameterTypes());
    MethodMetadata existingMethod =
        getGovernorMethod(methodToBeImplemented.getMethodName(),
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodToBeImplemented.getMethodName(),
            methodToBeImplemented.getReturnType(), parameterTypes, parameterNames, bodyBuilder);

    // Adding @Transactional
    if (isTransactional) {
      AnnotationMetadataBuilder transactionalAnnotation =
          new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
      methodBuilder.addAnnotation(transactionalAnnotation);
    }

    // Build and return a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * Build method body which delegates on repository
   *
   * @param methodToBeImplemented
   * @param isBatch
   * @param isDelete
   * @return
   */
  private InvocableMemberBodyBuilder builMethodBody(final MethodMetadata methodToBeImplemented) {
    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting parameters String
    String parametersString = "";
    for (JavaSymbolName parameterName : methodToBeImplemented.getParameterNames()) {
      parametersString = parametersString.concat(parameterName.getSymbolName()).concat(", ");
    }
    if (StringUtils.isNotBlank(parametersString)) {
      parametersString = parametersString.substring(0, parametersString.length() - 2);
    }

    bodyBuilder.appendFormalLine("%s %s().%s(%s);",
        methodToBeImplemented.getReturnType().equals(JavaType.VOID_PRIMITIVE) ? "" : "return",
        getAccessorMethod(repositoryFieldMetadata).getMethodName(), methodToBeImplemented
            .getMethodName().getSymbolName(), parametersString);
    return bodyBuilder;
  }

  /**
   * Build method body which delegates on repository
   *
   * @param methodToBeImplemented
   * @param isBatch
   * @param isDelete
   * @return
   */
  private InvocableMemberBodyBuilder builDeleteMethodBody(
      final MethodMetadata methodToBeImplemented, boolean isBatch) {
    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final JavaSymbolName param0 = methodToBeImplemented.getParameterNames().get(0);
    final String entity = getNameOfJavaType(this.entity);

    if (isBatch) {

      // List<Entity> toDelete = repositoryField.FIND_ALL_METHOD(paramName);
      bodyBuilder.appendFormalLine("%s<%s> toDelete = %s().%s(%s);",
          getNameOfJavaType(JavaType.LIST), entity, getAccessorMethod(repositoryFieldMetadata)
              .getMethodName(), this.findAllIterableMethod.getMethodName(), param0);

      bodyBuilder.appendFormalLine("%s().deleteInBatch(toDelete);",
          getAccessorMethod(repositoryFieldMetadata).getMethodName(),
          methodToBeImplemented.getMethodName());

    } else {
      // Clear relations as child part
      for (Pair<FieldMetadata, RelationInfo> item : childRelationsInfo) {
        final RelationInfo info = item.getRight();
        final FieldMetadata field = item.getLeft();
        final JavaType childType = field.getFieldType().getBaseType();
        String mappedByCapitalized = StringUtils.capitalize(info.mappedBy);
        if (info.cardinality == Cardinality.ONE_TO_ONE) {
          // Skip
        } else if (info.cardinality == Cardinality.ONE_TO_MANY) {
          // Clear bidirectional many-to-one child relationship with Customer
          /*
          if (customerOrder.getCustomer() != null) {
            customerOrder.getCustomer().getOrders().remove(customerOrder);
          }
          */
          bodyBuilder.appendFormalLine(
              "// Clear bidirectional many-to-one child relationship with %s",
              childType.getSimpleTypeName());
          bodyBuilder.appendFormalLine("if (%s.get%s() != null) {", param0, mappedByCapitalized);
          bodyBuilder.indent();
          bodyBuilder.appendFormalLine("%s.get%s().get%s().remove(%s);", param0,
              mappedByCapitalized, info.fieldMetadata.getFieldName()
                  .getSymbolNameCapitalisedFirstLetter(), param0);
          bodyBuilder.indentRemove();
          bodyBuilder.appendFormalLine("}");
          bodyBuilder.appendFormalLine("");
        } else {
          // MANY_TO_MANY
          String childTypeName = getNameOfJavaType(childType);
          /*
           // Clear bidirectional many-to-many child relationship with categories
           for (Category category : product.getCategories()) {
             category.getProducts().remove(product);
           }
           */
          bodyBuilder.appendFormalLine(
              "// Clear bidirectional many-to-many child relationship with %s", childTypeName);
          bodyBuilder.appendFormalLine("for (%s item : %s.get%s()) {", childTypeName, param0,
              mappedByCapitalized);
          bodyBuilder.indent();
          bodyBuilder.appendFormalLine("item.get%s().remove(%s);", info.fieldMetadata
              .getFieldName().getSymbolNameCapitalisedFirstLetter(), param0);
          bodyBuilder.indentRemove();
          bodyBuilder.appendFormalLine("}");
          bodyBuilder.appendFormalLine("");
        }

      }

      // Clear relations as parent
      for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
        if (info.cardinality == Cardinality.ONE_TO_ONE) {
          /*
          // Clear bidirectional one-to-one parent relationship with Address
          customer.removeFromAddress();
          */
          bodyBuilder.appendFormalLine(
              "// Clear bidirectional one-to-one parent relationship with %s",
              info.childType.getSimpleTypeName());
          bodyBuilder.appendFormalLine("%s.%s();", param0, info.removeMethod.getMethodName());
          bodyBuilder.appendFormalLine("");
        } else if (info.cardinality == Cardinality.ONE_TO_MANY) {
          String childTypeName = getNameOfJavaType(info.childType);
          /*
          // Clear bidirectional one-to-many parent relationship with CustomerOrders
          for (CustomerOrder order : customer.getOrders()) {
            order.setCustomer(null);
          }
           */
          bodyBuilder.appendFormalLine(
              "// Clear bidirectional one-to-many parent relationship with %s", childTypeName);
          bodyBuilder.appendFormalLine("for (%s item : %s.get%s()) {", childTypeName, param0,
              info.fieldMetadata.getFieldName().getSymbolNameCapitalisedFirstLetter());
          bodyBuilder.indent();
          bodyBuilder.appendFormalLine("item.set%s(null);", StringUtils.capitalize(info.mappedBy));
          bodyBuilder.indentRemove();
          bodyBuilder.appendFormalLine("}");
          bodyBuilder.appendFormalLine("");
        } else {
          String childTypeName = getNameOfJavaType(info.childType);
          // MANY_TO_MANY

          /*
          // Clear bidirectional many-to-many parent relationship with products
          for (Product product : category.getProducts()) {
            product.getCategories().remove(category);
          }
          */
          bodyBuilder.appendFormalLine(
              "// Clear bidirectional many-to-many parent relationship with %s", childTypeName);
          bodyBuilder.appendFormalLine("for (%s item : %s.get%s()) {", childTypeName, param0,
              info.fieldMetadata.getFieldName().getSymbolNameCapitalisedFirstLetter());
          bodyBuilder.indent();
          bodyBuilder.appendFormalLine("item.get%s().remove(%s);",
              StringUtils.capitalize(info.mappedBy), param0);
          bodyBuilder.indentRemove();
          bodyBuilder.appendFormalLine("}");
          bodyBuilder.appendFormalLine("");
        }
      }

      bodyBuilder.appendFormalLine("%s().delete(%s);", getAccessorMethod(repositoryFieldMetadata)
          .getMethodName(), param0);

    }
    return bodyBuilder;
  }

  /**
   * Build "findOneForUpdate" method body which delegates on repository
   * 
   * @param methodToBeImplemented
   * @return
   */
  private InvocableMemberBodyBuilder buildFindOneForUpdateBody(MethodMetadata methodToBeImplemented) {

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    final JavaSymbolName param0 = methodToBeImplemented.getParameterNames().get(0);

    // return entityRepository.findOneDetached(id);
    bodyBuilder.appendFormalLine("%s %s().%s(%s);",
        methodToBeImplemented.getReturnType().equals(JavaType.VOID_PRIMITIVE) ? "" : "return",
        getAccessorMethod(repositoryFieldMetadata).getMethodName(),
        FIND_ONE_DETACHED.getSymbolName(), param0);
    return bodyBuilder;
  }

  /**
   * Build "save" method body which delegates on repository
   *
   * @param methodToBeImplemented
   * @param isBatch
   * @param isDelete
   * @return
   */
  private InvocableMemberBodyBuilder builSaveMethodBody(final MethodMetadata methodToBeImplemented) {
    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    final JavaSymbolName param0 = methodToBeImplemented.getParameterNames().get(0);

    /*
     * // Ensure the relationships are maintained
     * entity.addToRelatedEntity(entity.getRelatedEntity());
     */
    boolean commentAdded = false;
    Map<String, RelationInfo> relationInfos = entityMetadata.getRelationInfos();
    for (Entry<String, RelationInfo> entry : relationInfos.entrySet()) {
      RelationInfo info = entry.getValue();
      if (info.cardinality == Cardinality.ONE_TO_ONE) {
        if (!commentAdded) {
          bodyBuilder.newLine();
          bodyBuilder.appendFormalLine("// Ensure the relationships are maintained");
          commentAdded = true;
        }
        bodyBuilder.appendFormalLine("%s.%s(%s.get%s());", param0, info.addMethod.getMethodName(),
            param0, StringUtils.capitalize(entry.getKey()));
        bodyBuilder.newLine();
      }
    }

    bodyBuilder.appendFormalLine("%s %s().%s(%s);",
        methodToBeImplemented.getReturnType().equals(JavaType.VOID_PRIMITIVE) ? "" : "return",
        getAccessorMethod(repositoryFieldMetadata).getMethodName(), methodToBeImplemented
            .getMethodName().getSymbolName(), param0);
    return bodyBuilder;
  }

  /**
   * This method returns field to included on service for a Service or
   * Repository
   *
   * @param service
   * @return
   */
  private FieldMetadata getFieldFor(JavaType type) {

    // Generating service field name
    final JavaSymbolName fieldName =
        new JavaSymbolName(StringUtils.uncapitalize(type.getSimpleTypeName()));

    FieldMetadata currentField = governorTypeDetails.getField(fieldName);
    if (currentField != null) {
      Validate.isTrue(currentField.getFieldType().equals(type),
          "Field %s already in %s but type not match: expected %s", currentField.getFieldName(),
          governorTypeDetails.getType(), type);
      return currentField;
    }

    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
        new ArrayList<AnnotationMetadataBuilder>(), fieldName, type).build();
  }

  public JavaType getEntity() {
    return entity;
  }

  public JavaType getRepository() {
    return repository;
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
