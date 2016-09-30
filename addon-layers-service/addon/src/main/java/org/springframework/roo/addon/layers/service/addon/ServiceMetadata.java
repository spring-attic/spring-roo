package org.springframework.roo.addon.layers.service.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.service.annotations.RooService;
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooService}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ServiceMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private JavaType entity;
  private JavaType identifierType;
  private List<MethodMetadata> finders;
  private MethodMetadata findAllGlobalSearchMethod;
  private List<MethodMetadata> allDefinedMethod;
  private Map<FieldMetadata, MethodMetadata> countByReferenceFieldDefinedMethod;
  private Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllDefinedMethods;
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
   * @param entity entity referenced on interface
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param readOnly specifies if current entity is defined as readOnly or not
   * @param finders list of finders added to current entity
   * @param findAllGlobalSearchMethod MethodMetadata with findAllGlobalSearch method
   * @param referencedFieldsFindAllMethods
   * @param countByReferencedFieldsMethods
   * @param customCountMethods 
   * 
   */
  public ServiceMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JavaType identifierType, final boolean readOnly, final List<MethodMetadata> finders,
      final MethodMetadata findAllGlobalSearchMethod,
      final Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethods,
      final Map<FieldMetadata, MethodMetadata> countByReferencedFieldsMethods,
      final List<MethodMetadata> customCountMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    Validate.notNull(entity, "ERROR: Entity required to generate service interface");
    Validate.notNull(identifierType,
        "ERROR: Entity identifier type required to generate service interface");

    this.entity = entity;
    this.identifierType = identifierType;
    this.finders = finders;
    this.findAllGlobalSearchMethod = findAllGlobalSearchMethod;
    this.referencedFieldsFindAllDefinedMethods = new HashMap<FieldMetadata, MethodMetadata>();
    this.allDefinedMethod = new ArrayList<MethodMetadata>();
    this.countByReferenceFieldDefinedMethod = new HashMap<FieldMetadata, MethodMetadata>();
    this.customCountMethods = customCountMethods;

    // Generating persistent methods for not readOnly entities
    if (!readOnly) {
      MethodMetadata saveMethod = getSaveMethod();
      this.allDefinedMethod.add(saveMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(saveMethod));

      MethodMetadata deleteMethod = getDeleteMethod();
      this.allDefinedMethod.add(deleteMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteMethod));

      MethodMetadata saveBatchMethod = getSaveBatchMethod();
      this.allDefinedMethod.add(saveBatchMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(saveBatchMethod));

      MethodMetadata deleteBatchMethod = getDeleteBatchMethod();
      this.allDefinedMethod.add(deleteBatchMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteBatchMethod));
    }

    // Generating readOnly methods for every services
    MethodMetadata findAllMethod = getFindAllMethod();
    this.allDefinedMethod.add(findAllMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(findAllMethod));

    MethodMetadata findAllIterableMethod = getFindAllIterableMethod();
    this.allDefinedMethod.add(findAllIterableMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(findAllIterableMethod));

    MethodMetadata findOneMethod = getFindOneMethod();
    this.allDefinedMethod.add(findOneMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(findOneMethod));

    MethodMetadata countMethod = getCountMethod();
    this.allDefinedMethod.add(countMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));

    // Generating finders
    for (MethodMetadata finder : finders) {
      MethodMetadata finderMethod = getFinderMethod(finder);
      this.allDefinedMethod.add(finderMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(finderMethod));
    }

    // Generating count finder methods
    for (MethodMetadata customCountMethod : customCountMethods) {
      MethodMetadata customCountServiceMethod = getCustomCountMethod(customCountMethod);
      this.allDefinedMethod.add(customCountServiceMethod);
      ensureGovernorHasMethod(new MethodMetadataBuilder(customCountServiceMethod));
    }

    // Generating findAll method that includes GlobalSearch parameter
    MethodMetadata findAllWithGlobalSearchMethod = getFindAllGlobalSearchMethod();
    this.allDefinedMethod.add(findAllWithGlobalSearchMethod);
    ensureGovernorHasMethod(new MethodMetadataBuilder(findAllWithGlobalSearchMethod));

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    referencedFieldsFindAllMethodsOrderedByFieldName.putAll(referencedFieldsFindAllMethods);

    // Generating all findAll method for every referenced fields
    for (Entry<FieldMetadata, MethodMetadata> findAllReferencedFieldMethod : referencedFieldsFindAllMethodsOrderedByFieldName
        .entrySet()) {
      MethodMetadata method =
          getFindAllReferencedFieldMethod(findAllReferencedFieldMethod.getValue());
      this.referencedFieldsFindAllDefinedMethods.put(findAllReferencedFieldMethod.getKey(), method);
      ensureGovernorHasMethod(new MethodMetadataBuilder(method));
    }

    // ROO-3765: Prevent ITD regeneration applying the same sort to provided map. If this sort is not applied, maybe some
    // method is not in the same order and ITD will be regenerated.
    Map<FieldMetadata, MethodMetadata> countByReferencedFieldsMethodsOrderedByFieldName =
        new TreeMap<FieldMetadata, MethodMetadata>(new Comparator<FieldMetadata>() {
          @Override
          public int compare(FieldMetadata field1, FieldMetadata field2) {
            return field1.getFieldName().compareTo(field2.getFieldName());
          }
        });
    countByReferencedFieldsMethodsOrderedByFieldName.putAll(countByReferencedFieldsMethods);

    // Generating all countByReferencedField methods
    if (countByReferencedFieldsMethods != null) {
      for (Entry<FieldMetadata, MethodMetadata> countByReferencedFieldMethod : countByReferencedFieldsMethodsOrderedByFieldName
          .entrySet()) {
        MethodMetadata method =
            getCountByReferencedFieldMethod(countByReferencedFieldMethod.getValue());
        this.countByReferenceFieldDefinedMethod.put(countByReferencedFieldMethod.getKey(), method);
        ensureGovernorHasMethod(new MethodMetadataBuilder(method));
      }
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates method "findAll" method. This method includes
   * GlobalSearch parameters to be able to filter results.
   * 
   * @return MethodMetadata
   */
  public MethodMetadata getFindAllGlobalSearchMethod() {
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

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            this.findAllGlobalSearchMethod.getReturnType(), parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" method.
   * 
   * @return MethodMetadataBuilder with public List <Entity> findAll();
   *         structure
   */
  public MethodMetadata getFindAllMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findAll");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" with iterable parameter.
   * 
   * @return MethodMetadataBuilder with public List <Entity> findAll(Iterable
   *         <Long> ids) structure
   */
  public MethodMetadata getFindAllIterableMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findAll");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(identifierType))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }


    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }


  /**
   * Method that generates method "findOne".
   * 
   * @return MethodMetadataBuilder with public Entity findOne(Long id);
   *         structure
   */
  public MethodMetadata getFindOneMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("findOne");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(identifierType));

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
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName, entity,
            parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "count".
   * 
   * @return MethodMetadataBuilder with public long count();
   *         structure
   */
  public MethodMetadata getCountMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("count");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

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
   * Method that generates "save" method.
   * 
   * @return MethodMetadataBuilder with public Entity save(Entity entity);
   *         structure
   */
  public MethodMetadata getSaveMethod() {
    // Define save method
    JavaSymbolName methodName = new JavaSymbolName("save");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.entity));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entity"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            this.entity, parameterTypes, parameterNames, null);

    // save method should be defined with @Transactional(readOnly = false)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", false);
    methodBuilder.addAnnotation(transactionalAnnotation);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "delete" method.
   * 
   * @return MethodMetadataBuilder with public void delete(Entity entity); structure
   */
  public MethodMetadata getDeleteMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.entity));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames
        .add(new JavaSymbolName(StringUtils.uncapitalize(this.entity.getSimpleTypeName())));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    // save method should be defined with @Transactional(readOnly = false)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", false);
    methodBuilder.addAnnotation(transactionalAnnotation);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "save" batch method.
   * 
   * @return MethodMetadataBuilder with public List<Entity> save(Iterable
   *         <Entity> entities); structure
   */
  public MethodMetadata getSaveBatchMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("save");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(entity))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entities"));

    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            listEntityJavaType, parameterTypes, parameterNames, null);

    // save method should be defined with @Transactional(readOnly = false)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", false);
    methodBuilder.addAnnotation(transactionalAnnotation);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "delete" batch method
   * 
   * @return MethodMetadataBuilder with public void delete(Iterable
   *         <Long> ids); structure
   */
  public MethodMetadata getDeleteBatchMethod() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(identifierType))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    // save method should be defined with @Transactional(readOnly = false)
    AnnotationMetadataBuilder transactionalAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.TRANSACTIONAL);
    transactionalAnnotation.addBooleanAttribute("readOnly", false);
    methodBuilder.addAnnotation(transactionalAnnotation);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates countByReferencedField method on current interface
   * 
   * @param countMethod
   * @return
   */
  private MethodMetadata getCountByReferencedFieldMethod(MethodMetadata countMethod) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    List<AnnotatedJavaType> methodParamTypes = countMethod.getParameterTypes();
    List<JavaSymbolName> methodParamNames = countMethod.getParameterNames();
    for (int i = 0; i < countMethod.getParameterTypes().size(); i++) {
      parameterTypes.add(methodParamTypes.get(i));
      parameterNames.add(methodParamNames.get(i));
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            countMethod.getMethodName(), countMethod.getReturnType(), parameterTypes,
            parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates findAll method for provided referenced fields on current interface
   * 
   * @param countMethod
   * @return
   */
  private MethodMetadata getFindAllReferencedFieldMethod(MethodMetadata method) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    List<AnnotatedJavaType> methodParamTypes = method.getParameterTypes();
    List<JavaSymbolName> methodParamNames = method.getParameterNames();
    for (int i = 0; i < method.getParameterTypes().size(); i++) {
      parameterTypes.add(methodParamTypes.get(i));
      parameterNames.add(methodParamNames.get(i));
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            method.getMethodName(), method.getReturnType(), parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates finder method on current interface
   * 
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderMethod(MethodMetadata finderMethod) {

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            finderMethod.getMethodName(), finderMethod.getReturnType(),
            finderMethod.getParameterTypes(), finderMethod.getParameterNames(), null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates custom count method.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCustomCountMethod(MethodMetadata customCountMethod) {

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            customCountMethod.getMethodName(), customCountMethod.getReturnType(),
            customCountMethod.getParameterTypes(), customCountMethod.getParameterNames(), null);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns all defined methods in service interface
   * 
   * @return
   */
  public List<MethodMetadata> getAllDefinedMethods() {
    return this.allDefinedMethod;
  }

  /**
   * This method returns all defined count methods in 
   * service interface
   * 
   * @return
   */
  public Map<FieldMetadata, MethodMetadata> getCountByReferenceFieldDefinedMethod() {
    return this.countByReferenceFieldDefinedMethod;
  }

  /**
   * This method returns all defined findAll methods for referenced fields in 
   * service interface
   * 
   * @return
   */
  public Map<FieldMetadata, MethodMetadata> getReferencedFieldsFindAllDefinedMethods() {
    return this.referencedFieldsFindAllDefinedMethods;
  }

  /**
   * Method that returns the finder methos.
   * 
   * @return a list of finder methods
   */
  public List<MethodMetadata> getFinders() {
    return finders;
  }

  /**
   * Method that returns the count methods.
   * 
   * @return a list of count methods
   */
  public List<MethodMetadata> getCountMethods() {
    return this.customCountMethods;
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
