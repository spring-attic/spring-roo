package org.springframework.roo.addon.layers.repository.jpa.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
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
  private boolean isDTO;

  private MethodMetadata findAllGlobalSearchMethod;

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
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      final boolean isDTO, final List<FieldMetadata> idFields, List<FieldMetadata> validFields,
      final MethodMetadata findAllGlobalSearchMethod) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.isDTO = isDTO;
    this.entity = domainType;

    // Get repository that needs to be implemented
    ensureGovernorImplements(annotationValues.getRepository());

    // All repositories should be generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Generate findAll implementation method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllImpl(idFields, validFields)));

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
    buildQuery(bodyBuilder, fields, entityVariable, globalSearch);

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
      bodyBuilder.appendFormalLine(String.format("query.orderBy(%s.asc());", id.getFieldName()));
    }

    bodyBuilder.appendFormalLine("");

    // List<Entity> results = query.list(ConstructorExpression.create(Entity.class, qEntity.parameter1, qEntity.parameter1, ...));
    if (!this.isDTO) {
      bodyBuilder.appendFormalLine(String.format("%1$s<%2$s> results = query.list(%3$s);",
          new JavaType("java.util.List").getNameIncludingTypeParameters(false, importResolver),
          returnType.getNameIncludingTypeParameters(false, importResolver), entityVariable));
    } else if (queryList.isEmpty()) {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, importResolver), returnType
              .getNameIncludingTypeParameters(false, importResolver), constructorExp
              .getNameIncludingTypeParameters(false, importResolver)));
    } else {
      bodyBuilder.appendFormalLine(String.format(
          "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class, %4$s ));", new JavaType(
              "java.util.List").getNameIncludingTypeParameters(false, importResolver), returnType
              .getNameIncludingTypeParameters(false, importResolver), constructorExp
              .getNameIncludingTypeParameters(false, importResolver), StringUtils.join(queryList,
              ", ")));
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

    // Date expressions
    bodyBuilder
        .appendFormalLine("final String[] FULL_DATE_PATTERNS = new String[] {\"dd-MM-yyyy HH:mm:ss\", \"dd/MM/yyyy HH:mm:ss\","
            + "\"MM-dd-yyyy HH:mm:ss\", \"MM/dd/yyyy HH:mm:ss\", \"dd-MM-yyyy HH:mm\",\"dd/MM/yyyy HH:mm\", \"MM-dd-yyyy HH:mm\", \"MM/dd/yyyy HH:mm\","
            + "\"dd-MM-yyyy\", \"dd/MM/yyyy\", \"MM-dd-yyyy\", \"MM/dd/yyyy\",\"dd-MMMM-yyyy HH:mm:ss\", \"dd/MMMM/yyyy HH:mm:ss\","
            + "\"MMMM-dd-yyyy HH:mm:ss\", \"MMMM/dd/yyyy HH:mm:ss\",\"dd-MMMM-yyyy HH:mm\", \"dd/MMMM/yyyy HH:mm\", \"MMMM-dd-yyyy HH:mm\","
            + "\"MMMM/dd/yyyy HH:mm\", \"dd-MMMM-yyyy\", \"dd/MMMM/yyyy\", \"MMMM-dd-yyyy\", \"MMMM/dd/yyyy\" };");


    // Getting id dynamically
    for (FieldMetadata id : ids) {
      bodyBuilder
          .appendFormalLine(String.format(
              "%1$s<%3$s> %2$s = new %1$s<%3$s>(%3$s.class, \"%2$s\");",
              numberPath.getNameIncludingTypeParameters(true, importResolver), id.getFieldName(),
              id.getFieldType().toObjectType()
                  .getNameIncludingTypeParameters(false, importResolver)));
    }

    bodyBuilder.appendFormalLine("\n");
  }

  /**
   * Builds the search query
   * 
   * @param bodyBuilder method body builder
   * @param fields fields to search for
   * @param entityVariable name of the variable that contains the Q entity 
   * @param globalSearch global search variable name
   */
  private void buildQuery(InvocableMemberBodyBuilder bodyBuilder, List<FieldMetadata> fields,
      String entityVariable, JavaSymbolName globalSearch) {

    JavaType booleanBuilder = new JavaType("com.mysema.query.BooleanBuilder");
    JavaType jpql = new JavaType("com.mysema.query.jpa.JPQLQuery");
    JavaType booleanExpr = new JavaType("com.mysema.query.types.expr.BooleanExpression");
    JavaType booleanUtils = new JavaType("org.apache.commons.lang3.BooleanUtils");
    JavaType dateUtils = new JavaType("org.apache.commons.lang3.time.DateUtils");
    JavaType calendar = new JavaType(Calendar.class);
    JavaType date = new JavaType(Date.class);

    //JPQLQuery query = getQueryFrom(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = getQueryFrom(%s);",
        jpql.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    // BooleanBuilder where = new BooleanBuilder()
    bodyBuilder.appendFormalLine(String.format("%1$s where = new %1$s();\n",
        booleanBuilder.getNameIncludingTypeParameters(false, importResolver)));

    // if (globalSearch != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", globalSearch));

    bodyBuilder.indent();

    // String txt = globalSearch.getText();
    bodyBuilder.appendFormalLine(String.format("String txt = %s.getText();", globalSearch));

    // Boolean searchBoolean = BooleanUtils.toBoolean(txt);
    bodyBuilder.appendFormalLine(String.format("Boolean searchBoolean = %s.toBooleanObject(txt);",
        booleanUtils.getNameIncludingTypeParameters(false, importResolver)));

    bodyBuilder.appendFormalLine(String.format("%s searchDate = null;",
        date.getNameIncludingTypeParameters(false, importResolver)));

    // Calendar searchCalendar = Calendar.getInstance();
    bodyBuilder.appendFormalLine(String.format("%1$s searchCalendar= %1$s.getInstance();\n",
        calendar.getNameIncludingTypeParameters(false, importResolver)));

    bodyBuilder.appendFormalLine(String.format("try{"));
    bodyBuilder.indent();

    // searchDate = DateUtils.parseDateStrictly(txt, FULL_DATE_PATTERNS);
    bodyBuilder.appendFormalLine(String.format(
        "searchDate = %s.parseDateStrictly(txt, FULL_DATE_PATTERNS);",
        dateUtils.getNameIncludingTypeParameters(false, importResolver)));

    bodyBuilder.appendFormalLine(String.format("searchCalendar.setTime(searchDate);"));

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine(String
        .format("}catch(Exception e){\nsearchDate = null;\n        searchCalendar = null;\n   }"));


    bodyBuilder.appendFormalLine(String.format("%s expression;",
        booleanExpr.getNameIncludingTypeParameters(true, importResolver)));


    List<FieldMetadata> convertedFields = new ArrayList<FieldMetadata>();
    boolean isFirst = true;

    for (FieldMetadata field : fields) {
      if (field.getFieldType().equals(JavaType.STRING)) {

        if (isFirst) {
          bodyBuilder.appendFormalLine(String.format("expression = %s;",
              buildStringExpression(entityVariable, field.getFieldName())));
        } else {
          bodyBuilder.appendFormalLine(String.format("expression = expression.or(%s);",
              buildStringExpression(entityVariable, field.getFieldName())));
        }
        isFirst = false;

      } else {

        try {
          if (ClassUtils.getClass(field.getFieldType().getFullyQualifiedTypeName()).getSuperclass()
              .equals(Number.class)) {
            if (isFirst) {
              bodyBuilder.appendFormalLine(String.format("expression = %s;",
                  buildNumberExpression(entityVariable, field.getFieldName())));
            } else {
              bodyBuilder.appendFormalLine(String.format("expression = expression.or(%s);",
                  buildNumberExpression(entityVariable, field.getFieldName())));
            }
          } else {
            convertedFields.add(field);
          }
        } catch (ClassNotFoundException e) {
        }
      }
    }

    isFirst = buildConvertedFieldsQuery(bodyBuilder, convertedFields, isFirst, entityVariable);

    if (!isFirst) {
      // where.and(property1.operator(text).or(property2.operator(text).or(...)))
      bodyBuilder.appendFormalLine(String.format("where.and(expression);\n"));
    }

    // End if 
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine(String.format("}"));

    bodyBuilder.appendFormalLine(String.format("query.where(where);\n"));
  }

  /**
   * Builds a query using field that needed to be converted previously to a java type.
   * Query method should check if converted type is correct (not null)
   * 
   * @param bodyBuilder method body builder
   * @param fields fields that need a value converted to a specific java type
   * @param isFirst indicates if the query  has already added expressions
   * @param entityVariable name of the variable that contains the Q entity 
   * @return True is returned if the query contains any field expression. Otherwise, returns false
   */
  private boolean buildConvertedFieldsQuery(InvocableMemberBodyBuilder bodyBuilder,
      List<FieldMetadata> fields, boolean isFirst, String entityVariable) {
    String expression = "";

    for (FieldMetadata field : fields) {
      if (field.getFieldType().equals(new JavaType(Date.class))) {
        expression = buildDateExpression(entityVariable, field.getFieldName());
      } else if (field.getFieldType().equals(new JavaType(Calendar.class))) {
        expression = buildCalendarExpression(entityVariable, field.getFieldName());
      } else if (field.getFieldType().isBoolean()) {
        expression = buildBooleanExpression(entityVariable, field.getFieldName());
      }

      bodyBuilder.appendFormalLine(String.format("if(search%s != null){", field.getFieldType()
          .getSimpleTypeName()));

      bodyBuilder.indent();

      if (isFirst) {
        bodyBuilder.appendFormalLine(String.format("expression = %s;", expression));
      } else {
        bodyBuilder.appendFormalLine(String.format("expression = expression.or(%s);", expression));
      }
      isFirst = false;

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine(String.format("}", expression));
    }

    return isFirst;
  }

  /**
   * Creates a calendar query expression with the given property
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param fieldName property name
   * @return string that represents the expression
   */
  private String buildCalendarExpression(String entityVariable, JavaSymbolName fieldName) {
    // qEntity.property.eq(searchCalendar);
    return String.format("%s.%s.eq(searchCalendar)", entityVariable, fieldName);
  }

  /**
   * Creates a boolean query expression with the given property
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param fieldName property name
   * @return string that represents the expression
   */
  private String buildBooleanExpression(String entityVariable, JavaSymbolName fieldName) {
    // qEntity.property.eq(BooleanUtils.toBooleanObject(txt));
    return String.format("%s.%s.eq(searchBoolean)", entityVariable, fieldName);
  }

  /**
   * Creates a number query expression with the given property
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param fieldName property name
   * @return string that represents the expression
   */
  private String buildNumberExpression(String entityVariable, JavaSymbolName fieldName) {
    // qEntity.property.like("%".concat(txt).concat("%"));
    return String
        .format("%s.%s.like(\"%%\".concat(txt).concat(\"%%\"))", entityVariable, fieldName);
  }

  /**
   * Creates a date query expression with the given property
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param fieldName property name
   * @return string that represents the expression
   */
  private String buildDateExpression(String entityVariable, JavaSymbolName fieldName) {
    // qEntity.property.eq(DateUtils.parseDateStrictly(txt, FULL_DATE_PATTERNS))
    return String.format("%s.%s.eq(searchDate)", entityVariable, fieldName);
  }

  /**
   * Creates a string query expression with the given property
   * 
   * @param entityVariable name of the variable that contains the Q entity 
   * @param fieldName property name
   * @return string that represents the expression
   */
  private String buildStringExpression(String entityVariable, JavaSymbolName fieldName) {
    // qEntity.property.containsIgnoreCase(txt)
    return String.format("%s.%s.containsIgnoreCase(txt)", entityVariable, fieldName);
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


  public boolean isDTO() {
    return this.isDTO;
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
