package org.springframework.roo.addon.web.selenium;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.web.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.controller.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides property file configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class SeleniumOperations {
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private BeanInfoMetadata beanInfoMetadata;
	private MenuOperations menuOperations;
	private SimpleDateFormat simpleDateFormat;
	
	private Logger logger = Logger.getLogger(SeleniumOperations.class.getName());
	
	public SeleniumOperations(MetadataService metadataService, FileManager fileManager, PathResolver pathResolver, MenuOperations menuOperations) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(menuOperations, "Menu operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.menuOperations = menuOperations;
	}
	
	/**
	 * Creates a new Selenium testcase
	 * 
	 * @param controller the JavaType of the controller under test (required)
	 * @param name the name of the test case (optional)
	 */
	public void generateTest(JavaType controller, String name, String serverURL) {
		Assert.notNull(controller, "Controller type required");	
		
		String webScaffoldMetadataIdentifier = WebScaffoldMetadata.createIdentifier(controller, Path.SRC_MAIN_JAVA);
		
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataIdentifier);
		Assert.notNull(webScaffoldMetadata, "Web controller '" + controller.getFullyQualifiedTypeName()  + "' does not appear to be an automatic, scaffolded controller");
		this.beanInfoMetadata = (BeanInfoMetadata) metadataService.get(webScaffoldMetadata.getIdentifierForBeanInfoMetadata());
		
		this.simpleDateFormat = webScaffoldMetadata.getAnnotationValues().getDateFormat();
		
		//we abort the creation of a selenium test if the controller does not allow the creation of new instances for the form backing object
		if (!webScaffoldMetadata.getAnnotationValues().isCreate()) {
			logger.warning("The controller you specified does not allow the creation of new instances of the form backing object. No Selenium tests created.");
			return;
		}
		
		if(!serverURL.endsWith("/")) {
			serverURL = serverURL + "/";
		}
		
		String relativeTestFilePath = "selenium/test-" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + ".xhtml";
		String seleniumPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, relativeTestFilePath);
		MutableFile seleniumMutableFile = null;
		
		name = (name != null ? name : "Selenium test for " + controller.getSimpleTypeName());
		
		Document selenium;
		try {
			if (fileManager.exists(seleniumPath)) {
				seleniumMutableFile = fileManager.updateFile(seleniumPath);
				selenium = XmlUtils.getDocumentBuilder().parse(seleniumMutableFile.getInputStream());
			} else {
				seleniumMutableFile = fileManager.createFile(seleniumPath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "selenium-template.xhtml");
				Assert.notNull(templateInputStream, "Could not acquire selenium.xhtml template");
				selenium = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");
		
		Element root = (Element) selenium.getLastChild();
		
		if(root == null || !"html".equals(root.getNodeName())) {
			throw new IllegalArgumentException("Could not parse selenium test case template file!");
		}		
		
		XmlUtils.findRequiredElement("/html/head/title", root).setTextContent(name);
		
		XmlUtils.findRequiredElement("/html/body/table/thead/tr/td", root).setTextContent(name);

		Element tbody = XmlUtils.findRequiredElement("/html/body/table/tbody", root);
		
		tbody.appendChild(openCommand(selenium, serverURL + projectMetadata.getProjectName() + "/" + webScaffoldMetadata.getAnnotationValues().getPath() + "/form"));							
//		tbody.appendChild(clickAndWaitCommand(selenium, "link=Create new " + beanInfoMetadata.getJavaBean().getSimpleTypeName()));		
		
		for (FieldMetadata field : getEligableFields()) {
			if (!field.getFieldType().isCommonCollectionType() && !isSpecialType(field.getFieldType())) {
				tbody.appendChild(typeCommand(selenium, field));
			} else {				
//				tbody.appendChild(typeKeyCommand(selenium, field));
			}
		}

		tbody.appendChild(clickAndWaitCommand(selenium, "//input[@id='proceed']" ));	
		
		XmlUtils.writeXml(seleniumMutableFile.getOutputStream(), selenium);
		
		manageTestSuite(relativeTestFilePath, name, serverURL);
		
		installMavenPlugin();
	}
	
	private void manageTestSuite(String testPath, String name, String serverURL) {
		
		String relativeTestFilePath = "selenium/test-suite.xhtml";
		String seleniumPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, relativeTestFilePath);
		MutableFile seleniumMutableFile = null;
		
		Document suite;
		try {
			if (fileManager.exists(seleniumPath)) {
				seleniumMutableFile = fileManager.updateFile(seleniumPath);
				suite = XmlUtils.getDocumentBuilder().parse(seleniumMutableFile.getInputStream());
			} else {
				seleniumMutableFile = fileManager.createFile(seleniumPath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "selenium-test-suite-template.xhtml");
				Assert.notNull(templateInputStream, "Could not acquire selenium test suite template");
				suite = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");
		
		Element root = (Element) suite.getLastChild();
		
		XmlUtils.findRequiredElement("/html/head/title", root).setTextContent("Test suite for " + projectMetadata.getProjectName() + "project");

		Element tr = suite.createElement("tr");
		Element td = suite.createElement("td");
		tr.appendChild(td);
		Element a = suite.createElement("a");
		a.setAttribute("href", serverURL + projectMetadata.getProjectName() + "/static/" + testPath);
		a.setTextContent(name);
		td.appendChild(a);
		
		XmlUtils.findRequiredElement("/html/body/table", root).appendChild(tr);
		
		XmlUtils.writeXml(seleniumMutableFile.getOutputStream(), suite);
		
		menuOperations.addMenuItem(
				"selenium_category", 
				new JavaSymbolName("SeleniumTests"), 
				"selenium_test_suite_menu_item", 
				new JavaSymbolName("Test"),
				"selenium.menu.test.suite",
				"/static/" + relativeTestFilePath);		
	}
	
	private void installMavenPlugin(){
		String pomFilePath = "pom.xml";
		String pomPath = pathResolver.getIdentifier(Path.ROOT, pomFilePath);
		MutableFile pomMutableFile = null;
		
		Document pom;
		try {
			if (fileManager.exists(pomPath)) {
				pomMutableFile = fileManager.updateFile(pomPath);
				pom = XmlUtils.getDocumentBuilder().parse(pomMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("This command cannot be run before a project has been created.");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) pom.getLastChild();
		
		//stop if the plugin is already installed
		if (XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId='selenium-maven-plugin']", root) != null) {
			return;
		}
		
		Element dependencies = XmlUtils.findRequiredElement("/project/dependencies", root);
		Assert.notNull(dependencies, "Could not find the first dependencies element in pom.xml");

		//now install the plugin itself
		Element plugin = pom.createElement("plugin");
		Element groupId = pom.createElement("groupId");
		groupId.setTextContent("org.codehaus.mojo");
		plugin.appendChild(groupId);
		Element artifactId = pom.createElement("artifactId");
		artifactId.setTextContent("selenium-maven-plugin");
		plugin.appendChild(artifactId);
		Element version = pom.createElement("version");
		version.setTextContent("1.0");
		plugin.appendChild(version);
		Element configuration = pom.createElement("configuration");
		Element suite = pom.createElement("suite");
		String suitePath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "selenium/test-suite.xhtml");
		suitePath = suitePath.substring(pathResolver.getRoot(Path.ROOT).length()+1);
		suite.setTextContent(suitePath);
		configuration.appendChild(suite);
		Element browser = pom.createElement("browser");
		browser.setTextContent("*firefox");
		configuration.appendChild(browser);
		Element results = pom.createElement("results");
		results.setTextContent("${project.build.directory}/target/selenium.txt");
		configuration.appendChild(results);
		Element startURL = pom.createElement("startURL");
		startURL.setTextContent("http://localhost:4444/");
		configuration.appendChild(startURL);
		plugin.appendChild(configuration);
		
		XmlUtils.findRequiredElement("/project/build/plugins", root).appendChild(plugin);

		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
	}
	
	private List<FieldMetadata> getEligableFields() {
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		for (MethodMetadata method : beanInfoMetadata.getPublicAccessors(false)) {
			JavaSymbolName propertyName = beanInfoMetadata.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
			
			if(field != null && hasMutator(field)) {
				
				// Never include id field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null) {
					continue;
				}
				// Never include version field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
					continue;
				}				
				fields.add(field);
			}
		}
		return fields;
	}	
	
	private boolean hasMutator(FieldMetadata fieldMetadata) {
		for (MethodMetadata mutator : beanInfoMetadata.getPublicMutators()) {
			if (fieldMetadata.equals(beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}
	
	private Node clickAndWaitCommand(Document document, String linkTarget){
		Node tr = document.createElement("tr");				
		
		Node td1 = tr.appendChild(document.createElement("td"));
		td1.setTextContent("clickAndWait");
		
		Node td2 = tr.appendChild(document.createElement("td"));
		td2.setTextContent(linkTarget);
		
		Node td3 = tr.appendChild(document.createElement("td"));
		td3.setTextContent(" ");
		
		return tr;
	}
	
	private Node typeCommand(Document document, FieldMetadata field){
		Node tr = document.createElement("tr");				
		
		Node td1 = tr.appendChild(document.createElement("td"));
		td1.setTextContent("type");
		
		Node td2 = tr.appendChild(document.createElement("td"));
		td2.setTextContent("_" + field.getFieldName().getSymbolName());		
		
		Node td3 = tr.appendChild(document.createElement("td"));	
		td3.setTextContent(convertToInitializer(field));		
			
		return tr;
	}
	
//	private Node typeKeyCommand(Document document, FieldMetadata field){
//		Node tr = document.createElement("tr");				
//		
//		Node td1 = tr.appendChild(document.createElement("td"));
//		td1.setTextContent("typeKey");
//		
//		Node td2 = tr.appendChild(document.createElement("td"));
//		td2.setTextContent(field.getFieldName().getSymbolName());		
//		
//		Node td3 = tr.appendChild(document.createElement("td"));	
//		td3.setTextContent("1");		
//			
//		return tr;
//	}
//	
	private String convertToInitializer(FieldMetadata field) {
		String initializer = " ";
		short index = 1;
		if (field.getFieldName().getSymbolName().contains("email") || field.getFieldName().getSymbolName().contains("Email")) {
			initializer = "some@email.com";
		} else if (field.getFieldType().equals(new JavaType(String.class.getName()))) {
			initializer = "some" + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + index;
		} else if (field.getFieldType().equals(new JavaType(Date.class.getName())) ||
				field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
			Calendar cal = Calendar.getInstance();
			if (null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past"))) {
				cal.add(Calendar.YEAR, -1);
				cal.add(Calendar.MONTH, -1);
				cal.add(Calendar.DAY_OF_MONTH, -1);
				initializer = simpleDateFormat.format(cal.getTime());
			} else if (null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future"))) {
				cal.add(Calendar.YEAR, +1);
				cal.add(Calendar.MONTH, +1);
				cal.add(Calendar.DAY_OF_MONTH, +1);
				initializer = simpleDateFormat.format(cal.getTime());
			} else {
				initializer = simpleDateFormat.format(cal.getTime());
			}
		} else if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) || field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {		
			initializer = new Boolean(true).toString();
		} else if (field.getFieldType().equals(JavaType.INT_OBJECT) || field.getFieldType().equals(JavaType.INT_PRIMITIVE)) {
			initializer = new Integer(index).toString();
		} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT) || field.getFieldType().equals(JavaType.DOUBLE_PRIMITIVE)) {
			initializer = new Double(index).toString();
		} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT) || field.getFieldType().equals(JavaType.FLOAT_PRIMITIVE)) {
			initializer = new Float(index).toString();
		} else if (field.getFieldType().equals(JavaType.LONG_OBJECT) || field.getFieldType().equals(JavaType.LONG_PRIMITIVE)) {
			initializer = new Long(index).toString();
		} else if (field.getFieldType().equals(JavaType.SHORT_OBJECT) || field.getFieldType().equals(JavaType.SHORT_PRIMITIVE)) {
			initializer = new Short(index).toString();
		} 
		return initializer;		
	}
	
	private Node openCommand(Document document, String linkTarget){
		Node tr = document.createElement("tr");				
		
		Node td1 = tr.appendChild(document.createElement("td"));
		td1.setTextContent("open");
		
		Node td2 = tr.appendChild(document.createElement("td"));
		td2.setTextContent(linkTarget);
		
		Node td3 = tr.appendChild(document.createElement("td"));
		td3.setTextContent(" ");
		
		return tr;
	}
	
	private boolean isSpecialType(JavaType javaType) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		//we are only interested if the type is part of our application and if no editor exists for it already
		if (metadataService.get(physicalTypeIdentifier) != null) {
		  return true;
		}		
		return false;
	}
}