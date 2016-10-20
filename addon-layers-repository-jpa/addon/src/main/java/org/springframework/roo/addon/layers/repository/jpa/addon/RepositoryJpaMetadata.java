package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
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

  private static final JavaSymbolName FIND_ONE_METHOD_NAME = new JavaSymbolName("findOne");
  private static final JavaSymbolName FIND_ALL_ITERATOR_METHOD_NAME = new JavaSymbolName("findAll");
  private static final String PROVIDES_TYPE_STRING = RepositoryJpaMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final Map<FieldMetadata, MethodMetadata> countMethodByReferencedFields;
  private final FieldMetadata compositionField;
  private final RelationInfo compositionInfo;
  private final MethodMetadata compositionCountMethod;
  private final List<JavaType> customRepositories;

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
   * @param entityMetadata boolean
   * @param readOnlyRepository JavaType
   * @param customRepositories List<JavaType>
   * @param relationsAsChild
   * @param referenceFields Map<JavaType, JavaType> that contains referenceField type
   * and its identifier type
   */
  public RepositoryJpaMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaAnnotationValues annotationValues, final JpaEntityMetadata entityMetadata,
      final JavaType readOnlyRepository, final List<JavaType> customRepositories,
      List<Pair<FieldMetadata, RelationInfo>> relationsAsChild) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");

    final FieldMetadata identifierField = entityMetadata.getCurrentIndentifierField();
    final JavaType identifierType = identifierField.getFieldType();
    final JavaType domainType = annotationValues.getEntity();

    this.customRepositories = Collections.unmodifiableList(customRepositories);

    countMethodByReferencedFields = new HashMap<FieldMetadata, MethodMetadata>();

    // Iterate over fields which are child fields
    boolean composition = false;
    MethodMetadata countMethod;
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
        ensureGovernorHasMethod(new MethodMetadataBuilder(getFindOneMethod(domainType,
            identifierField)));
        ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllIteratorMethod(domainType,
            identifierField)));
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
    if (composition) {
      // If composition extends Repository
      interfaceType = SpringJavaType.SPRING_DATA_REPOSITORY;
    } else if (entityMetadata.isReadOnly()) {
      // If readOnly, extends ReadOnlyRepository
      interfaceType = readOnlyRepository;
    } else {
      // Extends JpaRepository
      interfaceType = SpringJavaType.SPRING_JPA_REPOSITORY;
    }
    ensureGovernorExtends(JavaType.wrapperOf(interfaceType, annotationValues.getEntity(),
        identifierType));

    // If has some RepositoryCustom associated, add extends
    if (!customRepositories.isEmpty()) {
      // Extends RepositoryCustom
      for (JavaType repositoryCustom : customRepositories) {
        ensureGovernorExtends(repositoryCustom);
      }
    }

    // All repositories are generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Build the ITD
    itdTypeDetails = builder.build();
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
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(paramType.getSimpleTypeName())));

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
  public List<JavaType> getCustomRepositories() {
    return customRepositories;
  }

  /**
   * @return true if entity is composition
   */
  public boolean isComposition() {
    return compositionField != null;
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
