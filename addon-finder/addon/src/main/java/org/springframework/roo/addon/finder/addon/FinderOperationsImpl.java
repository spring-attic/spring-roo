package org.springframework.roo.addon.finder.addon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

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

  public void installFinder(final JavaType entity, final JavaSymbolName finderName) {
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

    // Add new finder to related repository using @RooFinder annotation
    AnnotationMetadata finderAnnotation = repository.getAnnotation(RooJavaType.ROO_FINDER);

    // Maybe, this repository already has finder annotation, if not, create new @RooFinder annotation
    AnnotationMetadataBuilder finderAnnotationBuilder = null;
    if (finderAnnotation == null) {
      finderAnnotationBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_FINDER);
      finderAnnotation = finderAnnotationBuilder.build();
    } else {
      // If provided finderName is not included, adds to @RooFinder annotation
      finderAnnotationBuilder = new AnnotationMetadataBuilder(finderAnnotation);
    }

    // Create list that will include finders to add
    List<AnnotationAttributeValue<?>> finders = new ArrayList<AnnotationAttributeValue<?>>();

    // Check if new finderName to be included already exists in @RooFinder annotation
    AnnotationAttributeValue<?> currentFinders = finderAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        StringAttributeValue finder = (StringAttributeValue) it.next();
        if (finder.getValue().equals(finderName.getSymbolName())) {
          LOGGER.log(
              Level.WARNING,
              String.format("ERROR: Finder '%s' already exists on entity '%s'",
                  finderName.getSymbolName(), entity.getSimpleTypeName()));
          return;
        }
        finders.add(finder);
      }
    }

    // If not exists current finder, include it
    finders.add(new StringAttributeValue(new JavaSymbolName("value"), finderName.getSymbolName()));

    // Add finder list to currentFinders
    ArrayAttributeValue<AnnotationAttributeValue<?>> newFinders =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("finders"), finders);

    // Include finder name to finders attribute
    finderAnnotationBuilder.addAttribute(newFinders);

    // Include @RooFinder on related repository
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
    /*Validate.notNull(typeName, "Java type required");
    
    final String id = getTypeLocationService()
            .getPhysicalTypeIdentifier(typeName);
    if (id == null) {
        throw new IllegalArgumentException("Cannot locate source for '"
                + typeName.getFullyQualifiedTypeName() + "'");
    }
    
    // Go and get the entity metadata, as any type with finders has to be an
    // entity
    final JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
    final LogicalPath path = PhysicalTypeIdentifier.getPath(id);
    final String entityMid = JpaActiveRecordMetadata.createIdentifier(
            javaType, path);
    
    // Get the entity metadata
    final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) getMetadataService()
            .get(entityMid);
    if (jpaActiveRecordMetadata == null) {
        throw new IllegalArgumentException(
                "Cannot provide finders because '"
                        + typeName.getFullyQualifiedTypeName()
                        + "' is not an 'active record' entity");
    }
    
    // Get the member details
    final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) getMetadataService()
            .get(PhysicalTypeIdentifier.createIdentifier(javaType, path));
    if (physicalTypeMetadata == null) {
        throw new IllegalStateException(
                "Could not determine physical type metadata for type "
                        + javaType);
    }
    final ClassOrInterfaceTypeDetails cid = physicalTypeMetadata
            .getMemberHoldingTypeDetails();
    if (cid == null) {
        throw new IllegalStateException(
                "Could not determine class or interface type details for type "
                        + javaType);
    }
    final MemberDetails memberDetails = getMemberDetailsScanner()
            .getMemberDetails(getClass().getName(), cid);
    final List<FieldMetadata> idFields = getPersistenceMemberLocator()
            .getIdentifierFields(javaType);
    final FieldMetadata versionField = getPersistenceMemberLocator()
            .getVersionField(javaType);
    
    // Compute the finders (excluding the ID, version, and EM fields)
    final Set<JavaSymbolName> exclusions = new HashSet<JavaSymbolName>();
    exclusions.add(jpaActiveRecordMetadata.getEntityManagerField()
            .getFieldName());
    for (final FieldMetadata idField : idFields) {
        exclusions.add(idField.getFieldName());
    }
    
    if (versionField != null) {
        exclusions.add(versionField.getFieldName());
    }
    
    final SortedSet<String> result = new TreeSet<String>();
    
    final List<JavaSymbolName> finders = getDynamicFinderServices().getFinders(
            memberDetails, jpaActiveRecordMetadata.getPlural(), depth,
            exclusions);
    for (final JavaSymbolName finder : finders) {
        // Avoid displaying problematic finders
        try {
            final QueryHolder queryHolder = getDynamicFinderServices()
                    .getQueryHolder(memberDetails, finder,
                            jpaActiveRecordMetadata.getPlural(),
                            jpaActiveRecordMetadata.getEntityName());
            final List<JavaSymbolName> parameterNames = queryHolder
                    .getParameterNames();
            final List<JavaType> parameterTypes = queryHolder
                    .getParameterTypes();
            final StringBuilder signature = new StringBuilder();
            int x = -1;
            for (final JavaType param : parameterTypes) {
                x++;
                if (x > 0) {
                    signature.append(", ");
                }
                signature.append(param.getSimpleTypeName()).append(" ")
                        .append(parameterNames.get(x).getSymbolName());
            }
            result.add(finder.getSymbolName() + "(" + signature + ")");
        }
        catch (final RuntimeException e) {
            result.add(finder.getSymbolName() + " - failure");
        }
    }
    return result;*/
    return null;
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
