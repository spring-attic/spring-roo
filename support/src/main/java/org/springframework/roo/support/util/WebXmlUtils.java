package org.springframework.roo.support.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper util class to allow more convenient handling of web.xml file in Web projects.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public abstract class WebXmlUtils {
	
	/**
	 * Set the displayname element in the web.xml document.
	 * 
	 * @param displayName (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @return the document representing the adjusted web.xml
	 */
	public static void setDisplayName(String displayName, Document webXml, String comment) {
		Assert.hasText(displayName, "display name required");
		Assert.notNull(webXml, "Web XML document required");
		
		Element displayNameE = XmlUtils.findFirstElement("/web-app/display-name", webXml.getDocumentElement());
		if (displayNameE == null) {
			displayNameE = webXml.createElement("display-name");
			insertBetween(displayNameE, "the-start", "description", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(displayNameE, comment, webXml);
			}
		}
		displayNameE.setTextContent(displayName);
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
		
		Element descriptionE = XmlUtils.findFirstElement("/web-app/description", webXml.getDocumentElement());
		if (descriptionE == null) {
			descriptionE = webXml.createElement("description");
			insertBetween(descriptionE, "display-name", "context-param", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(descriptionE, comment, webXml);
			}
		}
		descriptionE.setTextContent(description);
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
			insertBetween(contextParamE, "description", "filter", webXml);	
			if (comment != null && comment.length() > 0) {
				addCommentBefore(contextParamE, comment, webXml);
			}
		}
		appendChildIfNotPresent(contextParamE, new XmlElementBuilder("param-value", webXml).setText(contextParam.getValue()).build(), webXml);
	}
	
	/**
	 * Add a new filter definition to web.xml document. The filter will be added AFTER (FilterPosition.LAST) all existing filters.
	 * 
	 * @param filterPosition Filter position (required)
	 * @param filterPositionFilterName (optional for filter position FIRST and LAST, required for BEFORE and AFTER)
	 * @param filterName (required)
	 * @param filterClass the fully qualified name of the filter type (required)
	 * @param urlPattern (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @param initParams (optional)
	 */
	public static void addFilter(String filterName, String filterClass, String urlPattern, Document webXml, String comment, WebXmlParam... initParams) {
		addFilterAtPosition(FilterPosition.LAST, null, filterName, filterClass, urlPattern, webXml, comment, initParams);
	}

	/**
	 * Add a new filter definition to web.xml document. The filter will be added at the FilterPosition specified.
	 * 
	 * @param filterPosition Filter position (required)
	 * @param filterPositionFilterName (optional for filter position FIRST and LAST, required for BEFORE and AFTER)
	 * @param filterName (required)
	 * @param filterClass the fully qualified name of the filter type (required)
	 * @param urlPattern (required)
	 * @param webXml the web.xml document (required)
	 * @param comment (optional)
	 * @param initParams (optional)
	 */
	public static void addFilterAtPosition(FilterPosition filterPosition, String filterPositionFilterName, String filterName, String filterClass, String urlPattern, Document webXml, String comment, WebXmlParam... initParams) {
		Assert.notNull(webXml, "Web XML document required");
		Assert.hasText(filterName, "Filter name required");
		Assert.hasText(filterClass, "Filter class required");
		Assert.notNull(urlPattern, "Filter URL mapping pattern required");
		
		//creating filter
		Element filter = XmlUtils.findFirstElement("/web-app/filter[filter-name = '" + filterName + "']", webXml.getDocumentElement());
		if (filter == null) {
			filter = new XmlElementBuilder("filter", webXml)
						.addChild(new XmlElementBuilder("filter-name", webXml).setText(filterName).build())
					.build();
			if (filterPosition.equals(FilterPosition.FIRST)) {
				insertBetween(filter, "context-param", "filter", webXml);
			} else if (filterPosition.equals(FilterPosition.BEFORE)) {
				Assert.hasText(filterPositionFilterName, "The filter position filter name is required when using FilterPosition.BEFORE");
				insertBefore(filter, "filter[filter-name = '" + filterPositionFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.AFTER)) {
				Assert.hasText(filterPositionFilterName, "The filter position filter name is required when using FilterPosition.AFTER");
				insertAfter(filter, "filter[filter-name = '" + filterPositionFilterName + "']", webXml);
			} else {
				insertBetween(filter, "context-param", "filter-mapping", webXml);
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
				Assert.hasText(filterPositionFilterName, "The filter position filter name is required when using FilterPosition.BEFORE");
				insertBefore(filter, "filter-mapping[filter-name = '" + filterPositionFilterName + "']", webXml);
			} else if (filterPosition.equals(FilterPosition.AFTER)) {
				Assert.hasText(filterPositionFilterName, "The filter position filter class name is required when using FilterPosition.AFTER");
				insertAfter(filter, "filter-mapping[filter-name = '" + filterPositionFilterName + "']", webXml);
			} else {
				insertBetween(filterMappingE, "filter", "listener", webXml);
			}
		}
		appendChildIfNotPresent(filterMappingE, new XmlElementBuilder("url-pattern", webXml).setText(urlPattern).build(), webXml);
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
			insertBetween(listener, "filter-mapping", "servlet", webXml);
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
			insertBetween(servlet, "listener", "servlet-mapping", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(servlet, comment, webXml);
			}
		}
		appendChildIfNotPresent(servlet, new XmlElementBuilder("servlet-class", webXml).setText(className).build(), webXml);
		if (loadOnStartup != null) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("load-on-startup", webXml).setText(loadOnStartup.toString()).build(), webXml);
		}
		for (WebXmlParam initParam: initParams) {
			appendChildIfNotPresent(servlet, new XmlElementBuilder("init-param", webXml)
							.addChild(new XmlElementBuilder("param-name", webXml).setText(initParam.getName()).build())
							.addChild(new XmlElementBuilder("param-value", webXml).setText(initParam.getValue()).build())
						.build(), webXml);
		}
		
		//create servlet mapping
		Element servletMapping = XmlUtils.findFirstElement("/web-app/servlet-mapping[servlet-name = '" + servletName + "']", webXml.getDocumentElement());
		if (servletMapping == null) {
			servletMapping = new XmlElementBuilder("servlet-mapping", webXml)
								.addChild(new XmlElementBuilder("servlet-name", webXml).setText(servletName).build())
							.build();
			insertBetween(servletMapping, "servlet", "session-config", webXml);
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
			insertBetween(sessionConfig, "servlet-mapping", "welcome-file-list", webXml);
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
			insertBetween(welcomeFile,"session-config", "error-page", webXml);
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
			insertBetween(errorPage, "welcome-file-list", "the-end", webXml);
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
			insertBetween(errorPage, "welcome-file-list", "the-end", webXml);
			if (comment != null && comment.length() > 0) {
				addCommentBefore(errorPage, comment, webXml);
			}
		}
		appendChildIfNotPresent(errorPage, new XmlElementBuilder("location", webXml).setText(location).build(), webXml);
	}
	
	private static void insertBetween(Element element, String afterElementName, String beforeElementName, Document doc) {
		Element beforeElement = XmlUtils.findFirstElement("/web-app/" + beforeElementName, doc.getDocumentElement());
		if (beforeElement != null) {
			doc.getDocumentElement().insertBefore(element, beforeElement);
			addLineBreakBefore(element, doc);
			addLineBreakBefore(element, doc);
			return;
		} 
		
		Element afterElement = XmlUtils.findFirstElement("/web-app/" + afterElementName + "[last()]", doc.getDocumentElement());
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
		FIRST, LAST, BEFORE, AFTER;
	}
}
