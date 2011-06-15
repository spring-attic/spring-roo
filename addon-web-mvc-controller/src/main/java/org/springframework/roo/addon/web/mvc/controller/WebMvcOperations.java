package org.springframework.roo.addon.web.mvc.controller;

import org.springframework.roo.model.JavaPackage;

/**
 * Interface for {@link WebMvcOperationsImpl}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.1
 */
public interface WebMvcOperations {
	
	String OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME = "Spring OpenEntityManagerInViewFilter";
	
	String CHARACTER_ENCODING_FILTER_NAME =	"CharacterEncodingFilter";
	
	String HTTP_METHOD_FILTER_NAME = "HttpMethodFilter";

	void installMinmalWebArtefacts();
	
	void installAllWebMvcArtifacts();
	
	/**
	 * Installs and configures an application-wide FormattingConversionServiceFactoryBean that can be used
	 * to register application-specific Converters and Formatters. 
	 * 
	 * @param destinationPackage the package to install the conversion service class
	 */
	void installConversionService(JavaPackage destinationPackage);
	
	void registerWebFlowConversionServiceExposingInterceptor();
}