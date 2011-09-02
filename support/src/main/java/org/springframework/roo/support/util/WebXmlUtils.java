package org.springframework.roo.support.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper util class to allow more convenient handling of web.xml file in Web projects.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public final class WebXmlUtils {
	
	// Constants
	private static final String WHITESPACE = "[ \t\r\n]";
	
	/**
	 * Set the display-name element in the web.xml document.
	 * 
	 * @param displayName (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 */
	public static void setDisplayName(final String displayName, final Document webXml, final String comment) {
		Assert.hasText(displayName, "display name required");
		Assert.notNull(webXml, "Web XML document required");
		
		Element displayNameElement = XmlUtils.findFirstElement("/web-app/display-name", webXml.getDocumentElement());
		if (displayNameElement == null) {
			displayNameElement = webXml.createElement("display-name");
			insertBetween(displayNameElement, "the-start", "description", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(displayNameElement, comment, webXml);
			}
		}
		displayNameElement.setTextContent(displayName);
	}
	
	/**
	 * Set the description element in the web.xml document.
	 * 
	 * @param description (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 */
	public static void setDescription(final String description, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(description, "Description required");
		
		Element descriptionElement = XmlUtils.findFirstElement("/web-app/description", webXml.getDocumentElement());
		if (descriptionElement == null) {
			descriptionElement = webXml.createElement("description");
			insertBetween(descriptionElement, "display-name[last()]", "context-param", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(descriptionElement, comment, webXml);
			}
		}
		descriptionElement.setTextContent(description);
	}
	
	/**
	 * Add a context param to the web.xml document
	 * 
	 * @param contextParam (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 */
	public static void addContextParam(final WebXmlParam contextParam, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.notNull(contextParam, "Context param required");
		
		Element contextParamE = XmlUtils.findFirstElement("/web-app/context-param[param-name = '" + contextParam.getName() + "']", webXml.getDocumentElement());
		if (contextParamE == null) {
			contextParamE = new XmlElementBuilder("context-param", webXml)
						.addChild(new XmlElementBuilder("param-name", webXml).setText(contextParam.getName()).build())
					.build();
			insertBetween(contextParamE, "description[last()]", "filter", webXml);	
			if (comment != null && comment.length() > 0) {
				addCommentBefore(contextParamE, comment, webXml);
			}
		}
		appendChildIfNotPresent(contextParamE, new XmlElementBuilder("param-value", webXml).setText(contextParam.getValue()).build());
	}
	
	/**
	 * Add a new filter definition to web.xml document. The filter will be added AFTER (FilterPosition.LAST) all existing filters.
	 * 
	 * @param filterName (required)
	 * @param filterClass the fully qualified name of the filter type (required)
	 * @param urlPattern (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @param initParams a vararg of initial parameters (optional)
	 */
	public static void addFilter(final String filterName, final String filterClass, final String urlPattern, final Document webXml, final String comment, final WebXmlParam... initParams) {
		addFilterAtPosition(FilterPosition.LAST, null, null, filterName, filterClass, urlPattern, webXml, comment, initParams);
	}
	
	/**
	 * Add a new filter definition to web.xml document. The filter will be added at the FilterPosition specified.
	 * 
	 * @param filterPosition Filter position (required)
	 * @param beforeFilterName (optional for filter position FIRST and LAST, required for BEFORE and AFTER)
	 * @param filterName (required)
	 * @param filterClass the fully qualified name of the filter type (required)
	 * @param urlPattern (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @param initParams (optional)
	 */
	public static void addFilterAtPosition(final FilterPosition filterPosition, final String afterFilterName, final String beforeFilterName, final String filterName, final String filterClass, final String urlPattern, final Document webXml, final String comment, final WebXmlParam... initParams) {
		addFilterAtPosition(filterPosition, afterFilterName, beforeFilterName, filterName, filterClass, urlPattern, webXml, comment, initParams == null ? new ArrayList<WebXmlParam>() : Arrays.asList(initParams), new ArrayList<Dispatcher>());
	}

	/**
	 * Add a new filter definition to web.xml document. The filter will be added at the FilterPosition specified.
	 * 
	 * @param filterPosition Filter position (required)
	 * @param beforeFilterName (optional for filter position FIRST and LAST, required for BEFORE and AFTER)
	 * @param filterName (required)
	 * @param filterClass the fully qualified name of the filter type (required)
	 * @param urlPattern (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @param initParams (optional)
	 * @param dispatchers (optional)
	 */
	public static void addFilterAtPosition(final FilterPosition filterPosition, final String afterFilterName, final String beforeFilterName, final String filterName, final String filterClass, final String urlPattern, final Document webXml, final String comment, List<WebXmlParam> initParams, final List<Dispatcher> dispatchers) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(filterName, "Filter name required");
		Assert.hasText(filterClass, "Filter class required");
		Assert.notNull(urlPattern, "Filter URL mapping pattern required");
		
		if (initParams == null) {
			initParams = new ArrayList<WebXmlUtils.WebXmlParam>();
		}
		
		//creating filter
		Element filter = XmlUtils.findFirstElement("/web-app/filter[filter-name = '" + filterName + "']", webXml.getDocumentElement());
		if (filter == null) {
			filter = new XmlElementBuilder("filter", webXml)
						.addChild(new XmlElementBuilder("filter-name", webXml).setText(filterName).build())
					.build();
			if (filterPosition.equals(FilterPosition.FIRST)) {
				insertBetween(filter, "context-param", "filter", webXml);
			} else if (filterPosition.equals(FilterPosition.BEFORE)) {
				Assert.hasText(beforeFilterName, "The filter position filter name is required when using FilterPosition.BEFORE");
				insertBefore(filter, "filter[filter-name = '" + beforeFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.AFTER)) {
				Assert.hasText(afterFilterName, "The filter position filter name is required when using FilterPosition.AFTER");
				insertAfter(filter, "filter[filter-name = '" + afterFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.BETWEEN)) {
				Assert.hasText(beforeFilterName, "The 'before' filter name is required when using FilterPosition.BETWEEN");
				Assert.hasText(afterFilterName, "The 'after' filter name is required when using FilterPosition.BETWEEN");
				insertBetween(filter, "filter[filter-name = '" + afterFilterName + "']", "filter[filter-name = '" + beforeFilterName + "']", webXml);
			} else {
				insertBetween(filter, "context-param[last()]", "filter-mapping", webXml);
			}
			if (comment != null && comment.length() > 0) {
				addCommentBefore(filter, comment, webXml);
			}
		}
		appendChildIfNotPresent(filter, new XmlElementBuilder("filter-class", webXml).setText(filterClass).build());
		for (final WebXmlParam initParam: initParams) {
			appendChildIfNotPresent(filter, new XmlElementBuilder("init-param", webXml)
									.addChild(new XmlElementBuilder("param-name", webXml).setText(initParam.getName()).build())
									.addChild(new XmlElementBuilder("param-value", webXml).setText(initParam.getValue()).build())
								.build());
		}
		
		//creating filter mapping
		Element filterMappingE = XmlUtils.findFirstElement("/web-app/filter-mapping[filter-name = '" + filterName + "']", webXml.getDocumentElement());
		if (filterMappingE == null) {
			filterMappingE = new XmlElementBuilder("filter-mapping", webXml)
								.addChild(new XmlElementBuilder("filter-name", webXml).setText(filterName).build())
							.build();
			if (filterPosition.equals(FilterPosition.FIRST)) {
				insertBetween(filterMappingE, "filter", "filter-mapping", webXml);
			} else if (filterPosition.equals(FilterPosition.BEFORE)) {
				insertBefore(filterMappingE, "filter-mapping[filter-name = '" + beforeFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.AFTER)) {
				insertAfter(filterMappingE, "filter-mapping[filter-name = '" + beforeFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.BETWEEN)) {
				insertBetween(filterMappingE, "filter-mapping[filter-name = '" + afterFilterName + "']", "filter-mapping[filter-name = '" + beforeFilterName + "']", webXml);
			} else {
				insertBetween(filterMappingE, "filter-mapping[last()]", "listener", webXml);
			}
		}
		appendChildIfNotPresent(filterMappingE, new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build());
		for (final Dispatcher dispatcher: dispatchers) {
			appendChildIfNotPresent(filterMappingE, new XmlElementBuilder("dispatcher", webXml).setText(dispatcher.name()).build());
		}
	}
	
	/**
	 * Add listener element to web.xml document
	 * 
	 * @param className the fully qualified name of the listener type (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addListener(final String className, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(className, "Class name required");
		
		Element listener = XmlUtils.findFirstElement("/web-app/listener[listener-class = '" + className + "']", webXml.getDocumentElement());
		if (listener == null) {
			listener = new XmlElementBuilder("listener", webXml)
						.addChild(new XmlElementBuilder("listener-class", webXml).setText(className).build())
					.build();
			insertBetween(listener, "filter-mapping[last()]", "servlet", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(listener, comment, webXml);
			}
		}
	}
	
	/**
	 * Add servlet element to the web.xml document
	 *  
	 * @param servletName (required)
	 * @param className the fully qualified name of the servlet type (required)
	 * @param urlPattern this can be set to null in which case the servletName will be used for mapping (optional)
	 * @param loadOnStartup (optional)
	 * @param webXml (required)
	 * @param comment (optional)
	 * @param initParams (optional)
	 */
	public static void addServlet(final String servletName, final String className, final String urlPattern, final Integer loadOnStartup, final Document webXml, final String comment, final WebXmlParam... initParams) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(servletName, "Servlet name required");
		Assert.hasText(className, "Fully qualified class name required");

		//create servlet
		Element servlet = XmlUtils.findFirstElement("/web-app/servlet[servlet-name = '" + servletName + "']", webXml.getDocumentElement());
		if (servlet == null) {
			servlet = new XmlElementBuilder("servlet", webXml)
						.addChild(new XmlElementBuilder("servlet-name", webXml).setText(servletName).build())
					.build();
			insertBetween(servlet, "listener[last()]", "servlet-mapping", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(servlet, comment, webXml);
			}
		}
		appendChildIfNotPresent(servlet, new XmlElementBuilder("servlet-class", webXml).setText(className).build());
		for (final WebXmlParam initParam: initParams) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("init-param", webXml)
							.addChild(new XmlElementBuilder("param-name", webXml).setText(initParam.getName()).build())
							.addChild(new XmlElementBuilder("param-value", webXml).setText(initParam.getValue()).build())
						.build());
		}
		if (loadOnStartup != null) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("load-on-startup", webXml).setText(loadOnStartup.toString()).build());
		}
		
		//create servlet mapping
		Element servletMapping = XmlUtils.findFirstElement("/web-app/servlet-mapping[servlet-name = '" + servletName + "']", webXml.getDocumentElement());
		if (servletMapping == null) {
			servletMapping = new XmlElementBuilder("servlet-mapping", webXml)
								.addChild(new XmlElementBuilder("servlet-name", webXml).setText(servletName).build())
							.build();
			insertBetween(servletMapping, "servlet[last()]", "session-config", webXml);
		}
		if (urlPattern != null && urlPattern.length() > 0) {
			appendChildIfNotPresent(servletMapping, new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build());
		} else {
			appendChildIfNotPresent(servletMapping, new XmlElementBuilder("servlet-name", webXml).setText(servletName).build());
		}
	}
	
	/**
	 * Set session timeout in web.xml document
	 * 
	 * @param timeout
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void setSessionTimeout(final int timeout, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.notNull(timeout, "Timeout required");
		
		Element sessionConfig = XmlUtils.findFirstElement("/web-app/session-config", webXml.getDocumentElement());
		if (sessionConfig == null) {
			sessionConfig = webXml.createElement("session-config");			
			insertBetween(sessionConfig, "servlet-mapping[last()]", "welcome-file-list", webXml);
			if (StringUtils.hasText(comment)) {
				addCommentBefore(sessionConfig, comment, webXml);
			}
		}
		appendChildIfNotPresent(sessionConfig, new XmlElementBuilder("session-timeout", webXml).setText(String.valueOf(timeout)).build());
	}
	
	/**
	 * Add a welcome file definition to web.xml document
	 * 
	 * @param path (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addWelcomeFile(final String path, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText("Path required");
		
		Element welcomeFile = XmlUtils.findFirstElement("/web-app/welcome-file-list", webXml.getDocumentElement());
		if (welcomeFile == null) {
			welcomeFile = webXml.createElement("welcome-file-list");
			insertBetween(welcomeFile,"session-config[last()]", "error-page", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(welcomeFile, comment, webXml);
			}
		}
		appendChildIfNotPresent(welcomeFile, new XmlElementBuilder("welcome-file", webXml).setText(path).build());
	}
	
	/**
	 * Add exception type to web.xml document
	 * 
	 * @param exceptionType fully qualified exception type name (required)
	 * @param location (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addExceptionType(final String exceptionType, final String location, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(exceptionType, "Fully qualified exception type name required");
		Assert.hasText(location, "location required");
		
		Element errorPage = XmlUtils.findFirstElement("/web-app/error-page[exception-type = '" + exceptionType + "']", webXml.getDocumentElement());
		if (errorPage == null) {
			errorPage = new XmlElementBuilder("error-page", webXml)
								.addChild(new XmlElementBuilder("exception-type", webXml).setText(exceptionType).build())
							.build();
			insertBetween(errorPage, "welcome-file-list[last()]", "the-end", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(errorPage, comment, webXml);
			}
		}
		appendChildIfNotPresent(errorPage, new XmlElementBuilder("location", webXml).setText(location).build());
	}
	
	/**
	 * Add error code to web.xml document
	 * 
	 * @param errorCode (required)
	 * @param location (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addErrorCode(final Integer errorCode, final String location, final Document webXml, final String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.notNull(errorCode, "Error code required");
		Assert.hasText(location, "Location required");
		
		Element errorPage = XmlUtils.findFirstElement("/web-app/error-page[error-code = '" + errorCode.toString() + "']", webXml.getDocumentElement());
		if (errorPage == null) {
			errorPage = new XmlElementBuilder("error-page", webXml)
								.addChild(new XmlElementBuilder("error-code", webXml).setText(errorCode.toString()).build())
							.build();
			insertBetween(errorPage, "welcome-file-list[last()]", "the-end", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(errorPage, comment, webXml);
			}
		}
		appendChildIfNotPresent(errorPage, new XmlElementBuilder("location", webXml).setText(location).build());
	}

     /**
     * Add a security constraint to a web.xml document
     *
     * @param displayName (optional)
     * @param webResourceCollections (required)
     * @param roleNames (optional)
     * @param transportGuarantee (optional)
     * @param webXml (required)
     * @param comment (optional)
     * */
    public static void addSecurityConstraint(final String displayName, final List<WebResourceCollection> webResourceCollections, final List<String> roleNames, final String transportGuarantee, final Document webXml, final String comment) {
        Assert.notNull(webXml, "Web XML document required");
        Assert.notNull(webResourceCollections, "A security-constraint element must contain at least one web-resource-collection");
        Assert.isTrue(webResourceCollections.size() > 0, "A security-constraint element must contain at least one web-resource-collection");

        Element securityConstraint = XmlUtils.findFirstElement("security-constraint", webXml.getDocumentElement());
        if (securityConstraint == null) {
            securityConstraint = webXml.createElement("security-constraint");
            insertAfter(securityConstraint, "session-config[last()]", webXml);
            if (StringUtils.hasText(comment)) {
				addCommentBefore(securityConstraint, comment, webXml);
			}
        }

        if (StringUtils.hasText(displayName)) {
            appendChildIfNotPresent(securityConstraint, new XmlElementBuilder("display-name", webXml).setText(displayName).build());
        }

        for (final WebResourceCollection webResourceCollection :  webResourceCollections) {
            final XmlElementBuilder webResourceCollectionBuilder = new XmlElementBuilder("web-resource-collection", webXml);
            Assert.hasText(webResourceCollection.getWebResourceName(), "web-resource-name is required");
            webResourceCollectionBuilder.addChild(new XmlElementBuilder("web-resource-name", webXml).setText(webResourceCollection.getWebResourceName()).build());
            if (StringUtils.hasText(webResourceCollection.getDescription())) {
                webResourceCollectionBuilder.addChild(new XmlElementBuilder("description", webXml).setText(webResourceCollection.getWebResourceName()).build());
            }
            for (final String urlPattern : webResourceCollection.getUrlPatterns()) {
                if (StringUtils.hasText(urlPattern)) {
                    webResourceCollectionBuilder.addChild(new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build());
                }
            }
            for (final String httpMethod : webResourceCollection.getHttpMethods()) {
                if (StringUtils.hasText(httpMethod)) {
                    webResourceCollectionBuilder.addChild(new XmlElementBuilder("http-method", webXml).setText(httpMethod).build());
                }
            }
            appendChildIfNotPresent(securityConstraint, webResourceCollectionBuilder.build());
        }

        if (roleNames != null && roleNames.size() > 0) {
            final XmlElementBuilder authConstraintBuilder = new XmlElementBuilder("auth-constraint", webXml);
            for (final String roleName : roleNames) {
                if (StringUtils.hasText(roleName)) {
                    authConstraintBuilder.addChild(new XmlElementBuilder("role-name", webXml).setText(roleName).build());
                }
            }
            appendChildIfNotPresent(securityConstraint, authConstraintBuilder.build());
        }

        if (StringUtils.hasText(transportGuarantee)) {
            final XmlElementBuilder userDataConstraintBuilder = new XmlElementBuilder("user-data-constraint", webXml);
            userDataConstraintBuilder.addChild(new XmlElementBuilder("transport-guarantee", webXml).setText(transportGuarantee).build());
            appendChildIfNotPresent(securityConstraint, userDataConstraintBuilder.build());
        }
    }
	
	private static void insertBetween(final Element element, final String afterElementName, final String beforeElementName, final Document doc) {
		final Element beforeElement = XmlUtils.findFirstElement("/web-app/" + beforeElementName, doc.getDocumentElement());
		if (beforeElement != null) {
			doc.getDocumentElement().insertBefore(element, beforeElement);
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		} 
		
		final Element afterElement = XmlUtils.findFirstElement("/web-app/" + afterElementName, doc.getDocumentElement());
		if (afterElement != null && afterElement.getNextSibling() != null && afterElement.getNextSibling() instanceof Element) {
			doc.getDocumentElement().insertBefore(element, afterElement.getNextSibling());
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		}
		
		doc.getDocumentElement().appendChild(element);
		addLineBreakBefore(element, doc);
		addLineBreakBefore(element, doc);
	}
	
	private static void insertBefore(final Element element, final String beforeElementName, final Document doc) {
		final Element beforeElement = XmlUtils.findFirstElement("/web-app/" + beforeElementName, doc.getDocumentElement());
		if (beforeElement != null) {
			doc.getDocumentElement().insertBefore(element, beforeElement);
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		} 
		doc.getDocumentElement().appendChild(element);
		addLineBreakBefore(element, doc);
		addLineBreakBefore(element, doc);
	}
	
	private static void insertAfter(final Element element, final String afterElementName, final Document doc) {
		final Element afterElement = XmlUtils.findFirstElement("/web-app/" + afterElementName, doc.getDocumentElement());
		if (afterElement != null && afterElement.getNextSibling() != null && afterElement.getNextSibling() instanceof Element) {
			doc.getDocumentElement().insertBefore(element, afterElement.getNextSibling());
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		}
		doc.getDocumentElement().appendChild(element);
		addLineBreakBefore(element, doc);
		addLineBreakBefore(element, doc);
	}

	/**
	 * Adds the given child to the given parent if it's not already there
	 * 
	 * @param parent the parent to which to add a child (required)
	 * @param child the child to add if not present (required)
	 */
	private static void appendChildIfNotPresent(final Node parent, final Element child) {
		final NodeList existingChildren = parent.getChildNodes();
		for (int i = 0; i < existingChildren.getLength(); i++) {
			final Node existingChild = existingChildren.item(i);
			if (existingChild instanceof Element) {
				//attempt matching of possibly nested structures by using of 'getTextContent' as 'isEqualNode' does not match due to line returns, etc
				//note, this does not work if child nodes are appearing in a different order than expected
				if (existingChild.getNodeName().equals(child.getNodeName()) && existingChild.getTextContent().replaceAll(WHITESPACE, "").trim().equals(child.getTextContent().replaceAll(WHITESPACE, ""))) {
					//if we found a match, there is no need to append the child element
					return;
				}
			}
		}
		parent.appendChild(child);
	}
	
	private static void addLineBreakBefore(final Element element, final Document doc){
		doc.getDocumentElement().insertBefore(doc.createTextNode("\n    "), element);
	}
	
	private static void addCommentBefore(final Element element, final String comment, final Document doc) {
		if (null == XmlUtils.findNode("//comment()[.=' " + comment + " ']", doc.getDocumentElement())) {
			doc.getDocumentElement().insertBefore(doc.createComment(" " + comment + " "), element);
			addLineBreakBefore(element, doc);
		}
	}

	/**
	 * Value object that holds init-param style information
	 *
	 * @author Stefan Schmidt
	 * @since 1.1
	 */
	public static class WebXmlParam extends Pair<String, String> {

		/**
		 * Constructor
		 *
		 * @param name
		 * @param value
		 */
		public WebXmlParam(final String name, final String value) {
			super(name, value);
		}

		/**
		 * Returns the name of this parameter
		 * 
		 * @return
		 */
		public String getName() {
			return getKey();
		}
	}
	
	/**
	 * Enum to define filter position
	 * 
	 * @author Stefan Schmidt
	 * @since 1.1
	 *
	 */
	public static enum FilterPosition {
		FIRST, LAST, BEFORE, AFTER, BETWEEN;
	}
	
	/**
	 * Enum to define dispatcher
	 * 
	 * @author Stefan Schmidt
	 * @since 1.1.1
	 *
	 */
	public static enum Dispatcher {
		FORWARD, REQUEST, INCLUDE, ERROR;
	}

    /**
     * Convenience class for passing a web-resource-collection element's details
     * @since 1.1.1
     */
    public static class WebResourceCollection {

        private final String webResourceName;
        private final String description;
        private final List<String> urlPatterns;
        private final List<String> httpMethods;

        public WebResourceCollection(final String webResourceName, final String description, final List<String> urlPatterns, final List<String> httpMethods) {
            this.webResourceName = webResourceName;
            this.description = description;
            this.urlPatterns = urlPatterns;
            this.httpMethods = httpMethods;
        }

        public String getWebResourceName() {
            return webResourceName;
        }

        public List<String> getUrlPatterns() {
            return urlPatterns;
        }

        public List<String> getHttpMethods() {
            return httpMethods;
        }

        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Constructor is private to prevent instantiation
     */
    private WebXmlUtils() {}
}
