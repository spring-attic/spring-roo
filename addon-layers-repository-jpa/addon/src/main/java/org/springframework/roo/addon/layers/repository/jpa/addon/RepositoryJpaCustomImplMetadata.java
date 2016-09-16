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
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
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
  private Map<JavaType, JavaSymbolName> referencedFieldsIdentifierNames;
  private Map<JavaType, JavaSymbolName> referencedFieldsNames;
  private List<String> constructorFields;

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
   * @param referencedFieldsNames 
   * @param constructorFields list of field names to add to ConstructorExpression while 
   *            building findAll methods when return type is a projection.
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      final boolean returnTypeIsProjection, final List<FieldMetadata> idFields,
      final List<FieldMetadata> validFields, final MethodMetadata findAllGlobalSearchMethod,
      final Map<FieldMetadata, MethodMetadata> allFindAllReferencedFieldsMethods,
      final Map<JavaType, JavaSymbolName> referencedFieldsIdentifierNames,
      final Map<JavaType, JavaSymbolName> referencedFieldsNames,
      final List<String> constructorFields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.allFindAllReferencedFieldsMethods = allFindAllReferencedFieldsMethods;
    this.referencedFieldsIdentifierNames = referencedFieldsIdentifierNames;
    this.referencedFieldsNames = referencedFieldsNames;
    this.returnTypeIsProjection = returnTypeIsProjection;
    this.entity = domainType;
    this.constructorFields = constructorFields;

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

      JavaSymbolName identifierFieldName = referencedFieldsIdentifierNames.get(method.getKey());
      JavaSymbolName fieldName = method.getKey().getFieldName();

      ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllReferencedFieldsImpl(method
          .getKey().getFieldType(), method.getValue(), identifierFieldName, fieldName, idFields,
          validFields)));
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

    List<String> queryList = new ArrayList<String>();
    for (FieldMetadata field : fields) {
      queryList.add(String.format("%s.%s", entityVariable, field.getFieldName()));
    }


    // Types to import
    JavaType qEntity =
        new JavaType(this.entity.getPackage().getFullyQualifiedPackageName().concat(".Q")
            .concat(entity));
    JavaType returnType = findAllGlobalSearchMethod.getReturnType().getParameters().get(0);
    JavaType pageImpl = new JavaType("org.springframework.data.domain.PageImpl");
    JavaType constructorExp = new JavaType("com.mysema.query.types.ConstructorExpression");

    buildVariables(bodyBuilder, ids);

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    // Construct query
    buildQuery(bodyBuilder, fields, entityVariable, globalSearch, null, null);

    bodyBuilder.appendFormalLine(String.format("long totalFound = query.count();"));

    // if (pageable != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", pageable));

    bodyBuilder.indent();

    if (!fields.isEmpty()) {
      buildOrderClause(fields, bodyBuilder, entityVariable, pageable);
    }

    //  query.offset(pageable.getOffset()).limit(pageable.getPageSize());}
    bodyBuilder.appendFormalLine(String.format(
        "query.offset(%1$s.getOffset()).limit(%1$s.getPageSize());", pageable));

    bodyBuilder.indentRemove();

    // End if
    bodyBuilder.appendFormalLine("}");

    // query.orderBy(qEntity.id.asc());
    for (FieldMetadata id : ids) {
      bodyBuilder.appendFormalLine(String.format("query.orderBy(id%s.asc());", entity));
    }

    bodyBuilder.appendFormalLine("");

    // List<ReturnType> results = query.list(ConstructorExpression.create(ReturnType.class, qEntity.parameter1, qEntity.parameter1, ...));
    if (!this.returnTypeIsProjection) {
      bodyBuilder.appendFormalLine(String
          .format("%1$s<%2$s> results = query.list(%3$s);", new JavaType("java.util.List")
              .getNameIncludingTypeParameters(false, this.importResolver), returnType
              .getNameIncludingTypeParameters(false, this.importResolver), entityVariable));
    } else if (queryList.isEmpty()) {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver), constructorExp
              .getNameIncludingTypeParameters(false, this.importResolver)));
    } else {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class, %4$s ));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver), constructorExp
              .getNameIncludingTypeParameters(false, this.importResolver), StringUtils.join(
              this.constructorFields, ", ")));
    }

    //return new PageImpl<Entity>(results, pageable, totalFound);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(results, %s, totalFound);",
        pageImpl.getNameIncludingTypeParameters(false, this.importResolver),
        returnType.getNameIncludingTypeParameters(false, this.importResolver), pageable));

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates the findAll referenced fields implementation method
   * 
   * @param method to implement
   * @param identifierFieldName
   * @param fieldName
   * @param ids the entity id fields
   * @param fields the entity fields to search for 
   *
   * @return
   */
  public MethodMetadata getFindAllReferencedFieldsImpl(JavaType referencedField,
      MethodMetadata method, JavaSymbolName identifierFieldName, JavaSymbolName fieldName,
      List<FieldMetadata> ids, List<FieldMetadata> fields) {

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

    // Use provided findAll method to generate its implementation
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, method.getReturnType(),
            parameterTypes, parameterNames, null);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting variable name to use in the code
    JavaType referencedFieldParameterType = parameterTypes.get(0).getJavaType();
    JavaSymbolName referencedFieldParameterName = parameterNames.get(0);
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
    JavaType pageImpl = new JavaType("org.springframework.data.domain.PageImpl");
    JavaType constructorExp = new JavaType("com.mysema.query.types.ConstructorExpression");

    buildVariables(bodyBuilder, ids);

    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    // Construct query
    buildQuery(bodyBuilder, fields, entityVariable, globalSearch, fieldName,
        referencedFieldParameterName);

    bodyBuilder.appendFormalLine(String.format("long totalFound = query.count();"));

    // if (pageable != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", pageable));

    bodyBuilder.indent();

    if (!fields.isEmpty()) {
      buildOrderClause(fields, bodyBuilder, entityVariable, pageable);
    }

    //  query.offset(pageable.getOffset()).limit(pageable.getPageSize());}
    bodyBuilder.appendFormalLine(String.format(
        "query.offset(%1$s.getOffset()).limit(%1$s.getPageSize());", pageable));

    bodyBuilder.indentRemove();

    // End if
    bodyBuilder.appendFormalLine("}");

    // query.orderBy(qEntity.id.asc());
    for (FieldMetadata id : ids) {
      bodyBuilder.appendFormalLine(String.format("query.orderBy(id%s.asc());", entity));
    }

    bodyBuilder.appendFormalLine("");

    // List<Entity> results = query.list(ConstructorExpression.create(Entity.class, qEntity.parameter1, qEntity.parameter1, ...));
    if (!this.returnTypeIsProjection) {
      bodyBuilder.appendFormalLine(String
          .format("%1$s<%2$s> results = query.list(%3$s);", new JavaType("java.util.List")
              .getNameIncludingTypeParameters(false, this.importResolver), returnType
              .getNameIncludingTypeParameters(false, this.importResolver), entityVariable));
    } else if (queryList.isEmpty()) {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver), constructorExp
              .getNameIncludingTypeParameters(false, this.importResolver)));
    } else {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class, %4$s ));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, this.importResolver),
          returnType.getNameIncludingTypeParameters(false, this.importResolver), constructorExp
              .getNameIncludingTypeParameters(false, this.importResolver), StringUtils.join(
              this.constructorFields, ", ")));
    }

    //return new PageImpl<Entity>(results, pageable, totalFound);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(results, %s, totalFound);",
        pageImpl.getNameIncludingTypeParameters(false, importResolver),
        returnType.getNameIncludingTypeParameters(false, importResolver), pageable));

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
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
   * @param referencedFieldParameterName
   */
  private void buildQuery(InvocableMemberBodyBuilder bodyBuilder, List<FieldMetadata> fields,
      String entityVariable, JavaSymbolName globalSearch, JavaSymbolName referencedFieldName,
      JavaSymbolName referencedFieldParameterName) {

    JavaType booleanBuilder = new JavaType("com.mysema.query.BooleanBuilder");
    JavaType jpql = new JavaType("com.mysema.query.jpa.JPQLQuery");

    //JPQLQuery query = getQueryFrom(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = getQueryFrom(%s);",
        jpql.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    if (referencedFieldName != null && referencedFieldParameterName != null) {
      // BooleanBuilder where = new BooleanBuilder(entityVariable.referenceField.eq(referencedField));
      bodyBuilder.appendFormalLine(String.format("%1$s where = new %1$s(%2$s.%3$s.eq(%4$s));\n",
          booleanBuilder.getNameIncludingTypeParameters(false, importResolver), entityVariable,
          referencedFieldName, referencedFieldParameterName.getSymbolName()));
    } else {
      // BooleanBuilder where = new BooleanBuilder()
      bodyBuilder.appendFormalLine(String.format("%1$s where = new %1$s();\n",
          booleanBuilder.getNameIncludingTypeParameters(false, importResolver)));
    }

    // if (globalSearch != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", globalSearch));

    bodyBuilder.indent();

    // String txt = globalSearch.getText();
    bodyBuilder.appendFormalLine(String.format("String txt = %s.getText();", globalSearch));

    boolean isFirst = true;
    String expression;

    for (FieldMetadata field : fields) {

      expression = buildExpression(entityVariable, field);

      if (isFirst) {
        bodyBuilder.appendFormalLine(String.format("where.and("));
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(String.format("%s", expression));
      } else {
        bodyBuilder.appendFormalLine(String.format(".or(%s)", expression));
      }
      isFirst = false;
    }

    if (!isFirst) {
      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine(String.format(");\n"));
    }

    // End if 
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine(String.format("}"));

    bodyBuilder.appendFormalLine(String.format("query.where(where);\n"));
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
