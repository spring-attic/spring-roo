package org.springframework.roo.addon.ws.addon;

import java.util.List;

import org.springframework.roo.addon.ws.annotations.SoapBindingType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

/**
 * API that defines the available operations related with Web Service
 * management.
 * 
 * These operations will be used by the WsCommands to invoke its 
 * implementation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface WsOperations {

  /**
   * This method checks if it's possible to use the Web Service 
   * commands
   * 
   * @return true if is possible to use it and false if not.
   */
  boolean areWsCommandsAvailable();

  /**
   * This method includes new Web Service Client and install all the necessary
   * dependencies and configuration in the generated project.
   * 
   * @param wsdlLocation the location of the .wsdl file that should be used
   * to generate the client
   * @param endPoint endpoint provided by the .wsdl file
   * @param configClass the configuration class that should include the method to define
   * 		the Web Service client
   * @param bindingType the binding type to be used.
   * @param serviceUrl the service URL to be used.
   * @param profile the profile to be used
   */
  void addWsClient(String wsdlLocation, String endPoint, JavaType configClass,
      SoapBindingType bindingType, String serviceUrl, String profile);

  /**
   * This method includes new Service Endpoint Interface and its implementation in
   * the generated project.
   * 
   * @param service JavaType annotated with @RooService
   * @param sei JavaType of the new interface to be generated
   * @param endpointClass JavaType of the new endpoint class to be generated
   * @param configClass JavaType with the existing or new configuration class to register the new
   * 		generated enpoint
   * @param profile the profile to be used
   * @param force indicates if the developer wants to force the operation
   */
  void addSEI(JavaType service, JavaType sei, JavaType endpointClass, JavaType configClass,
      String profile, boolean force);

  /**
   * This method obtains the module where the .wsdl is located.
   * 
   * @param wsdlLocation that includes the .wsdl file name and 
   * the module name
   * 
   * @return Pom of the module where .wsdl file is located
   */
  Pom getModuleFromWsdlLocation(String wsdlLocation);

  /**
   * This method obtains the WSDL location excluding the module name
   * 
   * @param wsdlLocation
   * 
   * @return String with the .wsdl file name without including module name
   */
  String getWsdlNameWithoutModule(String wsdlLocation);

  /**
   * This method obtains the absolute path of the provided wsdl file.
   * 
   * This files always be inside 'sec/main/resources' folder
   * 
   * @param wsdlName the wsdlName including module if multimodule
   * 
   * @return String that defines the absolute path of the WSDL file.
   */
  String getWsdlAbsolutePathFromWsdlName(String wsdlName);

  /**
   * This method obtains all the registered endpoints inside the
   * .wsdl file. 
   * 
   * These elements are the attribute 'name' of the 'ports' element inside the 'services'
   * element of the provided .wsdl file.
   * 
   * @param wsdlPath absolute path where the .wsdl file is located
   * @return list with all registered endpoints.
   */
  List<String> getEndPointsFromWsdlFile(String wsdlPath);

  /**
   * This method obtains the targetNamespace attribute located inside
   * the provided .wsdl file
   * 
   * @param wsdlPath
   * @return
   */
  String getTargetNameSpaceFromWsdlFile(String wsdlPath);

  /**
   * This method obtains the binding type of the provided .wsdl file
   * 
   * @param wsdlPath the .wsdl file where binding is defined
   * 
   * @return SoapBindingType associated to the provided wsdl file.
   */
  SoapBindingType getBindingTypeFromWsdlFile(String wsdlPath);

  /**
   * This method obtains the service url attribute located inside the
   * provided .wsdl file
   * 
   * @param endPoint
   * @param wsdlPath
   * @return
   */
  String getServiceUrlForEndpointFromWsdlFile(String endPoint, String wsdlPath);

}
