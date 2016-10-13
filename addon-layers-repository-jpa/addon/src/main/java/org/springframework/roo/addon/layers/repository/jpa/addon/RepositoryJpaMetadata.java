package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
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
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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

  private static final String PROVIDES_TYPE_STRING = RepositoryJpaMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private Map<FieldMetadata, MethodMetadata> countMethodByReferencedFields;
  private FieldMetadata compositionField;
  private RelationInfo compositionInfo;
  private MethodMetadata compositionCountMethod;

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
   * @param entityMetadata boolean
   * @param readOnlyRepository JavaType
   * @param customRepositories List<JavaType>
   * @param referenceFields Map<JavaType, JavaType> that contains referenceField type
   * and its identifier type
   */
  public RepositoryJpaMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaAnnotationValues annotationValues, final JavaType identifierType,
      final JpaEntityMetadata entityMetadata, final JavaType readOnlyRepository,
      final List<JavaType> customRepositories, boolean composition,
      final Pair<FieldMetadata, RelationInfo> compositionInfo) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");
    Validate.notNull(identifierType, "Id type required");

    countMethodByReferencedFields = new HashMap<FieldMetadata, MethodMetadata>();

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
    ensureGovernorExtends(new JavaType(interfaceType.getFullyQualifiedTypeName(), 0, DataType.TYPE,
        null, Arrays.asList(annotationValues.getEntity(), identifierType)));

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

    if (composition) {
      MethodMetadata countMethod =
          getCountMethodByField(compositionInfo.getLeft(), compositionInfo.getRight());
      ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
      this.compositionField = compositionInfo.getLeft();
      this.compositionInfo = compositionInfo.getRight();
      this.compositionCountMethod = countMethod;
      countMethodByReferencedFields.put(compositionInfo.getLeft(), countMethod);
    }

    // Build the ITD
    itdTypeDetails = builder.build();
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
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(field.getFieldType()));
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(field.getFieldType()
        .getSimpleTypeName())));

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

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns all generated countMethodByReferencedFields
   *
   * @return Map with key that identifies referenced field and method metadata
   * @deprecated use {@link #getCompositionCountMethod()} and {@link #getCompositionField()}
   */
  public Map<FieldMetadata, MethodMetadata> getCountMethodByReferencedFields() {
    return countMethodByReferencedFields;
  }

  public MethodMetadata getCompositionCountMethod() {
    return compositionCountMethod;
  }

  public FieldMetadata getCompositionField() {
    return compositionField;
  }

  public RelationInfo getCompositionInfo() {
    return compositionInfo;
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
