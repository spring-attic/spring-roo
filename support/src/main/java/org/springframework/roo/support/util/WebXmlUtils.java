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
public abstract class WebXmlUtils {
	
	/**
	 * Set the display-name element in the web.xml document.
	 * 
	 * @param displayName (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 */
	public static void setDisplayName(String displayName, Document webXml, String comment) {
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
	public static void setDescription(String description, Document webXml, String comment) {
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
	public static void addContextParam(WebXmlParam contextParam, Document webXml, String comment) {
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
		appendChildIfNotPresent(contextParamE, new XmlElementBuilder("param-value", webXml).setText(contextParam.getValue()).build(), webXml);
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
	public static void addFilter(String filterName, String filterClass, String urlPattern, Document webXml, String comment, WebXmlParam... initParams) {
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
	public static void addFilterAtPosition(FilterPosition filterPosition, String afterFilterName, String beforeFilterName, String filterName, String filterClass, String urlPattern, Document webXml, String comment, WebXmlParam... initParams) {
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
	public static void addFilterAtPosition(FilterPosition filterPosition, String afterFilterName, String beforeFilterName, String filterName, String filterClass, String urlPattern, Document webXml, String comment, List<WebXmlParam> initParams, List<Dispatcher> dispatchers) {
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
		appendChildIfNotPresent(filter, new XmlElementBuilder("filter-class", webXml).setText(filterClass).build(), webXml);
		for (WebXmlParam initParam: initParams) {
			appendChildIfNotPresent(filter, new XmlElementBuilder("init-param", webXml)
									.addChild(new XmlElementBuilder("param-name", webXml).setText(initParam.getName()).build())
									.addChild(new XmlElementBuilder("param-value", webXml).setText(initParam.getValue()).build())
								.build(), webXml);
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
		appendChildIfNotPresent(filterMappingE, new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build(), webXml);
		for (Dispatcher dispatcher: dispatchers) {
			appendChildIfNotPresent(filterMappingE, new XmlElementBuilder("dispatcher", webXml).setText(dispatcher.name()).build(), webXml);
		}
	}
	
	/**
	 * Add listener element to web.xml document
	 * 
	 * @param className the fully qualified name of the listener type (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addListener(String className, Document webXml, String comment) {
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
	public static void addServlet(String servletName, String className, String urlPattern, Integer loadOnStartup, Document webXml, String comment, WebXmlParam... initParams) {
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
		appendChildIfNotPresent(servlet, new XmlElementBuilder("servlet-class", webXml).setText(className).build(), webXml);
		for (WebXmlParam initParam: initParams) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("init-param", webXml)
							.addChild(new XmlElementBuilder("param-name", webXml).setText(initParam.getName()).build())
							.addChild(new XmlElementBuilder("param-value", webXml).setText(initParam.getValue()).build())
						.build(), webXml);
		}
		if (loadOnStartup != null) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("load-on-startup", webXml).setText(loadOnStartup.toString()).build(), webXml);
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
			appendChildIfNotPresent(servletMapping, new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build(), webXml);
		} else {
			appendChildIfNotPresent(servletMapping, new XmlElementBuilder("servlet-name", webXml).setText(servletName).build(), webXml);
		}
	}
	
	/**
	 * Set session timeout in web.xml document
	 * 
	 * @param timeout (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void setSessionTimeout(Integer timeout, Document webXml, String comment) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.notNull(timeout, "Timeout required");
		
		Element sessionConfig = XmlUtils.findFirstElement("/web-app/session-config", webXml.getDocumentElement());
		if (sessionConfig == null) {
			sessionConfig = webXml.createElement("session-config");			
			insertBetween(sessionConfig, "servlet-mapping[last()]", "welcome-file-list", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(sessionConfig, comment, webXml);
			}
		}
		appendChildIfNotPresent(sessionConfig, new XmlElementBuilder("session-timeout", webXml).setText(timeout.toString()).build(), webXml);
	}
	
	/**
	 * Add a welcome file definition to web.xml document
	 * 
	 * @param path (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addWelcomeFile(String path, Document webXml, String comment) {
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
		appendChildIfNotPresent(welcomeFile, new XmlElementBuilder("welcome-file", webXml).setText(path).build(), webXml);
	}
	
	/**
	 * Add exception type to web.xml document
	 * 
	 * @param exceptionType fully qualified exception type name (required)
	 * @param location (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addExceptionType(String exceptionType, String location, Document webXml, String comment) {
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
		appendChildIfNotPresent(errorPage, new XmlElementBuilder("location", webXml).setText(location).build(), webXml);
	}
	
	/**
	 * Add error code to web.xml document
	 * 
	 * @param errorCode (required)
	 * @param location (required)
	 * @param webXml (required)
	 * @param comment (optional)
	 */
	public static void addErrorCode(Integer errorCode, String location, Document webXml, String comment) {
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
		appendChildIfNotPresent(errorPage, new XmlElementBuilder("location", webXml).setText(location).build(), webXml);
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
    public static void addSecurityConstraint(String displayName, List<WebResourceCollection> webResourceCollections, List<String> roleNames, String transportGuarantee, Document webXml, String comment) {
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
            appendChildIfNotPresent(securityConstraint, new XmlElementBuilder("display-name", webXml).setText(displayName).build(), webXml);
        }

        for (WebResourceCollection webResourceCollection :  webResourceCollections) {
            XmlElementBuilder webResourceCollectionBuilder = new XmlElementBuilder("web-resource-collection", webXml);
            Assert.hasText(webResourceCollection.getWebResourceName(), "web-resource-name is required");
            webResourceCollectionBuilder.addChild(new XmlElementBuilder("web-resource-name", webXml).setText(webResourceCollection.getWebResourceName()).build());
            if (StringUtils.hasText(webResourceCollection.getDescription())) {
                webResourceCollectionBuilder.addChild(new XmlElementBuilder("description", webXml).setText(webResourceCollection.getWebResourceName()).build());
            }
            for (String urlPattern : webResourceCollection.getUrlPatterns()) {
                if (StringUtils.hasText(urlPattern)) {
                    webResourceCollectionBuilder.addChild(new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build());
                }
            }
            for (String httpMethod : webResourceCollection.getHttpMethods()) {
                if (StringUtils.hasText(httpMethod)) {
                    webResourceCollectionBuilder.addChild(new XmlElementBuilder("http-method", webXml).setText(httpMethod).build());
                }
            }
            appendChildIfNotPresent(securityConstraint, webResourceCollectionBuilder.build(), webXml);
        }

        if (roleNames != null && roleNames.size() > 0) {
            XmlElementBuilder authConstraintBuilder = new XmlElementBuilder("auth-constraint", webXml);
            for (String roleName : roleNames) {
                if (StringUtils.hasText(roleName)) {
                    authConstraintBuilder.addChild(new XmlElementBuilder("role-name", webXml).setText(roleName).build());
                }
            }
            appendChildIfNotPresent(securityConstraint, authConstraintBuilder.build(), webXml);
        }

        if (StringUtils.hasText(transportGuarantee)) {
            XmlElementBuilder userDataConstraintBuilder = new XmlElementBuilder("user-data-constraint", webXml);
            userDataConstraintBuilder.addChild(new XmlElementBuilder("transport-guarantee", webXml).setText(transportGuarantee).build());
            appendChildIfNotPresent(securityConstraint, userDataConstraintBuilder.build(), webXml);
        }
    }
	
	private static void insertBetween(Element element, String afterElementName, String beforeElementName, Document doc) {
		Element beforeElement = XmlUtils.findFirstElement("/web-app/" + beforeElementName, doc.getDocumentElement());
		if (beforeElement != null) {
			doc.getDocumentElement().insertBefore(element, beforeElement);
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		} 
		
		Element afterElement = XmlUtils.findFirstElement("/web-app/" + afterElementName, doc.getDocumentElement());
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
	
	private static void insertBefore(Element element, String beforeElementName, Document doc) {
		Element beforeElement = XmlUtils.findFirstElement("/web-app/" + beforeElementName, doc.getDocumentElement());
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
	
	private static void insertAfter(Element element, String afterElementName, Document doc) {
		Element afterElement = XmlUtils.findFirstElement("/web-app/" + afterElementName, doc.getDocumentElement());
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
	
	private static void appendChildIfNotPresent(Element parent, Element child, Document doc) {
		NodeList childNodes = parent.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element element = (Element) node;
				//attempt matching of possibly nested structures by using of 'getTextContent' as 'isEqualNode' does not match due to line returns, etc
				//note, this does not work if child nodes are appearing in a different order than expected
				if (element.getNodeName().equals(child.getNodeName()) && element.getTextContent().replaceAll("[ \t\r\n]", "").trim().equals(child.getTextContent().replaceAll("[ \t\r\n]", ""))) {
					//if we found a match, there is no need to append the child element
					return;
				}
			}
		}
		parent.appendChild(child);
	}
	
	private static void addLineBreakBefore(Element element, Document doc){
		doc.getDocumentElement().insertBefore(doc.createTextNode("\n    "), element);
	}
	
	private static void addCommentBefore(Element element, String comment, Document doc) {
		if(null == XmlUtils.findNode("//comment()[.=' " + comment + " ']", doc.getDocumentElement())) {
			doc.getDocumentElement().insertBefore(doc.createComment(" " + comment + " "), element);
			addLineBreakBefore(element, doc);
		}
	}

	/**
	 * Value object used to hold init-param style information
	 *
	 * @author Stefan Schmidt
	 * @since 1.1
	 * 
	 */
	public static class WebXmlParam {
		
		private String name;
		private String value;
		
		public WebXmlParam(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
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

        private String webResourceName;
        private String description;
        private List<String> urlPatterns;
        private List<String> httpMethods;

        public WebResourceCollection(String webResourceName, String description, List<String> urlPatterns, List<String> httpMethods) {
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
}
