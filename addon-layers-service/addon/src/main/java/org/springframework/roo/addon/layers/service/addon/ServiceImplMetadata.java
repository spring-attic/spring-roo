package org.springframework.roo.addon.layers.service.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
 * Metadata for {@link RooServiceImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceImplMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ServiceImplMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private ImportRegistrationResolver importResolver;

  private JavaType repository;
  private JavaType entity;
  private List<MethodMetadata> allImplementedMethods;
  private Map<FieldMetadata, MethodMetadata> allCountByReferencedFieldMethods;
  private Map<FieldMetadata, MethodMetadata> allFindAllByReferencedFieldMethods;
  private MethodMetadata findAllIterableMethod;

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
   * @param serviceInterface JavaType with interface that this service will implement
   * @param methodsToBeImplemented list of MethodMetadata that represents all necessary methods
   *            that should be implemented on current service implementation
   */
  public ServiceImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType serviceInterface,
      final JavaType repository, final JavaType entity, final MethodMetadata findAllIterableMethod,
      final List<MethodMetadata> methodsToBeImplemented,
      final Map<FieldMetadata, MethodMetadata> countReferencedFieldsMethods,
      final Map<FieldMetadata, MethodMetadata> findAllReferencedFieldsMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.entity = entity;
    this.repository = repository;
    this.findAllIterableMethod = findAllIterableMethod;
    this.allImplementedMethods = methodsToBeImplemented;
    this.allCountByReferencedFieldMethods = countReferencedFieldsMethods;
    this.allFindAllByReferencedFieldMethods = findAllReferencedFieldsMethods;

    // Get service that needs to be implemented
    ensureGovernorImplements(serviceInterface);

    // All services should include @Service annotation
    AnnotationMetadataBuilder serviceAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.SERVICE);
    ensureGovernorIsAnnotated(serviceAnnotation);

    // All service related with repository should be generated with
    // @Transactional(readOnly = true) annotation
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", true);
    ensureGovernorIsAnnotated(transactionalAnnotation);

    // Services should include repository field if there's
    // a repository related with managed entity
    FieldMetadataBuilder repositoryFieldMetadata =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
            new ArrayList<AnnotationMetadataBuilder>(), getRepositoryField().getFieldName(),
            getRepositoryField().getFieldType());
    ensureGovernorHasField(repositoryFieldMetadata);

    // Add constructor
    ensureGovernorHasConstructor(getConstructor());

    // Generating all methods that should be implemented
    for (MethodMetadata method : methodsToBeImplemented) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getMethod(method)));
    }

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> countReferencedFieldsMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    countReferencedFieldsMethodsOrderedByFieldName.putAll(countReferencedFieldsMethods);

    // Generating all count methods that should be implemented
    for (Entry<FieldMetadata, MethodMetadata> method : countReferencedFieldsMethodsOrderedByFieldName
        .entrySet()) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getMethod(method.getValue())));
    }


    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> findAllReferencedFieldsMethodsOrderedByName =
        new TreeMap<FieldMetadata, MethodMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    findAllReferencedFieldsMethodsOrderedByName.putAll(findAllReferencedFieldsMethods);

    // Generating all findAll methods for referenced fields that should be implemented
    for (Entry<FieldMetadata, MethodMetadata> method : findAllReferencedFieldsMethodsOrderedByName
        .entrySet()) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getMethod(method.getValue())));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates Service implementation constructor. If exists a
   * repository, it will be included as constructor parameter
   * 
   * @return
   */
  public ConstructorMetadataBuilder getConstructor() {

    ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Append repository parameter if needed
    if (repository != null) {
      constructorBuilder.addParameter(getRepositoryField().getFieldName().getSymbolName(),
          this.repository);
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", getRepositoryField()
          .getFieldName().getSymbolName(), getRepositoryField().getFieldName().getSymbolName()));
    }

    constructorBuilder.setBodyBuilder(bodyBuilder);
    constructorBuilder.setModifier(Modifier.PUBLIC);

    // Adding @Autowired annotation
    constructorBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return constructorBuilder;
  }

  /**
   * Method that generates implementation of provided method
   * 
   * @return MethodMetadataBuilder 
   */
  private MethodMetadata getMethod(final MethodMetadata methodToBeImplemented) {
    // Define methodName
    JavaSymbolName methodName = methodToBeImplemented.getMethodName();

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = methodToBeImplemented.getParameterTypes();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = methodToBeImplemented.getParameterNames();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Check if is batch method
    boolean isBatch = false;
    for (AnnotatedJavaType parameters : parameterTypes) {
      JavaType type = parameters.getJavaType();
      isBatch = !type.getParameters().isEmpty();
      if (isBatch) {
        break;
      }
    }

    if (isBatch && methodName.getSymbolName().equals("delete")) {

      // List<Entity> toDelete = repositoryField.FIND_ALL_METHOD(paramName);
      bodyBuilder.appendFormalLine(String.format("%s<%s> toDelete = %s.%s(ids);", new JavaType(
          "java.util.List").getNameIncludingTypeParameters(false, importResolver), this.entity
          .getNameIncludingTypeParameters(false, importResolver), getRepositoryField()
          .getFieldName(), this.findAllIterableMethod.getMethodName()));

      bodyBuilder.appendFormalLine(String.format("%s.deleteInBatch(toDelete);",
          getRepositoryField().getFieldName(), methodToBeImplemented.getMethodName()
              .getSymbolName()));

    } else {
      // Getting parameters String
      String parametersString = "";
      for (JavaSymbolName parameterName : parameterNames) {
        parametersString = parametersString.concat(parameterName.getSymbolName()).concat(", ");
      }
      if (StringUtils.isNotBlank(parametersString)) {
        parametersString = parametersString.substring(0, parametersString.length() - 2);
      }

      bodyBuilder
          .appendFormalLine(String.format("%s %s.%s(%s);", methodToBeImplemented.getReturnType()
              .equals(JavaType.VOID_PRIMITIVE) ? "" : "return",
              getRepositoryField().getFieldName(), methodToBeImplemented.getMethodName()
                  .getSymbolName(), parametersString));
    }
    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            methodToBeImplemented.getReturnType(), parameterTypes, parameterNames, bodyBuilder);

    // Adding annotations
    for (AnnotationMetadata annotation : methodToBeImplemented.getAnnotations()) {
      methodBuilder.addAnnotation(annotation);
    }

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns repository field included on controller
   * 
   * @param service
   * @return
   */
  public FieldMetadata getRepositoryField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.repository.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.repository)
        .build();
  }

  public List<MethodMetadata> getAllImplementedMethods() {
    return allImplementedMethods;
  }

  public Map<FieldMetadata, MethodMetadata> getAllCountByReferencedFieldMethods() {
    return allCountByReferencedFieldMethods;
  }

  public Map<FieldMetadata, MethodMetadata> getAllFindAllByReferencedFieldMethods() {
    return allFindAllByReferencedFieldMethods;
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
