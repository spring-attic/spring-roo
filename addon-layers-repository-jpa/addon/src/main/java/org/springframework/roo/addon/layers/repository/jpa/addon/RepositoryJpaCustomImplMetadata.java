package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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
  private boolean returnTypeIsProjection;

  private MethodMetadata findAllGlobalSearchMethod;
  private Map<FieldMetadata, MethodMetadata> allFindAllReferencedFieldsMethods;
  private Map<FieldMetadata, String> referencedFieldsIdentifierNames;
  private Map<JavaType, Map<String, String>> projectionsFieldMaps;
  private List<MethodMetadata> projectionFinderMethods;
  private JavaType returnType;

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
   * @param projectionsFieldMaps the Map<JavaType, Map<String, String>> of each associated 
   *            projection (parent Map JavaType), property names (keys) and path names (values) 
   *            for building finders which return a projection.
   * @param allProjectionsIdFieldMaps the Map<JavaType, Map<String, String>> of each associated 
   *            projection (parent Map JavaType), property names (keys) and path names (values) 
   *            for each identifier field.
   * @param projectionFinderMethods 
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      final boolean returnTypeIsProjection, final List<FieldMetadata> idFields,
      final List<FieldMetadata> validFields, final MethodMetadata findAllGlobalSearchMethod,
      final Map<FieldMetadata, MethodMetadata> allFindAllReferencedFieldsMethods,
      final Map<FieldMetadata, String> referencedFieldsIdentifierNames,
      final Map<JavaType, Map<String, String>> projectionsFieldMaps,
      final List<MethodMetadata> projectionFinderMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.allFindAllReferencedFieldsMethods = allFindAllReferencedFieldsMethods;
    this.referencedFieldsIdentifierNames = referencedFieldsIdentifierNames;
    this.returnTypeIsProjection = returnTypeIsProjection;
    this.entity = domainType;
    this.projectionsFieldMaps = projectionsFieldMaps;
    this.projectionFinderMethods = projectionFinderMethods;

    // Get inner parameter of default return type (enclosed inside Page);
    this.returnType = findAllGlobalSearchMethod.getReturnType().getParameters().get(0);

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
    if (projectionFinderMethods != null) {
      for (MethodMetadata method : projectionFinderMethods) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(getProjectionfindersImpl(method)));
      }
    }

    // Add QueryDslRepositorySupportExt as superclass
    JavaType queryDslExtension =
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt"), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));
    builder.addExtendsTypes(queryDslExtension);

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

    List<String> queryList = new ArrayList<String>();
    for (FieldMetadata field : fields) {
      queryList.add(String.format("%s.%s", entityVariable, field.getFieldName()));
    }

    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType returnType = findAllGlobalSearchMethod.getReturnType().getParameters().get(0);
    JavaType constructorExp = new JavaType("com.mysema.query.types.ConstructorExpression");

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, fields, entityVariable, globalSearch, null, null);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    bodyBuilder.appendFormalLine(String.format(
        "%s mapping = buildMapper()",
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt").concat(LogicalPath.PATH_SEPARATOR)
            .concat("AttributeMappingBuilder")).getNameIncludingTypeParameters(false,
            this.importResolver)));

    // .map(entiyVarName, varName) ...
    if (!this.returnTypeIsProjection) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        bodyBuilder.appendIndent();
        if (iterator.hasNext()) {
          bodyBuilder.append(String.format(".map(\"%s\", %s.%s)", fieldName, entityVariable,
              fieldName));
        } else {
          bodyBuilder.append(String.format(".map(\"%s\", %s.%s);", fieldName, entityVariable,
              fieldName));
        }
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.projectionsFieldMaps.get(returnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        bodyBuilder.appendIndent();
        if (iterator.hasNext()) {
          bodyBuilder.appendFormalLine(String.format(".map(\"%s\", %s)", entry.getKey(),
              entry.getValue()));
        } else {
          bodyBuilder.appendFormalLine(String.format(".map(\"%s\", %s);", entry.getKey(),
              entry.getValue()));
        }
      }
    }
    bodyBuilder.newLine();

    // applyPagination(pageable, query, mapping);
    bodyBuilder.appendFormalLine(String.format("applyPagination(%s, query, mapping);", pageable));

    //applyOrderById(query);
    bodyBuilder.appendFormalLine("applyOrderById(query);");
    bodyBuilder.newLine();


    if (!this.returnTypeIsProjection) {

      // return loadPage(query, pageable, myEntity);
      bodyBuilder.appendFormalLine(String.format("return loadPage(query, pageable, %s);",
          entityVariable));
    } else if (queryList.isEmpty()) {

      // return loadPage(query, pageable, ConstructorExpression.create(MyProjection.class);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, pageable, %s.create(%s.class));",
          constructorExp.getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver)));
    } else {
      Map<String, String> projectionFields = this.projectionsFieldMaps.get(returnType);

      // return loadPage(query, pageable, ConstructorExpression.create(MyProjection.class, 
      //                    getEntityId(), myEntity.field1, myEntity.field2);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, %s, %s.create(%s.class, %s ));", pageable,
          constructorExp.getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver),
          StringUtils.join(projectionFields.values(), ", ")));
    }

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
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

    List<String> queryList = new ArrayList<String>();
    for (FieldMetadata field : fields) {
      queryList.add(String.format("%s.%s", entityVariable, field.getFieldName()));
    }

    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType returnType = findAllGlobalSearchMethod.getReturnType().getParameters().get(0);
    JavaType constructorExp = new JavaType("com.mysema.query.types.ConstructorExpression");

    bodyBuilder.newLine();

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    // Construct query
    buildQuery(bodyBuilder, fields, entityVariable, globalSearch, referencedFieldName,
        referencedPathFieldName);
    bodyBuilder.newLine();

    // AttributeMappingBuilder mapping = buildMapper()
    bodyBuilder.appendFormalLine(String.format(
        "%s mapping = buildMapper()",
        new JavaType(governorPhysicalTypeMetadata.getType().getPackage()
            .getFullyQualifiedPackageName().concat(LogicalPath.PATH_SEPARATOR)
            .concat("QueryDslRepositorySupportExt").concat(LogicalPath.PATH_SEPARATOR)
            .concat("AttributeMappingBuilder")).getNameIncludingTypeParameters(false,
            this.importResolver)));

    // .map(entiyVarName, varName) ...
    if (!this.returnTypeIsProjection) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String entityFieldName = field.getFieldName().getSymbolName();
        bodyBuilder.appendIndent();
        if (iterator.hasNext()) {
          bodyBuilder.append(String.format(".map(\"%s\", %s.%s)", entityFieldName, entityVariable,
              entityFieldName));
        } else {
          bodyBuilder.append(String.format(".map(\"%s\", %s.%s);", entityFieldName, entityVariable,
              entityFieldName));
        }
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.projectionsFieldMaps.get(returnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        bodyBuilder.appendIndent();
        if (iterator.hasNext()) {
          bodyBuilder.appendFormalLine(String.format(".map(\"%s\", %s)", entry.getKey(),
              entry.getValue()));
        } else {
          bodyBuilder.appendFormalLine(String.format(".map(\"%s\", %s);", entry.getKey(),
              entry.getValue()));
        }
      }
    }
    bodyBuilder.newLine();

    // applyPagination(pageable, query, mapping);
    bodyBuilder.appendFormalLine(String.format("applyPagination(pageable, query, mapping);",
        pageable));

    //applyOrderById(query);
    bodyBuilder.appendFormalLine("applyOrderById(query);");
    bodyBuilder.newLine();


    if (!this.returnTypeIsProjection) {

      // return loadPage(query, pageable, myEntity);
      bodyBuilder.appendFormalLine(String.format("return loadPage(query, %s, %s);", pageable,
          entityVariable));
    } else if (queryList.isEmpty()) {

      // return loadPage(query, pageable, ConstructorExpression.create(MyProjection.class);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, pageable, %s.create(%s.class));",
          constructorExp.getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver)));
    } else {
      Map<String, String> projectionFields = this.projectionsFieldMaps.get(returnType);

      // return loadPage(query, pageable, ConstructorExpression.create(MyProjection.class, 
      //                    getEntityId(), myEntity.field1, myEntity.field2);
      bodyBuilder.appendFormalLine(String.format(
          "return loadPage(query, pageable, %s.create(%s.class, %s ));",
          constructorExp.getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver),
          StringUtils.join(projectionFields.values(), ", ")));
    }

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates implementation methods for each finder which return type 
   * is a projection.
   * 
   * @param method
   * @return
   */
  private MethodMetadata getProjectionfindersImpl(MethodMetadata method) {

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

    // This implementation will be available soon
    bodyBuilder.appendFormalLine("// TODO: To be implemented");
    bodyBuilder.appendFormalLine("return null;");

    // Use provided finder method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Builds variables that the method needs internally
   * 
   * @param bodyBuilder method body builder
   * @param ids id fields
   */
  private void buildVariables(InvocableMemberBodyBuilder bodyBuilder, List<FieldMetadata> ids) {
    JavaType numberPath = new JavaType("com.mysema.query.types.path.NumberPath");

    // Getting id dynamically
    for (FieldMetadata id : ids) {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%3$s> id%4$s = new %1$s<%3$s>(%3$s.class, \"%2$s\");",
          numberPath.getNameIncludingTypeParameters(true, importResolver), id.getFieldName(), id
              .getFieldType().toObjectType().getNameIncludingTypeParameters(false, importResolver),
          entity.getSimpleTypeName()));
    }
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
   */
  private void buildQuery(InvocableMemberBodyBuilder bodyBuilder, List<FieldMetadata> fields,
      String entityVariable, JavaSymbolName globalSearch, JavaSymbolName referencedFieldName,
      String referencedFieldIdentifierPathName) {

    JavaType jpql = new JavaType("com.mysema.query.jpa.JPQLQuery");

    //JPQLQuery query = getQueryFrom(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = getQueryFrom(%s);",
        jpql.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    bodyBuilder.newLine();

    if (referencedFieldName != null && referencedFieldIdentifierPathName != null) {

      // Query should include a where clause
      // query.where(referencedFieldPath.eq(referencedFieldName));
      bodyBuilder.appendFormalLine(String.format("query.where(%s.eq(%s));\n",
          referencedFieldIdentifierPathName, referencedFieldName));
    }

    // applyGlobalSearch(search, query, ...
    bodyBuilder.appendIndent();
    bodyBuilder.append("applyGlobalSearch(globalSearch, query,");
    // ... returnType.field1, returnType.field2);
    if (!this.returnTypeIsProjection) {

      // Return type is the same entity
      Iterator<FieldMetadata> iterator = fields.iterator();
      while (iterator.hasNext()) {
        FieldMetadata field = iterator.next();
        String fieldName = field.getFieldName().getSymbolName();
        if (iterator.hasNext()) {
          bodyBuilder.append(String.format(" %s.%s,", entityVariable, fieldName));
        } else {
          bodyBuilder.append(String.format(" %s.%s);", entityVariable, fieldName));
        }
      }
    } else {

      // Return type is a projection
      Map<String, String> projectionFields = this.projectionsFieldMaps.get(this.returnType);
      Validate.notNull(projectionFields, "Couldn't get projection fields for %s", this.returnType);
      Iterator<Entry<String, String>> iterator = projectionFields.entrySet().iterator();
      while (iterator.hasNext()) {

        // Check if it is an identifierField

        Entry<String, String> entry = iterator.next();
        if (iterator.hasNext()) {
          bodyBuilder.append(String.format(" %s,", entry.getValue()));
        } else {
          bodyBuilder.append(String.format(" %s);", entry.getValue()));
        }
      }
    }
    bodyBuilder.newLine();
  }


  /**
   * Creates a query expression based on the field type and name
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param field the entity field
   * @return string that represents the expression
   */
  private String buildExpression(String entityVariable, FieldMetadata field) {

    // String or char type
    if (field.getFieldType().equals(JavaType.STRING)) {
      return String.format("%s.%s.containsIgnoreCase(txt)", entityVariable, field.getFieldName());
    }

    // Number type
    if (field.getFieldType().isNumber()) {
      return String.format("%s.%s.like(\"%%\".concat(txt).concat(\"%%\"))", entityVariable,
          field.getFieldName());
    }

    // Other type
    return String.format("%s.%s.eq((%s)txt)", entityVariable, field.getFieldName(), field
        .getFieldType().getSimpleTypeName());
  }


  /**
   * Builds the query clause for the given entity fields 
   * 
   * @param fields properties to order by
   * @param bodyBuilder method body builder
   * @param entityVariable name of the variable that contains the Q entity 
   * @param pageable pageable variable name
   */
  private void buildOrderClause(List<FieldMetadata> fields, InvocableMemberBodyBuilder bodyBuilder,
      String entityVariable, JavaSymbolName pageable) {

    JavaType orderSpecifier = new JavaType("com.mysema.query.types.OrderSpecifier");
    JavaType order = new JavaType("com.mysema.query.types.Order");
    JavaType sort = new JavaType("org.springframework.data.domain.Sort");

    //  if (pageable.getSort() != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s.getSort() != null) {", pageable));
    bodyBuilder.indent();

    //for (Sort.Order sortOrder : pageable.getSort()) {
    bodyBuilder.appendFormalLine(String.format("for (%s.Order order : %s.getSort()) {",
        sort.getNameIncludingTypeParameters(false, importResolver), pageable));
    bodyBuilder.indent();

    // Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
    bodyBuilder.appendFormalLine(String.format(
        "%1$s direction = order.isAscending() ? %1$s.ASC : %1$s.DESC;\n",
        order.getNameIncludingTypeParameters(false, importResolver)));

    bodyBuilder.appendFormalLine(String.format("switch(order.getProperty()){"));
    bodyBuilder.indent();

    for (FieldMetadata field : fields) {

      // case "property":
      bodyBuilder.appendFormalLine(String.format("case \"%s\":", field.getFieldName()));

      // query.orderBy(new OrderSpecifier<String>(dir, qEntity.property));
      bodyBuilder.appendFormalLine(String.format("   query.orderBy(new %s<%s>(direction, %s.%s));",
          orderSpecifier.getNameIncludingTypeParameters(false, importResolver), field
              .getFieldType().toObjectType().getNameIncludingTypeParameters(false, importResolver),
          entityVariable, field.getFieldName()));
      bodyBuilder.appendFormalLine("   break;");
    }

    // End switch
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // End  for
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // End if
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
  }


  public boolean isProjection() {
    return this.returnTypeIsProjection;
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
