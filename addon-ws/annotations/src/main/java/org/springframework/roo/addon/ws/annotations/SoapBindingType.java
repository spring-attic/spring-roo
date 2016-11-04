package org.springframework.roo.addon.ws.annotations;

/**
 * This enum provides the different types of SOAP Binding that
 * are allowed.
 * 
 * By default, the SOAP Binding type could be identified by the used namespace
 * in the .wsdl file.
 * 
 * SOAP 1.1 uses namespace http://schemas.xmlsoap.org/wsdl/soap/
 * SOAP 1.2 uses namespace http://schemas.xmlsoap.org/wsdl/soap12/
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public enum SoapBindingType {

  SOAP11, SOAP12;

}
