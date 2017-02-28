package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderMethod;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for {@link RooJpaRepository}.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryJpaMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaSymbolName SAVE_METHOD_NAME = new JavaSymbolName("save");
  private static final JavaSymbolName FIND_ONE_METHOD_NAME = new JavaSymbolName("findOne");
  private static final JavaSymbolName FIND_ALL_ITERATOR_METHOD_NAME = new JavaSymbolName("findAll");
  private static final String PROVIDES_TYPE_STRING = RepositoryJpaMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final Map<FieldMetadata, MethodMetadata> countMethodByReferencedFields;
  private final FieldMetadata compositionField;
  private final RelationInfo compositionInfo;
  private final MethodMetadata compositionCountMethod;
  private final JavaType customRepository;
  private final JavaType entity;
  private final JavaType defaultReturnType;

  private final List<FinderMethod> findersDeclared;
  private final List<MethodMetadata> findersGenerated;
  private final List<MethodMetadata> countMethods;
  private final List<Pair<FinderMethod, PartTree>> findersToAddInCustom;
  private final List<String> declaredFinderNames;

  private Map<JavaSymbolName, MethodMetadata> finderMethodsAndCounts;

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails repositoryDetails) {
    final LogicalPath repositoryLogicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    return createIdentifier(repositoryDetails.getType(), repositoryLogicalPath);
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
   * @param entityMetadata boolean
   * @param readOnlyRepository JavaType
   * @param customRepository
   * @param relationsAsChild
   * @param findersToAdd
   * @param findersToAddInCustom
   * @param declaredFinderNames
   * @param referenceFields Map<JavaType, JavaType> that contains referenceField type
   * and its identifier type
   */
  public RepositoryJpaMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaAnnotationValues annotationValues, final JpaEntityMetadata entityMetadata,
      final JavaType readOnlyRepository, final JavaType customRepository,
      final JavaType defaultReturnType, List<Pair<FieldMetadata, RelationInfo>> relationsAsChild,
      List<FinderMethod> findersToAdd, List<Pair<FinderMethod, PartTree>> findersToAddInCustom,
      List<String> declaredFinderNames) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    final FieldMetadata identifierField = entityMetadata.getCurrentIndentifierField();
    final JavaType identifierType = identifierField.getFieldType();
    this.entity = annotationValues.getEntity();
    this.defaultReturnType = defaultReturnType;

    this.findersToAddInCustom = Collections.unmodifiableList(findersToAddInCustom);
    this.customRepository = customRepository;
    this.countMethodByReferencedFields = new HashMap<FieldMetadata, MethodMetadata>();
    this.declaredFinderNames = Collections.unmodifiableList(declaredFinderNames);

    this.finderMethodsAndCounts = new HashMap<JavaSymbolName, MethodMetadata>();

    // Iterate over fields which are child fields
    boolean composition = false;
    MethodMetadata countMethod = null;
    MethodMetadata compositionCountMethod = null;
    Pair<FieldMetadata, RelationInfo> compositionFieldInfo = null;
    for (Pair<FieldMetadata, RelationInfo> fieldInfo : relationsAsChild) {
      countMethod = getCountMethodByField(fieldInfo.getLeft(), fieldInfo.getRight());
      ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
      countMethodByReferencedFields.put(fieldInfo.getLeft(), countMethod);

      // Check composition relation
      if (fieldInfo.getRight().type == JpaRelationType.COMPOSITION) {
        // check for more than one part of compositions as child part
        Validate.isTrue(!composition,
            "Entity %s has defined more than one relations as child part whit type composition.",
            aspectName);
        composition = true;
        ensureGovernorHasMethod(new MethodMetadataBuilder(getFindOneMethod(entity, identifierField)));
        ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllIteratorMethod(entity,
            identifierField)));
        ensureGovernorHasMethod(new MethodMetadataBuilder(getSaveMethod(entity)));
        compositionCountMethod = countMethod;
        compositionFieldInfo = fieldInfo;
      }
    }
    if (composition) {
      this.compositionField = compositionFieldInfo.getLeft();
      this.compositionInfo = compositionFieldInfo.getRight();
      this.compositionCountMethod = compositionCountMethod;
    } else {
      this.compositionField = null;
      this.compositionInfo = null;
      this.compositionCountMethod = null;
    }

    // Add Repository interface
    JavaType interfaceType = null;
    if (entityMetadata.isReadOnly()) {
      // If readOnly, extends ReadOnlyRepository
      interfaceType = readOnlyRepository;
    } else {
      // Extends JpaRepository
      interfaceType = SpringletsJavaType.SPRINGLETS_DETACHABLE_JPA_REPOSITORY;
    }
    ensureGovernorExtends(JavaType.wrapperOf(interfaceType, annotationValues.getEntity(),
        identifierType));

    // If has some RepositoryCustom associated, add extends
    ensureGovernorExtends(customRepository);

    // All repositories are generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Prepare list of ALL finders and count declared
    List<MethodMetadata> findersTmp = new ArrayList<MethodMetadata>();
    List<MethodMetadata> countMethodsTmp = new ArrayList<MethodMetadata>();
    if (compositionCountMethod != null) {
      countMethodsTmp.add(compositionCountMethod);
    }
    for (MethodMetadata declared : countMethodByReferencedFields.values()) {
      countMethodsTmp.add(declared);
    }

    // Including finders and count methods
    for (FinderMethod finderMethod : findersToAdd) {
      MethodMetadata method = getFinderMethod(finderMethod).build();
      if (!isAlreadyDeclaredMethod(method, findersTmp)) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(method));
      }
      findersTmp.add(method);

      // Generate a count method for each finder if they aren't count methods
      if (!StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
        countMethod = getCountMethod(finderMethod).build();
        if (!isAlreadyDeclaredMethod(countMethod, countMethodsTmp)) {
          ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
        }
        countMethodsTmp.add(countMethod);
      }
      finderMethodsAndCounts.put(method.getMethodName(), countMethod);
    }

    this.findersDeclared = Collections.unmodifiableList(new ArrayList<FinderMethod>(findersToAdd));
    this.findersGenerated = Collections.unmodifiableList(findersTmp);
    this.countMethods = Collections.unmodifiableList(countMethodsTmp);

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

  private MethodMetadata getFindOneMethod(JavaType entity, FieldMetadata identifierFieldMetadata) {
    // Define method parameter type and name
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterTypes
        .add(AnnotatedJavaType.convertFromJavaType(identifierFieldMetadata.getFieldType()));
    parameterNames.add(identifierFieldMetadata.getFieldName());

    MethodMetadata existingMethod =
        getGovernorMethod(FIND_ONE_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
        FIND_ONE_METHOD_NAME, entity, parameterTypes, parameterNames, null).build();
  }


  private MethodMetadata getSaveMethod(JavaType entity) {
    // Define method parameter type and name
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(entity.getSimpleTypeName())));

    MethodMetadata existingMethod =
        getGovernorMethod(SAVE_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
        SAVE_METHOD_NAME, entity, parameterTypes, parameterNames, null).build();
  }

  private MethodMetadata getFindAllIteratorMethod(JavaType entity,
      FieldMetadata identifierFieldMetadata) {
    // Define method parameter type and name
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType
        .iterableOf(identifierFieldMetadata.getFieldType())));
    parameterNames.add(identifierFieldMetadata.getFieldName());

    MethodMetadata existingMethod =
        getGovernorMethod(FIND_ALL_ITERATOR_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
        FIND_ALL_ITERATOR_METHOD_NAME, JavaType.listOf(entity), parameterTypes, parameterNames,
        null).build();
  }

  /**
   * Method that generates method "countByField" method.
   *
   * @param relationInfo
   * @param identifierType
   *
   * @return field
   */
  public MethodMetadata getCountMethodByField(FieldMetadata field, RelationInfo relationInfo) {

    // Define method name
    String countPattern = "countBy%s";
    if (relationInfo.cardinality == Cardinality.MANY_TO_MANY) {
      countPattern = "countBy%sContains";
    }

    final JavaSymbolName methodName =
        new JavaSymbolName(String.format(countPattern, field.getFieldName()
            .getSymbolNameCapitalisedFirstLetter()));

    // Define method parameter type and name
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final JavaType paramType = field.getFieldType().getBaseType();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(paramType));
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(field.getFieldName()
        .getSymbolName())));

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

    return methodBuilder.build();
  }

  /**
   * Method that generates finder method on current interface
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadataBuilder getFinderMethod(FinderMethod finderMethod) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    for (FinderParameter param : finderMethod.getParameters()) {
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(param.getType()));
      parameterNames.add(param.getName());
    }

    // Add additional Pageable method
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));
    parameterNames.add(new JavaSymbolName("pageable"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            finderMethod.getMethodName(), finderMethod.getReturnType(), parameterTypes,
            parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }



  /**
   * Method that generates finder method on current interface
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadataBuilder getCountMethod(FinderMethod finderMethod) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    for (FinderParameter param : finderMethod.getParameters()) {
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(param.getType()));
      parameterNames.add(param.getName());
    }

    // Create count method name
    String countName = finderMethod.getMethodName().getSymbolName();
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

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns all generated countMethodByReferencedFields
   *
   * @return Map with key that identifies referenced field and method metadata
   */
  public Map<FieldMetadata, MethodMetadata> getCountMethodByReferencedFields() {
    return countMethodByReferencedFields;
  }

  /**
   * @return composition count method
   */
  public MethodMetadata getCompositionCountMethod() {
    return compositionCountMethod;
  }

  /**
   * @return composition field
   */
  public FieldMetadata getCompositionField() {
    return compositionField;
  }

  /**
   * @return composition relation info
   */
  public RelationInfo getCompositionInfo() {
    return compositionInfo;
  }

  /**
   * @return list of related custom repositories
   */
  public JavaType getCustomRepository() {
    return customRepository;
  }

  /**
   * @return true if entity is composition
   */
  public boolean isComposition() {
    return compositionField != null;
  }

  /**
   * @return entity handles by current repository
   */
  public JavaType getEntity() {
    return entity;
  }

  /**
   * @return finders definition which should be add in custom repository
   */
  public List<Pair<FinderMethod, PartTree>> getFindersToAddInCustom() {
    return findersToAddInCustom;
  }

  /**
   * @return all countMethods declared in repository
   */
  public List<MethodMetadata> getCountMethods() {
    return countMethods;
  }

  /**
   * @return finders definition declared to implement in repository
   */
  public List<FinderMethod> getFindersDeclared() {
    return findersDeclared;
  }

  /**
   * @return finders method generated in repository
   */
  public List<MethodMetadata> getFindersGenerated() {
    return findersGenerated;
  }

  /**
   * @return all declared finder names
   */
  public List<String> getDeclaredFinderNames() {
    return declaredFinderNames;
  }

  /**
   * Return defaultReturnType specified value (if any)
   *
   * @return defaultReturnType or entity type if value is not set
   */
  public JavaType getDefaultReturnType() {
    return defaultReturnType;
  }

  /**
   * Return the finder name methods and the related count method if exists.
   * 
   * @return
   */
  public Map<JavaSymbolName, MethodMetadata> getFinderMethodsAndCounts() {
    return finderMethodsAndCounts;
  }

  @Override
  public int hashCode() {
    // Override hashCode to propagate changes in non-modified-itd changes
    // (as finders which will generate on RepositoryCustom)
    int result = super.hashCode();
    StringBuilder sb = new StringBuilder("");
    for (Pair<FinderMethod, PartTree> finder : findersToAddInCustom) {
      sb.append(finder.getLeft().getMethodName().getSymbolName());
    }

    // Add hashCode changes for normal finders as they change an annotation 
    // inside the annotation which triggers this metadata
    for (FinderMethod finder : findersDeclared) {
      sb.append(finder.getMethodName().getSymbolName());
    }

    // Combine hashCodes
    result += sb.toString().hashCode();

    return result;
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
    builder.append("entity", entity);
    return builder.toString();
  }
}
