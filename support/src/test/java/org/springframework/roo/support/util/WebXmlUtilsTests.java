package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.roo.support.util.WebXmlUtils.WebXmlParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit tests for {@link WebXmlUtils}
 * 
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public class WebXmlUtilsTests {
	private static Document webXml;

	@BeforeClass
	public static void setUp() throws Exception {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		webXml = builder.newDocument();
		webXml.appendChild(webXml.createElement("web-app"));
	}

	@Test
	public void testSetDisplayName() {
		WebXmlUtils.setDisplayName("display", webXml, null);
		
		Element display = XmlUtils.findFirstElement("display-name", webXml.getDocumentElement());
		assertNotNull(display);
		assertEquals("display", display.getTextContent());
	}

	@Test
	public void testSetDescription() {
		WebXmlUtils.setDescription("test desc", webXml, null);
		
		Element desc = XmlUtils.findFirstElement("description", webXml.getDocumentElement());
		assertNotNull(desc);
		assertEquals("test desc", desc.getTextContent());
	}

	@Test
	public void testAddContextParam() {
		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("key", "value"), webXml, null);
		
		Element ctxp = XmlUtils.findFirstElement("context-param", webXml.getDocumentElement());
		assertNotNull(ctxp);
		assertEquals(2, ctxp.getChildNodes().getLength());
		assertEquals("key", XmlUtils.findFirstElement("param-name", ctxp).getTextContent());
		assertEquals("value", XmlUtils.findFirstElement("param-value", ctxp).getTextContent());
	}

	@Test
	public void testAddFilter() {
		WebXmlUtils.addFilter("filter1", String.class.getName(), "/*", webXml, null, new WebXmlUtils.WebXmlParam("key", "value"), new WebXmlUtils.WebXmlParam("key2", "value2"));
		
		Element filter = XmlUtils.findFirstElement("filter", webXml.getDocumentElement());
		assertNotNull(filter);
		assertEquals("filter1", XmlUtils.findFirstElement("filter-name", filter).getTextContent());
		assertEquals(String.class.getName(), XmlUtils.findFirstElement("filter-class", filter).getTextContent());
		Element filterMapping = XmlUtils.findFirstElement("filter-mapping", webXml.getDocumentElement());
		assertNotNull(filterMapping);
		assertEquals("filter1", XmlUtils.findFirstElement("filter-name", filterMapping).getTextContent());
		assertEquals("/*", XmlUtils.findFirstElement("url-pattern", filterMapping).getTextContent());
		List<Element> initParams = XmlUtils.findElements("init-param", filter);
		assertEquals(2, initParams.size());
		assertEquals(2, initParams.get(0).getChildNodes().getLength());
		assertEquals("key", XmlUtils.findFirstElement("param-name", initParams.get(0)).getTextContent());
		assertEquals("value", XmlUtils.findFirstElement("param-value", initParams.get(0)).getTextContent());
		assertEquals("key2", XmlUtils.findFirstElement("param-name", initParams.get(1)).getTextContent());
		assertEquals("value2", XmlUtils.findFirstElement("param-value", initParams.get(1)).getTextContent());
	}

	@Test
	public void testAddFilterAtPositionWithDispatcher() {
		WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BEFORE, null, "filter1", "filter2", Object.class.getName(), "/test", webXml, null, null, Arrays.asList(WebXmlUtils.Dispatcher.ERROR, WebXmlUtils.Dispatcher.INCLUDE, WebXmlUtils.Dispatcher.FORWARD, WebXmlUtils.Dispatcher.REQUEST));
		
		Element filter = XmlUtils.findFirstElement("filter", webXml.getDocumentElement());
		assertNotNull(filter);
		assertEquals("filter2", XmlUtils.findFirstElement("filter-name", filter).getTextContent());
		assertEquals(Object.class.getName(), XmlUtils.findFirstElement("filter-class", filter).getTextContent());
		Element filterMapping = XmlUtils.findFirstElement("filter-mapping", webXml.getDocumentElement());
		assertNotNull(filterMapping);
		assertEquals("filter2", XmlUtils.findFirstElement("filter-name", filterMapping).getTextContent());
		assertEquals("/test", XmlUtils.findFirstElement("url-pattern", filterMapping).getTextContent());
		List<Element> dispatchers = XmlUtils.findElements("dispatcher", filterMapping);
		assertEquals(4, dispatchers.size());
		assertEquals(WebXmlUtils.Dispatcher.ERROR.name(), dispatchers.get(0).getTextContent());
		assertEquals(WebXmlUtils.Dispatcher.INCLUDE.name(), dispatchers.get(1).getTextContent());
		assertEquals(WebXmlUtils.Dispatcher.FORWARD.name(), dispatchers.get(2).getTextContent());
		assertEquals(WebXmlUtils.Dispatcher.REQUEST.name(), dispatchers.get(3).getTextContent());
	}

	@Test
	public void testAddFilterAtPosition() {
		WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BETWEEN, "filter2", "filter1", "filter3", Integer.class.getName(), "/test2", webXml, null, (WebXmlParam[]) null);
	
		List<Element> filters = XmlUtils.findElements("filter", webXml.getDocumentElement());
		assertEquals(3, filters.size());
		assertEquals("filter2", XmlUtils.findFirstElement("filter-name", filters.get(0)).getTextContent());
		assertEquals("filter3", XmlUtils.findFirstElement("filter-name", filters.get(1)).getTextContent());
		assertEquals("filter1", XmlUtils.findFirstElement("filter-name", filters.get(2)).getTextContent());
		assertEquals(Integer.class.getName(), XmlUtils.findFirstElement("filter-class", filters.get(1)).getTextContent());
		List<Element> filterMappings = XmlUtils.findElements("filter-mapping", webXml.getDocumentElement());
		assertEquals(3, filterMappings.size());
		assertEquals("filter2", XmlUtils.findFirstElement("filter-name", filterMappings.get(0)).getTextContent());
		assertEquals("filter3", XmlUtils.findFirstElement("filter-name", filterMappings.get(1)).getTextContent());
		assertEquals("filter1", XmlUtils.findFirstElement("filter-name", filterMappings.get(2)).getTextContent());
		assertEquals("/test2", XmlUtils.findFirstElement("url-pattern", filterMappings.get(1)).getTextContent());
	}

	@Test
	public void testAddListener() {
		WebXmlUtils.addListener(String.class.getName(), webXml, null);
		
		Element listener = XmlUtils.findFirstElement("listener", webXml.getDocumentElement());
		assertNotNull(listener);
		assertEquals(String.class.getName(), XmlUtils.findFirstElement("listener-class", listener).getTextContent());
	}

	@Test
	public void testAddServlet() {
		WebXmlUtils.addServlet("servlet1", Object.class.getName(), "/servlet1", 1, webXml, null, new WebXmlUtils.WebXmlParam("key1", "value1"), new WebXmlUtils.WebXmlParam("key2", "value2"));
		
		Element servlet = XmlUtils.findFirstElement("servlet", webXml.getDocumentElement());
		assertNotNull(servlet);
		assertEquals("servlet1", XmlUtils.findFirstElement("servlet-name", servlet).getTextContent());
		assertEquals(Object.class.getName(), XmlUtils.findFirstElement("servlet-class", servlet).getTextContent());
		Element servletMapping = XmlUtils.findFirstElement("servlet-mapping", webXml.getDocumentElement());
		assertNotNull(servletMapping);
		assertEquals("servlet1", XmlUtils.findFirstElement("servlet-name", servletMapping).getTextContent());
		assertEquals("/servlet1", XmlUtils.findFirstElement("url-pattern", servletMapping).getTextContent());
		List<Element> initParams = XmlUtils.findElements("init-param", servlet);
		assertEquals(2, initParams.size());
		assertEquals(2, initParams.get(0).getChildNodes().getLength());
		assertEquals("key1", XmlUtils.findFirstElement("param-name", initParams.get(0)).getTextContent());
		assertEquals("value1", XmlUtils.findFirstElement("param-value", initParams.get(0)).getTextContent());
		assertEquals("key2", XmlUtils.findFirstElement("param-name", initParams.get(1)).getTextContent());
		assertEquals("value2", XmlUtils.findFirstElement("param-value", initParams.get(1)).getTextContent());
	}

	@Test
	public void testSetSessionTimeout() {
		WebXmlUtils.setSessionTimeout(1000, webXml, null);
		
		Element time = XmlUtils.findFirstElement("session-config/session-timeout", webXml.getDocumentElement());
		assertNotNull(time);
		assertEquals("1000", time.getTextContent());
	}

	@Test
	public void testAddWelcomeFile() {
		WebXmlUtils.addWelcomeFile("/welcome", webXml, null);
		
		Element welcomeFile = XmlUtils.findFirstElement("welcome-file-list/welcome-file", webXml.getDocumentElement());
		assertNotNull(welcomeFile);
		assertEquals("/welcome", welcomeFile.getTextContent());
	}

	@Test
	public void testAddExceptionType() {
		WebXmlUtils.addExceptionType(IllegalStateException.class.getName(), "/illegal", webXml, null);
		
		Element errorPage = XmlUtils.findFirstElement("error-page", webXml.getDocumentElement());
		assertNotNull(errorPage);
		assertEquals(2, errorPage.getChildNodes().getLength());
		assertEquals(IllegalStateException.class.getName(), XmlUtils.findFirstElement("exception-type", errorPage).getTextContent());
		assertEquals("/illegal", XmlUtils.findFirstElement("location", errorPage).getTextContent());
	}

	@Test
	public void testAddErrorCode() {
		WebXmlUtils.addErrorCode(404, "/404", webXml, null);
		
		Element errorPage = (Element) webXml.getDocumentElement().getChildNodes().item(webXml.getDocumentElement().getChildNodes().getLength() - 1);
		assertNotNull(errorPage);
		assertEquals(2, errorPage.getChildNodes().getLength());
		assertEquals("404", XmlUtils.findFirstElement("error-code", errorPage).getTextContent());
		assertEquals("/404", XmlUtils.findFirstElement("location", errorPage).getTextContent());
	}

	@Test
	public void testAddSecurityConstraint() {
		WebXmlUtils.addSecurityConstraint("displayName", 
				Arrays.asList(new WebXmlUtils.WebResourceCollection("web-resource-name", "description", Arrays.asList("/", "/2"), Arrays.asList("POST", "GET"))), 
				Arrays.asList("user", "supervisor"), "transportGuarantee", webXml, null);
		
		Element secConstraint = XmlUtils.findFirstElement("security-constraint", webXml.getDocumentElement());
		assertNotNull(secConstraint);
		assertEquals("displayName", XmlUtils.findFirstElement("display-name", secConstraint).getTextContent());
		Element webResourceCollection = XmlUtils.findFirstElement("web-resource-collection", secConstraint);
		assertNotNull(webResourceCollection);
		assertEquals("web-resource-name", XmlUtils.findFirstElement("web-resource-name", webResourceCollection).getTextContent());
		assertEquals(2, XmlUtils.findElements("url-pattern", webResourceCollection).size());
		assertEquals(2, XmlUtils.findElements("http-method", webResourceCollection).size());
		Element authConstraint = XmlUtils.findFirstElement("auth-constraint", secConstraint);
		assertNotNull(authConstraint);
		assertEquals(2, authConstraint.getChildNodes().getLength());
		Element userDataConstraint = XmlUtils.findFirstElement("user-data-constraint", secConstraint);
		assertNotNull(userDataConstraint);
		assertEquals("transportGuarantee", userDataConstraint.getElementsByTagName("transport-guarantee").item(0).getTextContent());
	}
	
	@Test
	public void validateElementSequence() {
		List<Element> contents = XmlUtils.findElements("/web-app/*", webXml.getDocumentElement());
		
		assertEquals(17, contents.size());
		
		assertEquals("display-name", contents.get(0).getNodeName());
		
		assertEquals("description", contents.get(1).getNodeName());
		
		assertEquals("context-param", contents.get(2).getNodeName());
		
		//filters
		assertEquals("filter2", contents.get(3).getChildNodes().item(0).getTextContent());
		assertEquals("filter3", contents.get(4).getChildNodes().item(0).getTextContent());
		assertEquals("filter1", contents.get(5).getChildNodes().item(0).getTextContent());
		
		//filter-mappings
		assertEquals("filter2", contents.get(6).getChildNodes().item(0).getTextContent());
		assertEquals("filter3", contents.get(7).getChildNodes().item(0).getTextContent());
		assertEquals("filter1", contents.get(8).getChildNodes().item(0).getTextContent());
		
		assertEquals("listener", contents.get(9).getNodeName());
		
		assertEquals("servlet", contents.get(10).getNodeName());
		assertEquals("servlet-mapping", contents.get(11).getNodeName());
		
		assertEquals("session-config", contents.get(12).getNodeName());
		
		assertEquals("welcome-file-list", contents.get(13).getNodeName());
		
		assertEquals("error-page", contents.get(14).getNodeName());
		
		assertEquals("error-page", contents.get(15).getNodeName());
		
		assertEquals("security-constraint", contents.get(16).getNodeName());
	}
}
