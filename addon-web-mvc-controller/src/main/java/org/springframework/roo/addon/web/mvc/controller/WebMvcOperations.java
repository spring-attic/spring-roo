package org.springframework.roo.addon.web.mvc.controller;

/**
 * Interface for {@link WebMvcOperationsImpl}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface WebMvcOperations {
	
	public static final String OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME = "Spring OpenEntityManagerInViewFilter";
	public static final String CHARACTER_ENCODING_FILTER_NAME =	"CharacterEncodingFilter";
	public static final String HTTP_METHOD_FILTER_NAME = "HttpMethodFilter";
	public static final String URL_REWRITE_FILTER_NAME = "UrlRewriteFilter";

	void installMinmalWebArtefacts();
	
	void installAllWebMvcArtifacts();

}