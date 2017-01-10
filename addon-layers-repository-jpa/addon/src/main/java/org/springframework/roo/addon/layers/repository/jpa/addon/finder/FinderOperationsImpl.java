package org.springframework.roo.addon.layers.repository.jpa.addon.finder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
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
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

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

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();


  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  public void installFinder(final JavaType entity, final JavaSymbolName finderName,
      JavaType formBean, JavaType returnType) {
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
    ClassOrInterfaceTypeDetails repository = getRepositoryJpaLocator().getRepository(entity);

    // Entity must have a repository that manages it, if not, shows an error
    Validate
        .notNull(
            repository,
            "ERROR: You must generate a repository to the provided entity before to add new finder. You could use 'repository jpa' commands.");

    // Check if provided formBean contains the field names and types indicated as finder params
    if (formBean != null) {
      MemberDetails entityMemberDetails =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
              getTypeLocationService().getTypeDetails(entity));
      PartTree finderPartTree = new PartTree(finderName.getSymbolName(), entityMemberDetails);
      checkDtoFieldsForFinder(getTypeLocationService().getTypeDetails(formBean), finderPartTree,
          repository.getType());
    }

    // Get repository annotation
    AnnotationMetadata repositoryAnnotation =
        repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    AnnotationMetadataBuilder repositoryAnnotationBuilder =
        new AnnotationMetadataBuilder(repositoryAnnotation);

    // Create list that will include finders to add
    List<AnnotationAttributeValue<?>> finders = new ArrayList<AnnotationAttributeValue<?>>();

    // Check if new finder name to be included already exists in @RooRepositoryJpa annotation
    AnnotationAttributeValue<?> currentFinders = repositoryAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        NestedAnnotationAttributeValue finder = (NestedAnnotationAttributeValue) it.next();
        if (finder.getValue() != null && finder.getValue().getAttribute("value") != null) {
          if (finder.getValue().getAttribute("value").getValue().equals(finderName.getSymbolName())) {
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
    singleFinderAnnotation.addStringAttribute("value", finderName.getSymbolName());

    // Add returnType attribute
    singleFinderAnnotation.addClassAttribute("returnType", returnType);

    // Prevent errors validating if the return type contains a valid module
    if (returnType.getModule() != null) {
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          returnType.getModule());
    }

    // Add formBean attribute
    if (formBean != null) {
      singleFinderAnnotation.addClassAttribute("formBean", formBean);
      getProjectOperations().addModuleDependency(repository.getName().getModule(),
          formBean.getModule());
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
    repositoryAnnotationBuilder.addAttribute(newFinders);

    // Update @RooRepositoryJpa annotation
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(repository);

    // Update annotation
    cidBuilder.updateTypeAnnotation(repositoryAnnotationBuilder);

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

    // Get all DTO fields if form bean is a DTO or entity fields if not
    List<FieldMetadata> allFormBeanFields = new ArrayList<FieldMetadata>();
    if (getTypeLocationService().getTypeDetails(formBeanType) != null
        && getTypeLocationService().getTypeDetails(formBeanType).getAnnotation(RooJavaType.ROO_DTO) != null) {
      allFormBeanFields =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
              getTypeLocationService().getTypeDetails(formBeanType)).getFields();
    }

    // Iterate over all specified fields
    for (FinderParameter finderParameter : finderParameters) {
      JavaSymbolName fieldName = finderParameter.getName();
      JavaType fieldType = finderParameter.getType();
      boolean found = false;


      // Iterate over all entity fields
      for (FieldMetadata field : allEntityFields) {
        if (field.getFieldName().equals(fieldName) && field.getFieldType().equals(fieldType)) {

          // Field found, build field "path" name and add it to map
          // Path name for DTO's should be the path to entity's fields
          String fieldPathName =
              String.format("%s.%s", StringUtils.uncapitalize(entity.getSimpleTypeName()),
                  field.getFieldName());

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

  /**
   * Validates that all dto fields matches with parameters on finder of a repository
   *
   * @param formBeanDetails
   * @param finder
   * @param repository
   */
  private void checkDtoFieldsForFinder(ClassOrInterfaceTypeDetails formBeanDetails,
      PartTree finder, JavaType repository) {
    final JavaType dtoType = formBeanDetails.getName();
    final String finderName = finder.getOriginalQuery();
    FieldMetadata paramField, dtoField;
    JavaType paramType, dtoFieldType;
    for (FinderParameter param : finder.getParameters()) {
      paramField = param.getPath().peek();
      dtoField = formBeanDetails.getField(paramField.getFieldName());
      Validate.notNull(dtoField, "Field '%s' not found on DTO %s for finder '%s' of %s",
          paramField.getFieldName(), dtoType, finderName, repository);
      paramType = paramField.getFieldType().getBaseType();
      dtoFieldType = dtoField.getFieldType().getBaseType();
      Validate.isTrue(paramType.equals(dtoFieldType),
          "Type missmatch for field '%s' on DTO %s for finder '%s' of %s: excepted %s and got %s",
          dtoField.getFieldName(), dtoType, finderName, repository, paramType, dtoFieldType);
    }
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

  private MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  private RepositoryJpaLocator getRepositoryJpaLocator() {
    return serviceInstaceManager.getServiceInstance(this, RepositoryJpaLocator.class);
  }


}
