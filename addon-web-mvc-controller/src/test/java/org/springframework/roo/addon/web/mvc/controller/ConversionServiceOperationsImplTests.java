package org.springframework.roo.addon.web.mvc.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

public class ConversionServiceOperationsImplTests {

	private static final String WEB_MVC_CONFIG = "/org/springframework/roo/addon/web/mvc/controller/webmvc-config.xml";

	@Mock private FileManager fileManager;
	@Mock private PathResolver pathResolver;
	@Mock private ClasspathOperations classpathOperations;
	
	@Before
	public void setUp() {
		initMocks(this);
	}
	
	@Test
	public void testManageWebMvcConfig() throws Exception {
		String webMvcConfig = WEB_MVC_CONFIG;
		when(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml")).thenReturn(webMvcConfig);
		StubMutableFile webMvcConfigFile = new StubMutableFile(new File(getClass().getResource(webMvcConfig).getFile()));
		when(fileManager.exists(webMvcConfig)).thenReturn(true);
		when(fileManager.updateFile(webMvcConfig)).thenReturn(webMvcConfigFile);
		
		ConversionServiceOperationsImpl operations = new ConversionServiceOperationsImpl(fileManager, pathResolver, classpathOperations);
		operations.manageWebMvcConfig(new JavaPackage(getClass().getPackage().getName()));
		String output = webMvcConfigFile.getOutputAsString();

		assertThat(output, containsString("<mvc:annotation-driven conversion-service=\"applicationConversionService\"/>"));
		assertThat(output, containsString("\t<!--Installs application converters and formatters-->\n\t"));
		assertThat(output, containsString("\t<bean class=\"org.springframework.roo.addon.web.mvc.controller.ApplicationConversionServiceFactoryBean\" id=\"applicationConversionService\"/>\n"));
	}
	
	@Test
	public void testManageWebMvcConfigSecondTime() throws Exception {
		String webMvcConfig = WEB_MVC_CONFIG;
		String webMvcConfig2 = "/org/springframework/roo/addon/web/mvc/controller/webmvc-config-with-conversionService.xml";
		when(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml")).thenReturn(webMvcConfig);
		StubMutableFile webMvcConfigFile2 = new StubMutableFile(new File(getClass().getResource(webMvcConfig2).getFile()));
		when(fileManager.exists(webMvcConfig)).thenReturn(true);
		when(fileManager.updateFile(webMvcConfig)).thenReturn(webMvcConfigFile2);
		
		ConversionServiceOperationsImpl operations = new ConversionServiceOperationsImpl(fileManager, pathResolver, classpathOperations);
		operations.manageWebMvcConfig(new JavaPackage(getClass().getPackage().getName()));
		
		assertEquals(0, webMvcConfigFile2.getOutputAsString().length());
	}
	
	@Test
	public void testInstallJavaClass() throws Exception {
		JavaType javaType = new JavaType(getClass().getPackage().getName() + ".ApplicationConversionServiceFactoryBean");
		String targetPath = "doesntMatter";
		when(classpathOperations.getPhysicalLocationCanonicalPath(javaType , Path.SRC_MAIN_JAVA)).thenReturn(targetPath);
		when(pathResolver.getPath(targetPath)).thenReturn(new Path(targetPath));
		StubMutableFile file = new StubMutableFile();
		when(fileManager.createFile(targetPath)).thenReturn(file);
		
		ConversionServiceOperationsImpl operations = new ConversionServiceOperationsImpl(fileManager, pathResolver, classpathOperations);
		operations.installJavaClass(javaType.getPackage());
		
		assertThat(file.getOutputAsString(), containsString("package org.springframework.roo.addon.web.mvc.controller;"));
		assertThat(file.getOutputAsString(), containsString("package org.springframework.roo.addon.web.mvc.controller;"));
		
	}

	@Test
	public void testInstallConversionServiceFactoryBeanTwice() throws Exception {

	}
}
