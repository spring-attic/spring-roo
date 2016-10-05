package org.springframework.roo.addon.finder.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.finder.addon.parser.PartTree;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link FinderOperations}.
 * 
 * @author Stefan Schmidt
 * @autor Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class FinderOperationsImpl implements FinderOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(FinderOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  private DynamicFinderServices dynamicFinderServices;
  private MemberDetailsScanner memberDetailsScanner;
  private MetadataService metadataService;
  private PersistenceMemberLocator persistenceMemberLocator;
  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;

  private String getErrorMsg() {
    /* return "Annotation " + ROO_JPA_ACTIVE_RECORD.getSimpleTypeName()
             + " attribute 'finders' must be an array of strings";*/
    return "";
  }

  public void installFinder(final JavaType entity, final JavaSymbolName finderName,
      JavaType formBean, JavaType defaultReturnType) {
    Validate.notNull(entity, "ERROR: Entity type required to generate finder.");
    Validate.notNull(finderName, "ERROR: Finder name required to generate finder.");

    final String id = getTypeLocationService().getPhysicalTypeIdentifier(entity);
    if (id == null) {
      LOGGER.warning("Cannot locate source for '" + entity.getFullyQualifiedTypeName() + "'");
      return;
    }

    // Check if provided entity is annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);

    Validate.notNull(entityAnnotation,
        "ERROR: Provided entity must be annotated with @RooJpaEntity");

    // Getting repository that manages current entity
    Set<ClassOrInterfaceTypeDetails> allRepositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);

    ClassOrInterfaceTypeDetails repository = null;
    for (ClassOrInterfaceTypeDetails repo : allRepositories) {
      AnnotationAttributeValue<JavaType> managedEntity =
          repo.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");
      // Check if current repository manages provided entity
      if (managedEntity.getValue().equals(entity)) {
        repository = repo;
      }

    }

    // Entity must have a repository that manages it, if not, shows an error
    Validate
        .notNull(
            repository,
            "ERROR: You must generate a repository to the provided entity before to add new finder. You could use 'repository jpa' commands.");

    // Add new finder to related repository using @RooFinders annotation
    AnnotationMetadata findersAnnotation = repository.getAnnotation(RooJavaType.ROO_FINDERS);

    // Maybe, this repository already has finder annotation, if not, create new @RooFinders annotation
    AnnotationMetadataBuilder finderAnnotationBuilder = null;
    if (findersAnnotation == null) {
      finderAnnotationBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_FINDERS);
      findersAnnotation = finderAnnotationBuilder.build();
    } else {
      // If provided finder name is not included, adds to @RooFinders annotation
      finderAnnotationBuilder = new AnnotationMetadataBuilder(findersAnnotation);
    }

    // Create list that will include finders to add
    List<AnnotationAttributeValue<?>> finders = new ArrayList<AnnotationAttributeValue<?>>();

    // Check if new finder name to be included already exists in @RooFinders annotation
    AnnotationAttributeValue<?> currentFinders = findersAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        NestedAnnotationAttributeValue finder = (NestedAnnotationAttributeValue) it.next();
        if (finder.getValue() != null && finder.getValue().getAttribute("finder") != null) {
          if (finder.getValue().getAttribute("finder").getValue()
              .equals(finderName.getSymbolName())) {
            LOGGER.log(
                Level.WARNING,
                String.format("ERROR: Finder '%s' already exists on entity '%s'",
                    finderName.getSymbolName(), entity.getSimpleTypeName()));
            return;
          }
          finders.add(finder);
        }
      }
    }

    // Create @RooFinder
    AnnotationMetadataBuilder singleFinderAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_FINDER);

    // Add finder attribute
    singleFinderAnnotation.addStringAttribute("finder", finderName.getSymbolName());

    // Add defaultReturnType attribute
    if (defaultReturnType == null) {
      singleFinderAnnotation.addClassAttribute("defaultReturnType", entity);
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          entity.getModule());
    } else {
      singleFinderAnnotation.addClassAttribute("defaultReturnType", defaultReturnType);
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          defaultReturnType.getModule());
    }

    // Add formBean attribute
    if (formBean != null) {
      singleFinderAnnotation.addClassAttribute("formBean", formBean);
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          formBean.getModule());
    } else {
      singleFinderAnnotation.addClassAttribute("formBean", entity);
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          entity.getModule());
    }

    NestedAnnotationAttributeValue newFinder =
        new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
            singleFinderAnnotation.build());

    // If not exists current finder, include it
    finders.add(newFinder);

    // Add finder list to currentFinders
    ArrayAttributeValue<AnnotationAttributeValue<?>> newFinders =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("finders"), finders);

    // Include finder name to finders attribute
    finderAnnotationBuilder.addAttribute(newFinders);

    // Include @RooFinders on related repository
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(repository);

    // Update annotation
    cidBuilder.updateTypeAnnotation(finderAnnotationBuilder);

    // Save changes on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  public boolean isFinderInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA);
  }

  public SortedSet<String> listFindersFor(final JavaType typeName, final Integer depth) {
    return null;
  }

  /**
   * Build a Map<String, String> with form bean type field names and "path" field names
   * and adds it to the typesFieldMaps, typeFieldMetadataMap and finderParametersMap.
   *
   * @param entity the entity containing the FieldMetadata used as finder parameters. 
   *            The fields can be fields from other related entity
   * @param formBeanType the bean class containing the values for which to look up.
   * @param typesFieldMaps the map with field names and "path" field names mappings. 
   *            Can be null.
   * @param typeFieldMetadataMap the map with field names and FieldMetadata mappings. 
   *            Can be null.
   * @param finderName the name of the finder to build mappings.
   * @param finderParametersList the list of FinderParameters for each finder name. 
   */
  public void buildFormBeanFieldNamesMap(JavaType entity, JavaType formBeanType,
      Map<JavaType, Map<String, String>> typesFieldMaps,
      Map<JavaType, Map<String, FieldMetadata>> typeFieldMetadataMap, JavaSymbolName finderName,
      Map<JavaSymbolName, List<FinderParameter>> finderParametersMap) {

    // Get all entity fields
    ClassOrInterfaceTypeDetails entityCid = getTypeLocationService().getTypeDetails(entity);
    MemberDetails entityMemberDetails =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), entityCid);
    List<FieldMetadata> allEntityFields = entityMemberDetails.getFields();

    // Create inner Maps
    Map<String, String> fieldNamesMap = null;
    if (typesFieldMaps != null) {
      if (typesFieldMaps.get(formBeanType) == null) {
        fieldNamesMap = new HashMap<String, String>();
      } else {
        fieldNamesMap = typesFieldMaps.get(formBeanType);
      }
    }

    Map<String, FieldMetadata> fieldMetadataMap = null;
    if (typeFieldMetadataMap != null) {
      if (typeFieldMetadataMap.get(formBeanType) == null) {
        fieldMetadataMap = new HashMap<String, FieldMetadata>();
      } else {
        fieldMetadataMap = typeFieldMetadataMap.get(formBeanType);
      }
    }

    // Get finder fields
    PartTree partTree = new PartTree(finderName.getSymbolName(), entityMemberDetails);
    List<FinderParameter> finderParameters = partTree.getParameters();

    // Create list of parameters for this finder
    List<FinderParameter> finderParametersList = new ArrayList<FinderParameter>();

    // Iterate over all specified fields
    for (FinderParameter finderParameter : finderParameters) {
      JavaSymbolName fieldName = finderParameter.getName();
      JavaType fieldType = finderParameter.getType();
      boolean found = false;

      // Get all DTO fields if form bean is a DTO or entity fields if not
      List<FieldMetadata> allFormBeanFields = new ArrayList<FieldMetadata>();
      if (getTypeLocationService().getTypeDetails(formBeanType) != null
          && getTypeLocationService().getTypeDetails(formBeanType).getAnnotation(
              RooJavaType.ROO_DTO) != null) {
        allFormBeanFields =
            getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
                getTypeLocationService().getTypeDetails(formBeanType)).getFields();
      }

      // Iterate over all entity fields
      for (FieldMetadata field : allEntityFields) {
        if (field.getFieldName().equals(fieldName) && field.getFieldType().equals(fieldType)) {

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

          if (typesFieldMaps != null) {
            fieldNamesMap.put(fieldName.getSymbolName(), fieldPathName);
          }

          // Add FieldMetadata from DTO to fieldMetadataMap
          if (!allFormBeanFields.isEmpty()) {
            boolean fieldFoundInDto = false;
            for (FieldMetadata dtoField : allFormBeanFields) {
              if (dtoField.getFieldName().equals(fieldName)
                  && dtoField.getFieldType().equals(fieldType)) {

                if (typeFieldMetadataMap != null) {
                  fieldMetadataMap.put(fieldName.getSymbolName(), dtoField);
                }
                fieldFoundInDto = true;
              }
            }
            Validate.isTrue(fieldFoundInDto,
                "Couldn't find a field with same name and type that %s on DTO %s",
                fieldName.getSymbolName(), formBeanType.getSimpleTypeName());
          } else {
            fieldMetadataMap.put(fieldName.getSymbolName(), field);
          }

          found = true;
          break;
        }
      }

      if (!found) {

        // The field isn't in the entity, should be in one of its relations
        for (FieldMetadata field : allEntityFields) {
          found =
              findDtoFieldRecursivelyAndAddToMappings(entity, fieldNamesMap, fieldMetadataMap,
                  found, field, fieldName, fieldType, allFormBeanFields,
                  formBeanType.getSimpleTypeName());
        }
      }

      if (!found) {
        // Field not found in its
        throw new IllegalArgumentException(String.format(
            "Field %s couldn't be located in DTO %s. Please, be sure that it is well "
                + "written and exists in %s or its related entities.", fieldName,
            formBeanType.getSimpleTypeName(), entity.getSimpleTypeName()));
      }

      finderParametersList.add(finderParameter);
    }

    // Add dto mappings to domain type mappings
    if (typesFieldMaps != null) {
      typesFieldMaps.put(formBeanType, fieldNamesMap);
    }
    if (typeFieldMetadataMap != null) {
      typeFieldMetadataMap.put(formBeanType, fieldMetadataMap);
    }

    // Add finder params to Map
    finderParametersMap.put(finderName, finderParametersList);
  }

  private boolean findDtoFieldRecursivelyAndAddToMappings(JavaType entity,
      Map<String, String> fieldNamesMap, Map<String, FieldMetadata> fieldMetadataMap,
      boolean found, FieldMetadata field, JavaSymbolName finderFieldName, JavaType finderFieldType,
      List<FieldMetadata> allDtoFields, String dtoSimpleName) {
    JavaType currentEntity;
    if (getTypeLocationService().getTypeDetails(field.getFieldType()) != null
        && getTypeLocationService().getTypeDetails(field.getFieldType()).getAnnotation(
            RooJavaType.ROO_JPA_ENTITY) != null) {

      // Change current entity
      currentEntity = field.getFieldType();

      // Modify pathName with one more level
      String pathName =
          StringUtils.uncapitalize(entity.getSimpleTypeName()).concat(".")
              .concat(field.getFieldName().getSymbolName());

      List<FieldMetadata> relatedEntityFields =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
              getTypeLocationService().getTypeDetails(currentEntity)).getFields();
      for (FieldMetadata relatedField : relatedEntityFields) {
        if (relatedField.getFieldName().equals(finderFieldName)
            && relatedField.getFieldType().equals(finderFieldType)) {

          // Add FieldMetadata from DTO to fieldMetadataMap
          boolean fieldFoundInDto = false;
          for (FieldMetadata dtoField : allDtoFields) {
            if (dtoField.getFieldName().equals(finderFieldName)
                && dtoField.getFieldType().equals(finderFieldType)) {
              fieldMetadataMap.put(finderFieldName.getSymbolName(), dtoField);
              fieldFoundInDto = true;
            }
          }
          Validate.isTrue(fieldFoundInDto,
              "Couldn't find a field with same name and type that %s on DTO %s",
              finderFieldName.getSymbolName(), dtoSimpleName);
          fieldNamesMap.put(finderFieldName.getSymbolName(),
              pathName.concat(".").concat(relatedField.getFieldName().getSymbolName()));
          found = true;
          break;
        } else {
          findDtoFieldRecursivelyAndAddToMappings(currentEntity, fieldNamesMap, fieldMetadataMap,
              found, relatedField, finderFieldName, finderFieldType, allDtoFields, dtoSimpleName);
        }
      }
    }
    return found;
  }

  public DynamicFinderServices getDynamicFinderServices() {
    if (dynamicFinderServices == null) {
      // Get all Services implement DynamicFinderServices interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(DynamicFinderServices.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (DynamicFinderServices) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load DynamicFinderServices on FinderOperationsImpl.");
        return null;
      }
    } else {
      return dynamicFinderServices;
    }
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MemberDetailsScanner) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on FinderOperationsImpl.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }

  public MetadataService getMetadataService() {
    if (metadataService == null) {
      // Get all Services implement MetadataService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MetadataService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MetadataService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MetadataService on FinderOperationsImpl.");
        return null;
      }
    } else {
      return metadataService;
    }
  }

  public PersistenceMemberLocator getPersistenceMemberLocator() {
    if (persistenceMemberLocator == null) {
      // Get all Services implement PersistenceMemberLocator interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PersistenceMemberLocator.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (PersistenceMemberLocator) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PersistenceMemberLocator on FinderOperationsImpl.");
        return null;
      }
    } else {
      return persistenceMemberLocator;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (ProjectOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on FinderOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeLocationService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on FinderOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeManagementService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on FinderOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }
}
