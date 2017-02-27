package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_READ_ONLY_REPOSITORY;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderAutocomplete;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderMethod;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
import org.springframework.roo.addon.layers.repository.jpa.annotations.finder.RooFinder;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link RepositoryJpaMetadataProvider}.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements RepositoryJpaMetadataProvider, FinderAutocomplete {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToRepositoryMidMap =
      new LinkedHashMap<JavaType, String>();
  private final Map<String, JavaType> repositoryMidToDomainTypeMap =
      new LinkedHashMap<String, JavaType>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  //Map where entity details will be cached
  private Map<JavaType, MemberDetails> entitiesDetails = new HashMap<JavaType, MemberDetails>();


  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_REPOSITORY_JPA);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(cContext.getBundleContext(), getClass(),
            new LayerTypeMatcher(ROO_REPOSITORY_JPA, new JavaSymbolName(
                RooJpaRepository.ENTITY_ATTRIBUTE)));
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

    removeMetadataTrigger(ROO_REPOSITORY_JPA);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = RepositoryJpaMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RepositoryJpaMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository";
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
    final RepositoryJpaAnnotationValues annotationValues =
        new RepositoryJpaAnnotationValues(governorPhysicalTypeMetadata);
    final JavaType domainType = annotationValues.getEntity();

    // XXX clear entitiesDetails by security
    entitiesDetails.clear();

    // Remember that this entity JavaType matches up with this metadata
    // identification string
    // Start by clearing any previous association
    final JavaType oldEntity = repositoryMidToDomainTypeMap.get(metadataIdentificationString);
    if (oldEntity != null) {
      domainTypeToRepositoryMidMap.remove(oldEntity);
    }
    domainTypeToRepositoryMidMap.put(domainType, metadataIdentificationString);
    repositoryMidToDomainTypeMap.put(metadataIdentificationString, domainType);

    // Getting associated entity
    JavaType entity = annotationValues.getEntity();

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    MemberDetails entityMemberDetails =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), entityDetails);
    final String entityMetadataId = JpaEntityMetadata.createIdentifier(entityDetails);
    final JpaEntityMetadata entityMetadata = getMetadataService().get(entityMetadataId);

    if (entityMetadata == null) {
      return null;
    }

    // Getting java bean metadata
    final String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);

    // Register dependency between repositoryMetadata and jpaEntityMetadata
    registerDependency(entityMetadataId, metadataIdentificationString);

    // Create dependency between repository and java bean annotation
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Check if related entity is setted as readOnly
    JavaType readOnlyRepository = null;
    if (entityMetadata.isReadOnly()) {
      // Getting ReadOnlyRepository interface annotated with
      // @RooReadOnlyRepository
      Set<ClassOrInterfaceTypeDetails> readOnlyRepositories =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              ROO_READ_ONLY_REPOSITORY);

      Validate
          .notEmpty(
              readOnlyRepositories,
              "ERROR: You should define a ReadOnlyRepository interface annotated with @RooReadOnlyRepository to be able to generate repositories of readOnly entities.");

      Iterator<ClassOrInterfaceTypeDetails> it = readOnlyRepositories.iterator();
      while (it.hasNext()) {
        ClassOrInterfaceTypeDetails readOnlyRepositoryDetails = it.next();
        readOnlyRepository = readOnlyRepositoryDetails.getType();
        break;
      }
    }

    List<JavaType> repositoryCustomList = new ArrayList<JavaType>();
    // Getting RepositoryCustom interface annotated with
    // @RooJpaRepositoryCustom
    Set<ClassOrInterfaceTypeDetails> customRepositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            ROO_REPOSITORY_JPA_CUSTOM);

    Iterator<ClassOrInterfaceTypeDetails> customRepositoriesIt = customRepositories.iterator();
    while (customRepositoriesIt.hasNext()) {
      ClassOrInterfaceTypeDetails customRepository = customRepositoriesIt.next();
      AnnotationMetadata annotation = customRepository.getAnnotation(ROO_REPOSITORY_JPA_CUSTOM);
      if (annotation.getAttribute("entity").getValue().equals(entity)) {
        repositoryCustomList.add(customRepository.getType());
      }
    }
    Validate.notEmpty(repositoryCustomList,
        "Can't find any interface annotated with @%s(entity=%s)",
        ROO_REPOSITORY_JPA_CUSTOM.getSimpleTypeName(), entity.getSimpleTypeName());
    Validate.isTrue(repositoryCustomList.size() == 1,
        "More than one interface annotated with @%s(entity=%s): %s",
        ROO_REPOSITORY_JPA_CUSTOM.getSimpleTypeName(), entity.getSimpleTypeName(),
        StringUtils.join(repositoryCustomList, ","));

    JavaType defaultReturnType;
    final JavaType annotationDefaultReturnType = annotationValues.getDefaultReturnType();
    // Get and check defaultReturnType
    if (annotationDefaultReturnType == null || annotationDefaultReturnType.equals(JavaType.CLASS)
        || annotationDefaultReturnType.equals(entity)) {
      defaultReturnType = entity;
    } else {
      // Validate that defaultReturnValue is projection of current entity
      ClassOrInterfaceTypeDetails returnTypeCid =
          getTypeLocationService().getTypeDetails(annotationDefaultReturnType);
      AnnotationMetadata projectionAnnotation =
          returnTypeCid.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);
      Validate.notNull(projectionAnnotation,
          "ERROR: %s defined on %s.@%s.defaultReturnType must be annotated with @%s annotation",
          annotationDefaultReturnType, governorPhysicalTypeMetadata.getType(),
          RooJavaType.ROO_REPOSITORY_JPA, RooJavaType.ROO_ENTITY_PROJECTION);
      Validate
          .isTrue(
              entity.equals(projectionAnnotation.getAttribute("entity").getValue()),
              "ERROR: %s defined on %s.@%s.defaultReturnType must be annotated with @%s annotation and match the 'entity' attribute value",
              annotationDefaultReturnType, governorPhysicalTypeMetadata.getType(),
              RooJavaType.ROO_REPOSITORY_JPA, RooJavaType.ROO_ENTITY_PROJECTION);

      defaultReturnType = annotationDefaultReturnType;
    }

    // Get field which entity is field part
    List<Pair<FieldMetadata, RelationInfo>> relationsAsChild =
        getJpaOperations().getFieldChildPartOfRelation(entityDetails);

    // Get Annotation
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    AnnotationMetadata repositoryAnnotation = cid.getAnnotation(ROO_REPOSITORY_JPA);

    // Create list of finder to add
    List<FinderMethod> findersToAdd = new ArrayList<FinderMethod>();

    // Create list of finder to add in RespositoryCustom
    List<Pair<FinderMethod, PartTree>> findersToAddInCustom =
        new ArrayList<Pair<FinderMethod, PartTree>>();

    Map<JavaType, ClassOrInterfaceTypeDetails> detailsCache =
        new HashMap<JavaType, ClassOrInterfaceTypeDetails>();

    List<String> declaredFinderNames = new ArrayList<String>();


    RooFinder[] findersAnnValue = annotationValues.getFinders();

    // Get finders attributes
    AnnotationAttributeValue<?> currentFinders = repositoryAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> valuesIt = values.iterator();

      while (valuesIt.hasNext()) {
        NestedAnnotationAttributeValue finderAnnotation =
            (NestedAnnotationAttributeValue) valuesIt.next();
        if (finderAnnotation.getValue() != null
            && finderAnnotation.getValue().getAttribute("value") != null) {

          // Get finder name
          String finderName = null;
          if (finderAnnotation.getValue().getAttribute("value").getValue() instanceof String) {
            finderName = (String) finderAnnotation.getValue().getAttribute("value").getValue();
          }
          Validate.notNull(finderName, "'finder' attribute in @RooFinder must be a String");
          declaredFinderNames.add(finderName);

          // Get finder return type
          JavaType returnType = getNestedAttributeValue(finderAnnotation, "returnType");
          JavaType finderReturnType;
          if (returnType == null || JavaType.CLASS.equals(returnType)) {
            finderReturnType = defaultReturnType;
            returnType = null;
          } else {
            finderReturnType = returnType;
          }

          // Get finder return type
          JavaType formBean = getNestedAttributeValue(finderAnnotation, "formBean");
          boolean isDeclaredFormBean = false;
          if (JavaType.CLASS.equals(formBean)) {
            formBean = null;
          }

          // Create FinderMethods
          PartTree finder = new PartTree(finderName, entityMemberDetails, this, finderReturnType);

          Validate
              .notNull(
                  finder,
                  String
                      .format(
                          "ERROR: '%s' is not a valid finder. Use autocomplete feature (TAB or CTRL + Space) to include finder that follows Spring Data nomenclature.",
                          finderName));

          FinderMethod finderMethod = new FinderMethod(finder);

          // Add dependencies between modules
          List<JavaType> types = new ArrayList<JavaType>();
          types.add(finder.getReturnType());
          types.addAll(finder.getReturnType().getParameters());

          for (FinderParameter parameter : finder.getParameters()) {
            types.add(parameter.getType());
            types.addAll(parameter.getType().getParameters());
          }

          for (JavaType parameter : types) {
            getTypeLocationService().addModuleDependency(
                governorPhysicalTypeMetadata.getType().getModule(), parameter);
          }

          if (formBean == null && (returnType == null || entity.equals(returnType))) {

            // Add to finder methods list
            findersToAdd.add(finderMethod);

          } else {
            // If defaultReturnType is a Projection or formBean is a DTO, finder creation
            // should be avoided here and let RepositoryJpaCustomMetadata create it on
            // RepositoryCustom classes.
            if (returnType != null && !returnType.equals(entity)) {
              ClassOrInterfaceTypeDetails returnTypeDetails =
                  getDetailsFor(returnType, detailsCache);
              Validate
                  .isTrue(
                      returnTypeDetails != null
                          && returnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION) != null,
                      "ERROR: finder '%s' declared 'returnType' (%s) is not annotated with @%s annotation",
                      finderName, returnType.getSimpleTypeName(),
                      RooJavaType.ROO_ENTITY_PROJECTION.getSimpleTypeName());

            }

            Validate.notNull(formBean,
                "ERROR: finder '%s' requires 'formBean' value if 'defaultReturnType' is defined",
                finderName);

            ClassOrInterfaceTypeDetails formBeanDetails =
                getTypeLocationService().getTypeDetails(formBean);

            Validate.isTrue(
                formBeanDetails != null
                    && formBeanDetails.getAnnotation(RooJavaType.ROO_DTO) != null,
                "ERROR: finder '%s' declared 'formBean' (%s) is not annotated with @%s annotation",
                finderName, formBean.getSimpleTypeName(),
                RooJavaType.ROO_ENTITY_PROJECTION.getSimpleTypeName());

            checkDtoFieldsForFinder(formBeanDetails, finder, governorPhysicalTypeMetadata.getType());

            if (returnType == null) {
              returnType = entity;
            }

            finderMethod =
                new FinderMethod(returnType, new JavaSymbolName(finderName),
                    Arrays.asList(new FinderParameter(formBean, new JavaSymbolName(StringUtils
                        .uncapitalize(formBean.getSimpleTypeName())))));
            findersToAddInCustom.add(Pair.of(finderMethod, finder));

          }
        }
      }
    }

    return new RepositoryJpaMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, entityMetadata, readOnlyRepository,
        repositoryCustomList.get(0), defaultReturnType, relationsAsChild, findersToAdd,
        findersToAddInCustom, declaredFinderNames);
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

  /**
   * Gets attributo of a nested annotation attribute value
   *
   * @param newstedAnnotationAttr
   * @param attributeName
   * @return
   */
  private <T> T getNestedAttributeValue(NestedAnnotationAttributeValue newstedAnnotationAttr,
      String attributeName) {

    AnnotationMetadata annotationValue = newstedAnnotationAttr.getValue();
    if (annotationValue == null) {
      return null;
    }
    AnnotationAttributeValue<?> attribute = annotationValue.getAttribute(attributeName);
    if (attribute == null) {
      return null;
    }
    return (T) attribute.getValue();
  }

  private ClassOrInterfaceTypeDetails getDetailsFor(JavaType type,
      Map<JavaType, ClassOrInterfaceTypeDetails> detailsCache) {
    if (detailsCache.containsKey(type)) {
      return detailsCache.get(type);
    }
    ClassOrInterfaceTypeDetails details = getTypeLocationService().getTypeDetails(type);
    detailsCache.put(type, details);
    return details;
  }

  private JpaOperations getJpaOperations() {
    return getServiceManager().getServiceInstance(this, JpaOperations.class);
  }

  private RepositoryJpaLocator getRepositoryJpaLocator() {
    return getServiceManager().getServiceInstance(this, RepositoryJpaLocator.class);
  }

  public String getProvidesType() {
    return RepositoryJpaMetadata.getMetadataIdentiferType();
  }

  protected void registerDependency(final String upstreamDependency,
      final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  @Override
  public MemberDetails getEntityDetails(JavaType entity) {

    Validate.notNull(entity, "ERROR: Entity should be provided");

    if (entitiesDetails.containsKey(entity)) {
      return entitiesDetails.get(entity);
    }

    // We know the file exists, as there's already entity metadata for it
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entity);

    if (cid == null) {
      return null;
    }

    if (cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) == null) {
      return null;
    }

    entitiesDetails.put(entity,
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), cid));
    return entitiesDetails.get(entity);
  }
}
