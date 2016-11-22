package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderMethod;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustom;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
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
import java.util.Collections;
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


  private final JavaType defaultReturnType;
  private final Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethods;
  private final List<Pair<MethodMetadata, PartTree>> customFinderMethods;
  private final List<Pair<MethodMetadata, PartTree>> customCountMethods;

  private final MethodMetadata findAllGlobalSearchMethod;

  private Map<JavaSymbolName, MethodMetadata> finderMethodsAndCounts;

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
   * @param searchResult the java type o the search result returned by findAll finder
   * @param repositoryMetadata list of information of fields which entity is child part
   * @param relationsAsChild
   */
  public RepositoryJpaCustomMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaCustomAnnotationValues annotationValues, final JavaType domainType,
      final RepositoryJpaMetadata repositoryMetadata,
      List<Pair<FieldMetadata, RelationInfo>> relationsAsChild) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");
    Validate.notNull(repositoryMetadata, "Referenced fields could be empty but not null");

    this.defaultReturnType = repositoryMetadata.getDefaultReturnType();

    this.finderMethodsAndCounts = new HashMap<JavaSymbolName, MethodMetadata>();

    ArrayList<Pair<MethodMetadata, PartTree>> tmpCustomFinderMethods =
        new ArrayList<Pair<MethodMetadata, PartTree>>();
    ArrayList<Pair<MethodMetadata, PartTree>> tmpCustomCountMethods =
        new ArrayList<Pair<MethodMetadata, PartTree>>();

    Map<FieldMetadata, MethodMetadata> tempTeferencedFieldsFindAllMethods =
        new HashMap<FieldMetadata, MethodMetadata>(relationsAsChild.size());

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
      tempTeferencedFieldsFindAllMethods.put(referencedField.getLeft(), method);
    }
    referencedFieldsFindAllMethods =
        Collections.unmodifiableMap(tempTeferencedFieldsFindAllMethods);

    // Generate findAll method
    if (!composition) {
      findAllGlobalSearchMethod = getFindAllGlobalSearchMethod();
      ensureGovernorHasMethod(new MethodMetadataBuilder(findAllGlobalSearchMethod));
    } else {
      findAllGlobalSearchMethod = null;
    }

    // Prepare a list of all finder and count methods already declared on
    // repository. While generate new methods, this list will be ground.
    ArrayList<MethodMetadata> allCountMethods = new ArrayList<MethodMetadata>();
    allCountMethods.addAll(repositoryMetadata.getCountMethods());
    ArrayList<MethodMetadata> allFinderMethods = new ArrayList<MethodMetadata>();
    allCountMethods.addAll(repositoryMetadata.getFindersGenerated());

    // Generate finder methods if any
    if (repositoryMetadata.getFindersToAddInCustom() != null
        && !repositoryMetadata.getFindersToAddInCustom().isEmpty()) {
      FinderMethod finderMethod;
      for (Pair<FinderMethod, PartTree> finderInfo : repositoryMetadata.getFindersToAddInCustom()) {
        finderMethod = finderInfo.getKey();
        JavaType fromBean = finderMethod.getParameters().get(0).getType();
        MethodMetadata method =
            getCustomFinder(finderMethod.getReturnType(), finderMethod.getMethodName(), fromBean);
        if (!isAlreadyDeclaredMethod(method, allFinderMethods)) {
          ensureGovernorHasMethod(new MethodMetadataBuilder(method));
          tmpCustomFinderMethods.add(Pair.of(method, finderInfo.getRight()));
        }
        allFinderMethods.add(method);

        // Generate a count method for each custom finder if they aren't count methods
        MethodMetadata countMethod = null;
        if (!StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
          countMethod = getCustomCount(fromBean, finderMethod.getMethodName());
          if (!isAlreadyDeclaredMethod(countMethod, allCountMethods)) {
            ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
            tmpCustomCountMethods.add(Pair.of(countMethod, finderInfo.getRight()));
          }
          allCountMethods.add(countMethod);
        }
        finderMethodsAndCounts.put(method.getMethodName(), countMethod);
      }
    }
    customCountMethods = Collections.unmodifiableList(tmpCustomCountMethods);
    customFinderMethods = Collections.unmodifiableList(tmpCustomFinderMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private boolean isAlreadyDeclaredMethod(MethodMetadata method,
      List<MethodMetadata> alreadyDeclaredMethods) {
    for (MethodMetadata declared : alreadyDeclaredMethods) {
      if (method.matchSignature(declared)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method that generates the findAll method on current interface.
   *
   * @return
   */
  private MethodMetadata getFindAllGlobalSearchMethod() {

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
  private MethodMetadata getFindAllMethodByReferencedField(FieldMetadata referencedField,
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
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(referencedField.getFieldName()
        .getSymbolName())));
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
  private MethodMetadata getCustomFinder(JavaType finderReturnType, JavaSymbolName finderName,
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
  private MethodMetadata getCustomCount(JavaType formBean, JavaSymbolName finderName) {

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
  public List<Pair<MethodMetadata, PartTree>> getCustomFinderMethods() {
    return customFinderMethods;
  }

  /**
   * Returns all count methods of all custom finder methods
   *
   * @return
   */
  public List<Pair<MethodMetadata, PartTree>> getCustomCountMethods() {
    return customCountMethods;
  }

  /**
   * Return the finder name methods and the related count method if exists.
   * 
   * @return
   */
  public Map<JavaSymbolName, MethodMetadata> getFinderMethodsAndCounts() {
    return finderMethodsAndCounts;
  }

  /**
   *
   * @return method findAll declared for this repository
   */
  public MethodMetadata getCurrentFindAllGlobalSearchMethod() {
    return findAllGlobalSearchMethod;
  }
}
