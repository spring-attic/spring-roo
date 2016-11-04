package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.addon.ws.annotations.RooWsClient;
import org.springframework.roo.addon.ws.annotations.RooWsClients;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * The values of a {@link RooWsClients} annotation.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class RooWsClientsAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private RooWsClient[] endpoints;

  @AutoPopulate
  private String profile;

  /**
   * Constructor
   *
   * @param governorPhysicalTypeMetadata the metadata to parse (required)
   */
  public RooWsClientsAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(governorPhysicalTypeMetadata, RooJavaType.ROO_WS_CLIENTS);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  public RooWsClientsAnnotationValues(final ClassOrInterfaceTypeDetails cid) {
    super(cid, RooJavaType.ROO_WS_CLIENTS);
    AutoPopulationUtils.populate(this, annotationMetadata);
  }

  /**
   * Returns the endpoints that manages this configuration class
   *
   * @return String array
   */
  public RooWsClient[] getEndpoints() {
    return endpoints;
  }

  public String getProfile() {
    return profile;
  }

}
