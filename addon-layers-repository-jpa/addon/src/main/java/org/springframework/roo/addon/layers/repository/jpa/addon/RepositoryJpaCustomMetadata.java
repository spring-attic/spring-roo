package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustom;
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
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for {@link RooJpaRepositoryCustom}.
 *
 * @author Paula Navarro
 * @since 2.0
 */
public class RepositoryJpaCustomMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaSymbolName PAGEABLE_PARAMETER_NAME = new JavaSymbolName("pageable");
  private static final JavaSymbolName GOBAL_SEARCH_PARAMETER_NAME = new JavaSymbolName(
      "globalSearch");
  private static final AnnotatedJavaType PAGEABLE_PARAMETER = new AnnotatedJavaType(
      SpringJavaType.PAGEABLE);
  private static final AnnotatedJavaType GLOBAL_SEARCH_PARAMETER = AnnotatedJavaType
      .convertFromJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH);
  private static final String PROVIDES_TYPE_STRING = RepositoryJpaCustomMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private JavaType defaultReturnType;
  private Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethods;
  private List<MethodMetadata> customFinderMethods;
  private List<MethodMetadata> customCountMethods;

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
   * @param searchResult the java type o the search result returned by findAll finder
   * @param relationsAsChild list of information of fields which entity is child part
   * @param findersToAdd
   */
  public RepositoryJpaCustomMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomAnnotationValues annotationValues, final JavaType domainType,
      final JavaType defaultReturnType,
      final List<Pair<FieldMetadata, RelationInfo>> relationsAsChild,
      List<CustomFinderMethod> findersToAdd) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");
    Validate.notNull(defaultReturnType, "Search result required");
    Validate.notNull(relationsAsChild, "Referenced fields could be empty but not null");

    this.defaultReturnType = defaultReturnType;
    this.customFinderMethods = new ArrayList<MethodMetadata>();
    this.customCountMethods = new ArrayList<MethodMetadata>();

    referencedFieldsFindAllMethods = new HashMap<FieldMetadata, MethodMetadata>();

    boolean composition = false;
    // Generate findAllMethod for every referencedFields
    for (Pair<FieldMetadata, RelationInfo> referencedField : relationsAsChild) {
      if (referencedField.getRight().type == JpaRelationType.COMPOSITION) {
        // check for more than one part of compositions as child part
        Validate.isTrue(!composition,
            "Entity %s has defined more than one relations as child part whit type composition.",
            aspectName);
        composition = true;
      }
      MethodMetadata method =
          getFindAllMethodByReferencedField(referencedField.getLeft(), referencedField.getValue());
      ensureGovernorHasMethod(new MethodMetadataBuilder(method));
      referencedFieldsFindAllMethods.put(referencedField.getLeft(), method);
    }

    // Generate findAll method
    if (!composition) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllGlobalSearchMethod()));
    }

    // TODO
    // Generate finder methods if any
    if (findersToAdd != null) {
      for (CustomFinderMethod finderMethod : findersToAdd) {
        MethodMetadata method =
            getCustomFinder(finderMethod.getReturnType(), finderMethod.getMethodName(),
                finderMethod.getFormBean());
        ensureGovernorHasMethod(new MethodMetadataBuilder(method));
        if (!customFinderMethods.contains(method)) {
          customFinderMethods.add(method);
        }

        // Generate a count method for each custom finder if they aren't count methods
        if (!StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
          MethodMetadata countMethod =
              getCustomCount(finderMethod.getFormBean(), finderMethod.getMethodName());
          ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
          if (!customCountMethods.contains(countMethod)) {
            customCountMethods.add(countMethod);
          }
        }
      }
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates the findAll method on current interface.
   *
   * @return
   */
  public MethodMetadata getFindAllGlobalSearchMethod() {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    //Global search parameter
    parameterTypes.add(GLOBAL_SEARCH_PARAMETER);
    parameterNames.add(GOBAL_SEARCH_PARAMETER_NAME);

    // Pageable parameter
    parameterTypes.add(PAGEABLE_PARAMETER);
    parameterNames.add(PAGEABLE_PARAMETER_NAME);

    // Method name
    JavaSymbolName methodName = new JavaSymbolName("findAll");

    // Return type
    JavaType returnType =
        new JavaType("org.springframework.data.domain.Page", 0, DataType.TYPE, null,
            Arrays.asList(defaultReturnType));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            returnType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
  }

  /**
   * Method that generates the findAll method for provided referenced field on current interface.
   *
   * @param referencedField
   * @param identifierType
   *
   * @return
   */
  public MethodMetadata getFindAllMethodByReferencedField(FieldMetadata referencedField,
      RelationInfo relationInfo) {

    // Define method name
    String findPattern = "findBy%s";
    if (relationInfo.cardinality == Cardinality.MANY_TO_MANY) {
      findPattern = "findBy%sContains";
    }

    // Method name
    JavaSymbolName methodName =
        new JavaSymbolName(String.format(findPattern, referencedField.getFieldName()
            .getSymbolNameCapitalisedFirstLetter()));

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final JavaType paramType = referencedField.getFieldType().getBaseType();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(paramType));
    parameterTypes.add(GLOBAL_SEARCH_PARAMETER);
    parameterTypes.add(PAGEABLE_PARAMETER);

    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(paramType.getSimpleTypeName())));
    parameterNames.add(GOBAL_SEARCH_PARAMETER_NAME);
    parameterNames.add(PAGEABLE_PARAMETER_NAME);

    // Return type
    JavaType returnType =
        new JavaType(SpringJavaType.PAGE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(defaultReturnType));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            returnType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
  }

  /**
   * Method that generates finder methods whose return types are projections.
   *
   * @param finderReturnType
   * @param finderName
   * @param parameterType
   *
   * @return
   */
  public MethodMetadata getCustomFinder(JavaType finderReturnType, JavaSymbolName finderName,
      JavaType parameterType) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(parameterType));
    parameterTypes.add(GLOBAL_SEARCH_PARAMETER);
    parameterTypes.add(PAGEABLE_PARAMETER);

    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("formBean"));
    parameterNames.add(GOBAL_SEARCH_PARAMETER_NAME);
    parameterNames.add(PAGEABLE_PARAMETER_NAME);

    // Return type
    JavaType returnType =
        new JavaType(SpringJavaType.PAGE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(finderReturnType));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, finderName,
            returnType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
  }

  /**
   * Method that generates count methods for custom finders.
   *
   * @param formBean the object containing the properties to search to
   * @param javaSymbolName the finder name
   * @return
   */
  public MethodMetadata getCustomCount(JavaType formBean, JavaSymbolName finderName) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(formBean));
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("formBean"));

    // Create count method name
    String countName = finderName.getSymbolName();
    if (StringUtils.startsWith(countName, "find")) {
      countName = StringUtils.removeStart(countName, "find");
    } else if (StringUtils.startsWith(countName, "query")) {
      countName = StringUtils.removeStart(countName, "query");
    } else if (StringUtils.startsWith(countName, "read")) {
      countName = StringUtils.removeStart(countName, "read");
    }
    countName = "count".concat(countName);
    JavaSymbolName countMethodName = new JavaSymbolName(countName);

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, countMethodName,
            JavaType.LONG_PRIMITIVE, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
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

  public JavaType getDefaultReturnType() {
    return defaultReturnType;
  }

  /**
   * This method returns all findAll methods for
   * referenced fields
   *
   * @return
   */
  public Map<FieldMetadata, MethodMetadata> getReferencedFieldsFindAllMethods() {
    return referencedFieldsFindAllMethods;
  }

  /**
   * This method returns all finder methods which return a projection
   *
   * @return
   */
  public List<MethodMetadata> getCustomFinderMethods() {
    return customFinderMethods;
  }

  /**
   * This method returns all count methods of all custom finder methods
   *
   * @return
   */
  public List<MethodMetadata> getCustomCountMethods() {
    return customCountMethods;
  }
}
