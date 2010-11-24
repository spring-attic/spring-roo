package org.springframework.roo.addon.web.mvc.controller;

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
}