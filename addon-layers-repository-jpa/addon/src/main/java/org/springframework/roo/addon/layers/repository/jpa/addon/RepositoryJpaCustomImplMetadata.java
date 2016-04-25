package org.springframework.roo.addon.layers.repository.jpa.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
   * @param findAllGlobalSearchMethod the findAll metadata 
   */
  public RepositoryJpaCustomImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomImplAnnotationValues annotationValues, final JavaType domainType,
      final List<FieldMetadata> validFields, final MethodMetadata findAllGlobalSearchMethod) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    this.importResolver = builder.getImportRegistrationResolver();
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.entity = domainType;

    // Get repository that needs to be implemented
    ensureGovernorImplements(annotationValues.getRepository());

    // All repositories should be generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Generate findAll implementation method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllImpl(validFields)));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates the findAll implementation method
   * 
   * @return
   */
  public MethodMetadata getFindAllImpl(List<FieldMetadata> fields) {

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
    JavaType orderSpecifier = new JavaType("com.mysema.query.types.OrderSpecifier");
    JavaType booleanBuilder = new JavaType("com.mysema.query.BooleanBuilder");
    JavaType jpql = new JavaType("com.mysema.query.jpa.JPQLQuery");
    JavaType sort = new JavaType("org.springframework.data.domain.Sort");
    JavaType pathBuilder = new JavaType("com.mysema.query.types.path.PathBuilder");
    JavaType order = new JavaType("com.mysema.query.types.Order");
    JavaType pageImpl = new JavaType("org.springframework.data.domain.PageImpl");
    JavaType constructorExp = new JavaType("com.mysema.query.types.ConstructorExpression");


    // QEntity qEntity = QEntity.entity;
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = %1$s.%2$s;",
        qEntity.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    //JPQLQuery query = from(qEntity);
    bodyBuilder.appendFormalLine(String.format("%s query = from(%s);",
        jpql.getNameIncludingTypeParameters(false, importResolver), entityVariable));

    // BooleanBuilder where = new BooleanBuilder()
    bodyBuilder.appendFormalLine(String.format("%1$s where = new %1$s();\n",
        booleanBuilder.getNameIncludingTypeParameters(false, importResolver)));

    // if (globalSearch != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", globalSearch));

    // String txt = globalSearch.getText();
    bodyBuilder.appendFormalLine(String.format("    String txt = %s.getText();", globalSearch));

    // Start query
    bodyBuilder.appendFormalLine(String.format("    where.and("));
    boolean addOr = false;
    String query;
    for (FieldMetadata field : fields) {

      if (field.getFieldType().equals(JavaType.STRING)) {
        query =
            String.format("%s.%s.containsIgnoreCase(txt)", entityVariable, field.getFieldName());
      } else {
        query =
            String.format("%s.%s.like(\"%%\".concat(txt).concat(\"%%\"))", entityVariable,
                field.getFieldName());
      }

      if (addOr) {
        bodyBuilder.appendFormalLine(String.format("        .or(%s)", query));
      } else {
        bodyBuilder.appendFormalLine(String.format("        %s", query));
      }
      addOr = true;
    }

    // End query
    bodyBuilder.appendFormalLine(String.format("    );"));

    // End if 
    bodyBuilder.appendFormalLine(String.format("}"));

    bodyBuilder.appendFormalLine(String.format("query.where(where);\n"));

    bodyBuilder.appendFormalLine(String.format("long totalFound = query.count();"));


    // if (pageable != null) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null) {", pageable));

    //  if (pageable.getSort() != null) {
    bodyBuilder.appendFormalLine(String.format("    if (%s.getSort() != null) {", pageable));

    //for (Sort.Order sortOrder : pageable.getSort()) {
    bodyBuilder.appendFormalLine(String.format("       for (%s.Order sortOrder : %s.getSort()) {",
        sort.getNameIncludingTypeParameters(false, importResolver), pageable));

    //  PathBuilder<Entity> orderByExpression = new PathBuilder<Entity>(Entity.class, "entity");
    bodyBuilder.appendFormalLine(String.format(
        "           %3$s<%1$s> orderByExpression = new %3$s<%1s>(%1$s.class, \"%2$s\");", entity,
        entityVariable, pathBuilder.getNameIncludingTypeParameters(false, importResolver)));

    // Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
    bodyBuilder.appendFormalLine(String.format(
        "           %1$s direction = sortOrder.isAscending() ? %1$s.ASC : %1$s.DESC;\n",
        order.getNameIncludingTypeParameters(false, importResolver)));

    String condition = null;
    boolean elseCondition = false;
    for (FieldMetadata field : fields) {

      if (!field.getFieldType().equals(JavaType.STRING)) {
        continue;
      }

      if (!elseCondition) {
        condition = "if";
        elseCondition = true;
      } else {
        condition = "else if";
      }

      // if ("property1".equals(sortOrder.getProperty())) {
      bodyBuilder.appendFormalLine(String.format(
          "           %s (\"%s\".equals(sortOrder.getProperty())) {", condition,
          field.getFieldName()));

      // query.orderBy(new OrderSpecifier<String>(direction, qEntity.property1));
      bodyBuilder.appendFormalLine(String.format(
          "             query.orderBy(new %s<String>(direction, %s.%s));",
          orderSpecifier.getNameIncludingTypeParameters(false, importResolver), entityVariable,
          field.getFieldName()));
      bodyBuilder.appendFormalLine("            }");
    }

    if (condition != null) {
      bodyBuilder.appendFormalLine("            else {");
    }
    bodyBuilder
        .appendFormalLine(String
            .format(
                "               query.orderBy( new %s(direction, orderByExpression.get(sortOrder.getProperty())));",
                orderSpecifier.getNameIncludingTypeParameters(false, importResolver)));
    if (condition != null) {
      bodyBuilder.appendFormalLine("            }");
    }


    // End  if (%s.getSort() != null) 
    bodyBuilder.appendFormalLine("      }");

    // End if (%s != null)
    bodyBuilder.appendFormalLine("  }");

    //  query.offset(pageable.getOffset()).limit(pageable.getPageSize());}
    bodyBuilder.appendFormalLine(String.format(
        "   query.offset(%1$s.getOffset()).limit(%1$s.getPageSize());", pageable));

    // End for
    bodyBuilder.appendFormalLine("}");

    // List<Entity> results = query.list(ConstructorExpression.create(Entity.class, qEntity.parameter1, qEntity.parameter1, ...));
    bodyBuilder.appendFormalLine(String.format(
        "%1$s<%2$s> results = query.list(%3$s.create(%2$s.class, %4$s ));", new JavaType(
            "java.util.List").getNameIncludingTypeParameters(false, importResolver), returnType
            .getNameIncludingTypeParameters(false, importResolver), constructorExp
            .getNameIncludingTypeParameters(false, importResolver), StringUtils.join(queryList,
            ", ")));

    //return new PageImpl<Entity>(results, pageable, totalFound);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(results, %s, totalFound);",
        pageImpl.getNameIncludingTypeParameters(false, importResolver),
        returnType.getNameIncludingTypeParameters(false, importResolver), pageable));

    // Sets body to generated method
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
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
