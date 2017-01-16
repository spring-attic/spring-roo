package org.springframework.roo.addon.layers.repository.jpa.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaRepositoryCustomImpl}.
 *
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 2.0
 */
public class RepositoryJpaCustomImplMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String CONSTANT_SEPARATOR = "_";
  private static final String PROVIDES_TYPE_STRING = RepositoryJpaCustomImplMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType QUERYDSL_PATH = new JavaType("com.querydsl.core.types.Path");
  private static final JavaType QUERYDSL_BOOLEAN_BUILDER = new JavaType(
      "com.querydsl.core.BooleanBuilder");
  private static final JavaType QUERYDSL_PROJECTIONS = new JavaType(
      "com.querydsl.core.types.Projections");
  private static final JavaType QUERYDSL_JPQLQUERY = new JavaType("com.querydsl.jpa.JPQLQuery");


  final private ImportRegistrationResolver importResolver;
  final private JavaType entity;
  final private Map<JavaType, List<Pair<String, String>>> typesFieldMaps;
  final private JavaType defaultReturnType;
  final private Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadata;
  final private Map<JavaType, Boolean> typesAreProjections;
  final private JavaType entityQtype;
  final private JpaEntityMetadata entityMetadata;

  private Map<String, FieldMetadata> constantsForFields;

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

  /**
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param annotationValues (required)
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param domainType entity referenced on interface
   * @param entityMetadata
   * @param isDTO indicates if the provided domainType is a DTO or an entity
   * @param idFields entity id fields
   * @param validFields entity fields to search for (excluded id, reference and collection fields)
   * @param findAllGlobalSearchMethod the findAll metadata
   * @param defaultReturnType to use in finders
   * @param allFindReferencedFieldsMethods the metadata for all findByReference methods.
   * @param referencedFieldsIdentifierNames
   * @param typesFieldMaps the Map<JavaType, Map<String, String>> of each associated
   *            domain type (parent Map JavaType), property names (keys) and path names (values)
   *            for building finders which return a projection.
   * @param customFinderMethods list of custom methods
   * @param customCountMethods list of count methods for custom finder methods
   * @param typesFieldsMetadata the Map<JavaType, Map<String, FieldMetadata>> with
   *            the fields of each domain type.
   * @param typesAreProjections the Map<JavaType, Boolean> which tells if each type is
   *            a projection and must use a ConstructorExpression in finders implementations.
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      JpaEntityMetadata entityMetadata, final FieldMetadata idField,
      final List<FieldMetadata> validFields, final MethodMetadata findAllGlobalSearchMethod,
      final JavaType defaultReturnType,
      final Map<FieldMetadata, MethodMetadata> allFindReferencedFieldsMethods,
      final Map<FieldMetadata, String> referencedFieldsIdentifierNames,
      final Map<JavaType, List<Pair<String, String>>> typesFieldMaps,
      final List<Pair<MethodMetadata, PartTree>> customFinderMethods,
      final List<Pair<MethodMetadata, PartTree>> customCountMethods,
      final Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadata,
      final Map<JavaType, Boolean> typesAreProjections) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.entity = domainType;
    this.entityMetadata = entityMetadata;
    this.typesFieldMaps = typesFieldMaps;
    this.typesFieldsMetadata = typesFieldsMetadata;
    this.typesAreProjections = typesAreProjections;
    this.entityQtype = getQJavaTypeFor(domainType);

    // Get inner parameter of default return type (enclosed inside Page);
    this.defaultReturnType = defaultReturnType;

    // Initialize constants for fields map
    constantsForFields = new HashMap<String, FieldMetadata>();

    // Get repository that needs to be implemented
    ensureGovernorImplements(annotationValues.getRepository());

    // All repositories should be generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Creating new constant for every existing field in entity
    List<JavaSymbolName> addedFields = new ArrayList<JavaSymbolName>();
    for (FieldMetadata field : validFields) {
      FieldMetadata newConstant = getConstantForField(field.getFieldName().getSymbolName());
      if (!addedFields.contains(newConstant.getFieldName())) {
        ensureGovernorHasField(new FieldMetadataBuilder(newConstant));
        addedFields.add(newConstant.getFieldName());
      }
    }

    // Creating new constant for every field of the provided projections
    for (Entry<JavaType, Boolean> element : this.typesAreProjections.entrySet()) {
      JavaType projection = element.getKey();
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(projection);
      if (projectionFields != null && !projectionFields.isEmpty()) {
        Iterator<Pair<String, String>> iterator = projectionFields.iterator();
        while (iterator.hasNext()) {
          Pair<String, String> entry = iterator.next();
          FieldMetadata newConstant = getConstantForField(entry.getKey());
          if (!addedFields.contains(newConstant.getFieldName())) {
            ensureGovernorHasField(new FieldMetadataBuilder(newConstant));
            addedFields.add(newConstant.getFieldName());
          }
        }
      }
    }

    // Generate findAll implementation method
    if (findAllGlobalSearchMethod != null) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllImpl(findAllGlobalSearchMethod,
          idField, validFields)));
    }

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> allFindByReferencedFieldsMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(FieldMetadata.COMPARATOR_BY_NAME);
    allFindByReferencedFieldsMethodsOrderedByFieldName.putAll(allFindReferencedFieldsMethods);

    // Generate findAll referenced fields implementation methods
    for (Entry<FieldMetadata, MethodMetadata> method : allFindByReferencedFieldsMethodsOrderedByFieldName
        .entrySet()) {

      String referencedPathFieldName = referencedFieldsIdentifierNames.get(method.getKey());

      ensureGovernorHasMethod(new MethodMetadataBuilder(getFindByReferencedFieldsImpl(
          method.getKey(), method.getValue(), referencedPathFieldName, validFields)));
    }

    // Generate projection finder methods implementations
    if (customFinderMethods != null) {
      for (Pair<MethodMetadata, PartTree> methodInfo : customFinderMethods) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(getCustomFindersImpl(methodInfo,
            validFields)));
      }
    }

    // generate custom count methods
    for (Pair<MethodMetadata, PartTree> methodInfo : customCountMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getCustomCountImpl(methodInfo)));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates the findAll implementation method
   * @param findAllGlobalSearchMethod
   * @param ids the entity id fields
   * @param fields the entity fields to search for
   *
   * @return
   */
  private MethodMetadata getFindAllImpl(MethodMetadata findAllGlobalSearchMethod,
      FieldMetadata idField, List<FieldMetadata> fields) {

    // Define method name
    JavaSymbolName methodName = findAllGlobalSearchMethod.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = findAllGlobalSearchMethod.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = findAllGlobalSearchMethod.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use provided findAll method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
            findAllGlobalSearchMethod.getMethodName(), findAllGlobalSearchMethod.getReturnType(),
            findAllGlobalSearchMethod.getParameterTypes(),
            findAllGlobalSearchMethod.getParameterNames(), null);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable name to use in the code
    JavaSymbolName globalSearch = parameterNames.get(0);
    JavaSymbolName pageable = parameterNames.get(1);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);

    // Types to import
    JavaType projection = QUERYDSL_PROJECTIONS;

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        entityQtype.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, null, null, null, null, null,
        this.defaultReturnType, null, null);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine
        .append(String
            .format(
                "%s mapping = buildMapper()",
                getNameOfJavaType(SpringletsJavaType.SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_ATTRIBUTE_BUILDER)));

    if (!this.typesAreProjections.get(this.defaultReturnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s.%s)",
            getConstantForField(fieldName).getFieldName(), entityVariable, fieldName));
      }
    } else {

      // Return type is a projection
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(this.defaultReturnType);
      Iterator<Pair<String, String>> iterator = projectionFields.iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s)",
            getConstantForField(entry.getKey()).getFieldName(), entry.getValue()));
      }
    }
    mappingBuilderLine.append(";");
    bodyBuilder.appendFormalLine(mappingBuilderLine.toString());
    bodyBuilder.newLine();

    // applyPagination(pageable, query, mapping);
    bodyBuilder.appendFormalLine(String.format("applyPagination(%s, query, mapping);", pageable));

    //applyOrderById(query);
    bodyBuilder.appendFormalLine("applyOrderById(query);");
    bodyBuilder.newLine();


    buildQueryResult(bodyBuilder, pageable, entityVariable, projection, this.defaultReturnType);

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }


  /**
   * This method returns the associated constant to the provided
   * fieldName. 
   * 
   * If doesn't exists any constat defined to the provided fieldName
   * this method will create a new one.
   * 
   * If exists, this method will return the constant.
   * 
   * @param fieldName
   * @return
   */
  private FieldMetadata getConstantForField(String fieldName) {
    Validate.notEmpty(fieldName, "ERROR: You must provide a valid fieldName to be able to generate"
        + " the finder content");

    // If already created, return the existing constant associated 
    // to this field
    if (constantsForFields.get(fieldName) != null) {
      return constantsForFields.get(fieldName);
    }

    // If not exists, generate it and then, return its name
    String[] fieldNameParts = StringUtils.splitByCharacterTypeCamelCase(fieldName);
    String constantName = "";
    for (String part : fieldNameParts) {
      constantName = constantName.concat(part).concat(CONSTANT_SEPARATOR);
    }

    Validate.notEmpty(constantName, "ERROR: Some error appears during field constant generation.");

    // Creating the new constant name
    JavaSymbolName constantSymbolName =
        new JavaSymbolName(constantName.substring(0,
            constantName.length() - CONSTANT_SEPARATOR.length()).toUpperCase());

    // Creating field and adding it to the builder. Contants will be always publics to be used
    // in different locations.
    FieldMetadataBuilder constantField =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL,
            constantSymbolName, JavaType.STRING, "\"".concat(fieldName).concat("\""));

    // Cache it to prevent generate the same constant again
    constantsForFields.put(fieldName, constantField.build());

    return constantField.build();
  }

  /**
     * Builds the query return sentence
     *
     * @param bodyBuilder ITD body builder
     * @param pageable the Page implementation variable name
     * @param entityVariable the name of the variable owning the query
     * @param projection the projection expression for returning the query
     */
  private void buildQueryResult(InvocableMemberBodyBuilder bodyBuilder, JavaSymbolName pageable,
      String entityVariable, JavaType projection, JavaType returnType) {

    if (!this.typesAreProjections.get(returnType)) {

      // return loadPage(query, pageable, myEntity);
      bodyBuilder.appendFormalLine(String.format("return loadPage(query, pageable, %s);",
          entityVariable));
    } else {
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(returnType);

      // return loadPage(query, pageable, Projection.constructor(MyProjection.class,
      //                    myEntity.field1, myEntity.field2);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, %s, %s.constructor(%s.class, %s ));", pageable,
          getNameOfJavaType(projection), getNameOfJavaType(returnType),
          StringUtils.join(getListRightValueOfPair(projectionFields), ", ")));
    }
  }

  private List<String> getListRightValueOfPair(List<Pair<String, String>> projectionFields) {
    List<String> result = new ArrayList<String>(projectionFields.size());
    for (Pair<String, String> item : projectionFields) {
      result.add(item.getRight());
    }
    return result;
  }

  private List<String> getListLeftValueOfPair(List<Pair<String, String>> projectionFields) {
    List<String> result = new ArrayList<String>(projectionFields.size());
    for (Pair<String, String> item : projectionFields) {
      result.add(item.getLeft());
    }
    return result;
  }

  /**
   * Method that generates the findBy referenced fields implementation method
   *
   * @param the FieldMetadata of the referenced field
   * @param method to implement
   * @param referencedPathFieldName the String with the the referenced field name
   *            in "path" format.
   * @param ids the entity id fields
   * @param fields the entity fields to search for
   *
   * @return
   */
  private MethodMetadata getFindByReferencedFieldsImpl(FieldMetadata referencedField,
      MethodMetadata method, String referencedPathFieldName, List<FieldMetadata> fields) {

    // Define method name
    JavaSymbolName methodName = method.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = method.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use provided findAllByReference method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, null);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable name to use in the code
    JavaSymbolName referencedFieldParamName = parameterNames.get(0);
    JavaSymbolName globalSearch = parameterNames.get(1);
    JavaSymbolName pageable = parameterNames.get(2);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);

    // Types to import
    JavaType projection = QUERYDSL_PROJECTIONS;

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        entityQtype.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, referencedFieldParamName,
        referencedField, referencedPathFieldName, null, null, this.defaultReturnType, null, null);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine.append(String.format("%s mapping = buildMapper()",
        SpringletsJavaType.SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_ATTRIBUTE_BUILDER
            .getNameIncludingTypeParameters(false, this.importResolver)));

    // .map(entiyVarName, varName) ...
    if (!this.typesAreProjections.get(this.defaultReturnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String entityFieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s.%s)",
            getConstantForField(entityFieldName).getFieldName(), entityVariable, entityFieldName));
      }
    } else {

      // Return type is a projection
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(this.defaultReturnType);
      Iterator<Pair<String, String>> iterator = projectionFields.iterator();
      while (iterator.hasNext()) {
        Pair<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s)",
            getConstantForField(entry.getKey()).getFieldName(), entry.getValue()));
      }
    }
    mappingBuilderLine.append(";");
    bodyBuilder.appendFormalLine(mappingBuilderLine.toString());
    bodyBuilder.newLine();

    // applyPagination(pageable, query, mapping);
    bodyBuilder.appendFormalLine(String.format("applyPagination(pageable, query, mapping);",
        pageable));

    //applyOrderById(query);
    bodyBuilder.appendFormalLine("applyOrderById(query);");
    bodyBuilder.newLine();

    buildQueryResult(bodyBuilder, pageable, entityVariable, projection, this.defaultReturnType);

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates implementation methods for each finder which return type
   * is a projection or argument is a DTO.
   *
   * @param methodInfo
   * @return
   */
  private MethodMetadata getCustomFindersImpl(Pair<MethodMetadata, PartTree> methodInfo,
      List<FieldMetadata> fields) {

    MethodMetadata method = methodInfo.getLeft();
    // Define method name
    JavaSymbolName methodName = method.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = method.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable name to use in the code
    JavaSymbolName globalSearch =
        getParameterNameFor(method, SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH);
    JavaSymbolName pageable = getParameterNameFor(method, SpringJavaType.PAGEABLE);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);
    JavaType finderParamType = parameterTypes.get(0).getJavaType();
    String finderParamName = parameterNames.get(0).getSymbolName();

    // Types to import
    JavaType returnType = getDomainTypeOfFinderMethod(method);
    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        getNameOfJavaType(entityQtype), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, null, null, null, finderParamType,
        finderParamName, returnType, method.getMethodName(), methodInfo.getRight());
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine
        .append(String
            .format(
                "%s mapping = buildMapper()",
                getNameOfJavaType(SpringletsJavaType.SPRINGLETS_QUERYDSL_REPOSITORY_SUPPORT_ATTRIBUTE_BUILDER)));

    // .map(entiyVarName, varName) ...
    if (!this.typesAreProjections.get(returnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s.%s)",
            getConstantForField(fieldName).getFieldName(), entityVariable, fieldName));
      }
    } else {

      // Return type is a projection
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(returnType);
      Iterator<Pair<String, String>> iterator = projectionFields.iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(%s, %s)",
            getConstantForField(entry.getKey()).getFieldName(), entry.getValue()));
      }
    }
    mappingBuilderLine.append(";");
    bodyBuilder.appendFormalLine(mappingBuilderLine.toString());
    bodyBuilder.newLine();

    // applyPagination(pageable, query, mapping);
    bodyBuilder.appendFormalLine(String.format("applyPagination(%s, query, mapping);", pageable));

    //applyOrderById(query);
    bodyBuilder.appendFormalLine("applyOrderById(query);");
    bodyBuilder.newLine();

    buildQueryResult(bodyBuilder, pageable, entityVariable, QUERYDSL_PROJECTIONS, returnType);

    // Use provided finder method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, methodInfo.getLeft()
            .getReturnType(), parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  private JavaSymbolName getParameterNameFor(MethodMetadata method, JavaType type) {
    AnnotatedJavaType parameter;
    for (int i = 0; i < method.getParameterTypes().size(); i++) {
      parameter = method.getParameterTypes().get(i);
      if (parameter.getJavaType().equals(type)) {
        return method.getParameterNames().get(i);
      }
    }
    return null;
  }

  private JavaType getDomainTypeOfFinderMethod(MethodMetadata method) {
    JavaType returnType = method.getReturnType();
    if (returnType.getFullyQualifiedTypeName().equals(
        SpringJavaType.PAGE.getFullyQualifiedTypeName())) {
      if (returnType.getParameters() != null && returnType.getParameters().size() == 1) {
        return returnType.getParameters().get(0);
      }
    } else if (returnType.getEnclosingType() != null) {
      return returnType.getBaseType();
    }
    return null;
  }

  /**
   * Return QueryDSL Q JavaType for an entity JavaType
   *
   * @param entity
   * @return
   */
  private JavaType getQJavaTypeFor(JavaType entity) {
    return new JavaType(entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
        .concat(entity.getSimpleTypeName()));
  }

  /**
   * Method that generates implementation methods for each count method, associated
   * to each custom finder.
   *
   * @param methodInfo
   * @param validFields
   * @return
   */
  private MethodMetadata getCustomCountImpl(Pair<MethodMetadata, PartTree> methodInfo) {

    final MethodMetadata method = methodInfo.getLeft();

    // Define method name
    JavaSymbolName methodName = method.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = method.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable names to use in the code
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);
    JavaType finderParamType = parameterTypes.get(0).getJavaType();
    String finderParamName = parameterNames.get(0).getSymbolName();

    // Types to import
    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        getNameOfJavaType(entityQtype), entityVariable));
    bodyBuilder.newLine();

    // JPQLQuery query = from(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = from(%s);",
        getNameOfJavaType(getJPQLQueryFor(this.entity)), entityVariable));
    bodyBuilder.newLine();

    buildFormBeanFilterBody(bodyBuilder, finderParamType, finderParamName, entityVariable,
        methodInfo.getRight());

    // return query.fetchCount();
    bodyBuilder.appendFormalLine("return query.fetchCount();");

    // Use provided finder method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }


  /**
   * Return JPQLQuery JavaType for required entity
   *
   * @param entityType
   * @return
   */
  private JavaType getJPQLQueryFor(JavaType entityType) {
    return JavaType.wrapperOf(QUERYDSL_JPQLQUERY, entityType);
  }

  /**
   * Builds the search query
   *
   * @param bodyBuilder method body builder
   * @param entityVariable name of the variable that contains the Q entity
   * @param globalSearch global search variable name
   * @param referencedFieldParamName
   * @param referencedField
   * @param referencedFieldIdentifierPathName
   * @param formBeanType the JavaType which contains the fields to use for filtering.
   *            Can be null in findAll queries.
   * @param formBeanParameterName the name of the search param
   * @param returnType
   * @param finderName the name of the finder. Only available when method is a
   *            projection/DTO finder.
   * @param partTree
   */
  private void buildQuery(InvocableMemberBodyBuilder bodyBuilder, String entityVariable,
      JavaSymbolName globalSearch, JavaSymbolName referencedFieldParamName,
      FieldMetadata referencedField, String referencedFieldIdentifierPathName,
      JavaType formBeanType, String formBeanParameterName, JavaType returnType,
      JavaSymbolName finderName, PartTree partTree) {

    // Prepare leftJoin for compositions oneToOne
    StringBuilder fetchJoins = new StringBuilder();
    for (RelationInfo relationInfo : entityMetadata.getRelationInfos().values()) {
      if (relationInfo.type == JpaRelationType.COMPOSITION
          && relationInfo.cardinality == Cardinality.ONE_TO_ONE) {
        fetchJoins.append(".leftJoin(");
        fetchJoins.append(entityVariable);
        fetchJoins.append(".");
        fetchJoins.append(relationInfo.fieldName);
        fetchJoins.append(").fetchJoin()");
      }
    }

    //JPQLQuery query = from(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = from(%s)%s;",
        getNameOfJavaType(getJPQLQueryFor(entity)), entityVariable, fetchJoins));
    bodyBuilder.newLine();

    if (formBeanType != null) {
      // Query for finder



      // if (formSearch != null) {
      bodyBuilder.appendFormalLine(String.format("if (%s != null) {", formBeanParameterName));
      if (partTree != null) {

        buildFormBeanFilterBody(bodyBuilder, formBeanType, formBeanParameterName, entityVariable,
            partTree);

      } else {

        // formBean is an entity, filter by all its fields
        for (Entry<String, FieldMetadata> field : this.typesFieldsMetadata.get(formBeanType)
            .entrySet()) {
          // if (formSearch.getField() != null) {
          String accessorMethodName =
              BeanInfoUtils.getAccessorMethodName(field.getValue()).getSymbolName();
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {",
              formBeanParameterName, accessorMethodName));

          // Get path field name from field mappings
          String pathFieldName =
              getValueOfPairFor(this.typesFieldMaps.get(formBeanType), field.getKey());
          // query.where(myEntity.field.eq(formBean.getField()));
          bodyBuilder.appendIndent();
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s.%s()));", pathFieldName,
              formBeanParameterName, accessorMethodName));
          // }
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine("}");
        }
      }

      // }
      bodyBuilder.appendFormalLine("}");
      bodyBuilder.newLine();
    } else if (referencedFieldParamName != null && referencedFieldIdentifierPathName != null) {
      // Query for reference

      // Assert.notNull(referenced, "referenced is required");
      bodyBuilder.appendFormalLine(String.format("%s.notNull(%s, \"%s is required\");",
          SpringJavaType.ASSERT.getNameIncludingTypeParameters(false, importResolver),
          referencedFieldParamName, referencedFieldParamName));
      bodyBuilder.newLine();

      // Query should include a where clause
      if (referencedField.getAnnotation(JpaJavaType.MANY_TO_MANY) != null) {
        // query.where(referencedFieldPath.contains(referencedFieldName));
        bodyBuilder.appendFormalLine(String.format("query.where(%s.contains(%s));",
            referencedFieldIdentifierPathName, referencedFieldParamName));
      } else {
        // query.where(referencedFieldPath.eq(referencedFieldName));
        bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s));",
            referencedFieldIdentifierPathName, referencedFieldParamName));
      }
    }

    // Path<?>[] paths = new Path[] { .... };
    bodyBuilder.appendIndent();
    final String pathType = getNameOfJavaType(QUERYDSL_PATH);
    bodyBuilder.append(String.format("%s<?>[] paths = new %s<?>[] {", pathType, pathType));
    List<String> toAppend = new ArrayList<String>();

    // ... returnType.field1, returnType.field2);
    if (!this.typesAreProjections.get(returnType)) {

      // Return type is the same entity
      Map<String, FieldMetadata> projectionFields = this.typesFieldsMetadata.get(returnType);
      Iterator<Entry<String, FieldMetadata>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, FieldMetadata> field = iterator.next();
        String fieldName = field.getValue().getFieldName().getSymbolName();
        toAppend.add(entityVariable + "." + fieldName);
      }
    } else {

      // Return type is a projection
      List<Pair<String, String>> projectionFields = this.typesFieldMaps.get(returnType);
      Validate.notNull(projectionFields, "Couldn't get projection fields for %s",
          this.defaultReturnType);
      Iterator<Pair<String, String>> iterator = projectionFields.iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        toAppend.add(entry.getValue());
      }
    }
    bodyBuilder.append(StringUtils.join(toAppend, ','));
    bodyBuilder.append("};");
    bodyBuilder.newLine();

    // applyGlobalSearch(search, query, paths);
    bodyBuilder.appendFormalLine("applyGlobalSearch(globalSearch, query, paths);");

  }

  private String getValueOfPairFor(List<Pair<String, String>> list, String key) {
    for (Pair<String, String> item : list) {
      if (key.equals(item.getLeft())) {
        return item.getValue();
      }
    }
    return null;
  }

  private void buildFormBeanFilterBody(InvocableMemberBodyBuilder bodyBuilder,
      JavaType formBeanType, String formBeanParameterName, String entityVariable, PartTree partTree) {

    // BooleanBuilder searchCondition = new BooleanBuilder();
    bodyBuilder.appendFormalLine("%1$s searchCondition = new %1$s();",
        getNameOfJavaType(QUERYDSL_BOOLEAN_BUILDER));


    // formBean is a DTO, filter only by finder params
    List<FinderParameter> finderParamsList = partTree.getParameters();
    for (FinderParameter finderParameter : finderParamsList) {

      // if (formSearch.getField() != null) {
      String accessorMethodName =
          BeanInfoUtils.getAccessorMethodName(finderParameter.getName(), finderParameter.getType())
              .getSymbolName();
      bodyBuilder.appendIndent();
      bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {", formBeanParameterName,
          accessorMethodName));

      // Get path field name from field mappings
      String pathFieldName = getFinderParamPath(finderParameter, entityVariable);
      // query.where(myEntity.field.eq(formBean.getField()));
      bodyBuilder.appendIndent();
      bodyBuilder.appendIndent();
      bodyBuilder.appendFormalLine(String.format("searchCondition.and(%s.eq(%s.%s()));",
          pathFieldName, formBeanParameterName, accessorMethodName));

      // }
      bodyBuilder.appendIndent();
      bodyBuilder.appendFormalLine("}");
    }

    // if (searchCondition.hasValue()) {
    bodyBuilder.appendFormalLine("if (searchCondition.hasValue()) {");
    //     query.where(searchCondition);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("query.where(searchCondition);");
    // }
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
  }

  private String getFinderParamPath(FinderParameter finderParameter, String entityVariable) {
    StringBuilder sbuilder = new StringBuilder(entityVariable);
    for (FieldMetadata field : finderParameter.getPath()) {
      sbuilder.append('.');
      sbuilder.append(field.getFieldName().getSymbolName());
    }
    return sbuilder.toString();
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
