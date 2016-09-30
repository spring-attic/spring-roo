package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooJpaRepositoryCustomImpl}.
 *
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 2.0
 */
public class RepositoryJpaCustomImplMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = RepositoryJpaCustomImplMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private ImportRegistrationResolver importResolver;
  private JavaType entity;
  private MethodMetadata findAllGlobalSearchMethod;
  private Map<JavaType, Map<String, String>> typesFieldMaps;
  private JavaType defaultReturnType;
  private Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadata;
  private Map<JavaType, Boolean> typesAreProjections;
  private Map<JavaSymbolName, List<FinderParameter>> finderParametersMap;

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
   * @param annotationValues (required)
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param domainType entity referenced on interface
   * @param isDTO indicates if the provided domainType is a DTO or an entity
   * @param idFields entity id fields
   * @param validFields entity fields to search for (excluded id, reference and collection fields)
   * @param findAllGlobalSearchMethod the findAll metadata
   * @param allFindAllReferencedFieldsMethods the metadata for al findAllByReference methods.
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
   * @param finderParametersMap the Map with all projection finder names and its list 
   *            of finder params.
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      final List<FieldMetadata> idFields, final List<FieldMetadata> validFields,
      final MethodMetadata findAllGlobalSearchMethod,
      final Map<FieldMetadata, MethodMetadata> allFindAllReferencedFieldsMethods,
      final Map<FieldMetadata, String> referencedFieldsIdentifierNames,
      final Map<JavaType, Map<String, String>> typesFieldMaps,
      final List<MethodMetadata> customFinderMethods,
      final List<MethodMetadata> customCountMethods,
      final Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadata,
      final Map<JavaType, Boolean> typesAreProjections,
      final Map<JavaSymbolName, List<FinderParameter>> finderParametersMap) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.entity = domainType;
    this.typesFieldMaps = typesFieldMaps;
    this.typesFieldsMetadata = typesFieldsMetadata;
    this.typesAreProjections = typesAreProjections;
    this.finderParametersMap = finderParametersMap;

    // Get inner parameter of default return type (enclosed inside Page);
    this.defaultReturnType = findAllGlobalSearchMethod.getReturnType().getParameters().get(0);

    // Get repository that needs to be implemented
    ensureGovernorImplements(annotationValues.getRepository());

    // All repositories should be generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Generate findAll implementation method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllImpl(idFields, validFields)));

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> allFindAllReferencedFieldsMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    allFindAllReferencedFieldsMethodsOrderedByFieldName.putAll(allFindAllReferencedFieldsMethods);

    // Generate findAll referenced fields implementation methods
    for (Entry<FieldMetadata, MethodMetadata> method : allFindAllReferencedFieldsMethodsOrderedByFieldName
        .entrySet()) {

      String referencedPathFieldName = referencedFieldsIdentifierNames.get(method.getKey());

      ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllReferencedFieldsImpl(method
          .getKey().getFieldType(), method.getValue(), referencedPathFieldName, validFields)));
    }

    // Generate projection finder methods implementations
    if (customFinderMethods != null) {
      for (MethodMetadata method : customFinderMethods) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(getCustomFindersImpl(method, validFields)));
      }
    }

    // generate custom couont methods
    for (MethodMetadata method : customCountMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getCustomCountImpl(method)));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates the findAll implementation method
   * @param ids the entity id fields
   * @param fields the entity fields to search for
   *
   * @return
   */
  public MethodMetadata getFindAllImpl(List<FieldMetadata> ids, List<FieldMetadata> fields) {

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

    // Use provided findAll method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
            this.findAllGlobalSearchMethod.getMethodName(),
            this.findAllGlobalSearchMethod.getReturnType(),
            this.findAllGlobalSearchMethod.getParameterTypes(),
            this.findAllGlobalSearchMethod.getParameterNames(), null);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable name to use in the code
    JavaSymbolName globalSearch = parameterNames.get(0);
    JavaSymbolName pageable = parameterNames.get(1);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);

    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType projection = new JavaType("com.querydsl.core.types.Projections");

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, null, null, null, null,
        this.defaultReturnType, null);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine.append(String.format(
        "%s mapping = buildMapper()",
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt").concat(LogicalPath.PATH_SEPARATOR)
            .concat("AttributeMappingBuilder")).getNameIncludingTypeParameters(false,
            this.importResolver)));

    if (!this.typesAreProjections.get(this.defaultReturnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s.%s)", fieldName,
            entityVariable, fieldName));
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.typesFieldMaps.get(this.defaultReturnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s)", entry.getKey(),
            entry.getValue()));
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
   * Builds the query return sentence
   * 
   * @param bodyBuilder ITD boidy builder
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
      Map<String, String> projectionFields = this.typesFieldMaps.get(returnType);

      // return loadPage(query, pageable, Projection.constructor(MyProjection.class,
      //                    getEntityId(), myEntity.field1, myEntity.field2);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, %s, %s.constructor(%s.class, %s ));", pageable,
          projection.getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver),
          StringUtils.join(projectionFields.values(), ", ")));
    }
  }

  /**
   * Method that generates the findAll referenced fields implementation method
   *
   * @param the JavaType of the referenced field
   * @param method to implement
   * @param referencedPathFieldName the String with the the referenced field name
   *            in "path" format.
   * @param ids the entity id fields
   * @param fields the entity fields to search for
   *
   * @return
   */
  public MethodMetadata getFindAllReferencedFieldsImpl(JavaType referencedFieldType,
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
    JavaSymbolName referencedFieldName = parameterNames.get(0);
    JavaSymbolName globalSearch = parameterNames.get(1);
    JavaSymbolName pageable = parameterNames.get(2);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);

    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType projection = new JavaType("com.querydsl.core.types.Projections");

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, referencedFieldName,
        referencedPathFieldName, null, null, this.defaultReturnType, null);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine.append(String.format(
        "%s mapping = buildMapper()",
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt").concat(LogicalPath.PATH_SEPARATOR)
            .concat("AttributeMappingBuilder")).getNameIncludingTypeParameters(false,
            this.importResolver)));

    // .map(entiyVarName, varName) ...
    if (!this.typesAreProjections.get(this.defaultReturnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String entityFieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s.%s)", entityFieldName,
            entityVariable, entityFieldName));
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.typesFieldMaps.get(this.defaultReturnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s)", entry.getKey(),
            entry.getValue()));
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
   * @param method
   * @return
   */
  private MethodMetadata getCustomFindersImpl(MethodMetadata method, List<FieldMetadata> fields) {

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
    JavaSymbolName globalSearch = parameterNames.get(1);
    JavaSymbolName pageable = parameterNames.get(2);
    String entity = this.entity.getSimpleTypeName();
    String entityVariable = StringUtils.uncapitalize(entity);
    JavaType finderParamType = parameterTypes.get(0).getJavaType();
    String finderParamName = parameterNames.get(0).getSymbolName();

    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType returnType = method.getReturnType().getParameters().get(0);
    JavaType projection = new JavaType("com.querydsl.core.types.Projections");

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, entityVariable, globalSearch, null, null, finderParamType,
        finderParamName, returnType, method.getMethodName());
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    StringBuffer mappingBuilderLine = new StringBuffer();
    mappingBuilderLine.append(String.format(
        "%s mapping = buildMapper()",
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt").concat(LogicalPath.PATH_SEPARATOR)
            .concat("AttributeMappingBuilder")).getNameIncludingTypeParameters(false,
            this.importResolver)));

    // .map(entiyVarName, varName) ...
    if (!this.typesAreProjections.get(returnType)) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s.%s)", fieldName,
            entityVariable, fieldName));
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.typesFieldMaps.get(returnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        mappingBuilderLine.append(String.format("\n\t\t\t.map(\"%s\", %s)", entry.getKey(),
            entry.getValue()));
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

    buildQueryResult(bodyBuilder, pageable, entityVariable, projection, returnType);

    // Use provided finder method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Method that generates implementation methods for each count method, associated 
   * to each custom finder.
   *  
   * @param method
   * @param validFields
   * @return
   */
  private MethodMetadata getCustomCountImpl(MethodMetadata method) {

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
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // JPQLQuery query = from(qEntity);
    bodyBuilder.appendFormalLine(String.format("JPQLQuery query = from(%s);", entityVariable));
    bodyBuilder.newLine();

    // BooleanBuilder searchFormCondition = new BooleanBuilder();
    bodyBuilder.appendFormalLine(String.format("%1$s searchFormCondition = new %1$s();",
        new JavaType("com.querydsl.core.BooleanBuilder").getNameIncludingTypeParameters(false,
            importResolver)));
    bodyBuilder.newLine();

    // Filter only by finder params
    List<FinderParameter> finderParamsList = this.finderParametersMap.get(methodName);
    for (FinderParameter finderParameter : finderParamsList) {

      // if (formBean.getField() != null) {
      String accessorMethodName =
          BeanInfoUtils.getAccessorMethodName(finderParameter.getName(), finderParameter.getType())
              .getSymbolName();
      bodyBuilder.appendIndent();
      bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {", finderParamName,
          accessorMethodName));

      // Get path field name from field mappings
      String pathFieldName =
          this.typesFieldMaps.get(finderParamType).get(finderParameter.getName().getSymbolName());

      bodyBuilder.appendIndent();
      bodyBuilder.appendIndent();

      // query.where(myEntity.field.eq(formBean.getField()));
      if (pathFieldName.equals("getEntityId()")) {

        // Field is an id field
        bodyBuilder.appendFormalLine(String.format("query.where(getEntityId().eq(%s.%s()));",
            finderParamName, accessorMethodName));
      } else {
        bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s.%s()));", pathFieldName,
            finderParamName, accessorMethodName));
      }

      // }
      bodyBuilder.appendIndent();
      bodyBuilder.appendFormalLine("}");
    }

    // query.where(searchFormCondition);
    bodyBuilder.appendFormalLine("query.where(searchFormCondition);");
    bodyBuilder.newLine();

    // return query.fetchCount();
    bodyBuilder.appendFormalLine("return query.fetchCount();");

    // Use provided finder method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Builds the search query
   *
   * @param bodyBuilder method body builder
   * @param fields fields to search for
   * @param entityVariable name of the variable that contains the Q entity
   * @param globalSearch global search variable name
   * @param referencedFieldName
   * @param referencedFieldIdentifierPathName
   * @param finderParam the JavaType which contains the fields to use for filtering.
   *            Can be null in findAll queries.
   * @param formBean the name of the search param.
   * @param finderName the name of the finder. Only available when method is a 
   *            projection/DTO finder.
   */
  private void buildQuery(InvocableMemberBodyBuilder bodyBuilder, String entityVariable,
      JavaSymbolName globalSearch, JavaSymbolName referencedFieldName,
      String referencedFieldIdentifierPathName, JavaType finderParam, String formBean,
      JavaType returnType, JavaSymbolName finderName) {

    JavaType jpql = new JavaType("com.querydsl.jpa.JPQLQuery");

    //JPQLQuery query = from(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = from(%s);",
        jpql.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    if (finderParam != null) {
      // if (formSearch != null) {
      bodyBuilder.appendFormalLine(String.format("if (%s != null) {", formBean));
      Map<String, FieldMetadata> map = this.typesFieldsMetadata.get(finderParam);
      if (this.finderParametersMap != null && !this.finderParametersMap.isEmpty()
          && this.finderParametersMap.get(finderName) != null) {

        // formBean is a DTO, filter only by finder params
        List<FinderParameter> finderParamsList = this.finderParametersMap.get(finderName);
        for (FinderParameter finderParameter : finderParamsList) {

          // if (formSearch.getField() != null) {
          String accessorMethodName =
              BeanInfoUtils.getAccessorMethodName(finderParameter.getName(),
                  finderParameter.getType()).getSymbolName();
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {", formBean,
              accessorMethodName));

          // Get path field name from field mappings
          String pathFieldName =
              this.typesFieldMaps.get(finderParam).get(finderParameter.getName().getSymbolName());
          // query.where(myEntity.field.eq(formBean.getField()));
          bodyBuilder.appendIndent();
          bodyBuilder.appendIndent();
          if (pathFieldName.equals("getEntityId()")) {

            // Field is an id field
            bodyBuilder.appendFormalLine(String.format("query.where(getEntityId().eq(%s.%s()));",
                formBean, accessorMethodName));
          } else {
            bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s.%s()));",
                pathFieldName, formBean, accessorMethodName));
          }

          // }
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine("}");
        }
      } else {

        // formBean is an entity, filter by all its fields
        for (Entry<String, FieldMetadata> field : map.entrySet()) {
          // if (formSearch.getField() != null) {
          String accessorMethodName =
              BeanInfoUtils.getAccessorMethodName(field.getValue()).getSymbolName();
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {", formBean,
              accessorMethodName));

          // Get path field name from field mappings
          String pathFieldName = this.typesFieldMaps.get(finderParam).get(field.getKey());
          // query.where(myEntity.field.eq(formBean.getField()));
          bodyBuilder.appendIndent();
          bodyBuilder.appendIndent();
          if (pathFieldName.equals("getEntityId()")) {

            // Field is an id field
            bodyBuilder.appendFormalLine(String.format("query.where(getEntityId().eq(%s.%s()));",
                formBean, accessorMethodName));
          } else {
            bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s.%s()));",
                pathFieldName, formBean, accessorMethodName));
          }

          // }
          bodyBuilder.appendIndent();
          bodyBuilder.appendFormalLine("}");
        }
      }

      // }
      bodyBuilder.appendFormalLine("}");
      bodyBuilder.newLine();
    }

    if (referencedFieldName != null && referencedFieldIdentifierPathName != null) {

      // Query should include a where clause
      // query.where(referencedFieldPath.eq(referencedFieldName));
      bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s));\n",
          referencedFieldIdentifierPathName, referencedFieldName));
    }

    // applyGlobalSearch(search, query, ...
    bodyBuilder.appendIndent();
    bodyBuilder.append("applyGlobalSearch(globalSearch, query");
    // ... returnType.field1, returnType.field2);
    if (!this.typesAreProjections.get(returnType)) {

      // Return type is the same entity
      Map<String, FieldMetadata> projectionFields = this.typesFieldsMetadata.get(returnType);
      Iterator<Entry<String, FieldMetadata>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, FieldMetadata> field = iterator.next();
        String fieldName = field.getValue().getFieldName().getSymbolName();
        bodyBuilder.append(String.format(", %s.%s", entityVariable, fieldName));
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.typesFieldMaps.get(returnType);
      Validate.notNull(projectionFields, "Couldn't get projection fields for %s",
          this.defaultReturnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        bodyBuilder.append(String.format(", %s", entry.getValue()));
      }
    }
    bodyBuilder.append(");");
    bodyBuilder.newLine();
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
