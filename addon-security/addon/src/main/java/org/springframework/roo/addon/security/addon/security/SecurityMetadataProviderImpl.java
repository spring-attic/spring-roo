package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.model.RooJavaType.*;
import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.itd.*;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.*;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link SecurityMetadataProvider}.
 * <p/>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class SecurityMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements SecurityMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;
  private static final JavaType ROO_AUTHENTICATION_AUDITOR_AWARE = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooAuthenticationAuditorAware");

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_SECURITY_CONFIGURATION} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_SECURITY_CONFIGURATION);
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

    removeMetadataTrigger(ROO_SECURITY_CONFIGURATION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return SecurityMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = SecurityMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = SecurityMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "SecurityConfiguration";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final SecurityConfigurationAnnotationValues annotationValues =
        new SecurityConfigurationAnnotationValues(governorPhysicalTypeMetadata);

    // Get AuthenticationAuditorAware JavaType if exists
    Set<JavaType> authenticationClasses =
        getTypeLocationService().findTypesWithAnnotation(ROO_AUTHENTICATION_AUDITOR_AWARE);

    JavaType authenticationType = null;
    if (authenticationClasses.size() > 0) {
      for (JavaType authenticationClass : authenticationClasses) {
        if (authenticationClass.getModule() != null
            && authenticationClass.getModule().equals(
                governorPhysicalTypeMetadata.getType().getModule())) {
          authenticationType = authenticationClass;
          break;
        } else if (authenticationClasses.size() == 1) {

          // Project is single module. Pick single authentication class
          authenticationType = authenticationClass;
        }
      }
    }

    return new SecurityMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, authenticationType, annotationValues);
  }

  public String getProvidesType() {
    return SecurityMetadata.getMetadataIdentiferType();
  }

}
