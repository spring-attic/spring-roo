package org.springframework.roo.addon.email.addon;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.email.annotations.RooSimpleMailMessageConfig;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides @RooSimpleMailMessageConfig metadata
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class RooSimpleMailMessageConfigMetadataProvider extends AbstractItdMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RooSimpleMailMessageConfigMetadataProvider.class);

  public static final JavaType ROO_SIMPLE_MAIL_MESSAGE_CONFIG_ANNOTATION = new JavaType(
      RooSimpleMailMessageConfig.class);

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    getMetadataDependencyRegistry().addNotificationListener(this);
    getMetadataDependencyRegistry().registerDependency(
        PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    addMetadataTrigger(ROO_SIMPLE_MAIL_MESSAGE_CONFIG_ANNOTATION);
  }

  protected void deactivate(final ComponentContext context) {
    getMetadataDependencyRegistry().removeNotificationListener(this);
    getMetadataDependencyRegistry().deregisterDependency(
        PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    removeMetadataTrigger(ROO_SIMPLE_MAIL_MESSAGE_CONFIG_ANNOTATION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RooSimpleMailMessageConfigMetadata.createIdentifier(javaType, path);
  }


  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        RooSimpleMailMessageConfigMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        RooSimpleMailMessageConfigMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "SimpleMailMessageConfig";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {

    return new RooSimpleMailMessageConfigMetadata(metadataIdentificationString, aspectName,
        governorPhysicalType);
  }

  @Override
  public String getProvidesType() {
    return RooSimpleMailMessageConfigMetadata.getMetadataIdentiferType();
  }

}
