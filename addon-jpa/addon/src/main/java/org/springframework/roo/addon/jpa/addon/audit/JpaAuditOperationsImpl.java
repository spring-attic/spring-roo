package org.springframework.roo.addon.jpa.addon.audit;

import java.util.Set;
import java.util.logging.Logger;

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
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implements {@link JpaAuditOperations} to be able to include
 * Jpa Audit support in generated projects
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class JpaAuditOperationsImpl implements JpaAuditOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JpaAuditOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.0.0.BUILD-SNAPSHOT");
  private static final Dependency SPRINGLETS_DATA_JPA_STARTER_WITH_VERSION = new Dependency(
      "io.springlets", "springlets-boot-starter-data-jpa", "${springlets.version}");
  private static final Dependency SPRINGLETS_DATA_JPA_STARTER_WITHOUT_VERSION = new Dependency(
      "io.springlets", "springlets-boot-starter-data-jpa", null);


  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public boolean isJpaAuditSetupPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA)
        && !getProjectOperations().isFeatureInstalled(AUDIT_FEATURE_NAME);
  }

  @Override
  public boolean isJpaAuditAddPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.AUDIT);
  }

  @Override
  public void setupJpaAudit(Pom module) {

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    if (getProjectOperations().isMultimoduleProject()) {

      // If current project is a multimodule project, include dependencies first
      // on dependencyManagement and then on current module
      getProjectOperations().addDependencyToDependencyManagement("",
          SPRINGLETS_DATA_JPA_STARTER_WITH_VERSION);
      getProjectOperations().addDependency(module.getModuleName(),
          SPRINGLETS_DATA_JPA_STARTER_WITHOUT_VERSION);

    } else {

      // If not multimodule, include dependencies on root
      getProjectOperations().addDependency("", SPRINGLETS_DATA_JPA_STARTER_WITH_VERSION);
    }


  }

  @Override
  public void addJpaAuditToEntity(JavaType entity, String createdDateColumn,
      String modifiedDateColumn, String createdByColumn, String modifiedByColumn) {

    // Create @RooJpaAudit
    AnnotationMetadataBuilder rooJpaAudit =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_AUDIT);

    // Add parameters if required
    if (createdDateColumn != null) {
      rooJpaAudit.addStringAttribute("createdDateColumn", createdDateColumn);
    }
    if (modifiedDateColumn != null) {
      rooJpaAudit.addStringAttribute("modifiedDateColumn", modifiedDateColumn);
    }
    if (createdByColumn != null) {
      rooJpaAudit.addStringAttribute("createdByColumn", createdByColumn);
    }
    if (modifiedByColumn != null) {
      rooJpaAudit.addStringAttribute("modifiedByColumn", modifiedByColumn);
    }

    // Add annotation
    ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entity);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(cid);
    cidBuilder.addAnnotation(rooJpaAudit.build());

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
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
        LOGGER.warning("Cannot load ProjectOperations on SecurityOperationsImpl.");
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
        LOGGER.warning("Cannot load TypeLocationService on SecurityOperationsImpl.");
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
        LOGGER.warning("Cannot load TypeManagementService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  // FEATURE OPERATIONS

  @Override
  public String getName() {
    return AUDIT_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    boolean isInstalledInModule = false;
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> starter =
        module.getDependenciesExcludingVersion(SPRINGLETS_DATA_JPA_STARTER_WITH_VERSION);

    if (!starter.isEmpty()) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }
}
