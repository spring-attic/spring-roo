package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link RepositoryJpaCustomImplMetadataProvider}.
 *
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class RepositoryJpaCustomImplMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements RepositoryJpaCustomImplMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaCustomImplMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToRepositoryMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA_CUSTOM_IMPL} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    BundleContext localContext = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(localContext, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(localContext, getClass(), new LayerTypeMatcher(
            ROO_REPOSITORY_JPA_CUSTOM_IMPL, new JavaSymbolName(
                RooJpaRepositoryCustomImpl.REPOSITORY_ATTRIBUTE)));
    this.keyDecoratorTracker.open();
  }

  /**
   * This service is being deactivated so unregister upstream-downstream
   * dependencies, triggers, matchers and listeners.
   *
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaCustomImplMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        RepositoryJpaCustomImplMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RepositoryJpaCustomImplMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository_Impl";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToRepositoryMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToRepositoryMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final RepositoryJpaCustomImplAnnotationValues annotationValues =
        new RepositoryJpaCustomImplAnnotationValues(governorPhysicalTypeMetadata);

    // Getting repository custom
    JavaType repositoryCustom = annotationValues.getRepository();

    // Validate that contains repository interface
    Validate.notNull(repositoryCustom,
        "ERROR: You need to specify interface repository to be implemented.");

    ClassOrInterfaceTypeDetails repositoryDetails =
        getTypeLocationService().getTypeDetails(repositoryCustom);

    AnnotationMetadata repositoryCustomAnnotation =
        repositoryDetails.getAnnotation(ROO_REPOSITORY_JPA_CUSTOM);

    Validate.notNull(repositoryCustomAnnotation,
        "ERROR: Repository interface should be annotated with @RooJpaRepositoryCustom");

    AnnotationAttributeValue<JavaType> entityAttribute =
        repositoryCustomAnnotation.getAttribute("entity");

    Validate
        .notNull(entityAttribute,
            "ERROR: Repository interface should be contain an entity on @RooJpaRepositoryCustom annotation");

    JavaType entity = entityAttribute.getValue();

    RepositoryJpaMetadata repositoryMetadata =
        getRepositoryJpaLocator().getRepositoryMetadata(entity);

    if (repositoryMetadata == null) {
      // Can't generate it jet
      return null;
    }

    // Register downstream dependency for RepositoryJpaCustomImplMetadata to update projection
    // finders implementations
    String repositoryCustomMetadataKey =
        RepositoryJpaCustomMetadata.createIdentifier(repositoryDetails);
    registerDependency(repositoryCustomMetadataKey, metadataIdentificationString);

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);

    // Check if default return type is a Projection
    JavaType returnType = repositoryMetadata.getDefaultReturnType();
    ClassOrInterfaceTypeDetails returnTypeDetails =
        getTypeLocationService().getTypeDetails(returnType);
    AnnotationMetadata entityProjectionAnnotation =
        returnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
    boolean returnTypeIsProjection = entityProjectionAnnotation != null;

    // Get projection constructor fields from @RooEntityProjection and add it to a Map with
    // domain type's variable names
    Map<JavaType, List<Pair<String, String>>> typesFieldMaps =
        new LinkedHashMap<JavaType, List<Pair<String, String>>>();
    Map<JavaType, Boolean> typesAreProjections = new HashMap<JavaType, Boolean>();
    if (returnTypeIsProjection) {
      buildFieldNamesMap(entity, returnType, entityProjectionAnnotation, typesFieldMaps);
      typesAreProjections.put(returnType, true);
    }

    final RepositoryJpaCustomMetadata repositoryCustomMetadata =
        getMetadataService().get(repositoryCustomMetadataKey);
    // Prevent empty metadata
    if (repositoryCustomMetadata == null) {
      return null;
    }

    // Getting java bean metadata
    final String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);

    // Getting jpa entity metadata
    final String jpaEntityMetadataKey = JpaEntityMetadata.createIdentifier(entityDetails);

    JpaEntityMetadata entityMetadata = getMetadataService().get(jpaEntityMetadataKey);

    // Create dependency between repository and java bean annotation
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Create dependency between repository and jpa entity annotation
    registerDependency(jpaEntityMetadataKey, metadataIdentificationString);


    // Getting entity properties
    MemberDetails entityMemberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), entityDetails);

    // Getting valid fields to construct the findAll query
    List<FieldMetadata> validFields = new ArrayList<FieldMetadata>();
    loadValidFields(entityMemberDetails, entityMetadata, validFields);

    // Getting all necessary information about referencedFields
    Map<FieldMetadata, MethodMetadata> referencedFieldsMethods =
        repositoryCustomMetadata.getReferencedFieldsFindAllMethods();

    Map<FieldMetadata, String> referencedFieldsIdentifierNames =
        new HashMap<FieldMetadata, String>();

    List<Pair<MethodMetadata, PartTree>> customFinderMethods =
        repositoryCustomMetadata.getCustomFinderMethods();

    List<Pair<MethodMetadata, PartTree>> customCountMethods =
        repositoryCustomMetadata.getCustomCountMethods();
    if (customCountMethods == null) {
      customCountMethods = new ArrayList<Pair<MethodMetadata, PartTree>>();
    }

    for (Entry<FieldMetadata, MethodMetadata> referencedFields : referencedFieldsMethods.entrySet()) {

      // Get identifier field name in path format
      String fieldPathName =
          String.format("%s.%s", StringUtils.uncapitalize(entity.getSimpleTypeName()),
              referencedFields.getKey().getFieldName().getSymbolNameUnCapitalisedFirstLetter());

      // Put keys and values in map
      referencedFieldsIdentifierNames.put(referencedFields.getKey(), fieldPathName);
    }

    // Add valid entity fields to mappings
    Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadataMap =
        new HashMap<JavaType, Map<String, FieldMetadata>>();
    Map<String, FieldMetadata> entityFieldMetadata = new LinkedHashMap<String, FieldMetadata>();
    List<Pair<String, String>> entityFieldMappings = new ArrayList<Pair<String, String>>();
    typesAreProjections.put(entity, false);
    for (FieldMetadata field : validFields) {
      entityFieldMetadata.put(field.getFieldName().getSymbolName(), field);
      entityFieldMappings.add(Pair.of(
          field.getFieldName().getSymbolName(),
          StringUtils.uncapitalize(entity.getSimpleTypeName()).concat(".")
              .concat(field.getFieldName().getSymbolName())));
    }
    typesFieldsMetadataMap.put(entity, entityFieldMetadata);
    typesFieldMaps.put(entity, entityFieldMappings);

    // Make a list with all domain types, excepting entities
    List<JavaType> domainTypes = new ArrayList<JavaType>();
    domainTypes.add(returnType);
    for (Pair<MethodMetadata, PartTree> methodInfo : customFinderMethods) {

      // Get finder return type from first parameter of method return type (Page)
      JavaType finderReturnType = getDomainTypeOfFinderMethod(methodInfo.getKey());
      domainTypes.add(finderReturnType);

      // If type is a DTO, add finder fields to mappings
      JavaType parameterType = methodInfo.getKey().getParameterTypes().get(0).getJavaType();
      typesAreProjections.put(parameterType, false);
    }

    // Add typesFieldMaps for each projection finder and check for id fields
    for (JavaType type : domainTypes) {

      // Check if projection fields has been added already
      if (typesFieldMaps.containsKey(type)) {
        continue;
      }

      // Build Map with FieldMetadata of each projection
      ClassOrInterfaceTypeDetails typeDetails = getTypeLocationService().getTypeDetails(type);
      if (typeDetails == null) {
        LOGGER.warning("Detail not found for type: " + type);
        continue;
      }
      List<FieldMetadata> typeFieldList =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), typeDetails)
              .getFields();
      Map<String, FieldMetadata> fieldMetadataMap = new LinkedHashMap<String, FieldMetadata>();
      for (FieldMetadata field : typeFieldList) {
        fieldMetadataMap.put(field.getFieldName().getSymbolName(), field);
      }
      typesFieldsMetadataMap.put(type, fieldMetadataMap);

      AnnotationMetadata projectionAnnotation =
          typeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
      if (projectionAnnotation != null) {
        typesAreProjections.put(type, true);

        // Type is a Projection
        JavaType associatedEntity =
            (JavaType) projectionAnnotation.getAttribute("entity").getValue();

        // Add fields to typesFieldMaps
        buildFieldNamesMap(associatedEntity, type, projectionAnnotation, typesFieldMaps);

      }
    }

    return new RepositoryJpaCustomImplMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, entity, entityMetadata,
        entityMetadata.getCurrentIndentifierField(), validFields,
        repositoryCustomMetadata.getCurrentFindAllGlobalSearchMethod(),
        repositoryCustomMetadata.getDefaultReturnType(), referencedFieldsMethods,
        referencedFieldsIdentifierNames, typesFieldMaps, customFinderMethods, customCountMethods,
        typesFieldsMetadataMap, typesAreProjections);
  }

  private JavaType getDomainTypeOfFinderMethod(MethodMetadata method) {
    JavaType returnType = method.getReturnType();
    if (returnType.getFullyQualifiedTypeName().equals(
        SpringJavaType.PAGE.getFullyQualifiedTypeName())) {
      if (returnType.getParameters() != null && returnType.getParameters().size() == 1) {
        return returnType.getParameters().get(0);
      }
    } else if (returnType.getEnclosingType() != null) {
      return returnType.getBaseType();
    }
    return null;
  }

  private void loadValidFields(MemberDetails entityMemberDetails, JpaEntityMetadata entityMetadata,
      List<FieldMetadata> validFields) {


    for (FieldMetadata field : entityMemberDetails.getFields()) {


      // Exclude non-simple fields
      if (field.getFieldType().isMultiValued()) {
        continue;
      }

      // Exclude version field
      if (field.getAnnotation(JpaJavaType.VERSION) != null) {
        continue;
      }

      // Exclude id fields
      if (field.getAnnotation(JpaJavaType.ID) != null
          || field.getAnnotation(JpaJavaType.EMBEDDED_ID) != null) {
        continue;
      }

      // Exclude static fields
      int staticFinal = Modifier.STATIC + Modifier.FINAL;
      int publicStatic = Modifier.PUBLIC + Modifier.STATIC;
      int publicStaticFinal = Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL;
      int privateStatic = Modifier.PRIVATE + Modifier.STATIC;
      int privateStaticFinal = Modifier.PRIVATE + Modifier.STATIC + Modifier.FINAL;

      if (field.getModifier() == Modifier.STATIC || field.getModifier() == staticFinal
          || field.getModifier() == publicStatic || field.getModifier() == publicStaticFinal
          || field.getModifier() == privateStatic || field.getModifier() == privateStaticFinal) {
        continue;
      }

      // Exclude transient fields and annotated fields with @Transient
      int transientFinal = Modifier.TRANSIENT + Modifier.FINAL;
      int publicTransient = Modifier.PUBLIC + Modifier.TRANSIENT;
      int publicTransientFinal = Modifier.PUBLIC + Modifier.TRANSIENT + Modifier.FINAL;
      int privateTransient = Modifier.PRIVATE + Modifier.TRANSIENT;
      int privateTransientFinal = Modifier.PRIVATE + Modifier.TRANSIENT + Modifier.FINAL;
      if (field.getAnnotation(JpaJavaType.TRANSIENT) != null
          || field.getModifier() == Modifier.TRANSIENT || field.getModifier() == transientFinal
          || field.getModifier() == publicTransient || field.getModifier() == publicTransientFinal
          || field.getModifier() == privateTransient
          || field.getModifier() == privateTransientFinal) {
        continue;
      }

      // TODO Exclude audit fields
      /*isAudit = false;
      for (FieldMetadata auditField : auditFields) {
        if (auditField.getFieldName().equals(field.getFieldName())) {
          isAudit = true;
          break;
        }
      }

      if (isAudit) {
        continue;
      }*/

      validFields.add(field);
    }
  }

  /**
   * Build a Map<String, String> with field names and "path" field names
   * and adds it to the typesFieldMaps Map.
   *
   * @param entity
   * @param projection
   * @param entityProjectionAnnotation
   * @param typesFieldMaps
   */
  private void buildFieldNamesMap(JavaType entity, JavaType projection,
      AnnotationMetadata entityProjectionAnnotation,
      Map<JavaType, List<Pair<String, String>>> typesFieldMaps) {
    List<Pair<String, String>> projectionFieldNames = new ArrayList<Pair<String, String>>();
    if (!typesFieldMaps.containsKey(projection)) {
      AnnotationAttributeValue<?> projectionFields =
          entityProjectionAnnotation.getAttribute("fields");
      if (projectionFields != null) {
        @SuppressWarnings("unchecked")
        List<StringAttributeValue> values =
            (List<StringAttributeValue>) projectionFields.getValue();

        // Get entity name as a variable name for building constructor expression
        String entityVariableName = StringUtils.uncapitalize(entity.getSimpleTypeName());
        for (StringAttributeValue field : values) {
          String[] splittedByDot = StringUtils.split(field.getValue(), ".");
          StringBuffer propertyName = new StringBuffer();
          for (int i = 0; i < splittedByDot.length; i++) {
            if (i == 0) {
              propertyName.append(splittedByDot[i]);
            } else {
              propertyName.append(StringUtils.capitalize(splittedByDot[i]));
            }
          }
          String pathName = entityVariableName.concat(".").concat(field.getValue());
          projectionFieldNames.add(Pair.of(propertyName.toString(), pathName));
          typesFieldMaps.put(projection, projectionFieldNames);
        }
      }
    }
  }

  private void registerDependency(final String upstreamDependency, final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  public String getProvidesType() {
    return RepositoryJpaCustomImplMetadata.getMetadataIdentiferType();
  }

  private RepositoryJpaLocator getRepositoryJpaLocator() {
    return getServiceManager().getServiceInstance(this, RepositoryJpaLocator.class);
  }

}
