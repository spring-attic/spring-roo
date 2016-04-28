package org.springframework.roo.addon.finder.addon;

import static org.springframework.roo.model.RooJavaType.ROO_FINDERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.parser.FinderAutocomplete;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.finder.addon.parser.PartTree;
import org.springframework.roo.addon.finder.annotations.RooFinder;
import org.springframework.roo.addon.finder.annotations.RooFinders;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link FinderMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class FinderMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements FinderMetadataProvider, FinderAutocomplete {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FinderMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToRepositoryMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  //Map where entity details will be cached
  private Map<JavaType, MemberDetails> entitiesDetails = new HashMap<JavaType, MemberDetails>();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_FINDERS} as additional 
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

    addMetadataTrigger(ROO_FINDERS);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(ROO_FINDERS,
            new JavaSymbolName(RooFinders.FINDERS_ATTRIBUTE)));
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

    removeMetadataTrigger(ROO_FINDERS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return FinderMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = FinderMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Finder";
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
    //    final FindersAnnotationValues annotationValues =
    //        new FindersAnnotationValues(governorPhysicalTypeMetadata);

    // Getting related entity
    ClassOrInterfaceTypeDetails repository =
        getTypeLocationService().getTypeDetails(governorPhysicalTypeMetadata.getType());
    AnnotationMetadata repositoryAnnotation =
        repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA);

    Validate.notNull(repositoryAnnotation,
        "ERROR: Repository should be annotated with @RooJpaRepository");

    AnnotationAttributeValue<JavaType> relatedEntity = repositoryAnnotation.getAttribute("entity");

    Validate.notNull(relatedEntity, "ERROR: @RooJpaRepository should include entity attribute");

    JavaType entity = relatedEntity.getValue();

    // Getting member details
    MemberDetails entityMemberDetails = getMemberDetails(entity);

    // Create list of finder to add
    List<FinderMethod> findersToAdd = new ArrayList<FinderMethod>();

    // Get @RooFinders
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    AnnotationMetadata findersAnnotation = cid.getAnnotation(ROO_FINDERS);

    Validate
        .notNull(findersAnnotation, String.format("%s%s", "Cannot obtain @RooFinders from ", cid
            .getName().getSimpleTypeName()));

    // Get @RooFinders attributes
    AnnotationAttributeValue<?> currentFinders = findersAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        NestedAnnotationAttributeValue finderAnnotation =
            (NestedAnnotationAttributeValue) it.next();
        if (finderAnnotation.getValue() != null
            && finderAnnotation.getValue().getAttribute("finder") != null) {

          // Get finder name
          String finderName = null;
          if (finderAnnotation.getValue().getAttribute("finder").getValue() instanceof String) {
            finderName = (String) finderAnnotation.getValue().getAttribute("finder").getValue();
          }
          Validate.notNull(finderName, "'finder' attribute in @RooFinder must be a String");

          // Get finder return type
          JavaType returnType =
              (JavaType) finderAnnotation.getValue().getAttribute("returnType").getValue();

          // Create FinderMethod
          PartTree finder = new PartTree(finderName, entityMemberDetails, this, returnType);

          Validate
              .notNull(
                  finder,
                  String
                      .format(
                          "ERROR: '%s' is not a valid finder. Use autocomplete feature (TAB or CTRL + Space) to include finder that follows Spring Data nomenclature.",
                          finderName));

          FinderMethod finderMethod =
              new FinderMethod(finder.getReturnType(), new JavaSymbolName(finderName),
                  finder.getParameters());

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

          // Add to finder methods list
          findersToAdd.add(finderMethod);
        }
      }
    } else {
      LOGGER.log(Level.SEVERE,
          "ERROR: You must include 'finders' attribute on @RooFinders annotation");
      return null;
    }

    return new FinderMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, findersToAdd);
  }

  public String getProvidesType() {
    return FinderMetadata.getMetadataIdentiferType();
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
