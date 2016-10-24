package org.springframework.roo.addon.plural.addon;

import java.util.Locale;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * 
 * Implementation of {@link PluralServiceImpl}.
 * 
 * The implemented methods will be able to obtain the plural of the provided
 * elements and will return a pluralized String.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PluralServiceImpl implements PluralService {

  protected final static Logger LOGGER = HandlerUtils.getLogger(PluralServiceImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @Override
  public String getPlural(JavaType type) {
    return getPlural(type, Locale.ENGLISH);
  }

  @Override
  public String getPlural(JavaType type, Locale locale) {
    Validate.notNull(type, "ERROR: You must provide a valid JavaType");
    Validate.notNull(locale, "ERROR: You must provide a valid Locale");

    // Getting ClassOrInterfaceTypeDetails
    ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(type);
    Validate.notNull(cid, "ERROR: You must provide an existing JavaType");

    return getPlural(cid, locale);
  }

  @Override
  public String getPlural(ClassOrInterfaceTypeDetails cid) {
    return getPlural(cid, Locale.ENGLISH);
  }

  @Override
  public String getPlural(ClassOrInterfaceTypeDetails cid, Locale locale) {
    Validate.notNull(cid, "ERROR: You must provide a valid ClassOrInterfaceTypeDetails");
    Validate.notNull(locale, "ERROR: You must provide a valid Locale");

    final JavaType javaType = cid.getType();
    final LogicalPath logicalPath = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
    final String pluralMetadataKey = PluralMetadata.createIdentifier(javaType, logicalPath);
    final PluralMetadata pluralMetadata =
        (PluralMetadata) getMetadataService().get(pluralMetadataKey);
    if (pluralMetadata != null) {
      final String plural = pluralMetadata.getPlural();
      if (plural.equalsIgnoreCase(javaType.getSimpleTypeName())) {
        return plural + "Items";
      } else {
        return plural;
      }
    }
    return getPlural(javaType.getSimpleTypeName(), locale);
  }

  @Override
  public String getPlural(JavaSymbolName term) {
    return getPlural(term, Locale.ENGLISH);
  }

  @Override
  public String getPlural(JavaSymbolName term, Locale locale) {
    Validate.notNull(locale, "ERROR: You must provide a valid Locale");
    return getPlural(term.getSymbolName(), locale);
  }

  @Override
  public String getPlural(String term) {
    return getPlural(term, Locale.ENGLISH);
  }

  @Override
  public String getPlural(String term, Locale locale) {
    Validate.notNull(locale, "ERROR: You must provide a valid Locale");
    return PluralMetadata.getInflectorPlural(term, locale);
  }

  // OSGI Services

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

}
