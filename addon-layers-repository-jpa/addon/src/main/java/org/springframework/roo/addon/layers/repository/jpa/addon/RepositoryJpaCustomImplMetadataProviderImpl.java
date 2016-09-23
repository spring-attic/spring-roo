package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dto.addon.DtoOperations;
import org.springframework.roo.addon.dto.addon.DtoOperationsImpl;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.addon.security.addon.audit.AuditMetadata;
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
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Implementation of {@link RepositoryJpaCustomImplMetadataProvider}.
 * 
 * @author Juan Carlos García
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
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(
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

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);

    // Check if default return type is a Projection
    JavaType returnType =
        (JavaType) repositoryCustomAnnotation.getAttribute("defaultReturnType").getValue();
    ClassOrInterfaceTypeDetails returnTypeDetails =
        getTypeLocationService().getTypeDetails(returnType);
    AnnotationMetadata entityProjectionAnnotation =
        returnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
    boolean returnTypeIsProjection = entityProjectionAnnotation != null;

    // Get projection constructor fields from @RooEntityProjection and add it to a Map with 
    // domain type's variable names
    Map<JavaType, Map<String, String>> typesFieldMaps =
        new LinkedHashMap<JavaType, Map<String, String>>();
    Map<JavaType, Boolean> typesAreProjections = new HashMap<JavaType, Boolean>();
    if (returnTypeIsProjection) {
      buildProjectionFieldNamesMap(entity, returnType, entityProjectionAnnotation, typesFieldMaps);
      typesAreProjections.put(returnType, true);
    }

    // Getting repository metadata
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    final String repositoryMetadataKey =
        RepositoryJpaCustomMetadata.createIdentifier(repositoryDetails.getType(), logicalPath);
    final RepositoryJpaCustomMetadata repositoryCustomMetadata =
        (RepositoryJpaCustomMetadata) getMetadataService().get(repositoryMetadataKey);

    // Getting java bean metadata
    final LogicalPath entityLogicalPath =
        PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
    final String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(entityDetails.getType(), entityLogicalPath);

    // Getting jpa entity metadata
    final String jpaEntityMetadataKey =
        JpaEntityMetadata.createIdentifier(entityDetails.getType(), entityLogicalPath);

    // Get audit metadata
    final String auditMetadataKey =
        AuditMetadata.createIdentifier(entityDetails.getType(), entityLogicalPath);
    final AuditMetadata auditMetadata = (AuditMetadata) getMetadataService().get(auditMetadataKey);


    // Create dependency between repository and java bean annotation
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Create dependency between repository and jpa entity annotation
    registerDependency(jpaEntityMetadataKey, metadataIdentificationString);

    // Create dependency between repository and audit annotation
    registerDependency(auditMetadataKey, metadataIdentificationString);

    // Getting audit properties
    List<FieldMetadata> auditFields = new ArrayList<FieldMetadata>();
    if (auditMetadata != null) {
      auditFields = auditMetadata.getAuditFields();
    }

    // Getting persistent properties
    List<FieldMetadata> idFields = getPersistenceMemberLocator().getIdentifierFields(entity);
    if (idFields.isEmpty()) {
      throw new RuntimeException(String.format("Error: Entity %s does not have an identifier",
          entityAttribute.getName()));
    }

    // Getting entity properties for findAll method
    MemberDetails entityMemberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), entityDetails);

    // Removing duplicates in persistent properties
    boolean duplicated;
    List<FieldMetadata> validIdFields = new ArrayList<FieldMetadata>();
    for (FieldMetadata id : idFields) {
      duplicated = false;

      for (FieldMetadata validId : validIdFields) {
        if (id.getFieldName().equals(validId.getFieldName())) {
          duplicated = true;
          break;
        }
      }
      if (!duplicated) {
        validIdFields.add(id);
      }
    }

    // Getting valid fields to construct the findAll query
    final int maxFields = 5;
    boolean isAudit;
    List<FieldMetadata> validFields = new ArrayList<FieldMetadata>();

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

      // Exclude audit fields
      isAudit = false;
      for (FieldMetadata auditField : auditFields) {
        if (auditField.getFieldName().equals(field.getFieldName())) {
          isAudit = true;
          break;
        }
      }

      if (isAudit) {
        continue;
      }

      // Exclude non string or number fields
      if (field.getFieldType().equals(JavaType.STRING) || field.getFieldType().isNumber()) {
        validFields.add(field);
        if (validFields.size() == maxFields) {
          break;
        }
      }
    }

    // Getting all necessary information about referencedFields
    Map<FieldMetadata, MethodMetadata> referencedFieldsMethods =
        repositoryCustomMetadata.getReferencedFieldsFindAllMethods();

    Map<FieldMetadata, String> referencedFieldsIdentifierNames =
        new HashMap<FieldMetadata, String>();

    List<MethodMetadata> projectionFinderMethods =
        repositoryCustomMetadata.getProjectionFinderMethods();

    for (Entry<FieldMetadata, MethodMetadata> referencedFields : referencedFieldsMethods.entrySet()) {
      JavaType referencedField = referencedFields.getKey().getFieldType();

      // Get identifier field name in path format
      String fieldPathName =
          String.format("%s.%s", StringUtils.uncapitalize(entity.getSimpleTypeName()),
              StringUtils.uncapitalize(referencedField.getSimpleTypeName()));

      // Put keys and values in map
      referencedFieldsIdentifierNames.put(referencedFields.getKey(), fieldPathName);
    }

    // Add valid entity fields to mappings
    Map<JavaType, Map<String, FieldMetadata>> typesFieldsMetadata =
        new HashMap<JavaType, Map<String, FieldMetadata>>();
    Map<String, FieldMetadata> entityFieldMetadata = new LinkedHashMap<String, FieldMetadata>();
    Map<String, String> entityFieldMappings = new LinkedHashMap<String, String>();
    typesAreProjections.put(entity, false);
    for (FieldMetadata field : validFields) {
      entityFieldMetadata.put(field.getFieldName().getSymbolName(), field);
      if (field.getAnnotation(JpaJavaType.ID) != null
          || field.getAnnotation(JpaJavaType.EMBEDDED_ID) != null) {
        entityFieldMappings.put(field.getFieldName().getSymbolName(), "getEntityId()");
      } else {
        entityFieldMappings.put(
            field.getFieldName().getSymbolName(),
            StringUtils.uncapitalize(entity.getSimpleTypeName()).concat(".")
                .concat(field.getFieldName().getSymbolName()));
      }
    }
    typesFieldsMetadata.put(entity, entityFieldMetadata);
    typesFieldMaps.put(entity, entityFieldMappings);

    // Make a list with all domain types, excepting entities
    List<JavaType> domainTypes = new ArrayList<JavaType>();
    domainTypes.add(returnType);
    for (MethodMetadata method : projectionFinderMethods) {

      // Get projection from first parameter of method return type (Page)
      JavaType projection = method.getReturnType().getParameters().get(0);
      domainTypes.add(projection);

      // Add search parameter type if it is a DTO
      JavaType parameterType = method.getParameterTypes().get(0).getJavaType();
      if (getTypeLocationService().getTypeDetails(parameterType).getAnnotation(RooJavaType.ROO_DTO) != null) {
        domainTypes.add(parameterType);
      }
    }

    // Add typesFieldMaps for each projection finder and check for id fields
    for (JavaType type : domainTypes) {

      // Check if projection fields has been added already
      if (typesFieldMaps.containsKey(type)) {
        continue;
      }

      // Build Map with FieldMetadata of each projection
      ClassOrInterfaceTypeDetails typeDetails = getTypeLocationService().getTypeDetails(type);
      List<FieldMetadata> typeFieldList =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), typeDetails)
              .getFields();
      Map<String, FieldMetadata> fieldMetadataMap = new LinkedHashMap<String, FieldMetadata>();
      for (FieldMetadata field : typeFieldList) {
        fieldMetadataMap.put(field.getFieldName().getSymbolName(), field);
      }
      typesFieldsMetadata.put(type, fieldMetadataMap);

      AnnotationMetadata projectionAnnotation =
          typeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
      if (projectionAnnotation != null) {
        typesAreProjections.put(type, true);

        // Type is a Projection
        JavaType associatedEntity =
            (JavaType) projectionAnnotation.getAttribute("entity").getValue();

        // Get field values in "path" format from annotation
        AnnotationAttributeValue<?> projectionFields = projectionAnnotation.getAttribute("fields");
        @SuppressWarnings("unchecked")
        List<StringAttributeValue> values =
            (List<StringAttributeValue>) projectionFields.getValue();
        String projectionFieldsString = "";
        for (int i = 0; i < values.size(); i++) {
          if (i == 0) {
            projectionFieldsString = values.get(i).getValue();
          } else {
            projectionFieldsString =
                projectionFieldsString.concat(",").concat(values.get(i).getValue());
          }

        }

        // Add fields to typesFieldMaps
        buildProjectionFieldNamesMap(associatedEntity, type, projectionAnnotation, typesFieldMaps);

        // Get the original FieldMetadata with its Java name in Projection
        Map<String, FieldMetadata> projectionOriginalFieldMetadataValues =
            getDtoOperations().buildFieldsFromString(projectionFieldsString, associatedEntity);
        List<FieldMetadata> projectionIdentifierFields =
            getPersistenceMemberLocator().getIdentifierFields(associatedEntity);
        if (!getPersistenceMemberLocator().getEmbeddedIdentifierFields(associatedEntity).isEmpty()) {
          projectionIdentifierFields.addAll(getPersistenceMemberLocator()
              .getEmbeddedIdentifierFields(associatedEntity));
        }

        // Check if any projection field is an identifier field
        for (Entry<String, FieldMetadata> projectionOriginalValue : projectionOriginalFieldMetadataValues
            .entrySet()) {
          for (FieldMetadata field : projectionIdentifierFields) {
            if (field.getFieldName().equals(projectionOriginalValue.getValue().getFieldName())
                && field.getDeclaredByMetadataId().equals(
                    projectionOriginalValue.getValue().getDeclaredByMetadataId())) {

              // The projection contains identifier fields, so modify them and add them to 
              // specific Map
              String fieldPathName = "getEntityId()";
              typesFieldMaps.get(type).replace(projectionOriginalValue.getKey(), fieldPathName);
            }
          }
        }
      } else {

        // Type is a DTO
        typesAreProjections.put(type, false);
        buildDtoFieldNamesMap(entity, type, typesFieldMaps, typesFieldsMetadata, typeFieldList);
      }
    }

    return new RepositoryJpaCustomImplMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, entity, validIdFields, validFields,
        repositoryCustomMetadata.getFindAllGlobalSearchMethod(), referencedFieldsMethods,
        referencedFieldsIdentifierNames, typesFieldMaps, projectionFinderMethods,
        typesFieldsMetadata, typesAreProjections);
  }

  /**
   * Build a Map<String, String> with projection field names and "path" field names 
   * and adds it to the typesFieldMaps Map.
   * 
   * @param entity
   * @param projection
   * @param entityProjectionAnnotation
   * @param typesFieldMaps
   */
  private void buildProjectionFieldNamesMap(JavaType entity, JavaType projection,
      AnnotationMetadata entityProjectionAnnotation,
      Map<JavaType, Map<String, String>> typesFieldMaps) {
    Map<String, String> projectionFieldNames = new LinkedHashMap<String, String>();
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
          projectionFieldNames.put(propertyName.toString(), pathName);
          typesFieldMaps.put(projection, projectionFieldNames);
        }
      }
    }
  }

  /**
   * Build a Map<String, String> with DTO field names and "path" field names 
   * and adds it to the projectionsFieldMaps Map.
   * 
   * @param entity
   * @param dto
   * @param typesFieldMaps
   * @param typeFieldMetadataMap
   * @param allDtoFields
   */
  private void buildDtoFieldNamesMap(JavaType entity, JavaType dto,
      Map<JavaType, Map<String, String>> typesFieldMaps,
      Map<JavaType, Map<String, FieldMetadata>> typeFieldMetadataMap,
      List<FieldMetadata> allDtoFields) {

    // Get all entity fields
    ClassOrInterfaceTypeDetails entityCid = getTypeLocationService().getTypeDetails(entity);
    List<FieldMetadata> allEntityFields =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), entityCid)
            .getFields();

    // Create inner Maps
    Map<String, String> fieldNamesMap = new HashMap<String, String>();
    Map<String, FieldMetadata> fieldMetadataMap = new HashMap<String, FieldMetadata>();

    // Iterate over all specified fields
    for (int i = 0; i < allDtoFields.size(); i++) {
      String fieldName = "";
      boolean found = false;

      // Iterate over all entity fields
      for (FieldMetadata field : allEntityFields) {
        if (field.getFieldName().equals(allDtoFields.get(i).getFieldName())
            && field.getFieldType().equals(allDtoFields.get(i).getFieldType())) {

          // Field found, build field "path" name and add it to map
          String fieldPathName = "";

          // Check if field is @Id or @EmbeddedId field
          if (field.getAnnotation(JpaJavaType.ID) != null
              || field.getAnnotation(JpaJavaType.EMBEDDED_ID) != null) {
            fieldPathName = "getEntityId()";
          } else {

            // Path name for DTO's should be the path to entity's fields
            fieldPathName =
                String.format("%s.%s", StringUtils.uncapitalize(entity.getSimpleTypeName()),
                    field.getFieldName());
          }

          fieldNamesMap.put(allDtoFields.get(i).getFieldName().getSymbolName(), fieldPathName);
          fieldMetadataMap.put(allDtoFields.get(i).getFieldName().getSymbolName(),
              allDtoFields.get(i));
          found = true;
          break;
        }
      }
    }

    typesFieldMaps.put(dto, fieldNamesMap);
    typeFieldMetadataMap.put(dto, fieldMetadataMap);

    //      if (!found) {
    //
    //        // The field isn't in the entity, should be in one of its relations
    //        String[] splittedByDot = StringUtils.split(fields[i], ".");
    //        JavaType currentEntity = entity;
    //        if (fields[i].contains(".")) {
    //
    //          // Search a matching relation field
    //          for (int t = 0; t < splittedByDot.length; t++) {
    //            ClassOrInterfaceTypeDetails currentEntityCid =
    //                typeLocationService.getTypeDetails(currentEntity);
    //            List<FieldMetadata> currentEntityFields = memberDetailsScanner
    //                .getMemberDetails(this.getClass().getName(), currentEntityCid).getFields();
    //            boolean relationFieldFound = false;
    //
    //            // Iterate to build the field-levels of the relation field
    //            for (FieldMetadata field : currentEntityFields) {
    //              if (field.getFieldName().getSymbolName().equals(splittedByDot[t])
    //                  && t != splittedByDot.length - 1
    //                  && typeLocationService.getTypeDetails(field.getFieldType()) != null
    //                  && typeLocationService.getTypeDetails(field.getFieldType())
    //                      .getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
    //
    //                // Field is an entity and we should look into its fields
    //                currentEntity = field.getFieldType();
    //                found = true;
    //                relationFieldFound = true;
    //                if (t == 0) {
    //                  fieldName = fieldName.concat(field.getFieldName().getSymbolName());
    //                } else {
    //                  fieldName = fieldName
    //                      .concat(StringUtils.capitalize(field.getFieldName().getSymbolName()));
    //                }
    //                break;
    //              } else if (field.getFieldName().getSymbolName().equals(splittedByDot[t])) {
    //
    //                // Add field to projection fields
    //                fieldName =
    //                    fieldName.concat(StringUtils.capitalize(field.getFieldName().getSymbolName()));
    //                fieldsToAdd.put(fieldName, field);
    //                found = true;
    //                relationFieldFound = true;
    //                break;
    //              }
    //            }
    //
    //            // If not found, relation field is bad written
    //            if (!relationFieldFound) {
    //              throw new IllegalArgumentException(String.format(
    //                  "Field %s couldn't be located in %s. Please, be sure that it is well written.",
    //                  splittedByDot[t], currentEntity.getFullyQualifiedTypeName()));
    //            }
    //          }
    //        } else {
    //
    //          // Not written as a relation field
    //          throw new IllegalArgumentException(
    //              String.format("Field %s couldn't be located in entity %s", fields[i],
    //                  entity.getFullyQualifiedTypeName()));
    //        }
    //      }
    //
    //      // If still not found, field is bad written
    //      if (!found) {
    //        throw new IllegalArgumentException(String.format(
    //            "Field %s couldn't be located. Please, be sure that it is well written.", fields[i]));
    //      }
    //    }
    //
    //    return fieldsToAdd;
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

  public DtoOperationsImpl getDtoOperations() {

    // Get all Services implement DtoOperations interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(DtoOperations.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (DtoOperationsImpl) context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load DtoOperationsImpl on AbstractIdMetadataProvider.");
      return null;
    }
  }
}
