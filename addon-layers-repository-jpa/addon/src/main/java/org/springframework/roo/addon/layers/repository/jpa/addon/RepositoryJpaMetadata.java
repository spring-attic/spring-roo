package org.springframework.roo.addon.layers.repository.jpa.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

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
  private static final String SPRING_JPA_REPOSITORY =
      "org.springframework.data.jpa.repository.JpaRepository";

  private Map<FieldMetadata, MethodMetadata> countMethodByReferencedFields;

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
   * @param readOnly boolean
   * @param readOnlyRepository JavaType
   * @param customRepositories List<JavaType>
   * @param referenceFields Map<JavaType, JavaType> that contains referenceField type
   * and its identifier type
   */
  public RepositoryJpaMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaAnnotationValues annotationValues, final JavaType identifierType,
      final boolean readOnly, final JavaType readOnlyRepository,
      final List<JavaType> customRepositories,
      final Map<FieldMetadata, FieldMetadata> referenceFields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.notNull(annotationValues, "Annotation values required");
    Validate.notNull(identifierType, "Id type required");

    countMethodByReferencedFields = new HashMap<FieldMetadata, MethodMetadata>();

    if (readOnly) {
      // If readOnly, extends ReadOnlyRepository
      ensureGovernorExtends(new JavaType(readOnlyRepository.getFullyQualifiedTypeName(), 0,
          DataType.TYPE, null, Arrays.asList(annotationValues.getEntity(), identifierType)));
    } else {
      // Extends JpaRepository
      ensureGovernorExtends(new JavaType(SPRING_JPA_REPOSITORY, 0, DataType.TYPE, null,
          Arrays.asList(annotationValues.getEntity(), identifierType)));
    }

    // If has some RepositoryCustom associated, add extends
    if (!customRepositories.isEmpty()) {
      // Extends RepositoryCustom
      for (JavaType repositoryCustom : customRepositories) {
        ensureGovernorExtends(repositoryCustom);
      }
    }

    // Always add @Repository annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.REPOSITORY));

    // All repositories are generated with @Transactional(readOnly = true)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, FieldMetadata> referencedFieldsOrderedByFieldName =
        new TreeMap<FieldMetadata, FieldMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    referencedFieldsOrderedByFieldName.putAll(referenceFields);

    // Adding count methods for every referenced field
    for (Entry<FieldMetadata, FieldMetadata> field : referencedFieldsOrderedByFieldName.entrySet()) {
      MethodMetadata countMethod = getCountMethodByField(field.getKey(), field.getValue());
      ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
      countMethodByReferencedFields.put(field.getKey(), countMethod);
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates method "countByField" method. 
   * 
   * @param field
   * @param identifierType
   * 
   * @return field
   */
  public MethodMetadata getCountMethodByField(FieldMetadata field, FieldMetadata identifierType) {
    // Define method name
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("countBy%s%s", field.getFieldName()
            .getSymbolNameCapitalisedFirstLetter(), identifierType.getFieldName()
            .getSymbolNameCapitalisedFirstLetter()));

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(identifierType.getFieldType()));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            identifierType.getFieldType(), parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
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
