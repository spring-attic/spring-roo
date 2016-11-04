package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.model.EnumDetails;

/**
 * DTO that defines the necessary information about a
 * Endpoint of a WebService client.
 * 
 * Provides all necessary methods to manage client Endpoints
 * easily.
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class WsClientEndpoint {

  /**
   * Name of the client WebService Endpoint
   */
  private final String name;

  /**
   * targetNamespace associated to this Endpoint
   */
  private final String targetNameSpace;

  /**
   * SoapBinding associated to this endpoint
   */
  private final EnumDetails bindingType;

  public WsClientEndpoint(String name, String targetNameSpace, EnumDetails bindingType) {
    this.name = name;
    this.targetNameSpace = targetNameSpace;
    this.bindingType = bindingType;
  }

  public String getName() {
    return name;
  }

  public String getTargetNameSpace() {
    return targetNameSpace;
  }

  public EnumDetails getBindingType() {
    return bindingType;
  }

}
